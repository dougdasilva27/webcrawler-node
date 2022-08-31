package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
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

public class TottusCrawler extends Crawler {

   protected String homePage;
   protected String sellerName = "Tottus Peru";

   protected String channel = session.getOptions().optString("channel");


   public TottusCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   @Override
   protected Response fetchResponse() {
      String url = "https://www.tottus.com.pe/api/product-search/key/" + session.getOriginalURL().replace("https://www.tottus.com.pe/", "").replace("/p/", "");


      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "www.tottus.com.pe");
      headers.put("content-type", "application/json");

      if (channel != null) {
         headers.put("cookie", channel);
      }

      Request request = Request.RequestBuilder.create().setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .build();

      return this.dataFetcher.get(session, request);
   }


   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   public List<Product> extractInformation(JSONObject jsonObject) throws Exception {
      List<Product> products = new ArrayList<>();


      if (jsonObject.has("productType")) {

         Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());

         String name = jsonObject.optString("name");
         String internalId = jsonObject.optString("sku");
         List<String> images = crawlImages(jsonObject);
         String primaryImage = images != null ? images.remove(0) : null;
         String description = jsonObject.optString("description");
         boolean available = !JSONUtils.getValueRecursive(jsonObject, "availability.isOnStock", JSONArray.class, new JSONArray()).isEmpty();
         Offers offers = available ? scrapOffer(jsonObject) : new Offers();

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

      }

      return products;

   }

   private List<String> crawlImages(JSONObject jsonObject) {
      List<String> imagesList = new ArrayList<>();
      JSONArray images = JSONUtils.getJSONArrayValue(jsonObject, "images");

      if (images != null) {
         images.forEach(image -> {
            imagesList.add(image.toString());
         });
      }

      return imagesList;

   }

   private Offers scrapOffer(JSONObject jsonObject) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonObject);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(JSONObject jsonObject) throws MalformedPricingException {
      JSONObject prices = jsonObject.optJSONObject("prices");
      Double spotlightPrice = null;
      Double priceFrom = null;
      if (prices != null) {
         spotlightPrice = JSONUtils.getDoubleValueFromJSON(prices, "currentPrice", true);
         priceFrom = JSONUtils.getDoubleValueFromJSON(prices, "regularPrice", true);
      }

      if (spotlightPrice == null) {
         spotlightPrice = priceFrom;
         priceFrom = null;
      }

      if (Objects.equals(spotlightPrice, priceFrom)) {
         priceFrom = null;

      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
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


      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

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
