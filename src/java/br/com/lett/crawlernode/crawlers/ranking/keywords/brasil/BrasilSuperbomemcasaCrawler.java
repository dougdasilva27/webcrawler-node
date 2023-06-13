package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;

public class BrasilSuperbomemcasaCrawler extends CrawlerRankingKeywords {

   public BrasilSuperbomemcasaCrawler(Session session) {
      super(session);
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {

      String url = "https://www.redesuperbom.com.br/busca/?q=" + this.keywordEncoded + "&page=" + this.currentPage;

      Document doc = fetchDocument(url);

      Elements products = doc.select(".products-list > .item-produto");

      for (Element product : products) {
         String dataTrackingStr = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "> a", "data-tracking");
         JSONObject dataTrackingJson = JSONUtils.stringToJson(dataTrackingStr);
         String href = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "> a", "href");

         String productUrl = CrawlerUtils.completeUrl(href, "https", "www.redesuperbom.com.br");
         String internalId = dataTrackingJson.optString("id");
         String name = dataTrackingJson.optString("name");
         Integer price = JSONUtils.getPriceInCents(dataTrackingJson, "price", Integer.class, null);
         String imageUrl = scrapImage(product);

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setName(name)
            .setPriceInCents(price)
            .setAvailability(price != null)
            .setImageUrl(imageUrl)
            .build();

         saveDataProduct(productRanking);

         if (this.arrayProducts.size() == productsLimit) {
            break;
         }
      }
   }

   private String scrapImage(Element product) {
      String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(product, "> a > .item-produto__img > img", List.of("src"), "https", "www.redesuperbom.com.br");

      return imgUrl != null ? imgUrl.replace("media", "uploads").replaceFirst("/\\d{3,4}x\\d{3,4}", "") : null;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
