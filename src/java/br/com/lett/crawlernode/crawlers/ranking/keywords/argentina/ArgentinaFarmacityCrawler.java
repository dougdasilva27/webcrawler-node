package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

import java.net.MalformedURLException;
import java.util.Arrays;

public class ArgentinaFarmacityCrawler extends CrawlerRankingKeywords {

   public ArgentinaFarmacityCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);
      this.pageSize = 20;

      String url = "https://www.farmacity.com/" + this.keywordWithoutAccents.replace(" ", "") + "# " + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".main .prateleira ul > li[layout]");
      Elements productsPid = this.currentDoc.select(".prateleira ul > li[id]");

      int count = 0;

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalPid = crawlInternalPid(productsPid.get(count));
            String productUrl = crawlProductUrl(e);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, " a.product-card-information > div.product-card-name", true);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "a.product-card-information span.best-price", null, false, ',', session, null);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, "span.primary-image img", Arrays.asList("src"), "https", "www.farmacity.com");
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setImageUrl(imageUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
            count++;
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
      Element total = this.currentDoc.select(".resultado-busca-numero .value").first();

      if (total != null) {
         String text = total.ownText().replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            this.totalProducts = Integer.parseInt(text);

            this.log("Total products: " + this.totalProducts);
         }
      }
   }

   private String crawlInternalPid(Element e) {
      return CommonMethods.getLast(e.attr("id").split("_"));
   }

   private String crawlProductUrl(Element e) {
      String productUrl = null;
      Element href = e.selectFirst(".product-card-head");

      if (href != null) {
         productUrl = href.attr("href");
      }

      return productUrl;
   }
}
