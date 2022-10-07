package br.com.lett.crawlernode.crawlers.corecontent.costarica;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
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
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CostaricaAutomercadoCrawler extends Crawler {
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   public CostaricaAutomercadoCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.FETCHER);
   }

   private static final String SELLER_NAME = "automercado";
   private final String AUTH_TOKEN = session.getOptions().optString("authToken");

   @Override
   protected JSONObject fetch() {
      String internalId = getProductId();
      String API = "https://automercado.azure-api.net/prod-front/product/detail";

      String payload = "{\"productid\":\"" + internalId + "\", \"includeSpecialProducts\": true}";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json;charset=UTF-8");
      headers.put("Authorization", AUTH_TOKEN);
      headers.put("Platform", "WEB");
      headers.put("Referer", "https://www.automercado.cr/");
      headers.put("Connection", "keep-alive");

      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.SMART_PROXY_BR,
            ProxyCollection.SMART_PROXY_CL,
            ProxyCollection.SMART_PROXY_CO_HAPROXY
         ))
         .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create().mustUseMovingAverage(true).build())
         .mustSendContentEncoding(false)
         .setSendUserAgent(true)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new ApacheDataFetcher(), new JsoupDataFetcher()), session, "post");

      return CrawlerUtils.stringToJson(response.getBody());
   }

   private String getProductId() {
      String internalId = null;
      Pattern pattern = Pattern.compile("id\\/(.*)\\?");
      Matcher matcher = pattern.matcher(session.getOriginalURL());
      if (matcher.find()) {
         internalId = matcher.group(1);
      }
      if (internalId == null) {
         return CommonMethods.getLast(session.getOriginalURL().split("id/"));
      }
      return internalId;

   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();
      JSONObject data = json.optJSONObject("data");

      if (data != null && !data.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = getProductId();
         String internalPid = data.optString("productNumber");
         boolean available = data.optBoolean("inStock");
         String name = getName(data, available);
         String primaryImage = JSONUtils.getValueRecursive(data, "gallery.0", String.class);
         List<String> secondaryImages = getSecundaryImages(data, primaryImage);
         String description = data.optString("description");
         Offers offers = available ? scrapOffers(data) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> getSecundaryImages(JSONObject data, String primaryImage) {
      JSONArray arraySecondaryImagens = JSONUtils.getValueRecursive(data, "gallery", JSONArray.class);
      List<String> list = new ArrayList<>();
      if (arraySecondaryImagens != null && !arraySecondaryImagens.isEmpty()) {
         for (Integer i = 0; i < arraySecondaryImagens.length(); i++) {
            String image = (String) arraySecondaryImagens.get(i);
            if (image!=null && !image.isEmpty() &&!image.equals(primaryImage)) {
               list.add(image);
            }
         }
      }

      return list;
   }

   private String getName(JSONObject data, boolean available) {
      StringBuilder buildName = new StringBuilder();
      String name = data.optString("name");
      if (name != null && available) {
         buildName.append(name);
         String brand = data.optString("brand");
         if (brand != null) {
            buildName.append(" - ").append(brand);
         }
      } else {
         name = data.optString("presentation");
         if (name != null) {
            buildName.append(name);
         }
      }

      return buildName.toString();
   }

   private Offers scrapOffers(JSONObject productInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productInfo);
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

   private Pricing scrapPricing(JSONObject productInfo) throws MalformedPricingException {

      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(productInfo, "price", true);
      Double priceFrom = JSONUtils.getValueRecursive(productInfo, "discount.textValue", Double.class);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
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

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      String sale = CrawlerUtils.calculateSales(pricing);

      if (sale != null) {
         sales.add(sale);
      }
      return sales;
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String brand : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }

}
