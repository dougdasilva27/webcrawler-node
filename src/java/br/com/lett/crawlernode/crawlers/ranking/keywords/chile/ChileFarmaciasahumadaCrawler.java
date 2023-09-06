package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;

public class ChileFarmaciasahumadaCrawler extends CrawlerRankingKeywords {

   public ChileFarmaciasahumadaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      String url = "https://www.farmaciasahumada.cl/catalogsearch/result/index/?p=" + this.currentPage +"&q=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".search.results .item.product.product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {

            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".product-item-info .price-box","data-product-id");
            String internalId = internalPid;
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-info .product-item-photo", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e,".product-item-info .product-item-name a", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-item-info img.product-image-photo", Arrays.asList("src"), "https", "");
            Integer price = scrapPrice(e);

            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(imgUrl)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".toolbar.toolbar-products .items.pages-items .action.next") != null;
   }

   protected Integer scrapPrice(Element e) {
      Integer spotlightPrice = CrawlerUtils.scrapPriceInCentsFromHtml(e, "[data-price-type=\"finalPrice\"] .price", null, false, ',', session, null);
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapPriceInCentsFromHtml(e,".special-price .price-container.price-final_price .price", null,false,',',session, null);
      }
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".prueba .price", null, false, ',', session, 0);
      }

      return spotlightPrice;
   }
}
