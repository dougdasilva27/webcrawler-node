package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;

import br.com.lett.crawlernode.util.Logging;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;

import java.util.List;

public class BrasilBebidasStoreCrawler extends CrawlerRankingKeywords {
   public BrasilBebidasStoreCrawler(Session session) {
      super(session);
      pageSize = 15;
   }

   private static String HOME_PAGE = "https://www.bebidastore.com.br/";

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      String query = HOME_PAGE + "loja/busca.php?loja=1055537&palavra_busca=" + this.keywordEncoded + "&pg=" + this.currentPage;
      this.currentDoc = fetchDocument(query);
      if (this.currentDoc != null) {
         Elements products = this.currentDoc.select(".item.flex");
         for (Element product : products) {
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".product-price", null, false, ',', session, null);
            Boolean available = price != null;
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".list-variants", "data-id");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".product-name", true);
            String url = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".info-product", "href");
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".lazyload.transform", List.of("data-src"), "", "");
            RankingProduct productRanking = RankingProductBuilder.create()
               .setInternalId(internalId)
               .setUrl(url)
               .setName(name)
               .setAvailability(available)
               .setPriceInCents(price)
               .setImageUrl(imageUrl)
               .build();
            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
   }

   private Boolean checkAvailibity(String available) {
      if (available != null && !available.isEmpty()) {
         return !available.contains("Esgotado");
      }
      return false;
   }

   @Override
   protected boolean hasNextPage() {
      String nextPage = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, ".page-next.page-link", false);
      return nextPage != null && nextPage.contains("Próxima");
   }
}
