package br.com.lett.crawlernode.crawlers.corecontent.espana;

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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EspanaTiendaAnimalCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Tienda Animal";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public EspanaTiendaAnimalCrawler(Session session) {
      super(session);
   }

   protected JSONObject getVariations(String url) {

      Request request = Request.RequestBuilder.create().setUrl(url).build();
      String page = this.dataFetcher.get(session, request).getBody();

      return CrawlerUtils.stringToJson(page);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-detail.product-wrapper", "data-pid");
         Elements variations = doc.select(".row.mb-4 select.custom-select.form-control.d-none option:not(:first-child)");
         for (Element e : variations) {
            String variationUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "value");

            JSONObject json = getVariations(variationUrl);
            JSONObject productJson = json != null ? json.optJSONObject("product") : null;
            if (productJson != null) {
               String internalId = productJson.optString("uuid");
               String name = crawlName(productJson);
               String primaryImage = null;
               String description = null;
               boolean availableToBuy = JSONUtils.getValueRecursive(productJson, "availability.messages.0", String.class, "").contains("En Stock");
               Offers offers = availableToBuy ? scrapOffer(productJson) : new Offers();

               // Creating the product
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
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String crawlName(JSONObject jsonObject) {
      String name = jsonObject.optString("productName");
      String variation = JSONUtils.getValueRecursive(jsonObject, "variationAttributes.0.displayValue", String.class);
      if (variation != null && !variation.isEmpty()) {
         name = name + "-" + variation;
      }

      return name;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-detail.product-wrapper") != null;
   }

   private Offers scrapOffer(JSONObject jsonObject) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonObject);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private Pricing scrapPricing(JSONObject jsonObject) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getValueRecursive(jsonObject, "price.sales.value", Double.class);
      if (spotlightPrice == null){
         spotlightPrice = JSONUtils.getValueRecursive(jsonObject, "price.startingFromPrice.sales.value", Double.class);
      }
      String priceFromToConvert =  JSONUtils.getValueRecursive(jsonObject, "price.tiered.finalPrice", String.class, null);

      Double priceFrom = null;

      if (priceFromToConvert != null){
         priceFrom = Double.valueOf(priceFromToConvert.replaceAll("[^0-9.]", ""));
      }

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
