package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.HttpHeaders;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MexicoMultiherramientasCrawler extends CrawlerRankingKeywords {

   public MexicoMultiherramientasCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected Document fetchDocument(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "multiherramientas.mx");
      headers.put(HttpHeaders.ACCEPT, "*/*");
      headers.put("x-requested-with", "XMLHttpRequest");

      String payload = "price_level=2&order=1&page=" + this.currentPage + "&limit=25&pmin=&pmax=&ask0=&ask1=&ask2=&tspecial=" + this.keywordEncoded.replace(" ", "+");

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setPayload(payload)
         .setUrl(url)
         .setProxyservice(List.of(ProxyCollection.SMART_PROXY_MX, ProxyCollection.SMART_PROXY_MX_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_MX, ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new ApacheDataFetcher(), new FetcherDataFetcher(), new JsoupDataFetcher()), session, "post");

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 25;
      String url = "https://multiherramientas.mx/loadproducts.php";
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("div[class=\"adv-product produc \"]");

      if (products != null && !products.isEmpty()) {
         for (Element product : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".options > button", "data-fkproduct");
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, "div > span.label > a", true);
            String productUrl = CrawlerUtils.scrapUrl(product, "div > span.label > a", "href", "https", "multiherramientas.mx");
            String imageUrl = getImageUrl(product);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".price > a > span.final", null, false, '.', session, null);
            boolean available = price > 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(productName)
               .setPriceInCents(price)
               .setAvailability(available)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página: " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select("button[onclick=\"loadmoreproducts()\"]").isEmpty();
   }

   private String getImageUrl(Element element) {
      String imageUrl = null;
      String[] arrayImage;
      String selectorImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, "div[class=\"adv-product produc \"]  > figure.text-center", "style");
      if (selectorImage != null && !selectorImage.isEmpty()) {
         arrayImage = selectorImage.split(" ");
         if (arrayImage.length > 1 && arrayImage != null) {
            imageUrl = arrayImage[1].replace("url(/", "").replace(")", "");
         }
      }
      return CrawlerUtils.completeUrl(imageUrl, "https", "multiherramientas.mx");
   }
}
