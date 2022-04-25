package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class BrasilMercadinhosaoluizCrawler extends CrawlerRankingKeywords {
   public BrasilMercadinhosaoluizCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      this.pageSize = 8;

      this.log("Página " + this.currentPage);

      String url = "https://www.mercadinhossaoluiz.com.br/buscapagina?ft=" + this.keywordEncoded + "&PS=8&sl=6c852839-4fab-453f-a4d0-e7fc54f20719&cc=4&sm=0&PageNumber=" + currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".vitrine.prateleira.n4colunas >  ul > li:nth-child(odd)");
      if(products.isEmpty()){
         products = this.currentDoc.select(".vitrine > ul > li:nth-child(odd)");
      }

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".btn-like.js-btn-like", "id");
            String productUrl = CrawlerUtils.scrapUrl(e, ".product__content > .product__head > h4 > a", "href", "https:", url);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product__content > .product__head > h4 > a", true);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product__image > figure > .x-image-default > img", Arrays.asList("src"), "https:", url);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".product__content > .product__price > .best-price", null, true, ',', session, null);
            boolean isAvailable = price != null;

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
   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".vitrine.prateleira.n4colunas >  ul > li:nth-child(odd)") != null;
   }
}
