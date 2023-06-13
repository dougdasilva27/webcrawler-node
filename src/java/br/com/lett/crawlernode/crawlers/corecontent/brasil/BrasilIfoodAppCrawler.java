package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class BrasilIfoodAppCrawler extends Crawler {
   public BrasilIfoodAppCrawler(Session session) {
      super(session);
      config.setParser(Parser.JSON);
   }

   private static final String baseImageUrl = "https://static.ifood-static.com.br/image/upload/t_medium/pratos/";

   private final String merchant_id = session.getOptions().optString("merchant_id");
   private final String access_key = session.getOptions().optString("access_key");
   private final String secret_key = session.getOptions().optString("secret_key");

   private final String state = session.getOptions().optString("state");

   private static final String sellerFullName = "ifood app";

   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   @Override
   protected Response fetchResponse() {
      String url = "https://wsloja.ifood.com.br/ifood-ws-v3/restaurants/" + merchant_id + "/menuitem/" + session.getOriginalURL();
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .header("channel", "IFOOD")
            .header("user-agent", "okhttp/4.10.0")
            .header("app_package_name", "br.com.brainweb.ifood")
            .header("authority", "marketplace.ifood.com.br")
            .header("sec-fetch-dest", "empty")
            .header("sec-fetch-mode", "cors")
            .header("state", state)
            .header("access_key", access_key)
            .header("secret_key", secret_key)
            .build();

         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         return new Response.ResponseBuilder()
            .setBody(response.body())
            .setLastStatusCode(response.statusCode())
            .build();
      } catch (IOException | InterruptedException e) {
         throw new RuntimeException("Failed to load document: " + session.getOriginalURL(), e);
      }
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();
      if (json != null && !json.isEmpty() && json.has("data")) {
         Logging.printLogDebug(logger, session, "Item app identified: " + this.session.getOriginalURL());
         JSONObject item = JSONUtils.getValueRecursive(json, "data.menu.0.itens.0", JSONObject.class, new JSONObject());
         if (!item.isEmpty()) {
            String internalId = item.optString("id");
            String name = item.optString("description");
            String description = item.optString("additionalInfo");
            String ean = item.optString("ean");
            List<String> images = scrapImages(item);
            String primaryImage = images.size() > 0 ? images.remove(0) : null;
            String availability = item.optString("availability", "");
            Offers offers = availability.equals("AVAILABLE") ? scrapOffers(item) : new Offers();

            Product product = ProductBuilder.create()
               .setUrl(internalId)
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setEans(List.of(ean))
               .setDescription(description)
               .setOffers(offers)
               .build();
            products.add(product);

         }
      } else {
         String msg = json != null && !json.isEmpty() ? json.optString("message") : null;
         String messageError = msg != null ? "Not a item app " + this.session.getOriginalURL() + msg : "Not a item app " + this.session.getOriginalURL();
         Logging.printLogDebug(logger, session, messageError);
      }
      return products;
   }

   private List<String> scrapImages(JSONObject json) {
      JSONArray suffixesImages = json.optJSONArray("logosUrls");
      List<String> images = new ArrayList<>();
      if (suffixesImages != null && !suffixesImages.isEmpty()) {
         for (Object s : suffixesImages) {
            String suffixImage = (String) s;
            if (suffixImage != null && !suffixImage.isEmpty()) {
               images.add(baseImageUrl + suffixImage);
            }
         }
      }
      return images;
   }

   private Offers scrapOffers(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerFullName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double priceFrom = json.optDouble("unitPrice");
      Double spotlightPrice = json.optDouble("unitMinPrice");
      if (spotlightPrice.isNaN()) {
         spotlightPrice = priceFrom;
         priceFrom = null;
      }
      if (priceFrom != null && priceFrom.equals(spotlightPrice)) {
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
      if (installments.getInstallments().isEmpty()) {
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
