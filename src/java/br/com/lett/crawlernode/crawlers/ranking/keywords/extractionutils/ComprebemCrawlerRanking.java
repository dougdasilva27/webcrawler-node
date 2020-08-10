package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ComprebemCrawlerRanking extends CrawlerRankingKeywords {

   protected String HOME_PAGE = getHomePage();
   protected String CEP = getCep();

   protected abstract String getHomePage();

   protected abstract String getCep();

   public ComprebemCrawlerRanking(Session session) {
      super(session);
   }

   @Override
   protected void processBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put(HttpHeaders.ACCEPT, "*/*;q=0.5, text/javascript, application/javascript, application/ecmascript, application/x-ecmascript");
      headers.put(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br");
      headers.put("origin", "https://delivery.comprebem.com.br");
      headers.put("x-requested-with", "XMLHttpRequest");
      headers.put("connection", "keep-alive");

      StringBuilder payload = new StringBuilder();
      payload.append("utf8=%E2%9C%93");
      payload.append("&_method=put");
      payload.append("&order%5Bshipping_mode%5D=delivery");
      payload.append("&order%5Bship_address_attributes%5D%5Btemporary%5D=true");
      payload.append("&order%5Bship_address_attributes%5D%5Bzipcode%5D=" + CEP);
      payload.append("&button=");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://delivery.comprebem.com.br/current_stock")
         .setPayload(payload.toString())
         .setHeaders(headers)
         .setCookies(cookies)
         .mustSendContentEncoding(false)
         .build();
      List<Cookie> loadPageCookies = this.dataFetcher.post(session, request).getCookies();

      for (Cookie cookieResponse : loadPageCookies) {
         BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
         cookie.setDomain(HOME_PAGE);
         cookie.setPath("/");
         cookies.add(cookie);
      }
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 32;
      this.log("Página " + this.currentPage);

      String url = "https://delivery.comprebem.com.br/products?keywords=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".products > div");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element product : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "#variant_id", "value");
            String internalPid = internalId;
            String productUrl = CrawlerUtils.scrapUrl(product, ".text > a", "href", "https", HOME_PAGE);

            saveDataProduct(internalId, internalPid, productUrl);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + internalId +
                  " - InternalPid: " + internalPid +
                  " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit)
               break;
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".btn-block-responsive") != null;
   }
}
