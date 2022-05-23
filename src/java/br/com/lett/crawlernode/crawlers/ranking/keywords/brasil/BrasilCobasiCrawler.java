package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilCobasiCrawler extends CrawlerRankingKeywords {

   public BrasilCobasiCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://busca.cobasi.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div.ProductListItem");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, "div.ProductListItem a", "href", "https", "www.cobasi.com.br");
            String internalPid = crawlInternalPid(productUrl);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "div.ProductListItem div[class*=Title]", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, "div.ProductListItem img[decoding*=async]", Collections.singletonList("src"), "https", "cobasi.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "div[class*=PriceBox] span[data-testid*=product-price]", null, true, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
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
      Element totalElement = this.currentDoc.select("div[class*=TotalDescription]").first();

      if (totalElement != null) {
         String total = totalElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!total.isEmpty()) {
            this.totalProducts = Integer.parseInt(total);
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalPid(String url) {
      String id = null;
      Pattern pattern = Pattern.compile("-([0-9]+)\\/p");
      Matcher matcher = pattern.matcher(url);
      if (matcher.find()) {
         id = matcher.group(1);
      }
      return id;
   }

}
