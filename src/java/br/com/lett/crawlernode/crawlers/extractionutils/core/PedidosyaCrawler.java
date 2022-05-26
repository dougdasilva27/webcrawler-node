package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;

import java.util.*;

public class PedidosyaCrawler extends Crawler {
   public PedidosyaCrawler(Session session) {
      super(session);
      config.setParser(Parser.JSON);
      config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      String url = "https://www.pedidosya.com.ar";
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("authority", "www.pedidosya.com.ar");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(proxies)
         .setHeaders(headers)
         .setSendUserAgent(true)
         .build();
      Response response = new JsoupDataFetcher().get(session, request);

      if (!response.isSuccess()) {
         response = retryRequest(request);
      }

      this.cookies = response.getCookies();
   }

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPERCARD.toString(), Card.ELO.toString());
   private static final String MAINSELLER = "Pedidos Ya";

   private final List<String> proxies = Arrays.asList(
      ProxyCollection.LUMINATI_SERVER_BR,
      ProxyCollection.BUY,
      ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY);


   @Override
   protected Response fetchResponse() {
      String internalId = getInternalIdFromUrl();

      String storeId = session.getOptions().optString("store_id");

      String url = "https://www.pedidosya.com.ar/mobile/v1/products/" + internalId + "?restaurantId=" + storeId + "&businessType=GROCERIES";
      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", CommonMethods.cookiesToString(this.cookies));
      headers.put("authority", "www.pedidosya.com.ar");
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("referer", session.getOriginalURL());

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).setCookies(cookies).setProxyservice(proxies)
         .setSendUserAgent(true)
         .build();

      Response response = this.dataFetcher.get(session, request);

      if (!response.isSuccess()) {
         response = retryRequest(request);
      }

      return response;
   }

   private Response retryRequest(Request request) {
     Response response = new JsoupDataFetcher().get(session, request);

      if (!response.isSuccess()) {
         int tries = 0;
         while (!response.isSuccess() && tries < 3) {
            tries++;
            if (tries % 2 == 0) {
               response = new ApacheDataFetcher().get(session, request);
            } else {
               response = this.dataFetcher.get(session, request);
            }
         }
      }

      return response;
   }

   @Override
   public List<Product> extractInformation(JSONObject productInfo) throws Exception {
      super.extractInformation(productInfo);
      List<Product> products = new ArrayList<>();

      if (productInfo.has("name")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = productInfo.optString("id");
         String internalPid = internalId;
         String name = productInfo.optString("name");
         String primaryImage = "https://images.deliveryhero.io/image/pedidosya/products/" + productInfo.optString("image");
         boolean available = productInfo.optBoolean("enabled");
         Offers offers = available ? scrapOffers(productInfo) : new Offers();
         String description = productInfo.optString("description", "");
         List<String> eans = Collections.singletonList(productInfo.optString("gtin"));
         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String getInternalIdFromUrl() {
      String originalUrl = CommonMethods.getLast(session.getOriginalURL().split("p="));
      originalUrl = originalUrl.split("&")[0];
      return originalUrl;
   }

   private Offers scrapOffers(JSONObject productInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productInfo);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setMainPagePosition(1)
         .setSellerFullName(MAINSELLER)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setSales(sales)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      return sales;
   }


   private Pricing scrapPricing(JSONObject productInfo) throws MalformedPricingException {

      Double spotlightPrice = productInfo.optDouble("price");

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();

   }


   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());


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
