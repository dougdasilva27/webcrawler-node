package br.com.lett.crawlernode.crawlers.ranking.keywords.espana;

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

import java.util.List;

public class EspanaElcorteinglesCrawler extends CrawlerRankingKeywords {


   public EspanaElcorteinglesCrawler(Session session) {
      super(session);
      this.pageSize = 24;
   }

   @Override
   protected Document fetchDocument(String url) {
      Document doc = null;
      int attemp = 0;
      List<String> proxies = List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY);

      do {
         try {
            Logging.printLogDebug(logger, session, "Fetching page with webdriver...");

            webdriver = DynamicDataFetcher.fetchPageWebdriver(url, proxies.get(attemp % 2), session);
            webdriver.waitForElement(".dataholder.js-product", 10);

            doc = Jsoup.parse(webdriver.getCurrentPageSource());

         } catch (Exception e) {
            Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
            Logging.printLogWarn(logger, "Página não capturada");

         } finally {
            if (webdriver != null) {
               webdriver.terminate();
            }
         }
      } while (doc == null && attemp++ < 3);

      return doc;
   }


   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      String url = "https://www.elcorteingles.es/supermercado/buscar/" + currentPage + "/?term=" + keywordEncoded + "&search=text";

      this.currentDoc = this.fetchDocument(url);

      this.log("Link onde são feitos os crawlers: " + url);

      Elements products = this.currentDoc.select(".dataholder.js-product");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = e.attr("data-product-id");
            String productUrl = "https://www.elcorteingles.es" + e.selectFirst("a").attr("href");

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName("nome")
               .setPriceInCents(1)
               .setAvailability(true)
               . build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      }
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".grid-coincidences .semi", false, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
