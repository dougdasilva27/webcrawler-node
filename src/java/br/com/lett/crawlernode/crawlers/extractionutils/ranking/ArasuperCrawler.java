package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ArasuperCrawler extends CrawlerRankingKeywords {
   public ArasuperCrawler(Session session) {
      super(session);
   }

   @Override
   protected void processBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("referer", "https://www.arasuper.com.br/index/");

      String payload = "state=" + session.getOptions().optString("state");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.arasuper.com.br/home/state")
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      this.cookies = dataFetcher.post(session, request).getCookies();
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 20;

      String url = "https://www.arasuper.com.br/busca/?q=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.currentDoc = fetchDocument(url, this.cookies);

      Elements products = this.currentDoc.select(".products-list .item-produto");
      for (Element product : products) {
         String productUrl = CrawlerUtils.completeUrl(product.select("a").attr("href"), "https", "www.arasuper.com.br");
         String internalId = scrapInternalId(productUrl);
         Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".item-produto__price-por", null, false, ',', session, null);

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setName(CrawlerUtils.scrapStringSimpleInfo(product, ".item-produto__name", true))
            .setPriceInCents(price)
            .setAvailability(price != null)
            .setImageUrl(CrawlerUtils.scrapSimplePrimaryImage(product, ".item-produto__image", Arrays.asList("src"), "https", "www.arasuper.com.br"))
            .build();

         saveDataProduct(productRanking);
      }

   }

   private String scrapInternalId(String productUrl) {
      String[] productUrlSplit = productUrl.split("/");
      return productUrlSplit[productUrlSplit.length - 1];
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
