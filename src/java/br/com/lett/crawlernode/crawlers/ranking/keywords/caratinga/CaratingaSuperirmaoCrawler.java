package br.com.lett.crawlernode.crawlers.ranking.keywords.caratinga;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;

public class CaratingaSuperirmaoCrawler extends CrawlerRankingKeywords {

   public CaratingaSuperirmaoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      //número de produtos por página do market
      this.pageSize = 24;

      this.log("Página " + this.currentPage);
      String url = "https://superirmao.loji.com.br/produtos?q=" + this.keywordEncoded + "&fornecedor=&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".row .produto");
      if (products.size() >= 1) {

         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");

            String addToCarUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "input", "name");
            String internalId;

            if (addToCarUrl != null) {
               internalId = addToCarUrl.substring(addToCarUrl.indexOf("[")+1, addToCarUrl.indexOf("]"));
            } else {
               internalId = scrapIdFromUnavailableProduct(productUrl);
            }

            String internalPid = internalId;
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".titulo b", true);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, "img", Collections.singletonList("src"), "https", "superirmao.loji.com.br");
            boolean isAvailable = addToCarUrl != null;
            Integer price = isAvailable ? CrawlerUtils.scrapPriceInCentsFromHtml(e, ".preco b", null, false, ',', session, null) : null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
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

   private String scrapIdFromUnavailableProduct (String url) {
      String[] strArray = url != null ? url.split("/") : null ;

      if (strArray.length > 4) {
         return strArray[4];
      }

      return null;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
