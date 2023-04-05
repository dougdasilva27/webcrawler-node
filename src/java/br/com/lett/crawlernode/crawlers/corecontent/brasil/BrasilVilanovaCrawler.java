package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;

/**
 * Date: 09/07/2019
 *
 * @author Gabriel Dornelas
 */
public class BrasilVilanovaCrawler extends Crawler {

   public static final String HOME_PAGE = "https://www.vilanova.com.br/";
   private static final String IMAGES_HOST = "i2-vilanova.a8e.net.br";

   private static final String LOGIN_URL = "https://www.vilanova.com.br/Cliente/Logar";
   private static final String CNPJ = "33.033.028%2F0040-90";
   private static final String PASSWORD = "Mudar123";

   private LettProxy proxy = null;
   private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36";

   public BrasilVilanovaCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   public LettProxy getFixedIp() throws IOException {
      LettProxy lettProxy = new LettProxy();
      lettProxy.setSource("fixed_ip");
      lettProxy.setPort(3144);
      lettProxy.setAddress("haproxy.lett.global");
      lettProxy.setLocation("brazil");
      return lettProxy;
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Response loginResponse = new Response();
      try {
         Request request = RequestBuilder.create()
            .setUrl("https://www.vilanova.com.br/loginlett/access/account/token/60498706000157")
            .setProxy(
               getFixedIp()
            )
            .build();
         loginResponse = this.dataFetcher.post(session, request);
      } catch (IOException e) {
         e.printStackTrace();
      }

      List<Cookie> cookiesResponse = loginResponse.getCookies();

      for (Cookie cookieResponse : cookiesResponse) {
         BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
         cookie.setDomain("www.vilanova.com.br");
         cookie.setPath("/");
         this.cookies.add(cookie);
      }
   }

   @Override
   protected Response fetchResponse() {

      Response response = new Response();
      try {
         Request request = RequestBuilder.create()
            .setUrl(session.getOriginalURL())
            .setProxy(
               getFixedIp()
            )
            .setCookies(this.cookies)
            .build();
         response = this.dataFetcher.get(session, request);
      } catch (IOException e) {
         e.printStackTrace();
      }

      return response;
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

         JSONArray productsArray = productJson.optJSONArray("productSKUList");
         for (Object obj : productsArray) {
            JSONObject skuJson = (JSONObject) obj;

            String internalId = skuJson.optString("sku");
            String name = skuJson.optString("name");
            Float price = JSONUtils.getFloatValueFromJSON(skuJson, "price", true);

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrice(price)
               .setPrices(new Prices())
               .setAvailable(false)
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
      JSONObject productJson = new JSONObject();
      Object firstObjectFromArray = productJsonArray.length() > 0 ? productJsonArray.get(0) : null;

      if (firstObjectFromArray instanceof JSONObject) {
         productJson = (JSONObject) firstObjectFromArray;
      } else if (firstObjectFromArray instanceof JSONArray) {
         JSONArray prankArray = (JSONArray) firstObjectFromArray;
         productJson = prankArray.length() > 0 ? prankArray.getJSONObject(0) : new JSONObject();
      }

      return productJson.has("productData") ? productJson.getJSONObject("productData") : productJson;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".container #detalhes-container").isEmpty();
   }

   private String crawlInternalPid(JSONObject productJson) {
      String internalPid = null;

      if (productJson.has("productID") && !productJson.isNull("productID")) {
         internalPid = productJson.get("productID").toString();
      }

      return internalPid;
   }
}
