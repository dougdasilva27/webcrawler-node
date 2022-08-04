package br.com.lett.crawlernode.crawlers.ranking.keywords.costarica;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Collections;

public class CostaricaPeridomicilio extends CrawlerRankingKeywords {
   public CostaricaPeridomicilio(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      // Quantidade de produtos por página do market
      this.pageSize = 9;

      this.log("Página " + this.currentPage);

      // Monta a url com a keyword e a página
      String url = "https://www.peridomicilio.com/?subcats=Y&pcode_from_q=Y&pshort=Y&pfull=Y&pname=Y&pkeywords=Y&search_performed=Y&q=" + this.keywordEncoded + "&dispatch=products.search&security_hash=68728370519c419b334906a65ba0a328&page=1&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      // Chama a função a qual pega o html
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("#pagination_contents > .ty-product-list.clearfix");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".cm-disable-empty-files.cm-ajax.cm-ajax-full-render.cm-ajax-status-middle > input:nth-child(3)","value");
            String productUrl = CrawlerUtils.scrapUrl(e, ".ty-product-list__content > .ty-product-list__info > .ty-product-list__item-name > a", "href", "https://", "www.peridomicilio.com");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".ty-product-list__content > .ty-product-list__info > .ty-product-list__item-name > a", true);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".image-reload > a > .cm-image", Collections.singletonList("src"), "https://", "www.peridomicilio.com");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".ty-price > .ty-price-num:nth-child(even)", null, true, ',', session, null);
            boolean isAvailable = price != 0;

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
      return this.currentDoc.selectFirst("#pagination_contents > .ty-pagination > div > a") != null;
   }
}
