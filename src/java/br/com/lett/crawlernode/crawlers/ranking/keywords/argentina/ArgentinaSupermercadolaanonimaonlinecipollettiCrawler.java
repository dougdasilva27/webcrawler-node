package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgentinaSupermercadolaanonimaonlinecipollettiCrawler extends CrawlerRankingKeywords {

   private List<Cookie> cookies = new ArrayList<>();

   public ArgentinaSupermercadolaanonimaonlinecipollettiCrawler(Session session) {

      super(session);
      super.fetchMode = FetchMode.FETCHER;

   }

   @Override
   protected Document fetchDocument(String url, List<Cookie> cookies) {
      this.currentDoc = new Document(url);

      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }

      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", " laanonimasucursalnombre=CIPOLLETTI; laanonimasucursal=22");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      Response response = new ApacheDataFetcher().get(session, request);

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      // número de produtos por página do market
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      String url = "https://supermercado.laanonimaonline.com/buscar?pag=" + this.currentPage + "&clave=" + this.keywordWithoutAccents.replace(" ", "%20");

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url, cookies);
      Elements products = this.currentDoc.select(".caja1.producto > div");

      if (!products.isEmpty()) {

         for (Element e : products) {
            String productUrl = crawlProductUrl(e);
            String internalId = crawlInternalId(productUrl);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "div.titulo_puntos a", true);
            Integer price = crawlPrice(e);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, "div.producto > div > div > a > img", Arrays.asList("src"), "https", "d1on8qs0xdu5jz.cloudfront.net");
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
               .setImageUrl(imageUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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

   private String crawlInternalId(String url) {
      String internalId = null;

      if (url.contains("art_")) {
         internalId = CommonMethods.getLast(url.split("art_"));

         if (internalId.contains("?")) {
            internalId = internalId.split("\\?")[0];
         }

         if (internalId.contains("/")) {
            internalId = internalId.split("\\/")[0];
         }
      }

      return internalId;
   }

   private String crawlProductUrl(Element e) {

      String url = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".titulo_puntos a","href");
      String productUrl = CrawlerUtils.completeUrl(url,"https:","supermercado.laanonimaonline.com");

      return productUrl;
   }

   private Integer crawlPrice(Element e) {
      String priceStr = CrawlerUtils.scrapStringSimpleInfo(e, ".precio.semibold", true);
      String priceDecimal = CrawlerUtils.scrapStringSimpleInfo(e, ".decimales", true);
      String priceComplete = priceStr + priceDecimal;
      Double price = MathUtils.parseDoubleWithComma(priceComplete);

      return price != null ? (int) Math.round(price * 100) : null;
   }
}
