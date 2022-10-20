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
import java.util.Collections;

public class BrasilPeixotoMaisCrawler extends CrawlerRankingKeywords {

   public BrasilPeixotoMaisCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);
      String url = getPageUrl();
      this.log("URL : " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".item.product.product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".price-box.price-final_price", "data-product-id");
            String productUrl = "https://www.peixotomais.com.br/" + e.select("a").attr("href").replaceAll("(\\r|\\n)", "");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-item-link", true);
            String image = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image-photo.mplazyload.mplazyload", Collections.singletonList("src"), "https", "www.peixotomais.com.br");
            Integer priceInCents = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price", null, true, ',', session, 0);
            boolean available = priceInCents != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(available)
               .build();

            saveDataProduct(productRanking);
         }
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String getPageUrl() {
      int productsShow = this.currentPage == 1 ? 0 : this.pageSize * (this.currentPage - 1);
      return "https://www.peixotomais.com.br/catalogsearch/result/?q=" + this.keywordEncoded;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
