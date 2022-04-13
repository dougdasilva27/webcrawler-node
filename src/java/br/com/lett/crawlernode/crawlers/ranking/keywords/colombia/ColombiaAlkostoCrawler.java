package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColombiaAlkostoCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "www.alkosto.com";

   public ColombiaAlkostoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 25;
      this.log("Página " + this.currentPage);

      String url = "https://www.alkosto.com/search/?text=" + this.keywordEncoded + "&page=" + this.currentPage + "&pageSize=25&sort=relevance";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("ul.product__listing.product__list > li.product__list--item");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "h2.product__information--name > a.js-product-click-datalayer", "data-id");
            String productUrl = CrawlerUtils.scrapUrl(e, "h2.product__information--name > a.js-product-click-datalayer", Collections.singletonList("href"), "https", HOME_PAGE);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h2.product__information--name > a.js-product-click-datalayer", true);
            Integer price = crawlPrice(e);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, "div.product__image__container > img", Arrays.asList("data-src"), "https", "www.alkosto.com");
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


            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      String totalProductsStr = CrawlerUtils.scrapStringSimpleInfoByAttribute(this.currentDoc, "span.js-search-count", "data-count");

      if(totalProductsStr != null){
         int totalProducts = Integer.parseInt(totalProductsStr);
         return this.arrayProducts.size() < totalProducts;
      }

      return false;
   }

   private Integer crawlPrice(Element e){
      String priceStr = CrawlerUtils.scrapStringSimpleInfo(e, "span.price", true);
      Integer priceInCents = 0;

      if (priceStr != null) {
         final String regex = "[0-9]+\\.[0-9]++";
         final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         final Matcher matcher = pattern.matcher(priceStr);

         if (matcher.find()) {
            priceStr = matcher.group(0);
            priceInCents = CommonMethods.stringPriceToIntegerPrice(priceStr, '.', 0);
         }
      }
      return priceInCents;
   }
}
