package br.com.lett.crawlernode.crawlers.corecontent.peru;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PeruFazilCrawler extends Crawler {
   public PeruFazilCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   String SELLER_NAME = "tottusYaChannel";
   private final String storeName = getStoreName();

   protected String getStoreName() {
      return session.getOptions().optString("store_name");
   }

   @Override
   protected Response fetchResponse() {
      String url = "https://www.tottus.com.pe/s/product-search/v2/key/" + session.getOriginalURL() + "?returnAvailabilityKeys=true";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
         ))
         .build();
      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), true);
      return response;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json.has("results")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         JSONObject productSku = JSONUtils.getValueRecursive(json, "results.0", JSONObject.class);

         String internalId = productSku.optString("productId");
         String internalPid = productSku.optString("sku");
         String name = JSONUtils.getValueRecursive(productSku, "name.es-PE", String.class);
         String description = JSONUtils.getValueRecursive(productSku, "description.es-PE", String.class);
         List<String> listImages = getImageList(productSku);
         String primaryImage = !listImages.isEmpty() ? listImages.remove(0) : null;

         boolean isAvailable = getAvailability(productSku);
         Offers offers = isAvailable ? scrapOffers(productSku.optJSONArray("prices")) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setDescription(description)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(listImages)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean getAvailability(JSONObject productSku) {
      JSONArray inStock = JSONUtils.getValueRecursive(productSku, "availability.isOnStock", JSONArray.class);
      for (int i = 0; i < inStock.length(); i++) {
         String currentValue = inStock.optString(i);
         if (currentValue != null && currentValue.equals(storeName)) {
            return true;
         }
      }
      return false;
   }

   private List<String> getImageList(JSONObject productSku) {
      List<String> imagesList = new ArrayList<>();
      JSONArray imageArray = JSONUtils.getValueRecursive(productSku, "images", JSONArray.class);
      for (int i = 0; imageArray.length() > i; i++) {
         String aux = imageArray.get(i).toString();
         if (aux != null && !aux.isEmpty()) {
            imagesList.add(aux);
         }
      }
      return imagesList;
   }

   protected Offers scrapOffers(JSONArray json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONArray json) throws MalformedPricingException {
      JSONObject priceFields = getPriceFromStore(json);

      Double spotlightPrice = null;
      Double priceFrom = null;

      if (priceFields != null) {
         spotlightPrice = priceFields.optDouble("unitRegularPrice", 0);
         priceFrom = priceFields.optDouble("unitPrice", 0);

         spotlightPrice = spotlightPrice / 100000;
         priceFrom = priceFrom / 100000;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private JSONObject getPriceFromStore(JSONArray json) {
      for (Object o : json) {
         String channel = JSONUtils.getValueRecursive(o, "channel.key", String.class);
         if (channel.equals(SELLER_NAME)) {
            return JSONUtils.getValueRecursive(o, "custom.fields", JSONObject.class);
         }
      }
      return null;
   }

   private CreditCards scrapCreditCards(Double spotlighPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlighPrice)
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
