package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BrasilDutramaquinasCrawler extends CrawlerRankingKeywords {

   public BrasilDutramaquinasCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      String url = "https://busca.dutramaquinas.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".neemu-products-container .nm-product-item");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = crawlInternalId(e.attr("id"));
            String urlProduct = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".nm-product-info a", "href"), "https", "www.dutramaquinas.com");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".nm-product-name", false);
            String imgUrl = scrapImage(e);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".nm-price-container", null, false, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(urlProduct)
               .setInternalId(internalId)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String scrapImage(Element e) {
      String image = CrawlerUtils.scrapSimplePrimaryImage(e, ".nm-product-img", Collections.singletonList("src"), "https", "images.colombo.com.br");

      if (image != null && image.contains("/grande/")) {
         image = image.replace("/grande/", "/alta/");
      }

      return image;
   }

   private String crawlInternalId(String text) {
      String internalId = "";

      if (text != null && !text.isEmpty()) {
         try {
            internalId = text.split("-")[2];
         } catch (Exception e) {
            internalId = text.replaceAll("[^0-9]+", "");
         }
      }
      return internalId;
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.select(".neemu-pagination .neemu-pagination-inner a") != null;
   }

}
