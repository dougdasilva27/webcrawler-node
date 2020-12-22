package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class BrasilLojastaqiCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   private static final String SELLER_NAME = "Lojas Taqi Brasil";

   public BrasilLojastaqiCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   protected JSONObject fetch() {
      String id = getProductId();
      Map<String, String> headers = getHeaders();
      String API = "https://www.taqi.com.br/ccstoreui/v1/products?productIds=" + id;

      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setHeaders(headers)
         .build();
      String content = new FetcherDataFetcher()
         .get(session, request)
         .getBody();

      if (content == null || content.isEmpty()) {
         content = this.dataFetcher.get(session, request).getBody();
      }

      return CrawlerUtils.stringToJson(content);
   }

   private JSONObject getProductInfo() {
      String id = getProductId();
      Map<String, String> headers = getHeaders();
      String apiUrl = "https://www.taqi.com.br/ccstoreui/v1/stockStatus?products=" + id;

      Request request = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .setHeaders(headers)
         .build();
      String content = new FetcherDataFetcher()
         .get(session, request)
         .getBody();

      if (content == null || content.isEmpty()) {
         content = this.dataFetcher.get(session, request).getBody();
      }

      return CrawlerUtils.stringToJson(content);

   }

   private String getProductId() {
      String[] url = session.getOriginalURL().split("/");
      return url[url.length - 1].split("\\?")[0];
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (!json.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         Object dataJson = json.query("/items");

         JSONObject stockInfo = getProductInfo();
         String internalId = getProductId();
         String internalPid = JSONUtils.getValueRecursive(dataJson, "0.id", String.class);
         String name = JSONUtils.getValueRecursive(dataJson, "0.displayName", String.class);
         JSONArray imageJson = JSONUtils.getValueRecursive(dataJson, "0.largeImageURLs", JSONArray.class);
         List<String> images = imageJson != null ? CrawlerUtils.scrapImagesListFromJSONArray(imageJson, null, null, "https", "www.taqi.com.br", session) : null;
         String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
         String description = JSONUtils.getValueRecursive(dataJson, "0.x_caracteristicasHtml", String.class);
         String stock = JSONUtils.getValueRecursive(stockInfo, "items.0.stockStatus", String.class);
         boolean available = stock != null ? !stock.contains("OUT_OF_STOCK") : null;
         Offers offers = available ? scrapOffers(dataJson) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(Object jsonObject) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonObject);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String salesOnJson = CrawlerUtils.calculateSales(pricing);
      sales.add(salesOnJson);
      return sales;
   }

   private Map<String, String> getHeaders() {
      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
      headers.put("Referer", session.getOriginalURL());
      headers.put("content-type", "application/json; charset=UTF-8");

      return headers;
   }


   private JSONObject getPrices() {
      Map<String, String> headers = getHeaders();
      String API = "https://www.taqi.com.br/ccstorex/custom/v1/parcelamento/getParcelamentos";
      String payload = "{\"produtos\":[{\"id\":\"" + getProductId() + "\",\"quantity\":1}],\"siteId\":\"siteUS\",\"catalogId\":\"cloudCatalog\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setHeaders(headers)
         .setPayload(payload)
         .build();
      String content = new JsoupDataFetcher()
         .post(session, request)
         .getBody();

      if (content == null || content.isEmpty()) {
         content = this.dataFetcher.get(session, request).getBody();
      }

      return CrawlerUtils.stringToJson(content);

   }

   private Pricing scrapPricing(Object jsonObject) throws MalformedPricingException {

      JSONObject priceInfo = getPrices();
      Double priceFrom = JSONUtils.getValueRecursive(jsonObject, "0.listPrices.real", Double.class);
      String spotlightPriceStr = JSONUtils.getValueRecursive(priceInfo, "produtos.0.parcelas.boleto.0.value", String.class);
      Double spotlightPrice = spotlightPriceStr != null ? MathUtils.parseDoubleWithDot(spotlightPriceStr) : null;
      CreditCards creditCards = scrapCreditCards(priceInfo);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(JSONObject priceInfo) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      JSONArray installmentsArray = JSONUtils.getValueRecursive(priceInfo, "produtos.0.parcelas.outros", JSONArray.class);
      Installments installments = new Installments();
      if (installmentsArray != null) {
         for (Object obj : installmentsArray) {
            if (obj instanceof JSONObject) {
               JSONObject installmentsJson = (JSONObject) obj;
               Integer key = installmentsJson.optInt("key");
               String valueStr = installmentsJson.optString("total");
               Double value = MathUtils.parseDoubleWithDot(valueStr);
               Double total = installmentsJson.optDouble("total");
               installments.add(Installment.InstallmentBuilder.create()
                  .setInstallmentNumber(key)
                  .setFinalPrice(total)
                  .setInstallmentPrice(value)
                  .build());

            }
         }
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

   //Site hasn't Rating
}
