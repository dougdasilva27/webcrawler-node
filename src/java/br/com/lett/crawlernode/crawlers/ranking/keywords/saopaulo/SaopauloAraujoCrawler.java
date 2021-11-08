package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SaopauloAraujoCrawler extends CrawlerRankingKeywords {

   public SaopauloAraujoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 32;

      String key = this.keywordWithoutAccents.replaceAll(" ", "%20");

      this.log("Página " + this.currentPage);
      String url = "http://busca.araujo.com.br/busca?q=" + key + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".neemu-products-container .nm-product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalPid = crawlInternalPid(e);
            String productUrl = crawlProductUrl(e);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".nm-product-name a", true);
            String imageUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".nm-product-img-link img", "src"), "https","");
            int price = CrawlerUtils.scrapIntegerFromHtml(e, ".nm-offer .nm-price-value", true, 0);
            boolean isAvailable = price != 0;

            //New way to send products to save data product
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.select(".neemu-total-products-container").first();

      if (totalElement != null) {
         String total = totalElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!total.isEmpty()) {
            this.totalProducts = Integer.parseInt(total);
         }
      }

      this.log("Total da busca: " + this.totalProducts);
   }

   private String crawlInternalPid(Element e) {
      String internalPid = null;

      String text = e.attr("id");
      if (text.contains("-")) {
         internalPid = CommonMethods.getLast(text.split("-"));
      }

      return internalPid;
   }

   private String crawlProductUrl(Element e) {
      String productUrl = null;
      Element urlElement = e.select(".nm-product-name > a").first();

      if (urlElement != null) {
         productUrl = urlElement.attr("href");

         if (!productUrl.startsWith("http")) {
            productUrl = "https:" + productUrl;
         }
      }

      return productUrl;
   }
}
