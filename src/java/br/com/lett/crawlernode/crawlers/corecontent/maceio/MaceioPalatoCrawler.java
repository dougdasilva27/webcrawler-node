package br.com.lett.crawlernode.crawlers.corecontent.maceio;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.com.lett.crawlernode.util.CrawlerUtils.setCookie;

public class MaceioPalatoCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Palato";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public MaceioPalatoCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
      headers.put("Accept-Language", "pt-BR,pt;q=0.9");
      headers.put("Connection", "keep-alive");
      headers.put("Referer", session.getOriginalURL());
      headers.put("Upgrade-Insecure-Requests", "1");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://loja.palato.com.br?uf_verification=AL")
         .setHeaders(headers)
         .setSendUserAgent(true)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY
         ))
         .build();

      Response response = new ApacheDataFetcher().get(session, request);
      List<Cookie> cookiesResponse = response.getCookies();
      for (Cookie cookieResponse : cookiesResponse) {
         cookies.add(setCookie(cookieResponse.getName(), cookieResponse.getValue(), cookieResponse.getDomain(), cookieResponse.getPath()));
      }
   }

   @Override
   protected Response fetchResponse() {
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(session.getOriginalURL()))
            .header("Cookie", cookieString())
            .build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         return new Response.ResponseBuilder()
            .setBody(response.body())
            .setLastStatusCode(response.statusCode())
            .build();
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }
   }

   private String cookieString() {
      StringBuilder cookieStringBuilder = new StringBuilder();

      for (Cookie cookie : cookies) {
         cookieStringBuilder.append(cookie.getName())
            .append("=")
            .append(cookie.getValue())
            .append("; ");
      }

      String cookieString = cookieStringBuilder.toString();
      if (cookieString.endsWith("; ")) {
         cookieString = cookieString.substring(0, cookieString.length() - 2);
      }

      return cookieString;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-options input[name='sku']", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h3.font-weight-bold", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".sp-wrap img", Collections.singletonList("src"), "https", "loja.palato.com.br/");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".produto-informacoes"));

         boolean availableToBuy = !doc.select(".col-xl-6 .add-to-cart ").isEmpty();
         String internalId = availableToBuy ? CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-options input[name='id']", "value") : CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#id-produto", "value");
         Offers offers = availableToBuy ? scrapOffer(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private boolean isProductPage(Document doc) {
      return !doc.select("#product-details").isEmpty();
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      String sale = CrawlerUtils.calculateSales(pricing);

      if (sale != null) {
         sales.add(sale);
      }
      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".h3.green", null, true, ',', session);
      Double priceFrom = regexPriceFrom(doc);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private Double regexPriceFrom(Document doc) {
      String price = CrawlerUtils.scrapStringSimpleInfo(doc, ".size-2.mb-0.font-weight-normal.sale", true);

      if (price != null) {
         String regex = "R\\$ (.*), ";
         Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         Matcher matcher = pattern.matcher(price);

         if (matcher.find()) {
            String priceStr = matcher.group(1);
            String cleanPriceStr = priceStr.replaceAll("[^\\d.]", "");
            return Double.parseDouble(cleanPriceStr);
         }
      }

      return null;
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

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
}
