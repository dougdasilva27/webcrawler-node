package br.com.lett.crawlernode.crawlers.corecontent.costarica;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CostaricaAutomercadoCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   private static final String SELLER_NAME = "automercado";

   public CostaricaAutomercadoCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   protected JSONObject fetch() {
      String internalId = getProductId();
      String API = "https://www.automercado.cr/prod-front/product/detail";

      String payload = "{\"productid\":\"" + internalId + "\"}";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json;charset=UTF-8");
      headers.put("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6IlVTVUFSSU8gSU5WSVRBRE8iLCJzdWIiOiJjNjY5MDc0MS01Mjk2LWViMTEtYjFhYy0wMDBkM2EzNzY4MGIiLCJlbWFpbCI6Imludml0YWRvQGF1dG9tZXJjYWRvLmJpeiIsImlhdCI6MTYyNjM1ODQ3OX0.sR8zc4wIdfITf8WvKR26wPz8M79Xn_I4UKd-VXJXD9o");
      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setPayload(payload)
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .build();
      String content = this.dataFetcher
         .post(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);
   }

   private String getProductId() {
      String internalId = null;
      Pattern pattern = Pattern.compile("id\\/(.*)\\?");
      Matcher matcher = pattern.matcher(session.getOriginalURL());
      if (matcher.find()) {
         internalId = matcher.group(1);
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
         String description = data.optString("description");
         Offers offers = available ? scrapOffers(data) : new Offers();

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
