package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriverException;

public class MexicoSorianasuperCrawler extends CrawlerRankingKeywords {

   public MexicoSorianasuperCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private static final String PROTOCOL = "https://";
   private static final String DOMAIN = "superentucasa.soriana.com/Default.aspx";


   private Document webdriverRequest(String url) {
      Document doc;

      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(url, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, session);
         if (webdriver != null) {
            doc = Jsoup.parse(webdriver.getCurrentPageSource());
            webdriver.terminate();
         } else {
            throw new WebDriverException("Failed to instantiate webdriver");
         }
      } catch (Exception e) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
         throw e;
      }

      return doc;
   }


   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      String url = "https://superentucasa.soriana.com/Default.aspx?p=13365&Txt_Bsq_Descripcion=" + this.keywordWithoutAccents.replace(" ", "%20") + "&Paginacion=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = webdriverRequest(url);

      Elements products = this.currentDoc.select(".product-item");

      if (!products.isEmpty()) {

         for (Element e : products) {

            String internalId = crawlInternalId(e);
            String productUrl = PROTOCOL + DOMAIN + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a[href]:first-child", "href");

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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

   @Override
   protected boolean hasNextPage() {
      return true;
   }

   private String crawlInternalId(Element e) {
      String internalId = null;
      Element id = e.selectFirst("input[type=hidden][name=s]");

      if (id != null) {
         internalId = id.val();
      }

      return internalId;
   }
}
