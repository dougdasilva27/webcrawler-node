package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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
import models.Marketplace;
import models.Offer;
import models.Offers;
import models.prices.Prices;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MexicoWalmartCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.walmart.com.mx";
   private static final String SELLER = "walmart";

   public MexicoWalmartCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   @Override
   protected Response fetchResponse() {
      String internalId = crawlInternalId(session.getOriginalURL());

      String url = "https://www.walmart.com.mx/api/rest/model/atg/commerce/catalog/ProductCatalogActor/getProduct?id=" + internalId;

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("authority", "www.walmart.com.mx");
      headers.put("referer", session.getOriginalURL());

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
         ))
         .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create()
            .setForbiddenCssSelector("#px-captcha")
            .mustUseMovingAverage(false)
            .mustRetrieveStatistics(true).build())
         .build();

      return CrawlerUtils.retryRequest(request, session, new FetcherDataFetcher(), true);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json.has("product")) {

         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         JSONObject productJson = json.getJSONObject("product");

         CategoryCollection categories = crawlCategories(productJson);

         JSONArray childSkus = productJson.getJSONArray("childSKUs");

         for (Object object : childSkus) {
            JSONObject sku = (JSONObject) object;

            String name = crawlName(sku);
            String primaryImage = CrawlerUtils.completeUrl(JSONUtils.getValueRecursive(sku, "images.large", String.class), "https", "res.cloudinary.com/walmart-labs/image/upload/w_960,dpr_auto,f_auto,q_auto:best/mg/");
            List<String> secondaryImages = crawlSecondaryImages(sku);
            String description = crawlDescription(sku);
            String ean = scrapEan(sku);
            Offers offers = scrapOffers(sku);
            List<String> eans = List.of(ean);

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(crawlInternalId(session.getOriginalURL()))
               .setName(name)
               .setCategories(categories)
               .setOffers(offers)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setEans(eans)
               .build();

            products.add(product);
         }


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Offers scrapOffers(JSONObject sku) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      String sellerFullName = JSONUtils.getValueRecursive(sku, "offerList.0.sellerName", ".", String.class, null);
      boolean isMainRetailer = SELLER.toLowerCase(Locale.ROOT).equalsIgnoreCase(sellerFullName);

      Pricing pricing = scrapPricing(sku);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerFullName)
         .setMainPagePosition(1)
         .setIsBuybox(true)
         .setIsMainRetailer(isMainRetailer)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject sku) throws MalformedPricingException {
      JSONObject priceInfo = JSONUtils.getValueRecursive(sku, "offerList.0.priceInfo", JSONObject.class, new JSONObject());

      Double spotlightPrice = priceInfo.optDouble("specialPrice");
      Double priceFrom = priceInfo.optDouble("originalPrice");

      if (priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .build();
   }

   private String scrapEan(JSONObject sku) {
      String ean = null;

      if (sku.has("upc")) {
         ean = sku.getString("upc");
      }

      return ean;
   }

   private String crawlInternalId(String url) {
      String regex = "ip\\/[a-z0-9-]+\\/[a-z0-9-]+\\/(\\d+)";

      final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(url);

      return matcher.find() ? matcher.group(1) : null;
   }

   private String crawlName(JSONObject sku) {
      String name = null;

      if (sku.has("displayName")) {
         name = sku.getString("displayName");
      }

      return name;
   }

   private List<String> crawlSecondaryImages(JSONObject sku) {
      List<String> secondaryImages = new ArrayList<>();
      JSONArray secondaryImagesArray = sku.optJSONArray("secondaryImages");
      if (secondaryImagesArray != null) {
         for (Object o : secondaryImagesArray) {
            if (o instanceof JSONObject) {
               JSONObject imageJson = (JSONObject) o;
               String img = imageJson.optString("large");
               if (img != null) {
                  secondaryImages.add(CrawlerUtils.completeUrl(imageJson.optString("large"), "https", "res.cloudinary.com/walmart-labs/image/upload/w_960,dpr_auto,f_auto,q_auto:best/mg/"));
               }

            }
         }
      }

      return secondaryImages;
   }

   private CategoryCollection crawlCategories(JSONObject sku) {
      CategoryCollection categories = new CategoryCollection();

      JSONObject breadcrumb = sku.optJSONObject("breadcrumb");

      if (breadcrumb != null) {

         if (breadcrumb.has("departmentName")) {
            categories.add(breadcrumb.get("departmentName").toString());
         }

         if (breadcrumb.has("familyName")) {
            categories.add(breadcrumb.get("familyName").toString());
         }

         if (breadcrumb.has("fineLineName")) {
            categories.add(breadcrumb.get("fineLineName").toString());
         }
      }


      return categories;
   }

   private String crawlDescription(JSONObject sku) {
      StringBuilder description = new StringBuilder();

      if (sku.has("longDescription")) {
         description.append("<div id=\"desc\"> <h3> Descripci�n </h3>");
         description.append(sku.get("longDescription") + "</div>");
      }

      return description.toString();
   }
}
