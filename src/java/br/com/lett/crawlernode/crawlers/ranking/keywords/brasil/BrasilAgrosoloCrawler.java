package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilAgrosoloCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "www.agrosolo.com.br";

   public BrasilAgrosoloCrawler(Session session) {
      super(session);
      dataFetcher = new JsoupDataFetcher();
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = "https://www.agrosolo.com.br/busca?busca=" + this.keywordEncoded + "&pagina=" + this.currentPage + "&tamanho=24";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("div.category__spots div.spots__item");

      if (!products.isEmpty()) {

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, ".spots__image > a", "href", "https", HOME_PAGE);

            String[] urlParts = productUrl.split("-");
            String internalPid = urlParts[urlParts.length -1];

            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".spots__title", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".spots__image > a > img", "src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".spots__price > .product__price--after", null, true, ',', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(name)
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
      return !this.currentDoc.select("#next-page").isEmpty();
   }
}
