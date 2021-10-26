package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.prices.Prices;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilEvinoCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "evino";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());


   public BrasilEvinoCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject skuJson = getScriptJson(doc);
         JSONObject productBiggyJson = skuJson.has("productBiggyJson") ? new JSONObject(skuJson.get("productBiggyJson").toString()) : new JSONObject();
         JSONArray biggyJson = productBiggyJson.has("skus") ? productBiggyJson.getJSONArray("skus") : new JSONArray();

         String name = crawlName(productBiggyJson);
         String primaryImage = crawlPrimaryImage(productBiggyJson);
         CategoryCollection categories = crawlCategories(productBiggyJson);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".Product__details"));
         String internalPid = crawlInternalPid(productBiggyJson);

         for (Object object : biggyJson) {
            JSONObject variation = (JSONObject) object;

            String internalId = crawlInternalId(variation);
            Boolean available = crawlAvailability(variation);
            Integer stock = crawlStock(variation);
            Offers offers = available ? scrapOffers(productBiggyJson) : new Offers();

            // Creating the product
            Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
                 .setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setDescription(description).setOffers(offers)
                  .setStock(stock).setEans(null).build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private JSONObject getScriptJson(Document doc){
      JSONObject jsonObject = new JSONObject();
      Elements scripts = doc.select("script[type=\"text/javascript\"]");
      for (Element e : scripts){
         String script = e.html();

         if (script.contains("var TC = ")){
            String[] withoutToken = script.split("var TC = ");
            if (withoutToken.length > 0){
               jsonObject = CrawlerUtils.stringToJson(withoutToken[1].split("if")[0]);
            }
         }
      }

      return jsonObject;

   }

   private Integer crawlStock(JSONObject variation) {
      return variation.has("stockQuantity") ? variation.getInt("stockQuantity") : null;
   }


   private CategoryCollection crawlCategories(JSONObject productBiggyJson) {
      CategoryCollection categories = new CategoryCollection();
      JSONArray categoriesArray = productBiggyJson.has("categories") ? productBiggyJson.getJSONArray("categories") : new JSONArray();

      for (Object object : categoriesArray) {
         JSONObject categoriesObject = (JSONObject) object;
         String name = categoriesObject.has("name") ? categoriesObject.get("name").toString() : null;
         categories.add(name);
      }

      return categories;
   }

   private String crawlPrimaryImage(JSONObject productBiggyJson) {
      String primaryImage = null;
      JSONObject images = productBiggyJson.has("images") ? productBiggyJson.getJSONObject("images") : new JSONObject();

      if (images.has("extralarge")) {
         primaryImage = images.getString("extralarge");
      } else if (images.has("large")) {
         primaryImage = images.getString("large");
      } else if (images.has("medium")) {
         primaryImage = images.getString("medium");
      } else if (images.has("small")) {
         primaryImage = images.getString("small");
      }
      return primaryImage;
   }

   private Boolean crawlAvailability(JSONObject variation) {
      return variation.has("status") && variation.get("status").toString().equalsIgnoreCase("available");
   }

   private String crawlInternalId(JSONObject variation) {
      return variation.has("sku") ? variation.get("sku").toString() : null;
   }

   private String crawlName(JSONObject skuJson) {
      return skuJson.has("name") ? skuJson.get("name").toString() : null;
   }

   private String crawlInternalPid(JSONObject skuJson) {
      return skuJson.has("id") ? skuJson.get("id").toString() : null;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".Product") != null;
   }

   private Offers scrapOffers(JSONObject productBiggyJson) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productBiggyJson);
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

   private Pricing scrapPricing(JSONObject productBiggyJson) throws MalformedPricingException {
      Double priceFrom = productBiggyJson.has("oldPrice") ? MathUtils.parseDoubleWithDot(productBiggyJson.get("oldPrice").toString()) : null;
      Double spotlightPrice = productBiggyJson.has("price") ? MathUtils.parseDoubleWithDot(productBiggyJson.get("price").toString()) : null;
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
