package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
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
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilMuniCrawler extends Crawler {
   private static final String SELLER_FULL_NAME = "muni-tienda";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilMuniCrawler(Session session) {
      super(session);

   }

   @Override
   public void handleCookiesBeforeFetch() {
      this.cookies = CrawlerUtils.fetchCookiesFromAPage("https://shop.munitienda.com.br/", new ArrayList<>(), ".munitienda.com.br", "/", cookies, session, null, dataFetcher);
   }

   private Map<String, String> getHeaders() {
      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "https://shop.munitienda.com.br");
      headers.put("open-stack-service-zone", "BR-SAO");
      headers.put("authority", "api.munitienda.com.br");
      headers.put("referer", "https://shop.munitienda.com.br/");
      headers.put("accept", "application/json, text/plain, /");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");

      return headers;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      String internalId = getId();
      JSONObject productData = getProductList(internalId);

      if (productData != null && !productData.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());
         String name = getName(productData);
         String primaryImage = getPrimaryImage(productData);
         String description = getDescription(productData);
         boolean available = productData.optBoolean("is_active");

         Offers offers = available ? getOffer(productData) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String getDescription(JSONObject productList) {
      String description = "";
      String objDescription = productList.optString("content_description");
      if (objDescription != null) {
         description = objDescription;
      }
      return description;
   }

   private String getPrimaryImage(JSONObject productList) {
      String primaryImg = "";
      String objImg = JSONUtils.getValueRecursive(productList, "images.0.main", String.class);
      if (objImg != null) {
         primaryImg = objImg;
      }
      return primaryImg;
   }

   private String getName(JSONObject productList) {
      String name = productList.optString("name");
      if (name != null) {
         String unit = productList.optString("contentDescription");
         if (unit != null) {
            name = name + " " + unit;
         }
         String brand = JSONUtils.getValueRecursive(productList, "metadata.product_brand", String.class);
         if (brand != null && !name.contains(brand)) {
            name = name + " " + brand;
         }

      }

      return name;
   }


   private String getId() {
      String id = null;
      String regex = "mp/(.*)/";
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(session.getOriginalURL());

      if (matcher.find()) {
         id = matcher.group(1);
      }
      return id;
   }


   private JSONObject getProductList(String internalId) {
      String url = "https://api.munitienda.com.br/cata/v1/client/product/" + internalId;
      Map<String, String> headers = getHeaders();

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES,
            ProxyCollection.NETNUT_RESIDENTIAL_MX
         ))
         .setHeaders(headers)
         .setSendUserAgent(false)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);

      return CrawlerUtils.stringToJson(response.getBody());
   }

   private Offers getOffer(JSONObject productList) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = getPrice(productList);
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

   private Pricing getPrice(JSONObject productList) throws MalformedPricingException {
      JSONObject priceJson = productList.optJSONObject("price");

      if (priceJson != null) {
         Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(priceJson, "value", false);
         Double priceFrom = JSONUtils.getDoubleValueFromJSON(priceJson, "market_price", false);

         if (spotlightPrice == null && priceFrom != null) {
            spotlightPrice = priceFrom;
         }

         if (Objects.equals(priceFrom, spotlightPrice)) {
            priceFrom = null;
         }

         CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

         return Pricing.PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .build();
      }

      throw new MalformedPricingException("No price found");
   }
}
