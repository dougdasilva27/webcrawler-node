package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BrasilFrigelarCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.frigelar.com.br/";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   private static final String SELLER_NAME = "frigelar";

   public BrasilFrigelarCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected JSONObject fetch() {
      String nameProduct = CommonMethods.getLast(session.getOriginalURL().split("br/"));
      String url = HOME_PAGE + "ccstoreui/v1/pages/" + nameProduct + "?dataOnly=false&cacheableDataOnly=true&productTypesRequired=false";

      Request request = RequestBuilder.create().setUrl(url).build();

      String content = this.dataFetcher.get(session, request).getBody();

      JSONObject jsonObject = CrawlerUtils.stringToJson(content);
      return jsonObject;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      JSONObject productInfo = JSONUtils.getValueRecursive(json, "data.page.product", JSONObject.class);

      if (productInfo != null && !productInfo.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());


         String internalId = productInfo.optString("id");
         String name = productInfo.optString("displayName");
         List<String> images = CrawlerUtils.scrapImagesListFromJSONArray(productInfo.optJSONArray("mediumImageURLs"), null, null, "https", "www.frigelar.com.br", session);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         String description = productInfo.optString("longDescription");
         Offers offers = scrapOffers(productInfo);
         // I can'not catch availability, this crawler is only to EQI, in the EQI doesn't matter. In api of ranking have availability.

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
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

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      String salesOnJson = CrawlerUtils.calculateSales(pricing);
      if (salesOnJson != null) {
         sales.add(salesOnJson);

      }
      return sales;
   }

   private Pricing scrapPricing(JSONObject productInfo) throws MalformedPricingException {

      Double priceFrom = JSONUtils.getValueRecursive(productInfo, "childSKUs.0.listPrices.frigelarDefaultPF", Double.class);
      Double spotlightPrice = JSONUtils.getValueRecursive(productInfo, "listPrices.frigelarDefaultPF", Double.class);
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
