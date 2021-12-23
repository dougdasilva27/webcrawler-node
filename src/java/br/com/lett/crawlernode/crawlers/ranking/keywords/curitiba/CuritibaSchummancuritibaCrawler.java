package br.com.lett.crawlernode.crawlers.ranking.keywords.curitiba;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CuritibaSchummancuritibaCrawler extends CrawlerRankingKeywords {
   private String HOME_PAGE = "https://www.schumann.com.br/";
   private Integer pageSize = 12;
   public CuritibaSchummancuritibaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

     //String url = HOME_PAGE + "catalogsearch/result/index/?p=" + this.currentPage + "&product_list_limit=" + pageSize + "&q=" + this.keywordEncoded;
      String url = this.fetchApi();
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".row.mx-0 li");
      if (!products.isEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String productUrl = "https://www.schumann.com.br" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".name a", "href");
            String internalId = scrapInternalId(e);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".name", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".lazyload.current-img.no-effect", Arrays.asList("data-src"), "", "");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".instant-price", null, true, ',', session, 0);
            boolean isAvailable = price != 0;
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
   }

   private String scrapInternalId(Element e) {
      return CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".wd-product-line-medias.wd-widget-js", "data-pid");
   }

   protected String fetchApi() {
      Map<String, String> headers = new HashMap<>();

      String url = HOME_PAGE + this.keywordEncoded+"?pg="+ this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      headers.put("origin", HOME_PAGE);

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setCookies(cookies)
         .setFollowRedirects(true)
         .build();
      Response response = this.dataFetcher.get(session, request);
      return response.getRedirectUrl();
   }


   @Override
   protected void setTotalProducts() {
      //(this.currentDoc, ".product-count", false)
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".product-count", false);

      this.log("Total: " + this.totalProducts);
   }
}
