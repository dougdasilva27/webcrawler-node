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

public class EspanaAlcampoCrawler extends CrawlerRankingKeywords {

   public EspanaAlcampoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 18;
      this.log("Página " + this.currentPage);
      String url = "https://www.alcampo.es/compra-online/search?q=" + this.keywordEncoded + "%3Arelevance&text=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("URL : " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".productGridItem");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String internalPid = null;
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "h2 a", "data-id");
            String productUrl = CrawlerUtils.scrapUrl(e, ".productGridItem div a", "href", "https", "www.alcampo.es");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".productName span", true);
            String image = null;
            boolean available = e.select(".addQuantityProduct.positive.large.big-button.red-button.out-of-stock").isEmpty();
            Integer priceInCents = null;
            if (available) {
               priceInCents = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".priceContainer .price", null, false, ',', session, null);
            }

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(available)
               .build();

            saveDataProduct(productRanking);
         }
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }


   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".right .pagination .next").isEmpty();
   }

   @Override
   protected Document fetchDocument(String url) {
      Document doc = null;

      int attempts = 0;

      List<String> proxies = List.of(ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY);
      do {

         try {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(url, proxies.get(attempts), session);

            webdriver.waitLoad(30000);
            doc = Jsoup.parse(webdriver.getCurrentPageSource());

         } catch (Exception e) {
            Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));

         } finally {
            if (webdriver != null) {
               webdriver.terminate();
            }
         }
      } while (doc == null && attempts++ < 3);

      return doc;
   }
}
