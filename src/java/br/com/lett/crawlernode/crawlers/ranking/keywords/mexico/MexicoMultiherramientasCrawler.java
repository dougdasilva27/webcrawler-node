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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(List.of(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new JsoupDataFetcher(), new ApacheDataFetcher(), new FetcherDataFetcher()), session, "post");

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 25;
      String url = "https://multiherramientas.mx/loadproducts.php";
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".producto");

      if (products != null && !products.isEmpty()) {
         for (Element product : products) {
            String internalPid = scrapInternalPid(product);
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".noRastro .label", true);
            String productUrl = CrawlerUtils.scrapUrl(product, "a.noRastro", "href", "https", "multiherramientas.mx");
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(product, "a.noRastro figure img", Arrays.asList("src"), "https", "multiherramientas.mx");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, "span.precio", null, true, '.', session, null);
            boolean available = price != null;

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

   private String scrapInternalPid(Element product) {
      String text = CrawlerUtils.scrapStringSimpleInfo(product, ".noRastro .marca", true);
      String regex = "\\| ([0-9]*)";

      if (text != null) {
         Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         Matcher matcher = pattern.matcher(text);
         if (matcher.find()) {
            return matcher.group(1);
         }
      }
      return null;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
