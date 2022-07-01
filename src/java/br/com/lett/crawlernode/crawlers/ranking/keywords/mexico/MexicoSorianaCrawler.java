package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.HttpHeaders;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import schemasMicrosoftComOfficeOffice.STInsetMode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MexicoSorianaCrawler extends CrawlerRankingKeywords {

   public MexicoSorianaCrawler(Session session) {
      super(session);
      fetchMode = FetchMode.APACHE;
   }

   private Map<String, String> getHeaders() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("origin", "https://cruzverde.cl");
      headers.put("authority", "api.cruzverde.cl");
      headers.put("referer", "https://cruzverde.cl/");
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");

      return headers;
   }

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();
      Response response;
      String postalCode = session.getOptions().optString("postalCode");
      if (postalCode.isEmpty()) {
         Request request = Request.RequestBuilder.create().setUrl("https://www.soriana.com/").build();
         response = this.dataFetcher.get(session, request);
      } else {
         Request request = Request.RequestBuilder.create()
            .setUrl("https://www.soriana.com/on/demandware.store/Sites-Soriana-Site/default/Stores-UpdateStoreByPostalCode")
            .setHeaders(getHeaders())
            .setProxyservice(Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES,
               ProxyCollection.NETNUT_RESIDENTIAL_BR
            ))
            .setSendUserAgent(true)
            .setPayload("dwfrm_storeUpdate_postalCode=" + "&basketValidation=true&selectSubmitPc=true&methodid=homeDelivery")
            .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create().mustUseMovingAverage(false).mustRetrieveStatistics(true).build())
            .build();
         response = this.dataFetcher.post(session, request);
      }
      this.cookies = response.getCookies();
   }

   @Override
   protected Document fetchDocument(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("Accept", "*/*");
      headers.put("cookie", CommonMethods.cookiesToString(this.cookies));

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build();

      String content = this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(content);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;

      this.log("Página " + this.currentPage);
      String keyword = this.keywordEncoded.replace(" ", "+");
      //https://www.soriana.com/buscar?q=vino&cid=&search-button=&cref=0&view=grid
      String url = "https://www.soriana.com/on/demandware.store/Sites-Soriana-Site/default/Search-UpdateGrid?q=" + keyword + "&pmin=0.01&start=" + (this.currentPage - 1) * 12 + "&sz=12&selectedUrl=https://www.soriana.com/on/demandware.store/Sites-Soriana-Site/default/Search-UpdateGrid?q=" + keyword + "&pmin=0%2e01&start=" + (this.currentPage - 1) * 12 + "&sz=12";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-tile--wrapper.d-flex");

      if (products.size() >= 1) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product", "data-pid");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".tile-body.product-tile--body.w-100.p-0 > div.pdp-link.product-tile--name > a", true);
            String productUrl = CrawlerUtils.scrapUrl(e, ".image-container a", "href", "https", "www.soriana.com");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".tile-body.product-tile--body.w-100.p-0 > div.price.product-tile--price.p-0.border-x-1.pb-1 > div > div > div > span > span", null, true, ',', session, null);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".w-100.image-container.product-tile--image-container.d-flex.align-items-center.justify-content-start.border-x-1.mt-1.border-t-1 > a.w-100.justify-content-center > img", "data-src");
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
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

   @Override
   protected boolean hasNextPage() {
      return true;

   }

}
