package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BrasilMaxAtacadistaCrawler extends CrawlerRankingKeywords {
   public BrasilMaxAtacadistaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://delivery.maxatacadista.com.br/buscapagina?ft="+this.keywordEncoded+"&PS=48&sl=d85149b5-097b-4910-90fd-fa2ce00fe7c9&cc=48&sm=0&PageNumber="+this.currentPage;
      this.currentDoc = fetch(url);
      Elements products = this.currentDoc.select("ul li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = products.size();
         }

         for (Element e : products) {
            Elements valid = e.select(".row");
            if(!valid.isEmpty()){
               String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".col-xs-5.col-sm-12.prd-list-item-img a", "href");
               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".col-sm-6.col-lg-4.col-xl-3.prd-list-item","data-product-id");;
               String name = CrawlerUtils.scrapStringSimpleInfo(e,".prd-list-item-name", false);
               String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".prd-list-item-link img", Arrays.asList("src"), "https", "");
               Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".prd-list-item-price .prd-list-item-price-sell", null, false, ',', session, null);

               boolean isAvailable = price != null;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setName(name)
                  .setImageUrl(imgUrl)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(productRanking);


            }

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }

         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }


   }
   protected Document fetch(String url) {
      Map<String, String> headers = new HashMap<>();

      headers.put("Accept","*/*");
      headers.put("Accept-Encoding","gzip, deflate, br");
      headers.put("Connection","keep-alive");
      headers.put("cookie",session.getOptions().optString("cookie"));

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Collections.singletonList(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .setSendUserAgent(false)
         .build();

      Response a = this.dataFetcher.get(session, request);

      String content = a.getBody();

      return Jsoup.parse(content);
   }
   @Override
   protected boolean hasNextPage() {
      return arrayProducts.size() < this.totalProducts;
   }
}
