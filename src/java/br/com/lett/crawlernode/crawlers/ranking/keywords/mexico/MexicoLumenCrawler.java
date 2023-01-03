package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MexicoLumenCrawler extends CrawlerRankingKeywords {

   public MexicoLumenCrawler(Session session) {
      super(session);
   }

   protected Document fetch() {
      String url = "https://lumen.com.mx/#/b7a17c1f-db95-48f9-a052-062b8919bd34/embedded/autofilters=false&page=" + this.currentPage + "&query=" + this.keywordEncoded + "&query_name=match_and&rpp=50";
      Document doc = null;
      this.log("Link onde são feitos os crawlers: " + url);

      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(url, ProxyCollection.BUY_HAPROXY, session, this.cookiesWD, "https://lumen.com.mx");

         webdriver.waitForElement(".dfd-results-grid", 1200);

         doc = Jsoup.parse(webdriver.getCurrentPageSource());
         webdriver.terminate();

      } catch (Exception e) {
         Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));

      } finally {
         if (webdriver != null) {
            webdriver.terminate();
         }
      }

      return doc;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 50;
      this.log("Página " + this.currentPage);
      this.currentDoc = fetch();

      Elements products = this.currentDoc.select(".dfd-results-grid .dfd-card");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".dfd-card-link", "href");
            String internalId = getInternalId(productUrl);
            String internalPid = null;
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".dfd-card-title", true);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".dfd-card-thumbnail img", Arrays.asList("src"), "https", "lumen.com.mx");
            Integer price = crawlPrice(e);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setImageUrl(imageUrl)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String getInternalId(String productUrl) {
      String regex = "\\/([0-9]*).jpg";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(productUrl);
      if (matcher.find()) {
         return matcher.group(1);
      }

      return null;
   }

   private Integer crawlPrice(Element e) {
      Integer promotionalPrice = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".dfd-card-price--sale", null, false, '.', session, null);
      if (promotionalPrice == null) {
         return CrawlerUtils.scrapPriceInCentsFromHtml(e, ".dfd-card-price", null, false, '.', session, null);
      }
      return promotionalPrice;
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".dfd-meta strong", true, 0);
      this.log("Total: " + this.totalProducts);
   }
}
