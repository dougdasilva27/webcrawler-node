package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CornershopCrawler extends Crawler {

   public CornershopCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.FETCHER);

   }

   private String storeId = getStoreId();
   private final String SELLER_FULL_NAME = getSellerName();

   protected String getStoreId(){
      return session.getOptions().optString("STORE_ID");
   }

   private static final String HOME_PAGE = "https://web.cornershopapp.com";
   private static final String PRODUCTS_API_URL = "https://cornershopapp.com/api/v1/branches/";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(),
      Card.DINERS.toString(), Card.SHOP_CARD.toString());

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   protected String getSellerName() {
      return session.getOptions().optString("SELLER_FULL_NAME");
   }

   @Override
   public Object fetch() {
      String url = session.getOriginalURL();

      if (url.contains("product/")) {
         String id = CommonMethods.getLast(url.split("product/")).split("//?")[0].trim();

         if (!id.isEmpty()) {
            String urlApi = PRODUCTS_API_URL + storeId + "/products/" + id;

            Request request = RequestBuilder.create().setUrl(urlApi).setCookies(cookies).build();

            // fetcher is the best option because another services have problem with accents
            JSONArray array = CrawlerUtils.stringToJsonArray(new JsoupDataFetcher().get(session, request).getBody());

            if (array.length() > 0) {
               return array.getJSONObject(0);
            }
         }
      }

      return new JSONObject();
   }

   @Override
   public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
      List<Product> products = new ArrayList<>();

      if (jsonSku.length() > 0) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         Integer internalIdInt = JSONUtils.getIntegerValueFromJSON(jsonSku, "id", 0);
         String internalId = internalIdInt != null ? internalIdInt.toString() : null;
         String internalPid = internalId;
         CategoryCollection categories = new CategoryCollection();
         String description = crawlDescription(jsonSku);
         boolean available = crawlAvailability(jsonSku);
         String primaryImage = JSONUtils.getStringValue(jsonSku, "img_url");
         String name = crawlName(jsonSku);
         Offers offers = available ? scrapOffers(jsonSku) : new Offers();


         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setDescription(description)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setOffers(offers)
            .build();
         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private Offers scrapOffers(JSONObject jsonSku) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonSku);
      List<String> sales = new ArrayList<>();

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

   private Pricing scrapPricing(JSONObject jsonSku) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(jsonSku, "price", false);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(jsonSku, "original_price", false);
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

   private String crawlDescription(JSONObject json) {
      StringBuilder description = new StringBuilder();
      if (json.has("package") && json.get("package") instanceof String) {
         description.append(json.getString("package"));
         description.append("</br>");
      }
      if (json.has("description") && json.get("description") instanceof String) {
         description.append(json.getString("description"));
      }
      return description.toString();
   }

   private String crawlName(JSONObject json) {
      StringBuilder nameComplete = new StringBuilder();

      String name = JSONUtils.getStringValue(json, "name");
      String brand = JSONUtils.getValueRecursive(json, "brand.name", String.class);
      String quantity = JSONUtils.getStringValue(json, "package");
      if (name != null) {
         nameComplete.append(name).append(" . ");

         if (brand != null) {
            nameComplete.append(brand).append(" ");
         }
         if (quantity != null) {
            nameComplete.append(quantity);
         }
      }

      return nameComplete.toString();
   }


   private boolean crawlAvailability(JSONObject json) {
      return json.has("availability_status") && json.get("availability_status").toString().equalsIgnoreCase("available");
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
