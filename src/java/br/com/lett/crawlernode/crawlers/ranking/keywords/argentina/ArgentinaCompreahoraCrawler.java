package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ArgentinaCompreahoraCrawler extends CrawlerRankingKeywords {
   public ArgentinaCompreahoraCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected Document fetchDocument(String url) {Request request = Request.RequestBuilder.create()
      .setUrl(url)
      .setCookies(this.cookies)
      .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();

      return Jsoup.parse(content);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      handleCookiesBeforeFetch();
      String url = "https://www.compreahora.com.ar/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
     this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-items li");
      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-details .ulproduct-label", "data-product-id");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-details .product-item-link", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e,".product.name.product-item-name .product-item-link .ellipse-source", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image-photo", Arrays.asList("src"), "", "");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".box-price.special-price ", null, false, ',', session, 0);
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
   @Override
   protected boolean hasNextPage() {
      return !currentDoc.select(".action.next").isEmpty();
   }

   public void handleCookiesBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("x-requested-with", "XMLHttpRequest");
      headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("referer", "https://www.compreahora.com.ar/");

      String payload = "username=federico.serrano%40scmalaga.com.ar" +
         "&password=Cl112007*";


      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.compreahora.com.ar/customerlogin/ajax/login")
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .setPayload(payload)
         .build();

      Response response = this.dataFetcher.post(session, request);

      this.cookies = response.getCookies();
   }

}
