package br.com.lett.crawlernode.crawlers.ranking.keywords.maceio;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MaceioPalatoCrawler extends CrawlerRankingKeywords {

   public MaceioPalatoCrawler(Session session) {
      super(session);
      fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected Document fetchDocument(String url) {

      String payload = "offset=" + (this.currentPage - 1) + "&more=1";
      Request request = Request.RequestBuilder.create().setUrl(url).setPayload(payload).build();
      String content = this.dataFetcher.post(session, request).getBody();

      return Jsoup.parse(content.replace("\\\"","").replace("\\",""));
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      String keyword = this.keywordEncoded.replace(" ", "+");

      String url = "https://loja.palato.com.br/busca?q=" + keyword;

      this.log("Página" + this.currentPage);
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".item");

      if (!products.isEmpty()) {

         for (Element product : products) {

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-options input[name='id']", "value");
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-options input[name='sku']", "value");
            String productUrl = CrawlerUtils.scrapUrl(product, "a", "href", "https", "loja.palato.com.br");

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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
}
