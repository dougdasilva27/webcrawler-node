package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilB2WLojistasCrawler extends CrawlerRankingKeywords {

   private String store = getStore();
   private String homePage = getHomePage();


   public String getStore() {
      return session.getOptions().optString("store");
   }

   public String getHomePage() {
      return "www." + store + ".com.br";
   }

   public BrasilB2WLojistasCrawler(Session session) {
      super(session);
   }


   @Override
   protected void processBeforeFetch() {
      String urlAmericanas = "https://www.americanas.com.br";
      Request request = Request.RequestBuilder.create()
         .setHeaders(B2WCrawler.getHeaders())
         .setUrl(urlAmericanas)
         .setProxyservice(List.of(
            ProxyCollection.SMART_PROXY_MX_HAPROXY,
            ProxyCollection.SMART_PROXY_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY
         ))
         .build();
      Response response = CrawlerUtils.retryRequest(request, session, new ApacheDataFetcher(), true);
      List<Cookie> cookiesResponse = response.getCookies();
      for (Cookie cookieResponse : cookiesResponse) {
         this.cookies.add(CrawlerUtils.setCookie(cookieResponse.getName(), cookieResponse.getValue(), CommonMethods.getLast(urlAmericanas.split("//")), "/"));
      }
   }

   @Override
   protected Document fetchDocument(String url) {
      this.log("Link onde são feitos os crawlers: " + url);

      Map<String, String> headers = new HashMap<>();

      headers.put("authority", homePage);
      headers.put("upgrade-insecure-requests", "1");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,/;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setSendUserAgent(false)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .setForbiddenCssSelector("#px-captcha")
               .build()
         )
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_MX,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY
            )
         )
         .setCookies(this.cookies)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);

      return Jsoup.parse(response.getBody());

   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = getUrl();

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("[class^=\"inStockCard\"]");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalPid = scraperInternalPid(e);
            String productUrl = CrawlerUtils.completeUrl("produto/" + internalPid, "https", homePage);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "[class^=\"product-name\"]", true);
            Integer price = CrawlerUtils.scrapIntegerFromHtml(e, "[class^=\"price-info\"] span", true, 0);
            String imageUrl = scraperImage(internalPid, name);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   protected String getUrl() {
      String url = "";
      String keyword = this.keywordWithoutAccents.replace(" ", "-");

      if (this.currentPage == 1) {
         url = "https://www." + store + ".com.br/lojista/" + session.getOptions().optString("lojista") + "?ordenacao=relevance&conteudo=" + keyword;
      } else {
         url = "https://www." + store + ".com.br/lojista/" + session.getOptions().optString("lojista") + "/pagina-" + this.currentPage + "?conteudo=" + keyword + "&ordenacao=relevance";
      }
      return url;
   }


   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".pagination-product-grid.pagination li span .svg-icon.svg-icon-right.svg-icon-sm").isEmpty();
   }

   private String scraperImage(String internalPid, String name) {
      String slug = name.toLowerCase(Locale.ROOT).replace(" ", "-");
      return "https://images-" + store + ".b2w.io/produtos/" + internalPid + "/imagens/" + slug + "/" + internalPid + "_1_large.jpg";
   }

   private String scraperInternalPid(Element e) {
      String internalPid = "";
      String pid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "[class^=\"inStockCard\"] a", "href");

      if (pid != null) {
         String regex = "produto\\/([0-9]+)";
         Pattern pattern = Pattern.compile(regex);
         Matcher matcher = pattern.matcher(pid);

         if (matcher.find()) {
            internalPid = matcher.group(1);
         }
      }
      return internalPid;
   }
}
