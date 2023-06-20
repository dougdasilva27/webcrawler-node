package br.com.lett.crawlernode.crawlers.ranking.keywords.maceio;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.cookie.Cookie;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static br.com.lett.crawlernode.util.CrawlerUtils.setCookie;

public class MaceioPalatoCrawler extends CrawlerRankingKeywords {

   public MaceioPalatoCrawler(Session session) {
      super(session);
      fetchMode = FetchMode.FETCHER;
   }

   private String cookiesToString() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
      headers.put("Accept-Language", "pt-BR,pt;q=0.9");
      headers.put("Connection", "keep-alive");
      headers.put("Referer", session.getOriginalURL());
      headers.put("Upgrade-Insecure-Requests", "1");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://loja.palato.com.br?uf_verification=AL")
         .setHeaders(headers)
         .setSendUserAgent(true)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY
         ))
         .build();

      Response response = new ApacheDataFetcher().get(session, request);
      List<Cookie> cookiesResponse = response.getCookies();
      for (Cookie cookieResponse : cookiesResponse) {
         cookies.add(setCookie(cookieResponse.getName(), cookieResponse.getValue(), cookieResponse.getDomain(), cookieResponse.getPath()));
      }

      StringBuilder cookieStringBuilder = new StringBuilder();

      for (Cookie cookie : cookies) {
         cookieStringBuilder.append(cookie.getName())
            .append("=")
            .append(cookie.getValue())
            .append("; ");
      }

      String cookieString = cookieStringBuilder.toString();
      if (cookieString.endsWith("; ")) {
         cookieString = cookieString.substring(0, cookieString.length() - 2);
      }

      return cookieString;
   }

   private JSONObject fetchHtmlFromJSON(String url) {
      String cookieString = cookiesToString();
      String payload = "offset=" + (this.currentPage - 1) + "&more=1";

      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .uri(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Cookie", cookieString)
            .build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         String responseBody = response.body();
         JSONObject responseJson = new JSONObject(responseBody);
         return responseJson;
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      String keyword = this.keywordEncoded.replace(" ", "+");
      String url = "https://loja.palato.com.br/busca?q=" + keyword;

      this.log("Página" + this.currentPage);
      this.log("Link onde são feitos os crawlers: " + url);
      JSONObject objHtml = fetchHtmlFromJSON(url);

      String html = objHtml.getString("produtos");
      Document doc = Jsoup.parse(html);

      Elements products = doc.select(".item");

      if (!products.isEmpty()) {
         for (Element product : products) {
            String productUrl = CrawlerUtils.scrapUrl(product, "a", "href", "https", "loja.palato.com.br");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-options input[name='id']", "value");
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-options input[name='sku']", "value");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, "p.product-title", true);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(product, "a", Arrays.asList("src"), "https", "s3-sa-east-1.amazonaws.com");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, "p[class=\"product-price\"]", null, true, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
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
