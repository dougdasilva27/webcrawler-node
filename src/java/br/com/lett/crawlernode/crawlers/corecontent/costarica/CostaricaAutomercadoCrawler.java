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
import org.json.JSONArray;
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
      String internalPid = getProductPid();
      String API = "https://www.automercado.cr/algoliaSearch";

      String payload = "{\"facetFilters\":[[\"productNumber:" + internalPid + "\"],[\"storeid:03\"]]}";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json;charset=UTF-8");
      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setPayload(payload)
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .build();
      String content = this.dataFetcher
         .post(session, request)
         .getBody();

      JSONArray jsonArray = CrawlerUtils.stringToJsonArray(content);


      return jsonArray != null ? (JSONObject) jsonArray.get(0) : null;
   }


   private String getProductPid() {
      String url = null;
      Pattern pattern = Pattern.compile("\\/([0-9]*)\\?");
      Matcher matcher = pattern.matcher(session.getOriginalURL());
      if (matcher.find()) {
         url = matcher.group(1);
      }
      return url;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (!json.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = json.optString("productID");
         String internalPid = getProductPid();
         String name = json.optString("ecomDescription");
         String primaryImage = json.optString("imageUrl");
         String description = json.optString("ecomDescription");
         boolean available = json.optBoolean("productAvailable");
         Offers offers = available ? scrapOffers(json) : new Offers();

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

   private Offers scrapOffers(JSONObject productInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productInfo);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;

   }


   private Pricing scrapPricing(JSONObject productInfo) throws MalformedPricingException {

      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(productInfo, "uomPrice", true);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
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
