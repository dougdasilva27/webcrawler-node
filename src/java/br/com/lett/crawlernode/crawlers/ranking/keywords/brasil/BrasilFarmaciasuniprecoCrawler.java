package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilFarmaciasuniprecoCrawler extends CrawlerRankingKeywords {

   public BrasilFarmaciasuniprecoCrawler(Session session) {
      super(session);
   }

   final private String HOME_PAGE = "https://www.farmaciasunipreco.com.br";

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 15;

      String url = HOME_PAGE + "/pesquisa?t=" + this.keywordEncoded + "#/pagina-" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      this.log("Página " + this.currentPage);

      if (this.currentPage == 1) {
         setTotalProducts();
      }

      Elements products = this.currentDoc.select(".wd-browsing-grid-list ul.row > li[class^=product-]");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".wd-product-line", "data-product-id");
            String internalPid = internalId;
            String urlPath = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".item-description > .name > a", "href");
            String productUrl = urlPath != null ? HOME_PAGE + urlPath : null;
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".item-description > .name > a", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".medias .thumb img.current-img", "data-src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".wd-product-price-description .priceContainer .sale-price span", null, true, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
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
   protected void setTotalProducts() {
      if (this.totalProducts < 1 && this.arrayProducts.size() < productsLimit) {
         this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".wd-browsing-grid-pager .description .product-count span", true, 0);;
         this.log("Total: " + this.totalProducts);
      }
   }

}
