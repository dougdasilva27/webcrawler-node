package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrasilLojamondelezCrawler extends CrawlerRankingKeywords {
   private String cookiePHPSESSID = null;

   public BrasilLojamondelezCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   private static final String LOGIN_URL = "https://www.lojamondelez.com.br/Cliente/Logar";
   private static final String ADMIN_URL = "https://www.lojamondelez.com.br/VendaAssistida/login";
   private String SITE_ID;

   private final String CNPJ = session.getOptions().optString("cnpj");
   private final String PASSWORD = session.getOptions().optString("password");
   private final String MASTER_USER = session.getOptions().optString("master_user");

   private void loginMasterAccount() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
      headers.put(HttpHeaders.REFERER, "https://www.lojamondelez.com.br/");
      Response response = new Response();
      String payloadString = "usuario=" + this.MASTER_USER + "&Senha=" + this.PASSWORD;
      try {
         Thread.sleep(5000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      try {
      Request request = RequestBuilder.create()
         .setUrl(ADMIN_URL)
         .setPayload(payloadString)
         .setHeaders(headers)
         .setProxy(
            getFixedIp()
         )
         .build();
         response = this.dataFetcher.post(session, request);
      } catch (IOException e) {
         e.printStackTrace();
      }
      List<Cookie> cookiesResponse = response.getCookies();

      for (Cookie cookieResponse : cookiesResponse) {
         if (cookieResponse.getName().equalsIgnoreCase("PHPSESSID")) {
            this.cookiePHPSESSID = cookieResponse.getValue();
         }
      }
   }
   public LettProxy getFixedIp() throws IOException {

      LettProxy lettProxy = new LettProxy();
      lettProxy.setSource("fixed_ip");
      lettProxy.setPort(3144);
      lettProxy.setAddress("haproxy.lett.global");
      lettProxy.setLocation("brazil");

      return lettProxy;
   }
   @Override
   protected void processBeforeFetch() {
      loginMasterAccount();
      try {
         Thread.sleep(5000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      StringBuilder payload = new StringBuilder();
      payload.append("usuario_cnpj=" + this.CNPJ);

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
      headers.put("referer", "https://www.lojamondelez.com.br/VendaAssistida");
      headers.put("Cookie", "PHPSESSID=" + this.cookiePHPSESSID + ";");
      Response response = new Response();
      try {
      Request request = RequestBuilder.create()
         .setUrl(LOGIN_URL)
         .setPayload(payload.toString())
         .setProxy(
            getFixedIp()
         )
         .setHeaders(headers)
         .build();

         response = this.dataFetcher.post(session, request);
      } catch (IOException e) {
         e.printStackTrace();
      }
      BasicClientCookie cookie = new BasicClientCookie("PHPSESSID", this.cookiePHPSESSID);
      cookie.setDomain("www.lojamondelez.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   protected Document fetchDocument(String url, List<Cookie> cookies) {
      String response = "";
      try {
         Thread.sleep(5000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "PHPSESSID=" + this.cookiePHPSESSID + ";");
      try {
      Request request = RequestBuilder
         .create()
         .setHeaders(headers)
         .setUrl(url)
         .setProxy(
            getFixedIp()
         )
         .build();

    response = this.dataFetcher.get(session, request).getBody();
      } catch (IOException e) {
         e.printStackTrace();
      }
      return Jsoup.parse(response);
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
