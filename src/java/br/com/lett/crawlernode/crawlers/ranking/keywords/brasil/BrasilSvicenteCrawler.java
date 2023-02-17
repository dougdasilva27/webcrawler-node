package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;


import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import com.google.common.net.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class BrasilSvicenteCrawler extends CrawlerRankingKeywords {
   public BrasilSvicenteCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.svicente.com.br";

   @Override
   protected void processBeforeFetch() {
      HashMap<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.ACCEPT, "application/json, text/javascript, */*; q=0.01");
      headers.put(HttpHeaders.REFERER, "https://www.svicente.com.br/pagina-inicial");
      headers.put("authority", "www.svicente.com.br");
      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.svicente.com.br/pagina-inicial")
         .setHeaders(headers)
         .build();
      Response response = this.dataFetcher.get(session, request);
      if (response.getCookies() != null && !response.getCookies().isEmpty()) {
         for (Cookie cookie : response.getCookies()) {
            BasicClientCookie cookieAdd = new BasicClientCookie(cookie.getName(), cookie.getValue());
            cookieAdd.setDomain("www.svicente.com.br");
            cookieAdd.setPath("/");
            this.cookies.add(cookieAdd);
         }

      }
      String store = session.getOptions().optString("store");

      BasicClientCookie dw_store = new BasicClientCookie("dw_store", store);
      dw_store.setDomain("www.svicente.com.br");
      dw_store.setPath("/");
      this.cookies.add(dw_store);


      BasicClientCookie has_selected_store = new BasicClientCookie("hasSelectedStore", store);
      has_selected_store.setDomain("www.svicente.com.br");
      has_selected_store.setPath("/");
      this.cookies.add(has_selected_store);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      int start = (this.currentPage - 1) * 24;
      String url = HOME_PAGE + "/on/demandware.store/Sites-SaoVicente-Site/pt_BR/Search-UpdateGrid?q=" + this.keywordEncoded + "&start=" + start + "&sz=24&selectedUrl";
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".searchResults__productGrid__product");
      if (products != null && !products.isEmpty()) {
         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".quantity__maxorder.quantity__maxorder--producttile", "data-pid");
            String productUrl = scrapUrl(e);
            String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".productTile__name", "title");
            String image = scrapImage(e);
            boolean available = scrapAvailability(e);
            Integer price = available ? scrapPrice(e) : null;
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(available)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }

   }

   private String scrapUrl(Element product) {
      String suffixUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".productTile__body .productTile__name", "data-product-url");
      if (suffixUrl != null && !suffixUrl.isEmpty()) {
         return HOME_PAGE + suffixUrl;
      }
      return null;
   }

   private boolean scrapAvailability(Element product) {
      return CrawlerUtils.scrapStringSimpleInfo(product, ".quantity__unavailable", true) == null;
   }

   private String scrapImage(Element product) {
      String imagePath = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".productTile__imageWrapper__img", "src");
      if (imagePath != null && !imagePath.isEmpty()) {
         return CommonMethods.substring(imagePath, "", "?", false);
      }
      return null;
   }

   private Integer scrapPrice(Element product) {
      String spotlightPriceStr = CrawlerUtils.scrapStringSimpleInfo(product, ".productPrice__priceContainer .productPrice__price", true);
      if (spotlightPriceStr != null && !spotlightPriceStr.isEmpty()) {
         return CommonMethods.stringPriceToIntegerPrice(spotlightPriceStr, ',', null);
      }
      String priceStr = CrawlerUtils.scrapStringSimpleInfo(product, ".productPrice .productPrice__price", true);
      if (priceStr != null && !priceStr.isEmpty()) {
         return CommonMethods.stringPriceToIntegerPrice(priceStr, ',', null);
      }
      return null;
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".primaryButton.js-productGrid__showMoreBtn") != null;
   }
}
