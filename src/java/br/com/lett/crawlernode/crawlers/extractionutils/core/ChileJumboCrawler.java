package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.RatingsReviews;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

public class ChileJumboCrawler extends VTEXNewScraper {

   public final String CODE_LOCATE = getCodeLocate();

   private static final String MAIN_SELLER_NAME_LOWER = "jumbo chile";
   public static final String HOME_PAGE = "https://www.jumbo.cl/";
   public static final String HOST = "www.jumbo.cl";


   public ChileJumboCrawler(Session session) {
      super(session);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Logging.printLogDebug(logger, session, "Adding cookie...");

      BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=" + CODE_LOCATE);
      cookie.setDomain("." + ChileJumboCrawler.HOST);
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   protected String getCodeLocate() {
      return CODE_LOCATE;
   }

   private String getUrl() {
      String[] url = session.getOriginalURL().split("cl/");
      return url[url.length - 1].split("\\?")[0];
   }

   @Override
   protected String getHomePage() {
      return homePage;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList(MAIN_SELLER_NAME_LOWER);
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         JSONObject productJson = crawlProductApi();

         CategoryCollection categories = scrapCategories(productJson);
         String description = scrapDescription(doc, productJson);

         if (productJson != null) {
            JSONArray items = productJson.has("items") && !productJson.isNull("items") ? productJson.getJSONArray("items") : new JSONArray();
            String internalPid = productJson.has("productReference") ? productJson.get("productReference").toString() : null;
            for (int i = 0; i < items.length(); i++) {
               JSONObject jsonSku = items.getJSONObject(i);

               Product product = extractProduct(doc, internalPid, categories, description, jsonSku, productJson);
               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   protected JSONObject crawlProductApi() {
      String product = getUrl();
      Map<String, String> headers = new HashMap<>();
      headers.put("x-api-key", "IuimuMneIKJd3tapno2Ag1c1WcAES97j");
      String API = "https://apijumboweb.smdigital.cl/catalog/api/v1/catalog_system/pub/products/search/" + product + "?sc=" + CODE_LOCATE;

      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setHeaders(headers)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();

      JSONArray json = CrawlerUtils.stringToJsonArray(content);
      return json.optJSONObject(0);
   }

   @Override
   protected String scrapName(Document doc, JSONObject productJson, JSONObject jsonSku) {
      String name = super.scrapName(doc, productJson, jsonSku);
      String brand = productJson.has("brand") ? productJson.get("brand").toString() : null;
      String nameWithBrand = null;
      if (name != null && brand != null) {
         nameWithBrand = name + " - " + brand;
      }
      return nameWithBrand;
   }

}
