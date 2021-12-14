package br.com.lett.crawlernode.crawlers.corecontent.equador;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EquadorFrecuentoCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Frecuento";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DISCOVER.toString(), Card.DINERS.toString(), Card.AMEX.toString());

   public EquadorFrecuentoCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
      super.config.setFetcher(FetchMode.APACHE);
   }


   @Override
   protected Response fetchResponse() {
      List<Product> products = new ArrayList<>();

      String regex = "www.frecuento.com?\\/([0-9a-zA-Z\\s\\-]*)\\/?([0-9]*)";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(session.getOriginalURL());

      String id = null;
      if (matcher.find()) {
         id = matcher.group(2);
      }
      Request request = Request.RequestBuilder.create()
         .setUrl("https://app.frecuento.com/products/" + id)
         .setFollowRedirects(true)
         .build();
      return this.dataFetcher.get(session, request);
   }

   @Override
   public List<Product> extractInformation(JSONObject jsonObject) throws Exception {

      List<Product> products = new ArrayList<>();

      String primaryImage = (String) jsonObject.optJSONArray("photos").get(0);
      CategoryCollection categories = scrapCategories(jsonObject);
      List<String> secondaryImages = scrapSecondaryImages(jsonObject, primaryImage);

      Product product = ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(jsonObject.optString("code"))
         .setInternalPid(jsonObject.optString("id"))
         .setName(jsonObject.optString("name"))
         .setPrimaryImage(primaryImage)
         .setSecondaryImages(secondaryImages)
         .setCategories(categories)
         .setDescription(jsonObject.optString("description"))
         .setOffers(scrapOffer(jsonObject))
         .build();

      products.add(product);


      return products;

   }

   private List<String> scrapSecondaryImages(JSONObject jsonObject, String primaryImage) {
      List<String> secondaryImages = new ArrayList<>();
      for(Object image : jsonObject.optJSONArray("photos")) {
         if(image instanceof String && !((String) image).equals(primaryImage)) {
            secondaryImages.add((String) image);
         }
      }
      return secondaryImages;
   }

   private CategoryCollection scrapCategories(JSONObject jsonObject) {
      CategoryCollection categories = new CategoryCollection();
      for(Object category : jsonObject.optJSONArray("categories")) {
         if(category instanceof JSONObject) {
            JSONObject categoryJson = (JSONObject) category;
            categories.add(categoryJson.optString("name"));
         }
      }
      return categories;
   }


   private Offers scrapOffer(JSONObject obj) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(obj);

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

   private Pricing scrapPricing(JSONObject obj) throws MalformedPricingException {
      Double spotlightPrice = obj.optDouble("amount_total");
      Double priceFrom = obj.optDouble("amount_incl_tax");

      if (spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
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
