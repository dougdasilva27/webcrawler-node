package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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

public class KochCrawlerRanking extends CrawlerRankingKeywords {


   protected String storeId;

   public String getStoreId() {
      return storeId;
   }

   public void setStoreId(String storeId) {
      this.storeId = storeId;
   }


   public KochCrawlerRanking(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);
      this.currentDoc = fetchProducts();

      Elements elements = this.currentDoc.select(".item.product.product-item");

      if (!elements.isEmpty()) {
         for (Element e : elements) {

            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-info a", "href");

            String internalId = CommonMethods.getLast(productUrl.split("-"));

            saveDataProduct(internalId, null, productUrl);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + internalId +
                  " - InternalPid: " + null +
                  " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit)
               break;
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
      return !this.currentDoc.select(".item.pages-item-next").isEmpty();
   }

   private Document fetchProducts() {

      String url = "https://www.superkoch.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;

      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", "customer_website=website_lj" + storeId);

      Request req = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      String content = this.dataFetcher.get(session, req).getBody();


      return Jsoup.parse(content);
   }
}
