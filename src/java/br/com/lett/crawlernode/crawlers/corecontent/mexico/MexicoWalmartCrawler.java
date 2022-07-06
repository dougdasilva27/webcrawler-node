package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.RequestsStatistics;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;


public class MexicoWalmartCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.walmart.com.mx";
   private static final String SELLER = "walmart";

   public MexicoWalmartCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      super.extractInformation(document);
      List<Product> products = new ArrayList<>();
      String internalId = crawlInternalId(session.getOriginalURL());

      Map<String, String> headers = new HashMap<>();
      headers.put("accept-encoding", "");
      headers.put("accept-language", "");

      String url = "https://www.walmart.com.mx/api/rest/model/atg/commerce/catalog/ProductCatalogActor/getProduct?id=" + internalId;
      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setCookies(cookies)
         .mustSendContentEncoding(false)
         .setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .build();
      JSONObject apiJson = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (apiJson.has("product")) {

         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         JSONObject productJson = apiJson.getJSONObject("product");

         CategoryCollection categories = crawlCategories(productJson);

         JSONArray childSkus = productJson.getJSONArray("childSKUs");

         for (Object object : childSkus) {
            JSONObject sku = (JSONObject) object;

            String name = crawlName(sku);
            String primaryImage = CrawlerUtils.completeUrl(JSONUtils.getValueRecursive(sku, "images.large", String.class), "https", "res.cloudinary.com/walmart-labs/image/upload/w_960,dpr_auto,f_auto,q_auto:best/mg/");
            List<String> secondaryImages = crawlSecondaryImages(sku);
            String description = crawlDescription(sku);
            Integer stock = null;
            String ean = scrapEan(sku);
            List<String> eans = new ArrayList<>();
            eans.add(ean);

            Map<String, Prices> marketplaceMap = crawlMarketplace(sku);
            Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(SELLER), Arrays.asList(Card.AMEX), session);
            boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, Arrays.asList(SELLER));
            Prices prices = CrawlerUtils.getPrices(marketplaceMap, Arrays.asList(SELLER));
            Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.AMEX);

            // Creating the product
            Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
               .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
               .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

            products.add(product);
         }


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Map<String, Prices> crawlMarketplace(JSONObject sku) {

      Map<String, Prices> map = new HashMap<>();
      String name = null;
      Float price = null;

      if (sku.has("offerList")) {
         JSONArray offerList = sku.getJSONArray("offerList");

         for (Object object2 : offerList) {
            Prices prices = new Prices();
            JSONObject list = (JSONObject) object2;

            if (list.has("sellerName")) {
               name = list.getString("sellerName").toLowerCase().trim();

               if (list.has("priceInfo")) {
                  JSONObject priceInfo = list.getJSONObject("priceInfo");
                  price = priceInfo.getFloat("specialPrice");

                  if (price != null) {
                     Map<Integer, Float> installmentPriceMap = new TreeMap<>();
                     installmentPriceMap.put(1, price);
                     prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

                  }

                  if (priceInfo.has("originalPrice")) {
                     prices.setPriceFrom(priceInfo.getDouble("originalPrice"));
                  }

               }
            }
            map.put(name, prices);
         }
      }
      return map;
   }

   private String scrapEan(JSONObject sku) {
      String ean = null;

      if (sku.has("upc")) {
         ean = sku.getString("upc");
      }

      return ean;
   }

   private String crawlInternalId(String url) {
      String internalId = null;

      if (url.contains("_")) {
         if (url.contains("?")) {
            url = url.split("\\?")[0];
         }

         String[] tokens = url.split("_");
         internalId = tokens[tokens.length - 1].replaceAll("[^0-9]", "").trim();
      }

      return internalId;
   }

   private String crawlName(JSONObject sku) {
      String name = null;

      if (sku.has("displayName")) {
         name = sku.getString("displayName");
      }

      return name;
   }


   private  List<String> crawlSecondaryImages(JSONObject sku) {
      List<String> secondaryImages = new ArrayList<>();
      JSONArray secondaryImagesArray = sku.optJSONArray("secondaryImages");

      for (Object o : secondaryImagesArray) {
         if (o instanceof  JSONObject){
            JSONObject imageJson = (JSONObject) o;
            String img = imageJson.optString("large");
            if (img != null){
               secondaryImages.add(CrawlerUtils.completeUrl(imageJson.optString("large"), "https", "res.cloudinary.com/walmart-labs/image/upload/w_960,dpr_auto,f_auto,q_auto:best/mg/"));
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
