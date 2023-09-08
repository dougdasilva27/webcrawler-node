package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.HttpClientFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import static br.com.lett.crawlernode.util.CrawlerUtils.setCookie;

public class BrasilDrogarianisseiCrawler extends Crawler {
   private static final String HOME_PAGE = "https://www.farmaciasnissei.com.br/";
   private final String storeId = session.getOptions().optString("storeId", null);

   public BrasilDrogarianisseiCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(this.session.getOriginalURL()))
            .build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

         List<String> cookiesResponse = response.headers().map().get("Set-Cookie");
         for (String cookieStr : cookiesResponse) {
            HttpCookie cookie = HttpCookie.parse(cookieStr).get(0);
            cookies.add(setCookie(cookie.getName(), cookie.getValue(), "www.farmaciasnissei.com.br", "/"));
         }
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }
   }

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "www.farmaciasnissei.com.br");
      headers.put("origin", "https://www.farmaciasnissei.com.br");
      headers.put("referer", session.getOriginalURL());
      headers.put("x-requested-with", "XMLHttpRequest");

      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setProxyservice(List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .setSendUserAgent(false)
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new HttpClientFetcher(), new ApacheDataFetcher()), session, "get");

      return response;
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div[data-produto_id]", "data-produto_id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "[data-target=produto_view] [data-target=nome_produto]", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".small a", true);
         String primaryImage = fixUrlImage(doc, internalId);
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".dots-preview .swiper-slide img", Collections.singletonList("src"), "https", "www.farmaciasnissei.com.br", primaryImage);
         String description = CrawlerUtils.scrapElementsDescription(doc, List.of(".card #tabCollapse-descricao"));
         JSONObject json = accesAPIOffers(internalId);
         boolean available = storeId == null ? json.optBoolean("is_disponivel") : isAvailable(internalId);
         Offers offers = available ? scrapOffers(json) : new Offers();
         RatingsReviews ratingsReviews = getRatingsReviews(doc);
         List<String> eans = scrapEan(doc);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .setEans(eans)
            .setRatingReviews(ratingsReviews)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Boolean isAvailable(String id) {
      String token = "";
      String url = "https://www.farmaciasnissei.com.br/buscar/estoque";

      String cookies = CommonMethods.cookiesToString(this.cookies);

      token = CommonMethods.substring(cookies, "=", ";", true);

      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", cookies);
      headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("referer", session.getOriginalURL());
      headers.put("authority", "www.farmaciasnissei.com.br");
      headers.put("origin", "https://www.farmaciasnissei.com.br");

      String payload = "csrfmiddlewaretoken=" + token + "&produto_id=" + id;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.BUY_HAPROXY, ProxyCollection.BUY))
         .setSendUserAgent(false)
         .setPayload(payload)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new FetcherDataFetcher()), session, "post");

      JSONObject json = CrawlerUtils.stringToJson(response.getBody());
      JSONArray availableStores = JSONUtils.getJSONArrayValue(json, "lista_estoque");
      for (Object product : availableStores) {
         JSONObject currentStore = (JSONObject) product;
         if (JSONUtils.getIntegerValueFromJSON(currentStore, "cd_filial", 000) == Integer.parseInt(storeId)) {
            return true;
         }
      }
      return false;
   }

   private JSONObject accesAPIOffers(String internalId) {
      JSONObject jsonObject = new JSONObject();
      String token = "";
      String url = "https://www.farmaciasnissei.com.br/pegar/preco";

      String cookies = CommonMethods.cookiesToString(this.cookies);

      token = CommonMethods.substring(cookies, "=", ";", true);

      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", cookies);
      headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("referer", session.getOriginalURL());
      headers.put("authority", "www.farmaciasnissei.com.br");
      headers.put("origin", "https://www.farmaciasnissei.com.br");
      headers.put("x-requested-with", "XMLHttpRequest");

      String payload = "csrfmiddlewaretoken=" + token + "&produtos_ids%5B%5D=" + internalId;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.BUY))
         .setSendUserAgent(false)
         .setPayload(payload)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new FetcherDataFetcher()), session, "post");

      JSONObject json = CrawlerUtils.stringToJson(response.getBody());

      JSONObject prices = json != null ? json.optJSONObject("precos") : null;

      if (prices != null && !prices.isEmpty()) {
         JSONObject dataProduct = prices.optJSONObject(internalId);
         return dataProduct != null ? dataProduct.optJSONObject("publico") : jsonObject;

      }

      return jsonObject;
   }

   protected Object fetchDocument() {

      Logging.printLogDebug(logger, session, "Fetching page with webdriver...");
      ChromeOptions options = new ChromeOptions();
      options.addArguments("--window-size=1920,1080");
      options.addArguments("--headless");
      options.addArguments("--no-sandbox");
      options.addArguments("--disable-dev-shm-usage");

      Document doc = new Document("");
      int attempt = 0;
      boolean sucess = false;
      List<String> proxies = List.of(ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.BUY_HAPROXY);
      do {
         try {
            Logging.printLogDebug(logger, session, "Fetching page with webdriver...");

            webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), proxies.get(attempt), session, this.cookiesWD, HOME_PAGE, options);
            webdriver.waitLoad(1000);

            doc = Jsoup.parse(webdriver.getCurrentPageSource());
            sucess = doc.selectFirst("div[data-produto_id]") != null;
            webdriver.terminate();
            attempt++;

         } catch (Exception e) {
            Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
            Logging.printLogWarn(logger, "Página não capturada");
         }

      } while (attempt < 3 && !sucess);

      return doc;
   }


   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 20);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   private List<String> scrapEan(Document doc) {
      List<String> ean = new ArrayList<>();
      String productInfo = CrawlerUtils.scrapStringSimpleInfo(doc, "div .row div .mt-1", true);
      if (productInfo != null) {
         String[] split = productInfo.split("EAN:");
         if (split.length > 1) {
            ean.add(split[1].trim());
         }
      }

      return ean;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("div[data-produto_id]").isEmpty();
   }

   private String fixUrlImage(Document doc, String internalId) {
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".swiper-slide img", Collections.singletonList("src"), "https:", "www.farmaciasnissei.com.br");

      if (primaryImage.contains("caixa-nissei")) {
         return primaryImage.replace("caixa-nissei", internalId);

      }
      return primaryImage;
   }

   private Offers scrapOffers(JSONObject jsonInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing;
      List<String> sales;

      if (jsonInfo != null && !jsonInfo.isEmpty()) {
         pricing = scrapPricing(jsonInfo);
         sales = scrapSales(jsonInfo);
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("Drogaria Nissei")
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());

      } else {
         Document doc = (Document) fetchDocument();
         if (doc != null && !doc.select(".mt-md-2.mt-sm-2 > div > p").isEmpty()) {
            pricing = scrapPricingFromDocument(doc);
            sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));
            offers.add(Offer.OfferBuilder.create()
               .setUseSlugNameAsInternalSellerId(true)
               .setSellerFullName("Drogaria Nissei")
               .setMainPagePosition(1)
               .setIsBuybox(false)
               .setIsMainRetailer(true)
               .setPricing(pricing)
               .setSales(sales)
               .build());
         }
      }

      return offers;
   }

   private List<String> scrapSales(JSONObject jsonInfo) {
      List<String> sales = new ArrayList<>();

      String firstSales = jsonInfo.optString("per_desc");

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricing(JSONObject jsonInfo) throws MalformedPricingException {
      Double priceFrom = !scrapSales(jsonInfo).isEmpty() ? jsonInfo.optDouble("valor_ini") : null;
      Double spotlightPrice = jsonInfo.optDouble("valor_fim");
      if (spotlightPrice.equals(0.0) || spotlightPrice.isNaN()) {
         spotlightPrice = JSONUtils.getValueRecursive(jsonInfo, "progressivos.0.vlr_final", Double.class, 0d);
      }

      if (spotlightPrice.equals(0.0)) {
         spotlightPrice = priceFrom;
         priceFrom = null;
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private Pricing scrapPricingFromDocument(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".mt-md-2.mt-sm-2 > div > p", null, true, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".mt-md-2.mt-sm-2 > div > span", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   /*
In this store, grades are given with a double value, e.g: 4.5 instead of 5 or 4.
Therefore, the crawler structure, by accepting only integer values, which is common on most sites, will not be captured the advanced rating.
 */
   private RatingsReviews getRatingsReviews(Document doc) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      ratingsReviews.setDate(session.getDate());

      Integer reviews = CrawlerUtils.scrapIntegerFromHtml(doc, ".text-muted.font-xl", true, 0);
      ratingsReviews.setTotalWrittenReviews(reviews);
      ratingsReviews.setTotalRating(reviews);
      ratingsReviews.setAverageOverallRating(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".avaliacao-produto .rating-produto", null, true, ',', session));

      return ratingsReviews;
   }
}
