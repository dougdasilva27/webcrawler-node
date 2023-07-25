package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
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
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class BrasilAtacadaomarketplaceCrawler extends Crawler {

   private String cityId = session.getOptions().optJSONObject("cookies").optString("cb_user_city_id");

   private String userType = session.getOptions().optJSONObject("cookies").optString("cb_user_type");
   private final String SELLER_NAME = session.getOptions().optString("store");

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   public BrasilAtacadaomarketplaceCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
      super.config.setParser(Parser.HTML);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      JSONObject cookiesObject = session.getOptions() != null ? session.getOptions().optJSONObject("cookies") : null;
      if (cookiesObject != null) {
         Map<String, Object> cookiesMap = cookiesObject.toMap();
         for (Map.Entry<String, Object> entry : cookiesMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            cookies.add(new BasicClientCookie(key, value.toString()));
         }
      }
   }

   @Override
   protected Response fetchResponse() {
      try {
         HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(session.getOriginalURL()))
            .header("Accept", "*/*")
            .header("Cookie", CommonMethods.cookiesToString(cookies))
            .build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         return new Response.ResponseBuilder()
            .setBody(response.body())
            .setLastStatusCode(response.statusCode())
            .build();
      } catch (Exception e) {
         throw new RuntimeException("Failed in load document: " + session.getOriginalURL(), e);
      }
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".container > h1.h1", false);
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".card div[data-pk]", "data-pk");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".card > div > div.row:not(:first-child)"));
         List<String> categories = CrawlerUtils.crawlCategories(doc, ".js-sku-category");
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".card .product-image-box > img[src]", "src");
         boolean available = doc.select(".js-btn-add-product") != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setCategories(categories)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(Document document) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(document);
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));
      String mainSeller = isMainSeller(document);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(mainSeller)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(mainSeller.equals(SELLER_NAME))
         .setPricing(pricing)
         .setSales(sales)
         .build());


      return offers;
   }

   private String isMainSeller(Document doc) {
      String isMarketPlace = CrawlerUtils.scrapStringSimpleInfo(doc, ".js-product-box__supplier", false);

      if (isMarketPlace != null && !isMarketPlace.isEmpty() && !isMarketPlace.equalsIgnoreCase(SELLER_NAME)) {
         return isMarketPlace;
      }

      return SELLER_NAME;
   }

   private Pricing scrapPricing(Document document) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(document, ".product-box__price--number", null, true, ',', session);
      Double priceFrom = null;

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
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

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".box-product-information") != null;
   }

}
