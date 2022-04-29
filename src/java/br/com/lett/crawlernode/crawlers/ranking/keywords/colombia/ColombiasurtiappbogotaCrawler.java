package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import software.amazon.awssdk.http.Header;

import java.util.*;

public class ColombiasurtiappbogotaCrawler extends CrawlerRankingKeywords {
   private int login = 0;
   List<String> proxies = Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY);
   @Override
   protected void processBeforeFetch() {
   }

   @Override
   protected Document fetchDocument(String url) {

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(this.cookies)
         .setProxyservice(proxies)
         .build();
      String html = new JsoupDataFetcher().get(session, request).getBody();

      return Jsoup.parse(html);
   }


   public ColombiasurtiappbogotaCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      if (login == 0){
         getCookiesLogin();
      }
      this.pageSize = 25;
      this.log("Página " + this.currentPage);

      String url = "https://tienda.surtiapp.com.co/WithoutLoginB2B/Store/SearchResults/" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url, this.cookies);

      Elements products = this.currentDoc.select(".product-card.product-id-contaniner");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "data-product");
            String productUrl = CrawlerUtils.completeUrl(internalId, "https", "tienda.surtiapp.com.co/Store/ProductDetail/");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-card__name", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-card__image img", "src");
            Integer price = CrawlerUtils.scrapIntegerFromHtml(e, ".product-card__price", true, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 20);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   public void getCookiesLogin() {
      Map<String, String> Headers = new HashMap<>();
      Headers.put("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
      Request request = Request.RequestBuilder.create()
         .setUrl("https://tienda.surtiapp.com.co/WithoutLoginB2B/Store/ProductDetail/d7e53433-89a4-ec11-a99b-00155d30fb1f")
         .setHeaders(Headers)
         .mustSendContentEncoding(true)
         .build();
      Response responseApi = new JsoupDataFetcher().get(session, request);
      Document document = Jsoup.parse(responseApi.getBody());

      String verificationToken = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".mh__search-bar-wrapper > form > input[type=hidden]", "value");
      Headers.put("RequestVerificationToken", verificationToken);
      String payload = "username=1130409480&password=Lett12345.&isMobileLogin=false&RedirectTo=";
      Request requestLogin = Request.RequestBuilder.create()
         .setHeaders(Headers)
         .setCookies(responseApi.getCookies())
         .setPayload(payload)
         .mustSendContentEncoding(true)
         .setUrl("https://tienda.surtiapp.com.co/WithoutLoginB2B/Security/UserAccount?handler=Authenticate")
         .build();
      Response responseApiLogin = new JsoupDataFetcher().post(session, requestLogin);
       this.cookies.addAll(responseApiLogin.getCookies());
      login ++;
   }

}
