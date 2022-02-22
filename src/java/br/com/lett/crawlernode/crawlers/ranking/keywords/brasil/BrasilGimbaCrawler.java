package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;

public class BrasilGimbaCrawler extends CrawlerRankingKeywords {
   public BrasilGimbaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.pageSize = 21;

      this.log("Página " + this.currentPage);

      String url = "https://www.gimba.com.br/?txt-busca=" + this.keywordWithoutAccents + "&btn-buscar=Buscar&numeroPagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".card-product");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".card-product-thumbnail > a", "data-product-click");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".card-product-name", true);
            String urlProduct = crawlProductUrl(e, internalId, name);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".card-product-thumbnail img", Collections.singletonList("src"), "https", "imagens.gimba.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".card-product-offer .text-price", null, false, ',', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(urlProduct)
               .setInternalId(internalId)
               .setInternalPid(internalId)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   private String crawlProductUrl(Element e, String internalId, String name) {
      String url = CrawlerUtils.scrapUrl(e, ".card-product-thumbnail > a", "href", "https", "www.gimba.com.br");
      if(url == null || url.isEmpty()) {
         String slugifiedName = CommonMethods.toSlug(name);
         return "https://www.gimba.com.br/" + slugifiedName +"/" + "?PID=" + internalId;
      }
      return url;
   }

   private String crawlInternalId(Element e) {
      String internalId = e.attr("id");
      if(!internalId.isEmpty() && internalId != null) {
         return internalId.replaceAll("[^0-9]", "");
      }
      return null;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
