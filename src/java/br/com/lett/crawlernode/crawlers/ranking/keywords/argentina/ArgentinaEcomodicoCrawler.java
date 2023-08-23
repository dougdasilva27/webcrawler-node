package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgentinaEcomodicoCrawler extends CrawlerRankingKeywords {
   public ArgentinaEcomodicoCrawler(Session session) {
      super(session);
   }
   int aux = 1;

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 50;

      //https://www.ecomodico.com.ar/bebe_NoIndex_True
      //https://www.ecomodico.com.ar/bebe_Desde_51_NoIndex_True

      String url = "https://www.ecomodico.com.ar/" + this.keywordWithoutAccents.replace(" ", "-") + "_Desde_" + aux + "_NoIndex_True";
      aux = aux + 50;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      this.log("Página " + this.currentPage);
      Elements products = this.currentDoc.select(".ui-search-layout__item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0)
            setTotalProducts();

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, ".ui-search-link", "href", "https", "www.ecomodico.com.ar");
            String internalPid = regexInternalId(productUrl);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h2.ui-search-item__title", true);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".ui-search-result-image__element", Collections.singletonList("src"), "https", "http2.mlstatic.com");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".andes-money-amount__fraction", null, false, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
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

   private String regexInternalId(String productUrl) {
      String regex = "\\/p\\/([A-Z0-9]+)";
      if (productUrl != null) {
         Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         Matcher matcher = pattern.matcher(productUrl);
         if (matcher.find()) {
            return matcher.group(1);
         }
      }
      return null;
   }

   @Override
   protected void setTotalProducts() {
      String strTotal = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, ".ui-search-search-result__quantity-results", true).replace(" resultados", "");
      try {
         int number = Integer.parseInt(strTotal);
         this.totalProducts = number;
      } catch (NumberFormatException e) {
         System.out.println("Invalid number format");
      }
   }
}
