package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BrasilCaroneCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.carone.com.br/";
   private final String cep = getCep();


   public BrasilCaroneCrawler(Session session) {
      super(session);
   }

   private String getCep() {
      return session.getOptions().optString("cep", "");
   }

   @Override
   protected Document fetchDocument(String url) {
      Logging.printLogDebug(logger, session, "Fetching page with webdriver...");
      ChromeOptions options = new ChromeOptions();
      options.addArguments("--window-size=1920,1080");
      options.addArguments("--headless");
      options.addArguments("--no-sandbox");
      options.addArguments("--disable-dev-shm-usage");
      if (!cep.isEmpty()) {
         Cookie cookie = new Cookie.Builder("postcode", cep)
            .domain(".carone.com.br")
            .path("/")
            .isHttpOnly(true)
            .isSecure(false)
            .build();
         this.cookiesWD.add(cookie);
      }

      Document doc = new Document("");
      int attempt = 0;
      boolean sucess = false;
      List<String> proxies = List.of(ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.SMART_PROXY_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY);
      do {
         try {
            Logging.printLogDebug(logger, session, "Fetching page with webdriver...");

            webdriver = DynamicDataFetcher.fetchPageWebdriver(url, proxies.get(attempt), session, this.cookiesWD, HOME_PAGE, options);
            if (webdriver != null) {
               webdriver.waitLoad(1000);

               doc = Jsoup.parse(webdriver.getCurrentPageSource());
               sucess = doc.selectFirst("div.category-products > ul > li") != null;
               webdriver.terminate();
            }
         } catch (Exception e) {
            Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
            Logging.printLogWarn(logger, "Página não capturada");
         }

      } while (!sucess && attempt++ < (proxies.size() - 1));

      return doc;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 54;
      this.log("Página " + this.currentPage);

      String url = crawlUrl();

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("div.category-products > ul > li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(this.currentDoc);
         }
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".add-to-list a", "data-id");
            String productUrl = CrawlerUtils.scrapUrl(e, ".product-image a", Arrays.asList("href"), "https:", HOME_PAGE);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-name", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image img", Collections.singletonList("src"), "https", "carone.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".special-price .price", null, false, ',', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
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

   private String crawlUrl() {
      String link;
      if (this.currentPage == 1) {
         link = "https://www.carone.com.br/catalogsearch/result/?q=" + this.keywordEncoded;
      } else {
         link = "https://www.carone.com.br/search/page/" + this.currentPage + "?q=" + this.keywordEncoded;
      }
      return link;
   }

   private void setTotalProducts(Document doc) {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, ".amount .show > span", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
