package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;

public class SupernicoliniCrawler extends CrawlerRankingKeywords {
   private String HOME_PAGE = "";

   public SupernicoliniCrawler(Session session) {
      super(session);
      HOME_PAGE = getHomepage(session);
   }

   private String getHomepage(Session session) {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      String url = HOME_PAGE + "page/" + this.currentPage + "/?s=" + this.keywordEncoded + "&post_type=product";

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".products .product-small.box");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {

            String internalPid = null;
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".button.ajax_add_to_cart.add_to_cart_button", "data-product_id");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".image-fade_in_back a", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-title", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".box-image img", Collections.singletonList("src"), "https", HOME_PAGE);
            Integer price = scrapPrice(e);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private Integer scrapPrice(Element e) {
      Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price-wrapper .price:not(del) > span", null, false, ',', session, 0);

      if (price == 0) {
         price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price-wrapper .price ins > span", null, false, ',', session, 0);
      }

      return price;
   }

   @Override
   protected boolean hasNextPage() {
      return this.arrayProducts.size() < this.totalProducts;
   }

   @Override
   protected void setTotalProducts() {
      String allText = CrawlerUtils.scrapStringSimpleInfo(currentDoc, ".woocommerce-result-count", false);

      if (allText != null && allText.contains("de")) {
         String totalString = allText.split("de")[1];
         this.totalProducts = MathUtils.parseInt(totalString.replaceAll("[^0-9.]", "").trim());

      } else {
         this.totalProducts = MathUtils.parseInt(allText);
      }

   }

}
