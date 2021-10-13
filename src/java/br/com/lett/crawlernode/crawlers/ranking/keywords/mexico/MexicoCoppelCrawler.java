package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class MexicoCoppelCrawler extends CrawlerRankingKeywords{
   protected Integer PRODUCT_ID_SIZE = 7;
   protected Integer PRODUCTS_PER_PAGE = 12;


   private static final String HOME_PAGE = "https://www.coppel.com/";

   public MexicoCoppelCrawler(Session session) {
      super(session);
      dataFetcher = new JsoupDataFetcher();
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);
      Integer currentPageUrl = (this.currentPage - 1) * PRODUCTS_PER_PAGE;

      String url = HOME_PAGE + "SearchDisplay?categoryId=&storeId=10151&sType=SimpleSearch&showResultsPage=true&beginIndex=" +
         currentPageUrl + "&searchSource=Q&pageView=&pageGroup=Search&beginIndex=0&searchTerm=" +
         this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetch(url);

      Elements products = this.currentDoc.select(".product_listing_container li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = products.size();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".product_name a", "href");
            String internalId = scrapInternalId(e);
            String name = CrawlerUtils.scrapStringSimpleInfo(e,".product_name p", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product_image a img", Arrays.asList("data-original"), "https", "");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".unique_price", null, false, ',', session, 0);

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
                  " - Url: " + productUrl);

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

   private String scrapInternalId(Element doc) {
      String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,".product_name a", "href");

      return productUrl.substring(productUrl.length() - PRODUCT_ID_SIZE);
   }

   protected Document fetch(String url) {
      Map<String, String> headers = new HashMap<>();

      headers.put("Accept","*/*");
      headers.put("Accept-Encoding","gzip, deflate, br");
      headers.put("Connection","keep-alive");

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
   protected boolean hasNextPage(){
      return true; //TODO: verificação de paginação
   }

}
