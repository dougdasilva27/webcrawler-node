package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FavoCrawler extends Crawler {
   public FavoCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   private static final String SELLER_FULL_NAME = "Favo Market Deelsa";
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
      headers.put("referer", "https://" + STORE + ".mercadofavo.com/");
      headers.put("origin", "https://" + STORE + ".mercadofavo.com/");
      headers.put("accept", "application/json, text/plain, */*");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
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
      } else {
         productSlug = CommonMethods.getLast(session.getOriginalURL().split("/"));
      }

      productSlug = URLEncoder.encode(productSlug, StandardCharsets.UTF_8);

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
         int stock = productJson.optInt("stock", 0);
         boolean available = stock > 0;
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
            .setStock(0)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String scrapImage(JSONObject productJson) {
      String image = null;
      JSONObject images = productJson.optJSONObject("imagen");

      if (images != null) {
         image = images.optString("size-480");
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

      if (spotlightPrice.equals(priceFrom) || priceFrom == 0d) {
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
