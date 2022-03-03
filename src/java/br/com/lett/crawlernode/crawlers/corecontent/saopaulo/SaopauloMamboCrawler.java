package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Date: 30/08/17
 *
 * @author gabriel
 */

public class SaopauloMamboCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.mambo.com.br/";
   private static final String SELLER_FULLNAME = "mambo-sao-paulo";

   protected Set<String> cards = Sets.newHashSet(
      Card.ELO.toString(),
      Card.VISA.toString(),
      Card.MASTERCARD.toString(),
      Card.AMEX.toString(),
      Card.HIPERCARD.toString(),
      Card.DINERS.toString());

   public SaopauloMamboCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected JSONObject fetch() {
      JSONObject api = new JSONObject();
      Request requestPage = RequestBuilder.create().setUrl(session.getOriginalURL()).setCookies(cookies).build();
      Response pageResponse = this.dataFetcher.get(session, requestPage);

      if (pageResponse.getLastStatusCode() == 200) {
         String[] tokens = session.getOriginalURL().split("\\?")[0].split("/");
         String id = CommonMethods.getLast(tokens);
         String pathName = tokens[tokens.length - 2];

         String apiUrl =
            "https://www.mambo.com.br/ccstoreui/v1/pages/" + pathName + "/p/" + id + "?dataOnly=false&cacheableDataOnly=true&productTypesRequired=true";

         Request request = RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).build();
         JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

         if (response.has("data")) {
            api = response.optJSONObject("data");
         }
      } else {
         Logging.printLogDebug(logger, session, "404 Page: " + this.session.getOriginalURL());
      }

      return api;
   }

   @Override
   public List<Product> extractInformation(JSONObject pageJson) throws Exception {
      super.extractInformation(pageJson);
      List<Product> products = new ArrayList<>();

      JSONObject json = JSONUtils.getValueRecursive(pageJson, "page.product", JSONObject.class);

      if (json != null && !json.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = crawlInternalPid(json);
         String name = JSONUtils.getStringValue(json, "displayName");

         //in this website, we dont have a unavailable product page. Even if the product is unavailable, the page is the same - 04/05/2021
         boolean available = getAvailability(json);
         String primaryImage = crawlPrimaryImage(json);
         List<String> secondaryImages = crawlSecondaryImages(json, primaryImage);

         Offers offers = available ? scrapOffers(json) : new Offers();
         String description = crawlDescription(json);
         RatingsReviews ratingReviews = CrawlerUtils.scrapRatingReviewsFromYourViews(internalPid, "80c6264f-5023-4c73-918b-c1713a3d9dc7", ".yv-span.yv-align-left", ".yv-star-reviews strong", ".yv-paging.yv-hasresults:not(:last-child)", ".yv-star-reviews-content", ".fa.fa-star", this.dataFetcher, session, logger, cookies);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalPid)
            .setInternalPid(internalPid)
            .setName(name)
            .setRatingReviews(ratingReviews)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private boolean getAvailability(JSONObject json) {

      JSONArray prices = json.optJSONArray("childSKUs");
      return !prices.isEmpty();
   }

   private String crawlInternalPid(JSONObject json) {
      return json.has("id") ? json.get("id").toString() : null;
   }

   private String crawlPrimaryImage(JSONObject json) {
      String primaryImage = null;

      if (json.has("primaryFullImageURL") && !json.get("primaryFullImageURL").toString().equalsIgnoreCase("null")) {
         primaryImage = CrawlerUtils.completeUrl(json.get("primaryFullImageURL").toString().replace(" ", "%20"), "https:", "www.mambo.com.br");
      } else if (json.has("primaryLargeImageURL") && !json.get("primaryLargeImageURL").toString().equalsIgnoreCase("null")) {
         primaryImage = CrawlerUtils.completeUrl(json.get("primaryLargeImageURL").toString().replace(" ", "%20"), "https:", "www.mambo.com.br");
      } else if (json.has("primaryMediumImageURL") && !json.get("primaryMediumImageURL").toString().equalsIgnoreCase("null")) {
         primaryImage = CrawlerUtils.completeUrl(json.get("primaryMediumImageURL").toString().replace(" ", "%20"), "https:", "www.mambo.com.br");
      } else if (json.has("primarySmallImageURL") && !json.get("primarySmallImageURL").toString().equalsIgnoreCase("null")) {
         primaryImage = CrawlerUtils.completeUrl(json.get("primarySmallImageURL").toString().replace(" ", "%20"), "https:", "www.mambo.com.br");
      } else if (json.has("primaryThumbImageURL") && !json.get("primaryThumbImageURL").toString().equalsIgnoreCase("null")) {
         primaryImage = CrawlerUtils.completeUrl(json.get("primaryThumbImageURL").toString().replace(" ", "%20"), "https:", "www.mambo.com.br");
      }

      return primaryImage;
   }

   private List<String> crawlSecondaryImages(JSONObject json, String primaryImage) {
      List<String> secondaryImagesArray = new ArrayList<>();
      JSONArray images = new JSONArray();

      if (verifyImagesArray(json, "fullImageURLs")) {
         images = json.getJSONArray("fullImageURLs");
      } else if (verifyImagesArray(json, "largeImageURLs")) {
         images = json.getJSONArray("largeImageURLs");
      } else if (verifyImagesArray(json, "mediumImageURLs")) {
         images = json.getJSONArray("mediumImageURLs");
      } else if (verifyImagesArray(json, "smallImageURLs")) {
         images = json.getJSONArray("smallImageURLs");
      } else if (verifyImagesArray(json, "thumbImageURLs")) {
         images = json.getJSONArray("thumbImageURLs");
      }

      if (images.length() > 0) {
         images.remove(0);
      }

      images.forEach(image ->
         secondaryImagesArray.add(CrawlerUtils.completeUrl(image.toString().replace(" ", "%20"), "https:", "www.mambo.com.br")));

      return secondaryImagesArray;
   }

   private boolean verifyImagesArray(JSONObject json, String key) {
      if (json.has(key) && json.get(key) instanceof JSONArray) {
         JSONArray array = json.getJSONArray(key);

         return array.length() > 0;
      }

      return false;
   }

   private String crawlDescription(JSONObject json) {
      return json.has("description") && !json.isNull("description") ? json.optString("description") : null;
   }

   private Offers scrapOffers(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = crawlPricing(json);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULLNAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing crawlPricing(JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = null;
      Double priceFrom = null;

      JSONArray arraySkus = json.has("childSKUs") && !json.isNull("childSKUs") ? json.optJSONArray("childSKUs") : new JSONArray();

      if (arraySkus.length() > 0) {
         JSONObject jsonSku = arraySkus.getJSONObject(0);

         spotlightPrice = JSONUtils.getDoubleValueFromJSON(jsonSku, "salesPrice", false);
         System.out.println(spotlightPrice);
         if (spotlightPrice != null) {
            priceFrom = JSONUtils.getDoubleValueFromJSON(jsonSku, "listPrice", false);
         } else {
            spotlightPrice = JSONUtils.getDoubleValueFromJSON(jsonSku, "listPrice", false);
         }

      }

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

   private RatingsReviews scrapRating(JSONObject json) {
      RatingsReviews ratingsReviews = new RatingsReviews();

      return ratingsReviews;
   }

}
