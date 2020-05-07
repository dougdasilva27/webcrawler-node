
package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
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
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public abstract class ComperCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.comperdelivery.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "sdb comercio de alimentos ltda.";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   protected final String storeId = getStoreId();
   protected final String multiStoreId = getStoreId();

   protected abstract String getStoreId();

   protected abstract String getMultiStoreId();

   public ComperCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   private String userAgent;
   private LettProxy proxyUsed;

   @Override
   public void handleCookiesBeforeFetch() {
      this.userAgent = FetchUtilities.randUserAgent();

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.USER_AGENT, this.userAgent);

      Request request = RequestBuilder.create().setUrl(HOME_PAGE).setCookies(cookies).setHeaders(headers)
            .setProxyservice(Arrays.asList(ProxyCollection.BUY, ProxyCollection.BONANZA, ProxyCollection.NO_PROXY)).build();
      Response response = this.dataFetcher.get(session, request);

      this.proxyUsed = response.getProxyUsed();

      for (Cookie cookieResponse : response.getCookies()) {
         BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
         cookie.setDomain("www.comperdelivery.com.br");
         cookie.setPath("/");
         this.cookies.add(cookie);
      }

      Request request2 = RequestBuilder.create()
            .setUrl("https://www.comperdelivery.com.br/store/SetStore?storeId=" + storeId)
            .setProxy(proxyUsed)
            .setCookies(cookies)
            .setHeaders(headers)
            .setFollowRedirects(false)
            .build();
      this.dataFetcher.get(session, request2);

      BasicClientCookie cookieM = new BasicClientCookie("MultiStoreId", multiStoreId);
      cookieM.setDomain("www.comperdelivery.com.br");
      cookieM.setPath("/");
      this.cookies.add(cookieM);
   }

   @Override
   protected Object fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.USER_AGENT, this.userAgent);

      Request request = RequestBuilder.create().setUrl(session.getOriginalURL()).setCookies(cookies).setHeaders(headers).setProxy(proxyUsed).build();
      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);

         JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

         String internalPid = vtexUtil.crawlInternalPid(skuJson);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li:not(:first-child) > a");
         String description = crawlDescription(internalPid, vtexUtil);

         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

         JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = vtexUtil.crawlInternalId(jsonSku);
            JSONObject apiJSON = vtexUtil.crawlApi(internalId);
            String name = vtexUtil.crawlName(jsonSku, skuJson);
            Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
            Marketplace marketplace =
                  CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER), session);
            boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
            String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
            String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
            Prices prices = getPrices(marketplaceMap);
            Float price = vtexUtil.crawlMainPagePrice(prices);
            Integer stock = vtexUtil.crawlStock(apiJSON);

            String ean = i < eanArray.length() ? eanArray.getString(i) : null;

            List<String> eans = new ArrayList<>();
            eans.add(ean);

            // Creating the product
            Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
                  .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
                  .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Prices getPrices(Map<String, Prices> marketplaceMap) {
      Prices prices = new Prices();

      if (marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER)) {
         prices = marketplaceMap.get(MAIN_SELLER_NAME_LOWER);
      }

      return prices;
   }

   private String crawlDescription(String internalPid, VTEXCrawlersUtils vtexUtil) {
      StringBuilder description = new StringBuilder();

      JSONObject descriptionApi = vtexUtil.crawlDescriptionAPI(internalPid, "productId");
      if (descriptionApi.has("description")) {
         description.append(descriptionApi.get("description").toString());
      }

      List<String> specs = new ArrayList<>();

      if (descriptionApi.has("allSpecifications")) {
         JSONArray keys = descriptionApi.getJSONArray("allSpecifications");
         for (Object o : keys) {
            if (!o.toString().equalsIgnoreCase("Informações para Instalação") && !o.toString().equalsIgnoreCase("Portfólio")) {
               specs.add(o.toString());
            }
         }
      }

      for (String spec : specs) {

         description.append("<div>");
         description.append("<h4>").append(spec).append("</h4>");
         description.append(VTEXCrawlersUtils.sanitizeDescription(descriptionApi.get(spec)));
         description.append("</div>");
      }

      return description.toString();
   }

   private boolean isProductPage(Document doc) {
      return doc.select("#product-info").first() != null;
   }

}
