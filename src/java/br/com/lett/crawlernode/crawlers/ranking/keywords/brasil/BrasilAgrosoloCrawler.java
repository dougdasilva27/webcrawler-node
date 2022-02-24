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

      String url = "https://www.agrosolo.com.br/busca?busca=" + this.keywordEncoded + "&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("div.produtos div.fbits-item-lista-spot ");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".spot", "id").replace("produto-spot-item-", "");
            String productUrl = CrawlerUtils.scrapUrl(e, ".spot div.spotContent > a", "href", "https", HOME_PAGE);

            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".fbits-spot-conteudo > .spot-parte-dois > .spotTitle", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".spotImg > img", "src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".precoPor > .fbits-valor", null, true, ',', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + internalPid +
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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "span.fbits-qtd-produtos-pagina", null, null, false, true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
