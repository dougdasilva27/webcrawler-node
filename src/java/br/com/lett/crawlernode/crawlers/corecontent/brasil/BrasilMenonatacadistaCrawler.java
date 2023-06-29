package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.http.cookie.Cookie;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilMenonatacadistaCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Menon Atacadista";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString(),
      Card.AMEX.toString(), Card.DINERS.toString(), Card.DISCOVER.toString(), Card.JCB.toString(), Card.AURA.toString(), Card.HIPERCARD.toString());

   public BrasilMenonatacadistaCrawler(Session session) throws UnsupportedEncodingException {
      super(session);
      super.config.setParser(Parser.HTML);
   }

   private final String PASSWORD = getPassword();
   private final String LOGIN = getLogin();

   protected String getLogin() {
      return session.getOptions().optString("email");
   }

   protected String getPassword() {
      return session.getOptions().optString("password");
   }

   private String cookiePHPSESSID = null;

   public static Map<String, String> setIdenticalHeaders() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      headers.put("Origin", "https://www.menonatacadista.com.br");
      headers.put("Upgrade-Insecure-Requests", "1");

      return headers;
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Map<String, String> headers = setIdenticalHeaders();
      headers.put("Connection", "keep-alive");
      headers.put("Referer", "https://www.menonatacadista.com.br/index.php?route=account/login");

      String payloadString = "email=" + this.LOGIN + "&password=" + this.PASSWORD;

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.menonatacadista.com.br/index.php?route=account/login")
         .setPayload(payloadString)
         .setHeaders(headers)
         .setFollowRedirects(false)
         .setSendUserAgent(true)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_BR,
            ProxyCollection.SMART_PROXY_BR
         ))
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, Arrays.asList(new FetcherDataFetcher(), new ApacheDataFetcher(), new JsoupDataFetcher()), session, "post");

      List<Cookie> cookiesResponse = response.getCookies();

      for (Cookie cookieResponse : cookiesResponse) {
         if (cookieResponse.getName().equalsIgnoreCase("PHPSESSID")) {
            this.cookiePHPSESSID = cookieResponse.getValue();
            Logging.printLogDebug(logger, session, "Captured PHPSESSID cookie!");
         }
      }
   }

   @Override
   protected Response fetchResponse() {
      try {
         HttpResponse<String> response = retryRequest(session.getOriginalURL(), session);
         return new Response.ResponseBuilder()
            .setBody(response.body())
            .setLastStatusCode(response.statusCode())
            .build();
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }
   }

   public HttpResponse retryRequest(String url, Session session) throws IOException, InterruptedException {
      HttpResponse<String> response = null;
      ArrayList<Integer> ipPort = new ArrayList<>();
      ipPort.add(3135); //buy haproxy
      ipPort.add(3132); //netnut br haproxy
      ipPort.add(3133); //netnut ES haproxy
      ipPort.add(3138); //netnut AR haproxy

      try {
         for (int interable = 0; interable < ipPort.size(); interable++) {
            response = RequestHandler(url, ipPort.get(interable));
            if (response.statusCode() == 200) {
               return response;
            }
         }
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document (retryRequest): " + session.getOriginalURL(), e);
      }
      return response;
   }

   private HttpResponse RequestHandler(String url, Integer port) throws IOException, InterruptedException {
      Map<String, String> headers = setIdenticalHeaders();
      headers.put("Referer", "https://www.menonatacadista.com.br/");
      headers.put("Cookie", "PHPSESSID=" + this.cookiePHPSESSID + ";");

      List<String> listaHeaders = new ArrayList<>();
      headers.forEach((key, value) -> {
         listaHeaders.add(key);
         listaHeaders.add(value);
      });

      HttpClient client = HttpClient.newBuilder().proxy(ProxySelector.of(new InetSocketAddress("haproxy.lett.global", port))).build();
      HttpRequest request = HttpRequest.newBuilder()
         .GET()
         .headers(listaHeaders.toArray(new String[0]))
         .uri(URI.create(url))
         .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return response;
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product-title", true) == null ? CrawlerUtils.scrapStringSimpleInfo(doc, "h1.name", true) : null;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "ul.breadcrumb a", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "a.thumbnail img", Arrays.asList("src"), "https", "www.menonatacadista.com.br");

         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("div.text"));
         boolean availableToBuy = doc.select("button[id=button-out]").isEmpty();
         Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setOffers(offers)
            .setDescription(description)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("div.product-detail-info") != null || doc.selectFirst(".price") != null;
   }

   private String crawlInternalId(Document doc) {
      String id = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[name=\"product_id\"]", "value");

      if (id == null) {
         String regex = "product_id=([0-9]*)";
         Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         Matcher matcher = pattern.matcher(session.getOriginalURL());

         if (matcher.find()) {
            return matcher.group(1);
         }
      }

      return id;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setSales(sales)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.product-price .price", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span[class*=Information_discounted]", null, true, ',', session);
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price", null, true, ',', session);
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create().setSpotlightPrice(spotlightPrice).setPriceFrom(priceFrom).setCreditCards(creditCards).build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create().setInstallmentNumber(1).setInstallmentPrice(spotlightPrice).build());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create().setBrand(card).setInstallments(installments).setIsShopCard(false).build());
      }

      return creditCards;
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      String saleDiscount = CrawlerUtils.calculateSales(pricing);

      if (saleDiscount != null) {
         sales.add(saleDiscount);
      }

      return sales;
   }
}
