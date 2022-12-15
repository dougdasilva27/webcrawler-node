package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MexicoSorianaCrawler extends CrawlerRankingKeywords {

   public MexicoSorianaCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   @Override
   protected Document fetchDocument(String url, List<Cookie> cookies) {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.ACCEPT, "*/*");
      headers.put("cookie", CommonMethods.cookiesToString(this.cookies));

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.SMART_PROXY_MX,
            ProxyCollection.SMART_PROXY_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY
         ))
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new JsoupDataFetcher(), new ApacheDataFetcher()), session, "get");
      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 15;

      this.log("Página " + this.currentPage);
      String url = "https://www.soriana.com/buscar?q=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url, this.cookies);

      Elements products = this.currentDoc.select(".product-tile--wrapper.d-flex");

      if (products.size() >= 1) {
         int startSearch = (this.currentPage - 1) * 15;
         int endSearch = startSearch + 15;

         for (int i = startSearch; i < endSearch && startSearch < products.size(); i++) {
            Element e = products.get(i);
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product", "data-pid");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".tile-body.product-tile--body.w-100.p-0 > div.pdp-link.product-tile--name > a", true);
            String productUrl = CrawlerUtils.scrapUrl(e, ".image-container a", "href", "https", "www.soriana.com");
            Integer price = getPrice(e);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".w-100.image-container.product-tile--image-container.d-flex.align-items-center.justify-content-start.border-x-1.mt-1.border-t-1 > a.w-100.justify-content-center > img", "data-src");
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
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

   private Integer getPrice(Element e) {
      Integer priceWithDiscount = CrawlerUtils.scrapIntegerFromHtmlAttr(e, ".font-primary--bold.px-2.price-pdp", "value", 0);
      if (priceWithDiscount != 0) {
         return priceWithDiscount;
      }
      return CrawlerUtils.scrapIntegerFromHtmlAttr(e, ".value.d-none", "content", 0);
   }

   @Override
   protected boolean hasNextPage() {
      return true;

   }
}
