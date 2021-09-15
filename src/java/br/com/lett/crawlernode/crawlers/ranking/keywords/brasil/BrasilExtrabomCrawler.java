package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class BrasilExtrabomCrawler extends CrawlerRankingKeywords {

   private static final String API = "https://www.extrabom.com.br/carrinho/verificarCepDepositoType/";
   private String cep = this.session.getOptions().optString("cep");

   public BrasilExtrabomCrawler(Session session) {
      super(session);
   }

   @Override
   protected void processBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/x-www-form-urlencoded");
      String payload = "cep=" + cep;

      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      Response response = dataFetcher.post(session, request);
      this.cookies = response.getCookies();
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      String url = "https://www.extrabom.com.br/busca/?q=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".carousel__detalhes-top");

      if (!products.isEmpty()) {

         for (Element e : products) {
            String data = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");

            String urlProduct = "";
            String internalId = "";

            if (data != null) {
               urlProduct = "https://www.extrabom.com.br/" + data;
               String[] internalIdSplit = data.split("/");
               if (internalIdSplit.length > 2) {
                  internalId = internalIdSplit[2];
               }
            }

            saveDataProduct(internalId, null, urlProduct);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + internalId +
                  " - InternalPid: " + null +
                  " - Url: " + urlProduct);

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

   @Override
   protected boolean hasNextPage() {
      Element pagination = this.currentDoc.selectFirst(".pagination-box a:nth-last-child(2) > span");
      String selector = pagination != null ? pagination.text() : null;
      return selector != null && selector.contains("»");
   }
}
