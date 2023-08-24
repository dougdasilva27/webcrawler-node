package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
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
import models.pricing.BankSlip;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilOxxoCrawler extends Crawler {
   private static final String SELLER_NAME_LOWER = "Oxxo";
   private String hostName = "https://www.oxxo.com.br";
   private String locationId = this.session.getOptions().optString("locationId", "1097153");
   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.AMEX.toString(), Card.ELO.toString());

   public BrasilOxxoCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.HTTPCLIENT);
      super.config.setParser(Parser.JSON);
   }

   @Override
   protected Response fetchResponse() {
      String internalId = getInternalId();
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("https://www.oxxo.com.br/ccstore/v1/products/" + internalId))
            .build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         return new Response.ResponseBuilder()
            .setBody(response.body())
            .setLastStatusCode(response.statusCode())
            .build();
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }

   }

   @Override
   public List<Product> extractInformation(JSONObject productJson) throws Exception {
      List<Product> products = new ArrayList<>();

      if (productJson != null && !productJson.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = getInternalId();
         String name = productJson.optString("displayName");
         String description = getDescription(productJson);
         List<String> categories = Collections.singletonList(productJson.optString("type"));
         String primaryImage = hostName + productJson.optString("primaryFullImageURL");
         List<String> secondaryImages = getSecondaryImages(productJson);
         boolean isAvailable = isProductAvailable(internalId);
         JSONObject priceFromApi = getProductPrice(internalId);
         Offers offers = isAvailable ? scrapOffers(priceFromApi, internalId) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String getInternalId() {
      return CommonMethods.getLast(this.session.getOriginalURL().split("/"));
   }

   private String getDescription(JSONObject skuInfo) {
      StringBuilder description = new StringBuilder();

      String longDescription = skuInfo.optString("longDescription");
      if (longDescription != null) {
         description.append(longDescription);
      }

      String shortDescription = skuInfo.optString("description");
      if (shortDescription != null) {
         description.append(shortDescription);
      }

      return description.toString();
   }

   private boolean isProductAvailable(String internalId) {
      HttpResponse<String> response;
      JSONObject skuInfo;
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("https://www.oxxo.com.br/ccstore/v1/inventories?ids=" + internalId + "_" + locationId))
            .build();
         response = client.send(request, HttpResponse.BodyHandlers.ofString());

      } catch (Exception e) {
         throw new RuntimeException("Failed in load document: " + session.getOriginalURL(), e);
      }

      skuInfo = JSONUtils.stringToJson(response.body());
      String available = JSONUtils.getValueRecursive(skuInfo, "items.0.availabilityStatusMsg", String.class, "");

      return available.equalsIgnoreCase("outOfStock") ? false : true;
   }

   private List<String> getSecondaryImages(JSONObject skuInfo) {
      List<String> secondaryImages = new ArrayList<>();
      JSONArray imagesArray = JSONUtils.getValueRecursive(skuInfo, "largeImageURLs", JSONArray.class, new JSONArray());
      if (imagesArray != null) {
         for (int i = 0; i < imagesArray.length(); i++) {
            String image = hostName + imagesArray.get(i);
            if (image != null) {
               secondaryImages.add(image);
            }
         }
      }
      if (secondaryImages.size() > 0) {
         secondaryImages.remove(0);
      }

      return secondaryImages;
   }

   private JSONObject getProductPrice(String internalId) {
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("https://www.oxxo.com.br/ccstore/v1/prices/skus?ids=" + internalId + "_" + locationId))
            .build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         return JSONUtils.stringToJson(response.body());
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }
   }

   private Offers scrapOffers(JSONObject productList, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productList, internalId);
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME_LOWER)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setSales(sales)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject skuInfo, String internalId) throws MalformedPricingException {

      Double priceFrom = JSONUtils.getValueRecursive(skuInfo, "items.0." + internalId + "_" + locationId + ".listPrice", Double.class, null);
      Double spotlightPrice = JSONUtils.getValueRecursive(skuInfo, "items.0." + internalId + "_" + locationId + ".salePrice", Double.class, null);

      if (priceFrom == 0.0) {
         priceFrom = null;
      }

      if (priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }


}
