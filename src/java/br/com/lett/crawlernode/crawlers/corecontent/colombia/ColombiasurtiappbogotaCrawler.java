package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import software.amazon.awssdk.http.Header;

import java.util.*;

public class ColombiasurtiappbogotaCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Surtiapp Bogota";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   public ColombiasurtiappbogotaCrawler(Session session) {
      super(session);

   }


   @Override
   public void handleCookiesBeforeFetch() {
      Request requestCookies = Request.RequestBuilder.create()
         .setUrl("https://tienda.surtiapp.com.co/Security/Login")
         .build();

      Response responseCookies = dataFetcher.get(session, requestCookies);

      this.cookies.addAll(responseCookies.getCookies());


      Document document = Jsoup.parse(responseCookies.getBody());

      String requestVerificationToken = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "input[name=\"__RequestVerificationToken\"]", "value");

      Map<String, String> headers = new HashMap<>();

      headers.put("RequestVerificationToken",requestVerificationToken);
      headers.put(Header.CONTENT_TYPE,"application/x-www-form-urlencoded");
      headers.put("Cookie", CommonMethods.cookiesToString(responseCookies.getCookies()));


      Request requestLogin = Request.RequestBuilder.create()
         .setUrl("https://tienda.surtiapp.com.co/Security/Login?handler=Authenticate")
         .setHeaders(headers)
         .setPayload("email=1130409480&password=Lett12345.")
         .build();

    new JsoupDataFetcher().post(session,requestLogin);




   }

   @Override
   protected Object fetch() {

      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", CommonMethods.cookiesToString(this.cookies));
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setHeaders(headers)
         .build();
      Response response = new JsoupDataFetcher().get(session, request);

      String html = response.getBody();

      return Jsoup.parse(html);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String dataJson = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-id-contaniner", "data-json");
         JSONObject data = dataJson != null && !dataJson.isEmpty() ? CrawlerUtils.stringToJson(dataJson) : null;

         if (data != null) {
            String internalId = data.optString("Id");
            String name = data.optString("Name");
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-detail__image img", Collections.singletonList("src"), "https", "devinmotionstorage.blob.core.windows.net");
            List<String> images = CrawlerUtils.scrapSecondaryImages(doc, ".product-detail__image img", Collections.singletonList("src"), "https", "devinmotionstorage.blob.core.windows.net", primaryImage);
            //Site hasn't categories
            String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".product-detail__content--full"));
            int stock = data.optInt("Stock");
            boolean available = stock > 0;
            Offers offers = available ? scrapOffers(doc) : new Offers();

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setStock(stock)
               .setOffers(offers)
               .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".container.product-detail__wrapper") != null;
   }


   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setSales(sales)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-detail__price--full", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-detail__price--old--full del", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());


      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String saleDiscount = CrawlerUtils.calculateSales(pricing);

      if (saleDiscount != null) {
         sales.add(saleDiscount);
      }

      return sales;
   }


}
