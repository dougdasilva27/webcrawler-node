package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.*;

public class ChileLidersuperCrawler extends CrawlerRankingKeywords {

   public ChileLidersuperCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.lider.cl/supermercado/";

   private final List<String> proxies = Arrays.asList(
      ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY);

   @Override
   protected void processBeforeFetch() {
      Document doc = null;
      try {
         int attempts = 0;
         List<String> proxies = List.of(ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY);

         do {
            ChromeOptions chromeOptions = new ChromeOptions();
            chromeOptions.addArguments("--window-size=1920,1080");
            chromeOptions.addArguments("--headless");
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            webdriver = DynamicDataFetcher.fetchPageWebdriver(HOME_PAGE, proxies.get(attempts), session);
            if (webdriver != null) {
               webdriver.waitLoad(10000);
               doc = Jsoup.parse(webdriver.getCurrentPageSource());

               Set<Cookie> cookiesResponse = webdriver.driver.manage().getCookies();

               for (Cookie cookie : cookiesResponse) {
                  BasicClientCookie basicClientCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
                  basicClientCookie.setDomain(cookie.getDomain());
                  basicClientCookie.setPath(cookie.getPath());
                  basicClientCookie.setExpiryDate(cookie.getExpiry());
                  this.cookies.add(basicClientCookie);
               }

               webdriver.terminate();
            }
         } while (doc != null && doc.select("div #main-content").isEmpty() && attempts++ < 3);

      } catch (Exception e) {
         Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));
         if (webdriver != null) {
            webdriver.terminate();
         }
      }
   }

   @Override
   protected Document fetchDocument(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("authority", "www.lider.cl");
      headers.put("referer", url);
      headers.put("cookie", "cookieSearchTerms=" + keywordEncoded + ";" + CommonMethods.cookiesToString(cookies));
      headers.put("accept-language", "en-US,en;q=0.9,pt;q=0.8,pt-PT;q=0.7");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setSendUserAgent(false)
         .setProxyservice(proxies)
         .setHeaders(headers)
         .build();

      Response response = new JsoupDataFetcher().get(session, request);

      if (!response.isSuccess()) {
         response = retryRequest(request, List.of(new ApacheDataFetcher(), new FetcherDataFetcher(), this.dataFetcher));
      }

      return Jsoup.parse(response.getBody());

   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 15;
      this.log("Página " + this.currentPage);

      String url = "https://www.lider.cl/supermercado/search?Ntt=" + keywordEncoded + "&ost=" + keywordEncoded + "";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      StringBuilder payload = new StringBuilder();
      payload.append("productNumbers=");

      Elements products = this.currentDoc.select(".box-product.product-item-box");

      products.forEach(p -> {
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(p, ".box-product.product-item-box", "prod-number");
         if (internalId != null) {
            payload.append(internalId).append("%2C");
         }
      });

      JSONArray jsonArray = fetchAvaibility(payload);

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element element : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".box-product.product-item-box", "prod-number");
            Element e = this.currentDoc.selectFirst("." + internalId);
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-details a", "href"), "https:", "www.lider.cl");
            String name = scrapName(e);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".photo-container img", "src");
            int price = CommonMethods.doublePriceToIntegerPrice(CrawlerUtils.scrapDoublePriceFromHtml(e, ".price-sell b", null, true, ',', session), 0);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(setAvailability(jsonArray, internalId))
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String scrapName(Element e) {
      String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-description", true);
      String brand = CrawlerUtils.scrapStringSimpleInfo(e, ".product-name", true);
      if (brand != null && !brand.isEmpty()) {
         return name + " - " + brand;
      }
      return name;
   }

   protected boolean setAvailability(JSONArray jsonArray, String internalId) {

      for (int i = 0; i < jsonArray.length(); i++) {
         JSONObject jsonObject = jsonArray.optJSONObject(i);
         if (jsonObject.optString("productNumber") != null && jsonObject.optString("productNumber").equals(internalId)) {
            String level = jsonObject.optString("stockLevel");
            if (level != null && level.contains("1")) {
               return true;
            } else {
               return false;
            }

         }
      }
      return false;
   }

   protected JSONArray fetchAvaibility(StringBuilder payload) {

      String url = "https://www.lider.cl/supermercado/includes/inventory/inventoryInformation.jsp";
      payload.append("&useProfile=true&consolidate=true");
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/javascript, */*; q=0.01");
      headers.put("authority", "www.lider.cl");
      headers.put("referer", "https://www.lider.cl/supermercado/search?Ntt=" + keywordEncoded + "&ost=" + keywordEncoded + "");
      headers.put("cookie", CommonMethods.cookiesToString(cookies));
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(proxies)
         .setPayload(payload.toString())
         .build();

      Response response = new JsoupDataFetcher().post(session, request);

      if (!response.isSuccess()) {
         response = retryRequest(request, List.of(new JsoupDataFetcher(), new ApacheDataFetcher(), this.dataFetcher));
      }


      return CrawlerUtils.stringToJsonArray(response.getBody());
   }

   private Response retryRequest(Request request, List<DataFetcher> dataFetcherList) {
      Response response = dataFetcherList.get(0).get(session, request);

      if (!response.isSuccess()) {
         int tries = 0;
         while (!response.isSuccess() && tries < 3) {
            tries++;
            if (tries % 2 == 0) {
               response = dataFetcherList.get(1).get(session, request);
            } else {
               response = dataFetcherList.get(2).get(session, request);
            }
         }
      }

      return response;
   }

   @Override
   protected void setTotalProducts() {
      Element total = this.currentDoc.selectFirst(".result-departments a:last-child span");
      if (total != null) {
         String text = total.ownText().replaceAll("[^0-9]", "");

         if (!text.isEmpty()) {
            this.totalProducts = Integer.parseInt(text);
            this.log("Total da busca: " + this.totalProducts);
         }
      }
   }

}
