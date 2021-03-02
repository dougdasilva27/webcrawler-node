package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class BrasilLojasmmCrawler extends Crawler {

   private static final String MAINSELLER = "LojasMM";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPERCARD.toString(), Card.ELO.toString(),
      Card.SOROCRED.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public BrasilLojasmmCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      String HOME_PAGE = "https://www.lojasmm.com/";
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected JSONObject fetch() {
      String id = getProductId();
      String url = "https://www.allfront.com.br/api/product/" + id;

      Map<String, String> headers = new HashMap<>();
      headers.put("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6bnVsbCwiYWRtaW4iOnRydWUsImp0aSI6IjBkYmQ4ZWMyNTZhN2ZkNDdjZGY2NmNlN2M1NmI1YjVmNDI4MmU5MDI1MmM5NjllMzJlNWM5ZjJhNWJlYWEyY2EiLCJpYXQiOjE2MDE2NjYxNjcsImV4cCI6MTY1MTI1OTc2NywiZW1haWwiOiJldmVydG9uQHByZWNvZGUuY29tLmJyIiwiZW1wcmVzYSI6bnVsbCwic2NoZW1hIjoiTG9qYXNtbSIsImlkU2NoZW1hIjo0LCJpZFNlbGxlciI6IjExIiwiaWRVc2VyIjoxfQ==.mWjRUrIGznvcrZgpfL0rZsGs+hUA5VJ2uZQYqBmsvWg=");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();


      JSONArray jsonArray = CrawlerUtils.stringToJsonArray(content);

      return !jsonArray.isEmpty() ? JSONUtils.getValueRecursive(jsonArray, "0", JSONObject.class) : null;
   }

   private String getProductId() {

      if (session.getOriginalURL().contains("produto/")) {
         String[] getIdArray = session.getOriginalURL().split("produto/");
         if (getIdArray.length > 0) {
            return getIdArray[1].split("/")[0];
         }
      }
      return null;
   }


   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json.has("productInfo")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = getProductId();
         String name = JSONUtils.getStringValue(json, "nome");
         JSONArray variations = JSONUtils.getValueRecursive(json, "sellers.0.gradeSeller", JSONArray.class);
         JSONObject scrapSeller = JSONUtils.getValueRecursive(json, "sellers.0", JSONObject.class);
         String description = json.optString("descricao_geral");
         CategoryCollection categories = scrapCategories(json);
         List<String> images = CrawlerUtils.scrapImagesListFromJSONArray(json.optJSONArray("images"), "images", null, "https", "static.lojasmm.com", session);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;

         if (variations != null && scrapSeller != null) {

            for (Object e : variations) {

               JSONObject variation = (JSONObject) e;
               String nameVolts = getName(variation, name);
               String internalId = variation.optString("sku");
               boolean available = variation.optInt("disponivel") > 0;
               Offers offers = available ? scrapOffer(variation, scrapSeller) : new Offers();

               Product product = ProductBuilder
                  .create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(nameVolts)
                  .setDescription(description)
                  .setCategories(categories)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(images)
                  .setOffers(offers)
                  .build();

               products.add(product);
            }
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String getName(JSONObject variation, String name) {
      Integer value = JSONUtils.getIntegerValueFromJSON(variation, "idGradeY", 0);
      String volts = null;
      switch (value) {
         case 2:
            volts = "110 Volts";
            break;
         case 4:
            volts = "220 Volts";
            break;
         case 6:
            volts = "Bvolt";
            break;
         default:
            break;
      }

      return name != null ? name + " " + volts : null;
   }

   private CategoryCollection scrapCategories(JSONObject data) {
      CategoryCollection categoryCollection = new CategoryCollection();
      categoryCollection.add(data.optString("categoria"));
      categoryCollection.add(data.optString("subcategoria"));
      categoryCollection.add(data.optString("tercategoria"));

      return categoryCollection;

   }

   private String scrapSellerId(JSONObject scrapSeller) {

      return Integer.toString(scrapSeller.optInt("idSeller"));
   }

   private String scrapSellerName(JSONObject scrapSeller) {

      return scrapSeller.optString("sellerName");

   }

   private Offers scrapOffer(JSONObject variation, JSONObject scrapSeller) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(variation);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(false)
         .setInternalSellerId(scrapSellerId(scrapSeller))
         .setMainPagePosition(1)
         .setSellerFullName(scrapSellerName(scrapSeller))
         .setIsBuybox(true)
         .setIsMainRetailer(scrapSellerName(scrapSeller).equalsIgnoreCase(MAINSELLER))
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


   private Pricing scrapPricing(JSONObject variation) throws MalformedPricingException {

      Double priceFrom = variation.optDouble("preco");
      Double spotlightPrice = variation.optDouble("preco_promocional");

      CreditCards creditCards = scrapCreditCards(variation, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(scrapBankSlip(variation))
         .setCreditCards(creditCards)
         .build();

   }

   private BankSlip scrapBankSlip(JSONObject offer) throws MalformedPricingException {

      return BankSlip.BankSlipBuilder.create()
         .setFinalPrice(offer.getDouble("boletoValor"))
         .build();
   }

   private Pair<Integer, Float> pair(JSONObject variation) {
      JSONArray installmentsInfo = variation.optJSONArray("parcelamento");
      Pair<Integer, Float> pair = new Pair<>();
      if (installmentsInfo != null) {
         for (Object obj : installmentsInfo) {
            if (obj instanceof JSONObject) {
               JSONObject installmentJson = (JSONObject) obj;
               String valueStr = installmentJson.optString("valor");
               String instalmentStr = installmentJson.optString("parcela");

               String installmentText = instalmentStr != null && valueStr != null ? instalmentStr + valueStr : null;

               pair = installmentText != null ? CrawlerUtils.crawlSimpleInstallmentFromString(installmentText, "x", "juros", true) : null;

            }
         }
      }
      return pair;
   }


   private CreditCards scrapCreditCards(JSONObject variation, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      Pair<Integer, Float> pair = pair(variation);

      if (pair != null && !pair.isAnyValueNull()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(pair.getFirst())
            .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
            .build());

      } else {

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
