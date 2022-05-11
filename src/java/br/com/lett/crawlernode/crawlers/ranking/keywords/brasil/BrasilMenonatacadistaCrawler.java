package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilMenonatacadistaCrawler extends CrawlerRankingKeywords {
   public BrasilMenonatacadistaCrawler(Session session) {
      super(session);
   }
   private String cookiePHPSESSID = null;

   @Override
   protected void processBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      headers.put("authority","www.menonatacadista.com.br");
      String payloadString = "email=paulo.carvalho%40mdlz.com&password=c9d59";

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.menonatacadista.com.br/index.php?route=account/login")
         .setPayload(payloadString)
         .setHeaders(headers)
         .setFollowRedirects(false)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.BONANZA,
            ProxyCollection.LUMINATI_SERVER_BR_HAPROXY
         ))
         .build();
      Response response = CrawlerUtils.retryRequest(request, session, dataFetcher);

      List<Cookie> cookiesResponse = response.getCookies();

      for (Cookie cookieResponse : cookiesResponse) {
         if (cookieResponse.getName().equalsIgnoreCase("PHPSESSID")) {
            this.cookiePHPSESSID = cookieResponse.getValue();
         }
      }
   }

   @Override
   protected Document fetchDocument(String url, List<Cookie> cookies) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "PHPSESSID=" + this.cookiePHPSESSID + ";");

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(url)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.BONANZA,
            ProxyCollection.LUMINATI_SERVER_BR_HAPROXY
         ))
         .build();

      return Jsoup.parse(new ApacheDataFetcher().get(session, request).getBody());
   }


   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 100;
      this.log("Página " + this.currentPage);
      String url = "https://www.menonatacadista.com.br/index.php?route=product/search&search="+ this.keywordEncoded+"&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div.products-list-item");
      pageSize = products.size();

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String code = CrawlerUtils.scrapStringSimpleInfo(e, "h4.caption small",true);
            String internalId = getcodeId(code);
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "h4.caption a", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h4.caption a", true);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "div.qty-price .price", null, false, ',', session, null);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, "div.image img ", Arrays.asList("src"), "https", "www.menonatacadista.com.br");
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setImageUrl(imageUrl)
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

   private String getcodeId(String code) {
      String regex = "[0-9]+";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(code);
      return String.valueOf(matcher);
   }


}
