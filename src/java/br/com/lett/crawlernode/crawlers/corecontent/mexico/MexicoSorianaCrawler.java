package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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
import org.jsoup.nodes.Document;

import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class MexicoSorianaCrawler extends Crawler {

   private final String storeId = session.getOptions().optString("storeId");
   private final String postalCode = session.getOptions().optString("postalCode");
   private final String storeName = session.getOptions().optString("storeName");
   private static final String SELLER_FULL_NAME = "soriana";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public MexicoSorianaCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.HTTPCLIENT);
      super.config.setParser(Parser.HTML);
   }

   @Override
   protected Response fetchResponse() {
      String cookieSession = fetchCookieSession();
      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "www.soriana.com");
      headers.put("cookie", cookieSession);

      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setProxyservice(List.of(
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.SMART_PROXY_MX_HAPROXY
         ))
         .setHeaders(headers)
         .build();

      return this.dataFetcher.get(session, request);
   }

   private String fetchCookieSession() {
      String cookieDwsid = null;
      HttpResponse<String> response;
      List<Integer> idPort = Arrays.asList(3137, 3149, 3138);
      int attempts = 0;

      do {
         try {
            HttpClient client = HttpClient.newBuilder().proxy(ProxySelector.of(new InetSocketAddress("haproxy.lett.global", idPort.get(attempts)))).build();
            HttpRequest request = HttpRequest.newBuilder()
               .GET()
               .uri(URI.create("https://www.soriana.com/on/demandware.store/Sites-Soriana-Site/default/Stores-SelectStore?isStoreModal=true&id=" + storeId + "&postalCode=" + postalCode + "&storeName=" + storeName + "&methodid=pickup"))
               .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            List<String> cookiesResponse = response.headers().map().get("Set-Cookie");
            for (String cookieStr : cookiesResponse) {
               HttpCookie cookie = HttpCookie.parse(cookieStr).get(0);
               if (cookie.getName().equalsIgnoreCase("dwsid")) {
                  cookieDwsid = "dwsid=" + cookie.getValue();
                  break;
               }
            }
         } catch (Exception e) {
            throw new RuntimeException("Failed in load document: " + session.getOriginalURL(), e);
         }
      } while (attempts++ < 3 && response.statusCode() != 200);

      return cookieDwsid;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".container.product-detail.product-wrapper", "data-pid");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".carousel-item.active img", Collections.singletonList("src"), "https", "centralar.com.br");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".carousel-indicators li:not(:first-child) img", Collections.singletonList("src"), "https", "www.soriana.com", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".description-and-detail"));
         CategoryCollection categoryCollection = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:last-child) a", true);
         List<String> ean = scrapEan(doc);
         boolean availableToBuy = doc.select(".cart-icon.d-none").isEmpty();
         Offers offers = availableToBuy ? scrapOffer(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categoryCollection)
            .setDescription(description)
            .setOffers(offers)
            .setEans(ean)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".container.product-detail.product-wrapper").isEmpty();
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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".sales > span", null, true, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".value > span", null, true, '.', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
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

   protected List<String> scrapEan(Document doc) {
      List<String> eans = new ArrayList<>();
      String ean = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".container.product-detail.product-wrapper", "data-ean");
      if (ean != null) {
         eans.add(ean);
      }
      return eans;

   }
}
