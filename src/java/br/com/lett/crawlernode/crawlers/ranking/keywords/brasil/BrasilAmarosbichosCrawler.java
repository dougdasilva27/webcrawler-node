package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

import java.io.UnsupportedEncodingException;

public class BrasilAmarosbichosCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.petshopamarosbichos.com.br";

   public BrasilAmarosbichosCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = HOME_PAGE + "/busca?busca=" + this.keywordEncoded + "&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".fbits-item-lista-spot");

      if (!products.isEmpty()) {
         for (Element product : products) {
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".spotTitle", true);
            String productUrl = HOME_PAGE + CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".spotContent > a", "href");
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".jsImgSpot.imagem-primaria", "src");
            String internalPid = CommonMethods.getLast(productUrl.split("-"));
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, "div.precoPor > span.fbits-valor", null, false, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(productName)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);
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
      return this.currentDoc.selectFirst(".next_page.disabled") == null;
   }
}
