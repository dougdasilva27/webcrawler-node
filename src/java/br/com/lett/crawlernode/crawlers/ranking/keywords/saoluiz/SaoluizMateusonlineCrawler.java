package br.com.lett.crawlernode.crawlers.ranking.keywords.saoluiz;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class SaoluizMateusonlineCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "www.mateusonline.com.br";
   private static final Integer STORE_ID = 1;

   public SaoluizMateusonlineCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      this.currentDoc = fetchHtml();
      Elements products = this.currentDoc.select(".nm-product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element product : products) {
            String internalId = product.attr("data-pid");
            String internalPid = internalId;
            String productUrl = CrawlerUtils.scrapUrl(product, ".nm-product-img-container > a", "href", "https", HOME_PAGE).split("\\?")[0];

            saveDataProduct(internalId, internalPid, productUrl);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + internalId +
                  " - InternalPid: " + internalPid +
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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".neemu-total-products-container > span", true, 0);
      this.log("Total de produtos: " + this.totalProducts);
   }

   private Document fetchHtml() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "lx_sales_channel_linx=" + STORE_ID + ";");

      String url = "https://busca.mateusonline.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(url)
         .build();

      takeAScreenshot(url, cookies);

      return Jsoup.parse(dataFetcher.get(session, request).getBody());
   }
}
