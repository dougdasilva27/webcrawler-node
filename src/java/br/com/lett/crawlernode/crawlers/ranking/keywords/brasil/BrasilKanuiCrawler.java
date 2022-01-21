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

public class BrasilKanuiCrawler extends CrawlerRankingKeywords {

   public BrasilKanuiCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      // número de produtos por página do market
      this.pageSize = 48;

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://www.kanui.com.br/catalog/?q=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".content-products-list.row .product-box");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0)
            setTotalProducts();

         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-box-image", "id");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-box-title", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, " .product-box-image a", "data-model-picture");
            int price = CommonMethods.doublePriceToIntegerPrice(scrapPrice(e), 0);
            boolean isAvailable = price != 0;

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

   private Double scrapPrice(Element e) {
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(e, ".product-box-price-to", null, true, ',', session);
      if (price == null) {
         price = CrawlerUtils.scrapDoublePriceFromHtml(e, ".product-box-price-from", null, true, ',', session);
      }
      return price;

   }

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.select("div.items-products.select-options-item span").first();

      try {
         if (totalElement != null)
            this.totalProducts = Integer.parseInt(totalElement.text());
      } catch (Exception e) {
         this.logError(e.getMessage());
      }

      this.log("Total da busca: " + this.totalProducts);
   }
}
