package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SaoPauloJauServe extends CrawlerRankingKeywords {
   public SaoPauloJauServe(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.ACCEPT, "*/*");
      String postalCode = session.getOptions().optString("postal_code");
      BasicClientCookie cookie = new BasicClientCookie("dw_shippostalcode", postalCode);
      cookie.setDomain("www.jauserve.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
         ))
         .setCookies(this.cookies)
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new JsoupDataFetcher(), new ApacheDataFetcher()), session, "get");
      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      String baseUrl = "https://www.jauserve.com.br";
      int start = (this.currentPage - 1) * 16;
      String url = "https://www.jauserve.com.br/on/demandware.store/Sites-JauServe-Site/pt_BR/Search-UpdateGrid?q=" + this.keywordEncoded + "&pmin=0%2c01&start=" + start + "&sz=" + (start + 16) + "&selectedUrl=https%3A%2F%2Fwww.jauserve.com.br%2Fon%2Fdemandware.store%2FSites-JauServe-Site%2Fpt_BR%2FSearch-UpdateGrid%3Fq%3D" + this.keywordEncoded + "%26pmin%3D0%252c01%26start%3D" + start + "%26sz%3D" + (start + 16);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-tile");
      if (products != null) {
         for (Element product : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-tile", "data-itemid");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".link", true);
            String productUrl = baseUrl + CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".link", "href");
            Boolean isAvailable = getAvailabity(product);
            Integer price = isAvailable ? getPrice(product) : null;
            String image = getImage(product);
            RankingProduct productRanking = new RankingProductBuilder()
               .setInternalId(internalPid)
               .setInternalPid(internalPid)
               .setName(name)
               .setUrl(productUrl)
               .setPriceInCents(price)
               .setImageUrl(image)
               .setAvailability(isAvailable)
               .build();
            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }
      this.log("Products found: " + products.size() + "\n");
   }

   private Boolean getAvailabity(Element e) {
      String contentButton = CrawlerUtils.scrapStringSimpleInfo(e, ".btn span", true);
      if (contentButton != null && !contentButton.isEmpty()) {
         return contentButton.contains("Adicionar");
      }
      return false;
   }

   private Integer getPrice(Element e) {
      Integer price = CrawlerUtils.scrapIntegerFromHtmlAttr(e, ".sales .value", "content", null);
      return price != null ? price : CrawlerUtils.scrapIntegerFromHtmlAttr(e, ".value", "content", null);
   }

   private String getImage(Element e) {
      String imagePath = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".tile-image", "src");
      if (imagePath != null && !imagePath.isEmpty()) {
         return imagePath.substring(0, imagePath.indexOf('?'));
      }
      return null;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
