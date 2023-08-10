package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedUrlException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.BankSlip;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.apache.commons.lang.WordUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilMateusmaisCrawler extends Crawler {

   private String marketCode = session.getOptions().optString("marketId");
   private static final String SELLER_NAME_LOWER = "mateusmais";
   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.AMEX.toString(), Card.ELO.toString());

   public BrasilMateusmaisCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   @Override
   protected Response fetchResponse() {
      if (!session.getOriginalURL().contains(marketCode)) {
         throw new MalformedUrlException("URL não corresponde a localidade do market");
      }

      Request request = Request.RequestBuilder.create()
         .setUrl("https://app.mateusmais.com.br/api/products/product-detail/" + CommonMethods.getLast(session.getOriginalURL().split("/")) + "/")
         .build();

      return this.dataFetcher.get(session, request);

   }

   @Override
   public List<Product> extractInformation(JSONObject productJson) throws Exception {
      super.extractInformation(productJson);
      List<Product> products = new ArrayList<>();

      if (productJson != null && !productJson.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = productJson.optString("sku");
         String name = crawlName(productJson);
         String description = productJson.optString("description");
         List<String> secondaryImages = scraplImages(productJson);
         String primaryImage = productJson.optString("image");
         List<String> eans = Collections.singletonList(productJson.optString("barcode"));
         CategoryCollection categories = getCategory(productJson);
         Integer stock = productJson.optInt("amount_in_stock");
         Offers offers = stock > 0 ? scrapOffers(productJson) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categories)
            .setEans(eans)
            .setStock(stock)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> scraplImages(JSONObject productJson) {
      List<String> images = new ArrayList<>();
      JSONArray galeryImages = productJson.optJSONArray("gallery");
      if (galeryImages != null) {
         for (int i = 0; i < galeryImages.length(); i++) {
            String image = JSONUtils.getValueRecursive(galeryImages, i + ".url", String.class);
            images.add(image);
         }
      }
      return images;
   }

   private String crawlName(JSONObject productJson) {
      StringBuilder productName = new StringBuilder();
      String measure;
      String name = productJson.optString("name");
      Integer measureInt = productJson.optInt("measure", 0);
      if (measureInt != 0) {
         measure = measureInt.toString();
      } else {
         Double measureDouble = productJson.optDouble("measure", 0.0);
         measure = measureDouble.toString();
      }
      String measureType = productJson.optString("measure_type");
      String brand = productJson.optString("brand");

      if (brand != null && !brand.isEmpty()) {
         productName.append(brand).append(" ");
      }

      if (name != null && !name.isEmpty()) {
         productName.append(name).append(" ");
         if (!measure.equals("0") && !measure.equals("0.0")) {
            productName.append(measure);
            if (measureType != null && !measureType.isEmpty()) {
               productName.append(measureType);
            }
         }
      }

      return productName.toString();

   }

   private CategoryCollection getCategory(JSONObject productList) {
      CategoryCollection categories = new CategoryCollection();
      String objCategory = productList.optString("category");
      if (objCategory != null && !objCategory.isEmpty()) {
         String category = WordUtils.capitalize(objCategory);
         categories.add(category);
      }
      return categories;
   }

   private Offers scrapOffers(JSONObject productList) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productList);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME_LOWER)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject productList) throws MalformedPricingException {

      Double priceFrom = productList.optDouble("price");
      Double spotlightPrice = productList.optDouble("low_price");

      if (spotlightPrice.isNaN()) {
         spotlightPrice = priceFrom;
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

}
