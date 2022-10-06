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

import java.io.UnsupportedEncodingException;

public class BrasilShopcaoCrawler extends CrawlerRankingKeywords {
   public BrasilShopcaoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://shopcao.com.br/search?options%5Bprefix%5D=none&options%5Bunavailable_products%5D=last&page=" + this.currentPage + "&q=" + this.keywordEncoded + "%2A&type=product";
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div.products.nt_products_holder.row > div");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element product : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "div.jdgm-widget.jdgm-preview-badge", "data-id");
            String productUrl = CrawlerUtils.scrapUrl(product, "div.product-info.mt__15 > h3 > a", "href", "https", "www.shopcao.com.br");
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, "h3 > a", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "span[data-price-type=finalPrice]", "src");
            Integer price = getPrice(product);

            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create().setUrl(productUrl).setInternalPid(internalPid).setName(productName).setPriceInCents(price).setAvailability(isAvailable).setImageUrl(imageUrl).build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit) break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página: " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select("div.products-footer.tc.mt__40.mb__60 > nav > ul > li > a").isEmpty();
   }

   private Integer getPrice(Element element) {
      String price = CrawlerUtils.scrapStringSimpleInfo(element, ".price.dib.mb__5", true);

      if (price.isEmpty()) {
         Integer specialPrice = CrawlerUtils.scrapPriceInCentsFromHtml(element, ".price ins", null, true, ',', session, null);
         return specialPrice;
      }

      return priceVariation(price);
   }

   private Integer priceVariation(String price) {
      String priceString = "";
      String arrayPrice[] = price.split(" ");
      if (arrayPrice[1] != null && !arrayPrice[1].isEmpty()) {
         priceString = arrayPrice[1];
         return CommonMethods.stringPriceToIntegerPrice(priceString, ',', null);
      }
      return null;
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, " div.col-12.nt_pr__ > h4", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
