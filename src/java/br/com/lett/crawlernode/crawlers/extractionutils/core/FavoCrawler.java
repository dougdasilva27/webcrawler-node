package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FavoCrawler extends Crawler {
   public FavoCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   private static final String SELLER_FULL_NAME = "Favo Deelsa";
   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());
   private final String STORE = getStore();
   private final String ORIGIN_ID = getOriginId();

   private String getOriginId() {
      return session.getOptions().optString("origin_id");
   }

   private String getStore() {
      return session.getOptions().optString("tienda");
   }

   @Override
   protected Response fetchResponse() {
      String url = scrapProductUrl();

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("x-origin-id", this.ORIGIN_ID);

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      return this.dataFetcher.get(session, request);
   }

   private String scrapProductUrl() {
      String productSlug = null;
      String regex = "search=(.*)";
      Pattern pattern = Pattern.compile(regex);
      final Matcher matcher = pattern.matcher(session.getOriginalURL());

      if (matcher.find()) {
         productSlug = matcher.group(1);
      }

      return "https://customer-bff.favoapp.com.br/products/by_slug?product_slug=" + productSlug + "&store=" + STORE;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();
      Object productObj = json.optQuery("/data/product");

      if (productObj instanceof JSONObject) {
         JSONObject productJson = (JSONObject) productObj;

         String name = productJson.optString("descripcion");
         String internalId = productJson.optString("sku");
         String internalPid = productJson.optString("_id");
         String primaryImage = scrapImage(productJson);
         String description = productJson.optString("descripcion");
         CategoryCollection categories = scrapCategories(productJson);
         boolean available = productJson.optInt("stock", 0) > 0;
         Offers offers = available ? scrapOffers(productJson) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String scrapImage(JSONObject productJson) {
      String image = null;
      JSONObject images = productJson.optJSONObject("images");

      if (images != null) {
         if (images.has("size-1024")) {
            image = images.optString("size-1024");
         } else if (images.has("size-960")) {
            image = images.optString("size-960");
         } else if (images.has("size-480")) {
            image = images.optString("size-480");
         } else if (images.has("size-150")) {
            image = images.optString("size-150");
         }
      }

      return image;
   }

   private Offers scrapOffers(JSONObject product) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

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

   private Pricing scrapPricing(JSONObject product) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(product, "precio_aiyu", false);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(product, "precio_super", false);
      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

      if (spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private CategoryCollection scrapCategories(JSONObject product) {
      CategoryCollection categories = new CategoryCollection();
      List<String> keys = new ArrayList<String>(product.keySet());

      keys.stream().filter(key -> key.startsWith("categoria_")).forEach(key -> {
         categories.add(product.optString(key));
      });

      return categories;
   }

}
