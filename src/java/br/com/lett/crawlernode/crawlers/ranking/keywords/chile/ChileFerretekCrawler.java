package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import java.util.*;

public class ChileFerretekCrawler extends CrawlerRankingKeywords {
   public ChileFerretekCrawler(Session session) {
      super(session);
//      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected Document fetchDocument(String url) {
      this.currentDoc = new Document(url);

      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }


      Map<String,String> headers = new HashMap<>();
//      headers.put("Cookies", "frontend_lang=es_CL; session_id=48493f26b2b0b2fc8207742e47ee45c61d158c94");
//      headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36");
//      headers.put("Accept","*/*");
//      headers.put("Referer", "https://herramientas.cl/");
//      headers.put("Accept-Encoding", "gzip, deflate, br");
//      headers.put("Connection", "keep-alive");
      headers.put("Host", "herramientas.cl");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setTimeout(60000)
         .setCookies(this.cookies)
         .setProxyservice(List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.BONANZA, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY))
         .build();

      Response response = dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void processBeforeFetch() {
      this.cookies = CrawlerUtils.fetchCookiesFromAPage( "https://herramientas.cl",null,"www.herramientas.cl", "/", null, session, dataFetcher);
   }
   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 20;

      this.log("Página : " + this.currentPage);

      String url = "https://herramientas.cl/shop/page/" + currentPage + "?search=" + keywordEncoded;
      this.log("URL : " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".oe_product_cart");

      if(!products.isEmpty()) {
         for(Element product : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "button", "data-product-id");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".card-body > a", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".card-body h6", true);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".card-body img", Collections.singletonList("src"), "https", "herramientas.cl");
            Integer price = CrawlerUtils.scrapIntegerFromHtml(product, ".oe_currency_value", true, 0);
            boolean isAvailable = checkIfIsAvailable(product);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultados!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   private boolean checkIfIsAvailable(Element product) {
      return product.select(".out-stock-msg").isEmpty();
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".pagination .page-item:last-child").hasClass("disabled");
   }
}
