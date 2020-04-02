package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class BrasilCatdogshopCrawler extends Crawler {
   
   private static final String MAIN_SELLER_NAME = "Cat Dog Shop";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), 
         Card.HIPERCARD.toString(), Card.AMEX.toString(), Card.DINERS.toString(), Card.ELO.toString(), Card.HIPER.toString());

   public BrasilCatdogshopCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONArray jsonArr = extractPageJSON(doc);

         for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject skuJson = jsonArr.getJSONObject(i);

            String internalId = skuJson.has("product_id") && !skuJson.isNull("product_id") ? skuJson.get("product_id").toString() : null;
            String internalPid = skuJson.has("sku") && !skuJson.isNull("sku") ? skuJson.get("sku").toString() : null;
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-form-container .product-name", true);
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs > a.breadcrumb-crumb", true);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc,
                  "#product-slider-container .product-slide > a", Arrays.asList("href"), "https:", "d26lpennugtm8s.cloudfront.net");
            String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc,
                  "#product-slider > .product-slide > a", Arrays.asList("href"), "https:", "d26lpennugtm8s.cloudfront.net", primaryImage);
            String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".description", false);
            Integer stock = skuJson.has("stock") && skuJson.get("stock") instanceof Integer ? skuJson.getInt("stock") : null;
            Offers offers = scrapOffers(doc, skuJson);
            
            // Creating the product
            Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
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
   
   private Offers scrapOffers(Document doc, JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc, json);

      if(pricing != null) {
        offers.add(OfferBuilder.create()
              .setUseSlugNameAsInternalSellerId(true)
              .setSellerFullName(MAIN_SELLER_NAME)
              .setSellersPagePosition(1)
              .setIsBuybox(false)
              .setIsMainRetailer(true)
              .setPricing(pricing)
              .build());
      }
      
      return offers;
   }
   
   private Pricing scrapPricing(Document doc, JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(json, "price_number", true);

      if(spotlightPrice != null) {
        Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#compare_price_display", null, false, ',', session);
        CreditCards creditCards = scrapCreditCards(doc, json, spotlightPrice);

        return PricingBuilder.create()
              .setSpotlightPrice(spotlightPrice)
              .setPriceFrom(priceFrom)
              .setCreditCards(creditCards)
              .setBankSlip(BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
              .build();
      }

      return null;
   }
   
   private CreditCards scrapCreditCards(Document doc, JSONObject skuJson, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (skuJson.has("installments_data") && !skuJson.isNull("installments_data") && skuJson.get("installments_data") instanceof String) {
         String installmentsJsonString = skuJson.getString("installments_data");
         JSONObject installmentsJson = JSONUtils.stringToJson(installmentsJsonString);

         installmentsJson = JSONUtils.getJSONValue(installmentsJson, "Wirecard");

         for (String key : installmentsJson.keySet()) {
            Integer first = MathUtils.parseInt(key);
            JSONObject installmentJson = installmentsJson.get(key) instanceof JSONObject ? installmentsJson.getJSONObject(key) : new JSONObject();
            Double second = JSONUtils.getDoubleValueFromJSON(installmentJson, "installment_value", true);

            if (first != null && second != null) {
               installments.add(InstallmentBuilder.create()
                     .setInstallmentNumber(first)
                     .setInstallmentPrice(second)
                     .build());
            }
         }
      }
      
      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(brand)
               .setIsShopCard(false)
               .setInstallments(installments)
               .build());
      }
      
      return creditCards;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".template-product") != null;
   }

   private JSONArray extractPageJSON(Document doc) {
      String arrString = "[]";
      Element jsonElement = doc.selectFirst(".js-product-container");

      if (jsonElement != null && jsonElement.hasAttr("data-variants")) {
         arrString = jsonElement.attr("data-variants");
      }

      return CrawlerUtils.stringToJsonArray(arrString);
   }
}
