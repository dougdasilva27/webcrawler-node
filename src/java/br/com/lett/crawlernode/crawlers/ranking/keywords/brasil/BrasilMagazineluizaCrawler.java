package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilMagazineluizaCrawler extends CrawlerRankingKeywords {

   public BrasilMagazineluizaCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {
      Document doc;
      int attempts = 0;
      Map<String, String> headers = new HashMap<>();
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36");
      headers.put("authority", "www.magazineluiza.com.br");
      headers.put("accept-encoding", "gzip, deflate, br");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");

      do {
         Request request = Request.RequestBuilder.create()
            .setUrl(url)
            .setProxyservice(Arrays.asList(
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES,
               ProxyCollection.NETNUT_RESIDENTIAL_MX,
               ProxyCollection.NETNUT_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.SMART_PROXY_BR_HAPROXY
            ))
            .setHeaders(headers)
            .setSendUserAgent(false)
            .setFetcheroptions(
               FetcherOptions.FetcherOptionsBuilder.create()
                  .setForbiddenCssSelector("#recaptcha_response")
                  .build()
            ).build();

         Response response = new ApacheDataFetcher().get(session, request);
         doc = Jsoup.parse(response.getBody());

         attempts++;

         if (attempts == 3) {
            if (isBlockedPage(doc)) {
               Logging.printLogInfo(logger, session, "Blocked after 3 retries.");
            }
            break;
         }
      }
      while (isBlockedPage(doc));

      return doc;
   }

   private boolean isBlockedPage(Document doc) {

      return doc.toString().contains("We are sorry") || doc.selectFirst("div") == null;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 60;
      this.log("Página " + this.currentPage);

      String url = "https://www.magazineluiza.com.br/busca/" + keywordEncoded + "?page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements elements = this.currentDoc.select("div[data-testid='product-list'] li");

      if (!elements.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : elements) {

            String urlProduct = CrawlerUtils.scrapUrl(e, "> a", "href", "https", "www.magazineluiza.com.br");
            String internalPid = getProductPid(urlProduct);
            String imageUrl = CrawlerUtils.scrapUrl(e, "img", "src", "https", "a-static.mlcdn.com.br");
            int price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "p[data-testid='price-value']", null, true, ',', session, 0);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h2", true);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(urlProduct)
               .setInternalPid(internalPid)
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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "div[data-testid='mod-searchheader'] p", true, 0);
      this.log("Total: " + this.totalProducts);
   }

   private String getProductPid(String url) {
      String id = null;
      Pattern pattern = Pattern.compile("p\\/([a-z-0-9]+)\\/");
      Matcher matcher = pattern.matcher(url);
      if (matcher.find()) {
         id = matcher.group(1);
      }
      return id;
   }
}
