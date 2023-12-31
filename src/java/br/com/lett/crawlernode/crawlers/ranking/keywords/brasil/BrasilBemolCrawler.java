package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilBemolCrawler extends CrawlerRankingKeywords {

   public BrasilBemolCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      // número de produtos por página do market
      this.pageSize = 32;

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://www.bemol.com.br/pesquisa?pg=" + this.currentPage + "&t=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      // chama função de pegar o html
      this.currentDoc = fetchDocument(url, cookies);

      Elements products = this.currentDoc.select(".row li .wd-product-line");

      // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String internalId = e.attr("data-product-id");
            String productUrl = crawlProductUrl(e);

            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".item-description > h3 > a", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".variation-root> img", "data-src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".priceContainer > div > strong > span", null, true, ',', session, null);
            boolean isAvailable = price != null;

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
      Element totalElement = this.currentDoc.selectFirst(".product-count > span");

      if (totalElement != null) {
         String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            this.totalProducts = Integer.parseInt(text);
         }
      }

      this.log("Total da busca: " + this.totalProducts);
   }

   private String crawlProductUrl(Element e) {
      String productUrl = e.attr("href");

      Element href = e.selectFirst(".item-description .name a");
      if (href != null) {
         productUrl = CrawlerUtils.completeUrl(href.attr("href"), "https:", "www.bemol.com.br");
      }

      return productUrl;
   }
}
