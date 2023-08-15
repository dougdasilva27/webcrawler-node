package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;

public class BrasilDrogarianovaesperancaCrawler extends CrawlerRankingKeywords {

   private static final int PAGE_SIZE = 18;
   private static final String homePage = "www.drogarianovaesperanca.com.br";

   public BrasilDrogarianovaesperancaCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = PAGE_SIZE;
      this.log("Página " + this.currentPage);

      String url = "https://www.drogarianovaesperanca.com.br/busca/?Pagina=" + this.currentPage +
         "&q=" + keywordWithoutAccents.replaceAll(" ", "+") + "&ULTP=S";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("div.dg-boxproduto");

      // In this scraper we need scrap products total first
      // because we need this information for verify if the site returns products for the keyword we send
      // or returns suggestions (in this case we don't have total Products),
      // we don't scrap suggestions products
      if (this.totalProducts == 0) {
         this.setTotalProducts();
      }

      if (!products.isEmpty() && this.totalProducts > 0) {
         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, "h3.dg-boxproduto-titulo a", "href", "https", homePage);
            String internalId = crawlInternalId(productUrl);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h3.dg-boxproduto-titulo a", false);
            String imgUrl = scrapFullImage(e);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e,"span[class*=preco-por]", null, false, ',', session, null);

            boolean isAvailable = CrawlerUtils.scrapStringSimpleInfo(e, "span.dg-icon-avise-me", false) == null;
            if (isAvailable == false) {price = null;}

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
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
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, "h1.dg-categoria-desc", true, 0);
      this.log("Total: " + this.totalProducts);
   }

   /**
    * For scrap internal Id we need the url, is the only "safe" place
    * <p>
    * Ex: Internal Id will be found in the end of url
    * <p>
    * Url:
    * https://www.drogarianovaesperanca.com.br/naturais/fitoterapicos/comprar-prevelip-com-60-capsulas-16970/
    * <p>
    * InternalId: 16970
    *
    * @param url
    * @return
    */
   private String crawlInternalId(String url) {
      String internalId = null;

      String lastStringUrl = CommonMethods.getLast(url.split("\\?")[0].split("-"));

      if (lastStringUrl.contains("/")) {
         internalId = lastStringUrl.split("/")[0];
      }

      return internalId;
   }

   private String scrapFullImage (Element e) {
      String miniImage = CrawlerUtils.scrapSimplePrimaryImage(e, "a[class*=boxproduto-img] img", List.of("data-src-lazy"), "https", homePage);

      return miniImage.replaceAll("imagens/200x200/", "imagens-complete/445x445/");
   }
}
