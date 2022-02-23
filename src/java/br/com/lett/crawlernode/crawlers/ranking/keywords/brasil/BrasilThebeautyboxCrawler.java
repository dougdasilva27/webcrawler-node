package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;

public class BrasilThebeautyboxCrawler extends CrawlerRankingKeywords {

   public BrasilThebeautyboxCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 36;
      this.log("Página " + this.currentPage);

      String url = "https://www.beautybox.com.br/busca?q=" + this.keywordEncoded + "&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".showcase-gondola .showcase-item.js-event-search");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {

            JSONObject data = JSONUtils.stringToJson(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".showcase-item.js-event-search", "data-event"));

            String internalId = data != null ? data.optString("sku") : null;

            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".showcase-item a", "href");

            String name = data.optString("productName");
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".showcase-item-image img", Collections.singletonList("data-src"), "", "");
            Double priceDouble = data.optDouble("price");
            Integer price =  (int) Math.round(100 * priceDouble);;
            Boolean isAvailable = price > 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".pagination-total strong", null, null, false, true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

}
