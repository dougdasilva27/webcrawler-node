package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;

/**
 * Date: 09/07/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilVilanovamondelezCrawler extends Crawler {

   public static final String HOME_PAGE = "https://www.vilanova.com.br/";
   private static final String IMAGES_HOST = "i2-vilanova.a8e.net.br";

   private static final String LOGIN_URL = "https://www.vilanova.com.br/Cliente/Logar";
   private static final String CNPJ = "33033028004090";
   private static final String PASSWORD = "681543";

   public BrasilVilanovamondelezCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   private String cookiePHPSESSID = null;

   @Override
   public void handleCookiesBeforeFetch() {
      StringBuilder payload = new StringBuilder();
      payload.append("usuario_cnpj=" + CNPJ);
      payload.append("&usuario_senha=" + PASSWORD);

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("sec-fetch-mode", "cors");
      headers.put("origin", "https://www.vilanova.com.br");
      headers.put("sec-fetch-site", "same-origin");
      headers.put("x-requested-with", "XMLHttpRequest");


      Request request = RequestBuilder.create().setUrl(LOGIN_URL).setPayload(payload.toString()).setHeaders(headers).build();
      Response response = this.dataFetcher.post(session, request);

      List<Cookie> cookiesResponse = response.getCookies();

      for (Cookie cookieResponse : cookiesResponse) {
         if (cookieResponse.getName().equalsIgnoreCase("PHPSESSID")) {
            cookiePHPSESSID = cookieResponse.getValue();
         }
      }
   }

   @Override
   protected Object fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "PHPSESSID=" + cookiePHPSESSID);

      Request request = RequestBuilder.create()
            .setUrl(session.getOriginalURL())
            .setHeaders(headers)
            .build();

      return Jsoup.parse(new JavanetDataFetcher().get(session, request).getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONArray productJsonArray = CrawlerUtils.selectJsonArrayFromHtml(doc, "script", "var dataLayer = ", ";", false, true);
         JSONObject productJson = extractProductData(productJsonArray);

         String internalPid = crawlInternalPid(productJson);
         List<String> eans = Arrays.asList(CrawlerUtils.scrapStringSimpleInfo(doc, ".product-ean .value", true));
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#Breadcrumbs li a", true);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("#info-abas-mobile"));
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#imagem-produto #elevateImg", Arrays.asList("data-zoom-image", "href", "src"),
               "https", IMAGES_HOST);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#imagem-produto #elevateImg", Arrays.asList("data-zoom-image", "href",
               "src"),
               "https", IMAGES_HOST, primaryImage);

         JSONArray productsArray = getSkusList(productJson);

         for (Object obj : productsArray) {
            JSONObject skuJson = (JSONObject) obj;

            String internalId = crawlInternalId(skuJson);
            String name = crawlName(skuJson);
            Float price = JSONUtils.getFloatValueFromJSON(skuJson, "price", true);
            Prices prices = scrapPrices(price);

            Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPrice(price)
                  .setPrices(prices)
                  .setAvailable(price != null && price > 0)
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
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

   private JSONObject extractProductData(JSONArray productJsonArray) {
      JSONObject firstObjectFromArray = productJsonArray.length() > 0 ? productJsonArray.getJSONObject(0) : new JSONObject();
      return firstObjectFromArray.has("productData") ? firstObjectFromArray.getJSONObject("productData") : firstObjectFromArray;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".container #detalhes-container").isEmpty();
   }

   private String crawlInternalId(JSONObject skuJson) {
      String internalId = null;

      if (skuJson.has("sku") && !skuJson.isNull("sku")) {
         internalId = skuJson.get("sku").toString();
      }

      return internalId;
   }

   private String crawlInternalPid(JSONObject productJson) {
      String internalPid = null;

      if (productJson.has("productID") && !productJson.isNull("productID")) {
         internalPid = productJson.get("productID").toString();
      }

      return internalPid;
   }

   private String crawlName(JSONObject skuJson) {
      String name = null;

      if (skuJson.has("name") && skuJson.get("name") instanceof String) {
         name = skuJson.getString("name");
      }

      return name;
   }

   private JSONArray getSkusList(JSONObject productJson) {
      JSONArray skus = new JSONArray();

      if (productJson.has("productSKUList") && productJson.get("productSKUList") instanceof JSONArray) {
         skus = productJson.getJSONArray("productSKUList");
      } else if (productJson.has("productSKUList") && productJson.get("productSKUList") instanceof JSONObject) {
         JSONObject skusJSON = productJson.getJSONObject("productSKUList");

         for (String key : skusJSON.keySet()) {
            skus.put(skusJSON.get(key));
         }
      }

      return skus;
   }

   private Prices scrapPrices(Float price) {
      Prices prices = new Prices();

      if (price != null && price > 0) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);

         prices.setBankTicketPrice(price);
         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      }

      return prices;
   }
}
