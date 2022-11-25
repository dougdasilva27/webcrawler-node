package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

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

import java.io.UnsupportedEncodingException;
import java.util.List;

public class PeruLinioCrawler extends CrawlerRankingKeywords {
   public PeruLinioCrawler(Session session) {

      super(session);
   }

   String HOME_PAGE = "https://www.linio.com.pe";
   String url = HOME_PAGE + "/search?scroll=&q=" + this.keywordEncoded + "&page=" + this.currentPage;

   @Override
   protected Document fetchDocument(String url) {
      List<String> proxies = List.of(ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY, ProxyCollection.SMART_PROXY_PE_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY);
      int attemp = 0;
      boolean succes = false;
      Document doc = new Document("");
      do {
         try {
            Logging.printLogDebug(logger, session, "Fetching page with webdriver...");
            webdriver = DynamicDataFetcher.fetchPageWebdriver(url, proxies.get(attemp), session);
            if (webdriver != null) {
               doc = Jsoup.parse(webdriver.getCurrentPageSource());
               succes = !doc.select(".catalogue-product.row").isEmpty();
               webdriver.terminate();
            }
         } catch (Exception e) {
            Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
            Logging.printLogWarn(logger, "Page not captured");
         }
      } while (!succes && attemp++ < proxies.size());
      return doc;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".catalogue-product.row");

      if (!products.isEmpty()) {
         for (Element product : products) {
            String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".col-12.pl-0.pr-0", "title");
            Integer priceInCents = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".lowest-price .price-main-md",
               null, false, '.', session, null);
            String productUrl = getProductUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".catalogue-product.row a", "href"));
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".catalogue-product.row", "data-card-sku");
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".image", List.of("data-lazy"),
               "https", "");
            RankingProduct rankingProduct = RankingProductBuilder.create()
               .setInternalId(internalPid)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(imageUrl)
               .setAvailability(true)
               .setPriceInCents(priceInCents)
               .setUrl(productUrl)
               .build();

            saveDataProduct(rankingProduct);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      }
   }

   private String getProductUrl(String url) {
      return url != null && !url.isEmpty() ? HOME_PAGE + url : null;
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".page-link page-link-icon") != null;
   }
}
