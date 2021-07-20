package br.com.lett.crawlernode.crawlers.corecontent.costarica;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CostaricaMaspormenosCrawler extends VTEXNewScraper {


   public CostaricaMaspormenosCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected List<String> getMainSellersNames() {
      return session.getOptions().optJSONArray("sellers").toList().stream().map(Object::toString).collect(Collectors.toList());

   }


   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      String internalPid = scrapInternalpid(doc);
      String parameters = CommonMethods.getLast(session.getOriginalURL().split("cr/"));

      if (internalPid != null && isProductPage(doc)) {
         JSONObject productJson = crawlProductApi(internalPid, parameters);

         CategoryCollection categories = scrapCategories(productJson);
         String description = scrapDescription(doc, productJson);
         processBeforeScrapVariations(doc, productJson, internalPid);
         if (productJson != null) {
            JSONArray items = JSONUtils.getJSONArrayValue(productJson, "items");

            for (int i = 0; i < items.length(); i++) {
               JSONObject jsonSku = items.optJSONObject(i);
               if (jsonSku == null) {
                  jsonSku = new JSONObject();
               }
               Product product = extractProduct(doc, internalPid, categories, description, jsonSku, productJson);
               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      JSONObject productApi = new JSONObject();
      String cookies = session.getOptions().optString("cookie");

      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", cookies);

      String url = homePage + "_v/segment/routing/vtex.store@2.x/product/" + internalPid + "/" + (parameters == null ? "" : parameters) + "?__pickRuntime=queryData";
      ;

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build();
      String content = this.dataFetcher.get(session, request).getBody();
      JSONObject jsonObject = CrawlerUtils.stringToJson(content);

      productApi = scrapProductJson(jsonObject);

      return productApi;
   }

   private JSONObject scrapProductJson(JSONObject stateJson) {
      JSONObject product = new JSONObject();
      Object queryData = JSONUtils.getValue(stateJson, "queryData");
      JSONObject queryDataJson = new JSONObject();

      if (queryData instanceof JSONObject) {
         queryDataJson = (JSONObject) queryData;
      } else if (queryData instanceof JSONArray) {
         JSONArray queryDataArray = (JSONArray) queryData;
         if (queryDataArray.length() > 0 && queryDataArray.get(0) instanceof JSONObject) {
            queryDataJson = queryDataArray.getJSONObject(0);
         }
      }

      if (queryDataJson.has("data") && queryDataJson.get("data") instanceof JSONObject) {
         product = queryDataJson.getJSONObject("data");
      } else if (queryDataJson.has("data") && queryDataJson.get("data") instanceof String) {
         product = CrawlerUtils.stringToJson(queryDataJson.getString("data"));
      }
      return JSONUtils.getJSONValue(product, "product");
   }
}
