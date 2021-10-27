package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.apache.commons.lang.WordUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ColombiaLarebajaCrawler extends CrawlerRankingKeywords {

   public ColombiaLarebajaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      String url = "https://www.larebajavirtual.com/catalogo/buscar?busqueda=" + this.keywordEncoded;

      if (this.currentPage > 1) {
         url = CrawlerUtils.scrapUrl(currentDoc, ".pager .next a", "href", "https:", "www.larebajavirtual.com");
      }

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".listaProductos li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, ".img-list-products a", "href", "https:", "larebajavirtual.com");
            String internalPid = CommonMethods.getLast(productUrl.substring(0, productUrl.indexOf("/descripcion")).split("/"));

            //For some reason, the product name in html is all lower case
            String name = WordUtils.capitalize(CrawlerUtils.scrapStringSimpleInfo(e, "div.nameProduct a", true));
            String imageUrl = CrawlerUtils.scrapUrl(e, ".img-list-products img", "src", "https:", "larebajavirtual.com");
            int price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "div.priceFinal", null, true, ',', session, 0);
            boolean isAvailable = price != 0;

            //New way to send products to save data product
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(imageUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
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
      Element total = this.currentDoc.selectFirst("#id-productos-list .summary");
      if (total != null) {
         String text = total.ownText().toLowerCase();

         if (text.contains("de")) {
            String totalText = CommonMethods.getLast(text.split("de")).replaceAll("[^0-9]", "");

            if (!totalText.isEmpty()) {
               this.totalProducts = Integer.parseInt(totalText);
               this.log("Total da busca: " + this.totalProducts);
            }
         }
      }
   }
}
