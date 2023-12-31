package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class KochCrawlerRanking extends CrawlerRankingKeywords {
   protected String storeId;

   public KochCrawlerRanking(Session session) {
      super(session);
      this.storeId = session.getOptions().optString("storeId");
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);
      this.currentDoc = fetchProducts();

      Elements elements = this.currentDoc.select(".item.product.product-item");

      if (!elements.isEmpty()) {
         for (Element e : elements) {
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-info a", "href");
            String internalId = CommonMethods.getLast(productUrl.split("-"));
            String name = CrawlerUtils.scrapStringSimpleInfo(e,".product.name.product-item-name .product-item-link", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image-photo", Arrays.asList("src"), "", "");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price-wrapper ", "data-price-amount", true, '.', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + internalId +
                  " - InternalPid: " + null +
                  " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit)
               break;
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
      return !this.currentDoc.select(".item.pages-item-next").isEmpty();
   }

   private Document fetchProducts() {

      String url = "https://www.superkoch.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;

      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", "customer_website=website_lj" + storeId);

      Request req = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      String content = this.dataFetcher.get(session, req).getBody();


      return Jsoup.parse(content);
   }
}
