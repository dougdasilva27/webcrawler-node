package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class BrasilCasaeconstrucaoCrawler extends CrawlerRankingKeywords {

   public BrasilCasaeconstrucaoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      // número de produtos por página do market
      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      // se a key contiver o +, substitui por %20, pois nesse market a pesquisa na url é assim
      String url =
         "https://www.cec.com.br/busca?q=" + this.keywordWithoutAccents.replace(" ", "%20") + "&page=" + this.currentPage + "&resultsperpage=64";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div.product.d-flex");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = crawlInternalId(e);
            String productUrl = crawlProductUrl(e);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-name-text", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a.photo > img", "src");
            int price = CrawlerUtils.scrapIntegerFromHtml(e, ".price > span", true, 0);
            boolean isAvailable = setDisponibility(e);

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
      Element totalElement = this.currentDoc.selectFirst("#lblCountProductsNeemu");

      if (totalElement != null) {
         String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            this.totalProducts = Integer.parseInt(text);
         }
      }

      this.log("Total da busca: " + this.totalProducts);
   }

   private String crawlInternalId(Element e) {
      return e.attr("data-product-id");
   }

   private String crawlProductUrl(Element e) {
      String productUrl = null;
      Element urlElement = e.selectFirst(".name-and-brand");

      if (urlElement != null) {
         productUrl = CrawlerUtils.sanitizeUrl(urlElement, Arrays.asList("href"), "https:", "www.cec.com.br");
      }

      return productUrl;
   }

   private boolean setDisponibility(Element e){
      String avaliabilityProduct = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".price > meta:nth-child(3)","content");
      return avaliabilityProduct.contains("InStock");

   }
}
