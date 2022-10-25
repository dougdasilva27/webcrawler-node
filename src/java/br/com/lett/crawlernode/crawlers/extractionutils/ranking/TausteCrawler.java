package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;

public class TausteCrawler extends CrawlerRankingKeywords {

   private final String searchUrl = "https://tauste.com.br/";

   private final String location = getLocation();
   protected String getLocation() {
      return session.getOptions().optString("LOCATION");
   }

   public TausteCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      totalProducts = 43;
      this.log("Página " + this.currentPage);
      String url = searchUrl + location + "/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-item-info");
      pageSize = products.size();

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-info .product-image-photo", "alt");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-photo", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-item-name .product-item-link", false);
            String image = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image-photo", Collections.singletonList("src"), "https", "tauste.com.br");
            Integer priceInCents = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price-wrapper span", null, true, ',', session, null);
            boolean available = priceInCents != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(available)
               .build();

            saveDataProduct(productRanking);
         }
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".item.pages-item-next").isEmpty();
   }
}
