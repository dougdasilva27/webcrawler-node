package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilBalarotiCrawler extends CrawlerRankingKeywords {

   public BrasilBalarotiCrawler(Session session) {
      super(session);
   }

   private boolean hasNextPage = true;

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {

      this.pageSize = 16;

      String url = "https://busca.balaroti.com.br/busca?q=" + this.keywordEncoded;

      this.log("Página " + this.currentPage);

      Document doc = fetchDocument(url);

      Elements products = doc.select(".neemu-products-container li");

      if (!products.isEmpty()) {
         for (Element product : products) {

            String productUrl = "https:" + CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "a", "href");
            String internalPid = crawlInternalPid(productUrl);
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".nm-product-name", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".nm-product-img", Collections.singletonList("src"), "https", "balaroti.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".nm-price-value", null, false, ',', session, 0);
            boolean isAvailable = price != 0;

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

   private String crawlInternalPid(String productUrl) {
      Pattern p = Pattern.compile("-([0-9]*)\\/p");
      Matcher m = p.matcher(productUrl);
      if (m.find()) {
         return m.group(1);
      }
      return null;
   }
}
