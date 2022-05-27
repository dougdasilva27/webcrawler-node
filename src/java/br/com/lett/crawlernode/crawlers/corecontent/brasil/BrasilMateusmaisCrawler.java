package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedUrlException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
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
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilMateusmaisCrawler extends Crawler {

   private static final String SELLER_NAME_LOWER = "mateusmais";
   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.AMEX.toString(), Card.ELO.toString());

   public BrasilMateusmaisCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   String marketCode = session.getOptions().optString("marketId");

   @Override
   protected Response fetchResponse() {
      if (!session.getOriginalURL().contains(marketCode)) {
         throw new MalformedUrlException("URL não corresponde a localidade do market");
      }

      Request request = Request.RequestBuilder.create()
         .setUrl("https://app.mateusmais.com.br/market/" + marketCode + "/product/" + CommonMethods.getLast(session.getOriginalURL().split("/")))
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
         String name = productJson.optString("name") + " " + productJson.optString("measure") + productJson.optString("measure_type");
         ;
         String description = productJson.optString("description");
         String primaryImage = productJson.optString("image");
         List<String> eans = Collections.singletonList(productJson.optString("barcode"));
         CategoryCollection categories = getCategory(productJson);
         String brand = productJson.optString("brand");
         boolean available = productJson.optBoolean("available");
         Offers offers = available ? scrapOffers(productJson) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(brand + " " + name)
            .setPrimaryImage(primaryImage)
            .setCategories(categories)
            .setEans(eans)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
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
