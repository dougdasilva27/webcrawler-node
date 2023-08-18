package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SaopauloAraujoCrawler extends CrawlerRankingKeywords {

   public SaopauloAraujoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 50;

      String key = this.keywordWithoutAccents.replaceAll(" ", "%20");

      this.log("Página " + this.currentPage);
      String url = "https://www.araujo.com.br/busca?q=" + key + "&start=" + 50 * (this.currentPage  - 1) + "&sz=50&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".searchResults__productGrid .productTile");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalPid = e.attr("data-pid");
            String productUrl = "https://www.araujo.com.br" + e.attr("data-url");
            String name = e.attr("title");
            String imageUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "img", "src"), "https", "");
            int price = crawlPrice(e);
            boolean isAvailable = CrawlerUtils.scrapStringSimpleInfo(e, ".productDetails__unavailable", true) == null;

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
      Element totalElement = this.currentDoc.select(".quantity-products-found span").first();

      if (totalElement != null) {
         String total = totalElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!total.isEmpty()) {
            this.totalProducts = Integer.parseInt(total);
         }
      }

      this.log("Total da busca: " + this.totalProducts);
   }

   private Integer crawlPrice(Element e) {
      String gtmdata = e.attr("data-gtmdata");
      String jsonData = gtmdata.replace("&quot;", "\"");
      JSONObject jsonObject = new JSONObject(jsonData);
      Double price = jsonObject.optDouble("price");
      int priceInCents = (int) Math.round((Double) price * 100);
      return priceInCents;
   }

}
