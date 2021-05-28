package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

import java.time.LocalDate;

public class SaopauloPanvelCrawler extends CrawlerRankingKeywords {

   public SaopauloPanvelCrawler(Session session) {
      super(session);
      fetchMode = FetchMode.FETCHER;
      cookies.add(new BasicClientCookie("stc112189", String.valueOf(LocalDate.now().toEpochDay())));
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 16;
      this.log("Página " + this.currentPage);

      String url = "https://www.panvel.com/panvel/buscarProduto.do?termoPesquisa=" + keywordEncoded + "&paginaAtual=" + currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      Request request = Request.RequestBuilder.create().setCookies(cookies).setUrl(url)
         .mustSendContentEncoding(false)
         .setSendUserAgent(false)
         .build();
      Response response = dataFetcher.get(session, request);

      currentDoc = Jsoup.parse(response.getBody());
      Elements products = this.currentDoc.select("li.search-item");

      for (Element e : products) {
         String urlProduct = "https://www.panvel.com" + e.selectFirst(".details").attr("href");
         String internalId = CommonMethods.getLast(urlProduct.split("-"));

         saveDataProduct(internalId, null, urlProduct);

         this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
            + null + " - Url: " + urlProduct);
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return (arrayProducts.size() % pageSize - currentPage) < 0;
   }
}
