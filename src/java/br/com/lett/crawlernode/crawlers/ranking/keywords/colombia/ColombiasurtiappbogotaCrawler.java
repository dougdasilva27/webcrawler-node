package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.models.RankingProducts;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import software.amazon.awssdk.http.Header;

import java.util.HashMap;
import java.util.Map;

public class ColombiasurtiappbogotaCrawler extends CrawlerRankingKeywords {


   @Override
   protected void processBeforeFetch() {
      Request requestCookies = Request.RequestBuilder.create()
         .setUrl("https://tienda.surtiapp.com.co/Security/Login")
         .build();

      Response responseCookies = dataFetcher.get(session, requestCookies);

      this.cookies.addAll(responseCookies.getCookies());

      Document document = Jsoup.parse(responseCookies.getBody());

      String requestVerificationToken = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "input[name=\"__RequestVerificationToken\"]", "value");

      Map<String, String> headers = new HashMap<>();

      headers.put("RequestVerificationToken", requestVerificationToken);
      headers.put(Header.CONTENT_TYPE, "application/x-www-form-urlencoded");
      headers.put("Cookie", CommonMethods.cookiesToString(responseCookies.getCookies()));

      String payload = "email=" +
         session.getOptions().optString("email", "") +
         "&password=" +
         session.getOptions().optString("password", "");


      Request requestLogin = Request.RequestBuilder.create()
         .setUrl("https://tienda.surtiapp.com.co/Security/Login?handler=Authenticate")
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      new JsoupDataFetcher().post(session, requestLogin);


   }

   @Override
   protected Document fetchDocument(String url) {

      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", CommonMethods.cookiesToString(this.cookies));
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      String html = this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(html);
   }


   public ColombiasurtiappbogotaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 25;
      this.log("Página " + this.currentPage);

      String url = "https://tienda.surtiapp.com.co/Store/SearchResults/" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-card.product-id-contaniner");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "data-product");
            String productUrl = CrawlerUtils.completeUrl(internalId, "https", "tienda.surtiapp.com.co/Store/ProductDetail/");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-card__name", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-card__image img", "src");
            int price = CrawlerUtils.scrapIntegerFromHtml(e, ".product-card__price", true, 0);
            boolean isAvailable = price != 0;

            //New way to send products to save data product
            RankingProducts productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null
               + " - Url: " + productUrl);

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

}
