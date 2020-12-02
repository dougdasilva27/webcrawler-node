package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public abstract class EnxutoSupermercadosCrawler extends CrawlerRankingKeywords {

   protected String storeId = getStoreId();

   protected abstract String getStoreId();

   public EnxutoSupermercadosCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 20;

      this.log("Página " + this.currentPage);
      this.currentDoc = acessApi();
      Elements products = this.currentDoc.select(".ui-datascroller-list li .ui-corner-all.oferta");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {

            String internalPid = CommonMethods.getLast(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".ui-panelgrid.oferta-actions .ui-g .ui-panelgrid-cell:first-child a span", "id").split("_"));
            String fakeUrl = "search="+ this.keywordEncoded + "/product_pid=" + internalPid;
            String productUrl = CrawlerUtils.completeUrl(fakeUrl,"https://", "drive.enxuto.com.br");

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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

   private Document acessApi() {
      String url = "https://drive.enxuto.com.br/shop/home.xhtml";

      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "JSESSIONID=fa09b6760425579e0fe62ac2feaf");
      headers.put("Connection","keep-alive");

      StringBuilder payload = new StringBuilder();

      payload.append("formPesquisa:pesquisa: " + this.keywordEncoded);
      payload.append("javax.faces.ViewState: " + storeId);
      payload.append("ofertas_offset: 160");

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).setPayload(payload.toString()).build();
      return  Jsoup.parse(this.dataFetcher.post(session,request).getBody());

   }

}
