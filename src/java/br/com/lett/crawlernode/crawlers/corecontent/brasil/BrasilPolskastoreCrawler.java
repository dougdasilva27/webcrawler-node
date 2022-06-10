package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class BrasilPolskastoreCrawler extends Crawler {

   private static final String IMAGES_HOST = "dafitistatic-a.akamaihd.net";
   private static final String PRODUCT_API_URL = "https://www.tricae.com.br/catalog/detailJson?sku=";
   private static final List<String> cards = Arrays.asList(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.ELO.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   public BrasilPolskastoreCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=p]", "value");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".box-description", ".box-informations"));
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#gallery li.gallery-items > a", Arrays.asList("data-img-zoom", "href"),
            "https", IMAGES_HOST);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#gallery li.gallery-items > a", Arrays.asList("data-img-zoom", "href"),
            "https", IMAGES_HOST, primaryImage);
         String productMainName = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product-name", true);

         JSONObject productApi = crawlProductAPI(internalPid);
         Offers offers = new Offers();
         if (productApi != null) offers = scrapOffers(doc, productApi);

         JSONArray productsArray = productApi.has("sizes") && !productApi.isNull("sizes") ? productApi.getJSONArray("sizes") : new JSONArray();

         for (Object obj : productsArray) {
            JSONObject skuJson = (JSONObject) obj;

            String internalId = crawlInternalId(skuJson);
            String name = crawlName(skuJson, productMainName);
            Integer stock = CrawlerUtils.getIntegerValueFromJSON(skuJson, "stock", null);

            // Price on this market is the same for all variations
            // Avaiability is for variation
            boolean buyable = stock != null && stock > 0;
            Offers offersVarition = buyable ? offers : new Offers();

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setOffers(offersVarition)
               .setStock(stock)
               .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".product-page").isEmpty();
   }

   private String crawlInternalId(JSONObject skuJson) {
      String internalId = null;

      if (skuJson.has("sku") && !skuJson.isNull("sku")) {
         internalId = skuJson.get("sku").toString();
      }

      return internalId;
   }

   private JSONObject crawlProductAPI(String internalPid) {
      String url = PRODUCT_API_URL + internalPid;
      Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).build();

      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }

   private String crawlName(JSONObject skuJson, String productMainName) {
      String name = productMainName;

      if (skuJson.has("name") && !skuJson.isNull("name")) {
         name += " Tamanho " + skuJson.get("name").toString();
      }

      return name;
   }

   private Offers scrapOffers(Document doc, JSONObject productApi) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productApi);

      String sellerId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=seller]", "value");
      Element elementDoc = doc.selectFirst(".label-super-price");
      List<String> sales = new ArrayList<>();
      if (elementDoc != null) sales.add(elementDoc.text());
      String sellerName = scrapSellerName(doc);
      offers.add(new Offer.OfferBuilder()
         .setInternalSellerId(sellerId)
         .setSellerFullName(sellerName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(Pattern.matches("(?i)tricae\\s?", sellerName))
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   /**
    * Scrap seller name from this text: "Vendido e entregue por Kyly"
    * <p>
    * Where kyly is the seller name
    *
    * @param doc
    * @return
    */
   private String scrapSellerName(Document doc) {
      String sellerName = null;

      String sellerText = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-seller-name", false);
      if (sellerText != null && sellerText.contains("por")) {
         sellerName = CommonMethods.getLast(sellerText.split("por")).trim();
      }

      return sellerName;
   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {

      String priceString = productJson.optString("price");
      String specialPriceString = productJson.optString("specialPrice");

      Double spotlightPrice = MathUtils.parseDoubleWithComma(priceString);
      Double priceFrom = MathUtils.parseDoubleWithComma(specialPriceString);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(scrapCreditCards(productJson.optJSONObject("installments")))
         .setBankSlip(BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build())
         .build();
   }

   private CreditCards scrapCreditCards(JSONObject installmentsJson) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      String installmentNumber = installmentsJson.optString("count");
      String installmentValue = installmentsJson.optString("value");

      Integer count = MathUtils.parseInt(installmentNumber);
      Double value = MathUtils.parseDoubleWithComma(installmentValue);

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(count)
         .setInstallmentPrice(value)
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
