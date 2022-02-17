package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ChileLidersuperCrawler extends CrawlerRankingKeywords {

   public ChileLidersuperCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 15;
      this.log("Página " + this.currentPage);

      String url = "https://www.lider.cl/supermercado/search?No=" + this.arrayProducts.size() + "&Ntt=" + this.keywordEncoded
         + "&isNavRequest=Yes&Nrpp=40&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("#content-prod-boxes div[prod-number]");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".box-product.product-item-box", "prod-number");
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-details a", "href"), "https:", "www.lider.cl");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-description", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".photo-container img", "src");
            int price = CommonMethods.doublePriceToIntegerPrice(CrawlerUtils.scrapDoublePriceFromHtml(e, ".price-sell b", null, true, ',', session), 0);
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
   protected void setTotalProducts() {
      Element total = this.currentDoc.selectFirst(".result-departments a:last-child span");
      if (total != null) {
         String text = total.ownText().replaceAll("[^0-9]", "");

         if (!text.isEmpty()) {
            this.totalProducts = Integer.parseInt(text);
            this.log("Total da busca: " + this.totalProducts);
         }
      }
   }

}
