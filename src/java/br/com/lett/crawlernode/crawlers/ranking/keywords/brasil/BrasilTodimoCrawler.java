package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class BrasilTodimoCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.todimo.com.br/";
   private final String vtexSegment = this.session.getOptions().optString("vtex_segment");
   private final String zipcode = this.session.getOptions().optString("jb_zipcode");
   private String cookiesHeader = "";

   public BrasilTodimoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie cookie1 = new BasicClientCookie("vtex_segment", vtexSegment);
      cookie1.setDomain("www.todimo.com.br");
      cookie1.setPath("/");
      BasicClientCookie cookie2 = new BasicClientCookie("jb-zipCodeCurrent", zipcode);
      cookie2.setDomain("www.todimo.com.br");
      cookie2.setPath("/");

      this.cookies.add(cookie1);
      this.cookies.add(cookie2);

      cookiesHeader += "vtex_segment=" + vtexSegment;
      cookiesHeader += "; jb-zipCodeCurrent=" + zipcode;
   }

   protected Document fetchPage(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", cookiesHeader);
      headers.put("x-requested-with", "XMLHttpRequest");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      Response response = dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      String url = HOME_PAGE + "buscapagina?ft=" + this.keywordEncoded + "&PS=50&sl=e6e2754e-fe1c-45f2-8cdf-7875b008e35a&cc=100&sm=0&PageNumber=" + this.currentPage;

      Document doc = fetchPage(url);

      Elements products = doc.select("div.box-item");

      for (Element product : products) {
         String internalPid = product.attr("data-productId");
         String internalId = product.attr("data-productSkuId");
         String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-price.price a", "href");
         String name = CrawlerUtils.scrapStringSimpleInfo(product, ".product-name a", true);
         String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-image img", "src");
         Integer price = CrawlerUtils.scrapIntegerFromHtml(product, ".best-price-price", true, null);
         boolean isAvailable = price != null;

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
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
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
