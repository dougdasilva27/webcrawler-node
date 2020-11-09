package br.com.lett.crawlernode.crawlers.ranking.keywords.portugal;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class PortugalElcorteinglesCrawler extends CrawlerRankingKeywords {

   public PortugalElcorteinglesCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
      this.pageSize = 24;
   }

   @Override
   protected Document fetchDocument(String url) {
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create()
            .mustUseMovingAverage(false)
            .mustRetrieveStatistics(true)
            .build())
         .mustSendContentEncoding(false)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.INFATICA_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_ES,
               ProxyCollection.BUY
            )
         ).build();

      String content = this.dataFetcher.get(session, request).getBody();

      if (content == null || content.isEmpty()) {
         content = new ApacheDataFetcher().get(session, request).getBody();
      }

      return Jsoup.parse(content);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://www.elcorteingles.pt/supermercado/pesquisar/" + currentPage + "/?term=" + keywordEncoded + "&search=text";
      this.currentDoc = fetchDocument(url);

      this.log("Link onde são feitos os crawlers: " + url);

      Elements products = this.currentDoc.select(".dataholder.js-product");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = e.attr("data-product-id");
            String productUrl = "https://www.elcorteingles.pt" + e.selectFirst("a").attr("href");

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
         }
      }
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".grid-coincidences .semi", false, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
