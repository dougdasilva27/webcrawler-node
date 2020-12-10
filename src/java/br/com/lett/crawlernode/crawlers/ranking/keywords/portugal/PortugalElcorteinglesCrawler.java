package br.com.lett.crawlernode.crawlers.ranking.keywords.portugal;

import java.util.Arrays;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class PortugalElcorteinglesCrawler extends CrawlerRankingKeywords {

   public PortugalElcorteinglesCrawler(Session session) {
      super(session);
      this.pageSize = 24;
   }

   private String verificationToken;

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
                        ProxyCollection.NETNUT_RESIDENTIAL_BR,
                        ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY,
                        ProxyCollection.LUMINATI_SERVER_BR
                  )
            ).build();

      String content = new JsoupDataFetcher().get(session, request).getBody();

      Document doc = Jsoup.parse(content);

      if (doc.select(".header-logo").isEmpty() && !doc.select("meta[http-equiv=refresh]").isEmpty()) {
         String contentRefresh = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "meta[http-equiv=refresh]", "content");

         String token = "bm-verify=";

         if (contentRefresh.contains(token)) {
            int firstIndex = contentRefresh.indexOf(token) + token.length();
            int lastIndex = contentRefresh.indexOf("'", firstIndex);

            verificationToken = contentRefresh.substring(firstIndex, lastIndex);

            String urlToken = "https://www.elcorteingles.pt/supermercado/pesquisar/" + currentPage + "/?term=" + keywordEncoded + "&search=text"
                  + "&bm-verify=" + this.verificationToken;

            request.setUrl(urlToken);
            doc = Jsoup.parse(new JsoupDataFetcher().get(session, request).getBody());
         }
      }

      return doc;
   }


   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      String url = "https://www.elcorteingles.pt/supermercado/pesquisar/" + currentPage + "/?term=" + keywordEncoded + "&search=text";

      if (this.verificationToken != null) {
         url += "&bm-verify=" + this.verificationToken;
      }

      this.currentDoc = this.fetchDocument(url);

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
