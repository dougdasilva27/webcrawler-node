package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrasilLojamondelezCrawler extends CrawlerRankingKeywords {
   private String cookiePHPSESSID = null;

   public BrasilLojamondelezCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private static final String LOGIN_URL = "https://www.lojamondelez.com.br/Cliente/Logar";
   private static final String ADMIN_URL = "https://www.lojamondelez.com.br/VendaAssistida/login";
   private String SITE_ID;

   private final String CNPJ = session.getOptions().optString("cnpj");
   private final String PASSWORD = session.getOptions().optString("password");
   private final String MASTER_USER = session.getOptions().optString("master_user");

   private void loginMasterAccount() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("sec-fetch-mode", "cors");
      headers.put("origin", "https://www.lojamondelez.com.br");
      headers.put("sec-fetch-site", "same-origin");
      headers.put("x-requested-with", "XMLHttpRequest");
      headers.put("accept", "application/json, text/javascript, */*; q=0.01");

      String payloadString = "usuario=" + this.MASTER_USER + "&Senha=" + this.PASSWORD;

      Request request = RequestBuilder.create()
         .setUrl(ADMIN_URL)
         .setPayload(payloadString)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_ES,
            ProxyCollection.NETNUT_RESIDENTIAL_BR
         ))
         .build();
      Response response = CrawlerUtils.retryRequest(request, session, dataFetcher);

      List<Cookie> cookiesResponse = response.getCookies();

      for (Cookie cookieResponse : cookiesResponse) {
         if (cookieResponse.getName().equalsIgnoreCase("PHPSESSID")) {
            this.cookiePHPSESSID = cookieResponse.getValue();
         }
      }
   }

   @Override
   protected void processBeforeFetch() {
      loginMasterAccount();

      StringBuilder payload = new StringBuilder();
      payload.append("usuario_cnpj=" + this.CNPJ);

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("sec-fetch-mode", "cors");
      headers.put("origin", "https://www.lojamondelez.com.br");
      headers.put("sec-fetch-site", "same-origin");
      headers.put("x-requested-with", "XMLHttpRequest");
      headers.put("accept", "application/json, text/javascript, */*; q=0.01");

      headers.put("Cookie", "PHPSESSID=" + this.cookiePHPSESSID + ";");

      Request request = RequestBuilder.create()
         .setUrl(LOGIN_URL)
         .setPayload(payload.toString())
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_ES,
            ProxyCollection.NETNUT_RESIDENTIAL_BR
         ))
         .setHeaders(headers)
         .build();

      CrawlerUtils.retryRequest(request, session, dataFetcher);

      BasicClientCookie cookie = new BasicClientCookie("PHPSESSID", this.cookiePHPSESSID);
      cookie.setDomain("www.lojamondelez.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   protected Document fetchDocument(String url, List<Cookie> cookies) {
      Request request = RequestBuilder
         .create()
         .setCookies(cookies)
         .setUrl(url)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_ES,
            ProxyCollection.NETNUT_RESIDENTIAL_BR
         ))
         .build();

      return Jsoup.parse(new ApacheDataFetcher().get(session, request).getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      this.pageSize = 24;
      String url = "https://www.lojamondelez.com.br/Busca/Resultado/?p=" + this.currentPage + "&loja=&q=" + this.keywordEncoded
         + "&ordenacao=6&limit=24";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url, this.cookies);

      Elements products = this.currentDoc.select(".card-product");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         int alternativePosition = 1;
         for (Element product : products) {
            String internalPid = String.valueOf(CrawlerUtils.scrapIntegerFromHtmlAttr(product, null, "id", null));
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".product-name", false);
            String productUrl = getUrl(product, internalPid, name);

            Elements variations = product.select(".sku-variation-content .picking");
            if (!variations.isEmpty()) {
               for (Element variation : variations) {
                  String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(variation, null, "data-sku-id");
                  Integer price = CrawlerUtils.scrapIntegerFromHtmlAttr(variation, null, "data-preco-por", null);
                  String imageUrl = scrapImageUrl(variation);
                  String variationName = assembleName(name, variation);
                  boolean available = !variation.classNames().contains("sem-estoque");

                  if (!available) price = null;

                  RankingProduct productRanking = RankingProductBuilder.create()
                     .setUrl(productUrl)
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setImageUrl(imageUrl)
                     .setName(variationName)
                     .setPriceInCents(price)
                     .setAvailability(available)
                     .setPosition(alternativePosition)
                     .build();

                  saveDataProduct(productRanking);
               }
            }

            alternativePosition++;

            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String getUrl(Element product, String internalPid, String name) {
      String productUrl = CrawlerUtils.scrapUrl(product, "> a", "href", "https", "www.lojamondelez.com.br");
      if (productUrl != null && !productUrl.contains("/Produto/")) {
         if (SITE_ID == null) {
            SITE_ID = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".card-footer button", "data-siteid");
         }
         productUrl = "https://www.lojamondelez.com.br/Produto/" + name.replace(" ", "-") + "/10-10-" + internalPid + "?site_id=" + SITE_ID;
      }

      return productUrl;

   }

   private String assembleName(String name, Element variation) {
      String variationName = CrawlerUtils.scrapStringSimpleInfo(variation, ".caixa-com", false);
      if (variationName != null && !variationName.isEmpty()) {
         name += " " + variationName;
      }
      return name.trim();
   }

   private String scrapImageUrl(Element variation) {
      String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(variation, null, "data-foto");

      if (imageUrl != null && !imageUrl.isEmpty()) {
         imageUrl = imageUrl.replace("/200x200/", "/1000x1000/");
      }

      return imageUrl;
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".qtd-produtos", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
