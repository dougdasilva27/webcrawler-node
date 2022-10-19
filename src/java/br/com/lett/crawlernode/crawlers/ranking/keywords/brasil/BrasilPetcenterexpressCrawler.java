package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;

import java.util.Arrays;

public class BrasilPetcenterexpressCrawler extends CrawlerRankingKeywords {

   private static final String HOST = "www.petcenterexpress.com.br";

   public BrasilPetcenterexpressCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 15;
      this.log("Página " + this.currentPage);

      String url = "https://www.petcenterexpress.com.br/loja/busca.php?loja=1059813&palavra_busca=" + CommonMethods.encondeStringURLToISO8859(this.location, logger, session)
         + "&pg=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".showcase__list .showcase__item");
      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".paginate__count strong", false, 0);
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product__link", "href");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product__quickviwer.quick__button", "data-id");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product__name .product__link", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product__image", Arrays.asList("src"), "https", "");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".prices__price", null, false, '.', session, null);

            boolean isAvailable = e.select(".product__unavailable").isEmpty();
            if (!isAvailable) {
               price = null;
            }

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

            this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
               + this.arrayProducts.size() + " produtos crawleados");

         }
      }


   }
}
