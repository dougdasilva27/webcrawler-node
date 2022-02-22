package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;

import java.util.*;

import models.Offer;
import models.Offers;
import models.pricing.BankSlip;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilCaroneCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Carone";
   private static final String HOME_PAGE = "https://www.carone.com.br/";
   private static final String API_LINK = "https://www.carone.com.br/carone/index/ajaxCheckPostcode/";
   private final String cep = getCep();

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(),
      Card.DINERS.toString());

   public BrasilCaroneCrawler(Session session) {
      super(session);
   }

   private String getCep() {
      return session.getOptions().optString("cep");
   }

   @Override
   public void handleCookiesBeforeFetch() {

      //If the market is Carone (id 1214): We don't need to set any location.
      if (cep != null && !cep.equals("")) {
         Request request = Request.RequestBuilder.create()
            .setUrl(HOME_PAGE)
            .build();
         Response response = dataFetcher.get(session, request);
         Document document = Jsoup.parse(response.getBody());
         String key = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "input[name=form_key]", "value");

         String payload = "form_key=" + key + "&postcode=" + cep;

         Map<String, String> headers = new HashMap<>();
         headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");

         Request requestApi = Request.RequestBuilder.create()
            .setUrl(API_LINK)
            .setPayload(payload)
            .setHeaders(headers)
            .build();

         Response responseApi = dataFetcher.post(session, requestApi);
         this.cookies.addAll(responseApi.getCookies());
      }
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject data = getJson(doc);

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-essential [name='product'][value]", "value");
         String internalPid = data != null ? data.optString("sku") : null;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".i-breadcrumb li:not(.home):not(.product)");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#image", Arrays.asList("src"), "https:", "www.carone.com.br/");
         List<String> secondaryImages =  CrawlerUtils.scrapSecondaryImages(doc,".cloud-zoom-gallery img", Arrays.asList("src"),"", "", primaryImage);
         boolean available = !doc.select(".add-to-box .add-to-cart").isEmpty();
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-essential") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }


   private JSONObject getJson(Document doc) {
      JSONObject product = new JSONObject();
      Elements scripts = doc.select("script[type]");
      if (!scripts.isEmpty()) {
         for (Element e : scripts) {
            String element = e.html().replace(" ", "");
            if (element.contains("vardataLayer=dataLayer") && element.contains("dataLayer.push(")) {
               String[] vetor = element.split("dataLayer.push\\(");
               if (vetor != null && vetor.length > 0) {
                  String fistIndex = vetor[1];
                  if (fistIndex.contains(");")) {
                     String jsonString = fistIndex.split("\\);")[0];
                     JSONObject json = CrawlerUtils.stringToJson(jsonString);
                     JSONArray products = json.optJSONArray("products");
                     product = products != null && !products.isEmpty() ? products.optJSONObject(0) : new JSONObject();
                  }
               }
            }
         }
      }
      return product;
   }

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".price-box .off .price");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".special-price .price", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "p.old-price", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
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

}
