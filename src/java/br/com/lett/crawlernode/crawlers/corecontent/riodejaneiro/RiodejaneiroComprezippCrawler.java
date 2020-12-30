package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;


import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import org.json.JSONObject;

import java.util.*;

public class RiodejaneiroComprezippCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   private static final String SELLER_NAME = "Compre Zipp";

   public RiodejaneiroComprezippCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   protected JSONObject fetch() {
      String slug = getProductSlug();
      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
      headers.put("Referer", session.getOriginalURL());
      String API = "https://search.comprezipp.com/search?filters%5Bslug%5D=" + slug;

      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setHeaders(headers)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);
   }

   private String getProductSlug() {
      String[] url = session.getOriginalURL().split("/");
      return url[url.length - 1].split("\\?")[0];
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();
      JSONObject data = JSONUtils.getValueRecursive(json, "data.0", JSONObject.class);
      if (data != null && !data.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = JSONUtils.getIntegerValueFromJSON(data, "id", 0).toString();
         String internalPid = internalId;
         String name = JSONUtils.getStringValue(data, "name");
         String primaryImage = JSONUtils.getStringValue(data, "image");
         //Site hasn't secondary images
         CategoryCollection categories = scrapCategories(data);
         String description = JSONUtils.getStringValue(data, "description");

         Boolean available = JSONUtils.getValueRecursive(data, "available", Boolean.class);
         Offers offers = available != null && available ? scrapOffers(data) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setCategories(categories)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(JSONObject data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(data);
      List<String> sales = scrapSales(data, pricing);

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

   private String scrapSalesSecondUnid(JSONObject data) {
      String sale = null;
      String discount = JSONUtils.getStringValue(data, "tag_left");
      if (!discount.isEmpty()) {
         String secondUnid = JSONUtils.getStringValue(data, "tag_right");
         sale = discount + " " + secondUnid;
      }
      return sale;
   }

   private List<String> scrapSales(JSONObject data, Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String salesOnJson = CrawlerUtils.calculateSales(pricing);
      if (salesOnJson != null) {
         sales.add(salesOnJson);
      }
      if (scrapSalesSecondUnid(data) != null) {
         sales.add(scrapSalesSecondUnid(data));
      }
      return sales;
   }

   private CategoryCollection scrapCategories(JSONObject data) {

      CategoryCollection categories = new CategoryCollection();

      categories.add(data.optString("department_slug", null));
      categories.add(data.optString("category_slug", null));

      return categories;
   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {

      String priceFromStr = JSONUtils.getValueRecursive(data, "price.value", String.class);
      Double priceFrom = priceFromStr != null ? MathUtils.parseDoubleWithDot(priceFromStr) : JSONUtils.getValueRecursive(data, "price.value", Double.class);
      String spotlightPriceStr = JSONUtils.getValueRecursive(data, "promotional_price.value", String.class);
      Double spotlightPrice = spotlightPriceStr != null ? MathUtils.parseDoubleWithDot(spotlightPriceStr) : JSONUtils.getValueRecursive(data, "promotional_price.value", Double.class);
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

   //Site hasn't Rating
}
