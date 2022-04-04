package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class BrasilMegustavegCrawler extends CrawlerRankingKeywords {

   public BrasilMegustavegCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 40;

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://www.megustaveg.com.br/buscar?q=" + this.keywordEncoded + "&pagina=" + currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      // chama função de pegar o html
      this.currentDoc = fetchDocument(url, cookies);

      Elements products = this.currentDoc.select("ul > .span3 > .listagem-item");

      // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".info-produto > .hide.trustvox-stars", "data-trustvox-product-code");
            String productUrl = CrawlerUtils.scrapUrl(e, "div > .info-produto > a", "href", "https", "www.megustaveg.com.br");

            String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div > .produto-sobrepor", "title");
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".imagem-produto.has-zoom > img", Arrays.asList("src"), "https", "www.megustaveg.com.br");
            Integer price = null;
            boolean isAvailable = getAvaliability(e);
            if (isAvailable) {
               price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".preco-produto  > div:first-child > .preco-promocional", "data-sell-price", true, '.', session, null);
            }

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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

   private boolean getAvaliability(Element e) {
      return CrawlerUtils.scrapStringSimpleInfo(e, ".bandeiras-produto > .bandeira-indisponivel.fundo-secundario", true) == null;
   }

   @Override
   protected boolean hasNextPage() {
      String hasNext = CrawlerUtils.scrapStringSimpleInfoByAttribute(this.currentDoc, ".ordenar-listagem.rodape.borda-alpha > div > div > div > ul > li:last-child > a", "href");
      return !hasNext.contains("#");
   }
}
