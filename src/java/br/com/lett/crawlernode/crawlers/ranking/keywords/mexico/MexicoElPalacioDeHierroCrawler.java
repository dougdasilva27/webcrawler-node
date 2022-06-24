package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class MexicoElPalacioDeHierroCrawler extends CrawlerRankingKeywords {

   public MexicoElPalacioDeHierroCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected Document fetchDocument(String url, List<Cookie> cookies) {

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(this.cookies)
         .build();

      return Jsoup.parse(new ApacheDataFetcher().get(session, request).getBody());
   }

   private static final String HOME_PAGE = "https://www.elpalaciodehierro.com/";
   int valNext= 0;
   //https://www.elpalaciodehierro.com/on/demandware.store/Sites-palacio-MX-Site/es_MX/Search-UpdateGrid?q=hombre&start=51&sz=51&ajax=true
   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 51;
      this.log("Página " + this.currentPage);
      String url = HOME_PAGE + "on/demandware.store/Sites-palacio-MX-Site/es_MX/Search-UpdateGrid?q=" + this.keywordWithoutAccents + "&start=" + valNext + "&sz=" + this.pageSize + "&ajax=true";
      valNext += this.pageSize;
      this.log("URL : " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div.b-product");
      pageSize = products.size();

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.b-product" ,"data-pid");
            String productUrl = HOME_PAGE + e.select("a.b-product_tile-name").attr("href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "a.b-product_tile-name", true);
            String image = CrawlerUtils.scrapSimplePrimaryImage(e, "picture.b-product_image source", Collections.singletonList("data-srcset"), "https", "www.elpalaciodehierro.com");
            Integer priceInCents = CrawlerUtils.scrapPriceInCentsFromHtml(e, "div.b-product_price-sales span.b-product_price-value", null, false, '.', session, 0);
            boolean available = priceInCents != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(available)
               .build();

            saveDataProduct(productRanking);
         }
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, "div.b-results_found", true, 0);
      this.log("Total: " + this.totalProducts);
   }
   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
