package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

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

import java.util.Collections;

public class ChileCruzverdeCrawler extends CrawlerRankingKeywords {

   public ChileCruzverdeCrawler(Session session) {
      super(session);
   }

   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 30;
      this.log("Página " + this.currentPage);

      String url = "https://www.cruzverde.cl/busqueda?q=" + this.keywordWithoutAccents + "&search-button=&lang=es_CL";
      String urlWithoutSpaces = url.replaceAll(" ", "+");

      this.log("Link onde são feitos os crawlers: " + urlWithoutSpaces);
      this.currentDoc = fetchDocument(urlWithoutSpaces);
      Elements products = this.currentDoc.select(".product.product-wrapper");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = e.attr("data-pid");
            String productUrl = "https://www.cruzverde.cl" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, " .image-container a", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".pdp-link", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".tile-image", Collections.singletonList("src"), "https", "cruzverde.cl");
            Integer price = crawlPrice(e);
            boolean isAvailable = CrawlerUtils.scrapStringSimpleInfo(e, ".add-to-cart:not([disabled])", false) != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
               .setImageUrl(imgUrl)
               .setName(name)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private Integer crawlPrice(Element e) {
      int price = 0;
      String priceString = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".sales .value", "content");
      if (priceString != null && !priceString.isEmpty() && !priceString.equals("null")) {
         price = Integer.parseInt(priceString) * 100;
      } else {
         price = CrawlerUtils.scrapPriceInCentsFromHtml(e,".d-block .large-price .value", null, true, ',', session,0);
      }
      return price;
   }

   @Override
   protected boolean hasNextPage() {
      return currentDoc.selectFirst(".pagination.pagination-footer button:not(first-child)") != null;
   }
}
