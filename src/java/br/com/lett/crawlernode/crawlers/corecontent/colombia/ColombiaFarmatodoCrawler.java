package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class ColombiaFarmatodoCrawler extends Crawler {

   private static final String API_TOKEN = "f0b269756fc30c5adce554a04b52ec7b";
   private static final String WEB_SAFE_TOKEN = "ahZzfnN0dW5uaW5nLWJhc2UtMTY0NDAycl0LEgRVc2VyIiQ4ZDFkMDhlMy04NDMzLTRiY2MtYjM2Mi1hNWFhNmUyNGJjZGEMCxIFVG9rZW4iJDk5Njg3ODMyLWJkN2QtNDgyOS1iZjdlLTkyN2VlYWQ3NGJlNgw";
   private static final String PRODUCTS_API_URL = "https://stunning-base-164402.uc.r.appspot.com/_ah/api/productEndpoint/getItem?idItem=";
   private static final String RATING_API_KEY = "caRsE2BkjeLAPlvqv4kY8SPNrY032XNfzo6M2cKaZgNjY";
   private static final String RATING_API_URL = "https://api.bazaarvoice.com/data/display/0.2alpha/product/summary";
   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.DINERS.toString());
   private static final String SELLER_FULL_NAME = "farmatodo-colombia";

   public ColombiaFarmatodoCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
      super.config.setParser(Parser.JSON);
   }

   @Override
   protected Response fetchResponse() {
      String productId = scrapId();
      Map<String, String> headers = new HashMap<>();
      headers.put("referer", " https://www.farmatodo.com.co/producto/" + productId);

      Request request = Request.RequestBuilder.create()
         .setUrl(PRODUCTS_API_URL + productId + "&idStoreGroup=26&token=" + API_TOKEN + "&tokenIdWebSafe=" + WEB_SAFE_TOKEN + "&key=AIzaSyDASDi-v-kJzulGnaRwT7sAfG44KEqaudA")
         .setProxyservice(List.of(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.BUY
         ))
         .setHeaders(headers)
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, this.dataFetcher, true);
      return response;
   }

   private String scrapId() {
      String url = session.getOriginalURL();

      if (url.contains("producto/")) {
         String id = CommonMethods.getLast(url.split("producto/")).split("-")[0].trim();
         if (!id.isEmpty()) {
            return id;
         }
      }
      return null;
   }

   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();


      if (isProductPage(json)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject seo = JSONUtils.getJSONValue(json, "seo");

         String internalId = JSONUtils.getStringValue(seo, "sku");
         String internalPid = JSONUtils.getStringValue(json, "id");
         String name = scrapName(seo);
         String description = scrapDescription(json);
         String primaryImage = JSONUtils.getStringValue(seo, "image");

         List<String> secundaryImages = scrapImages(json);
         CategoryCollection categories = scrapCategories(json);
         RatingsReviews ratingsReviews = scrapRatingsReviews(internalPid);
         boolean available = !json.optBoolean("without_stock", false);
         Offers offers = available ? scrapOffers(seo) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setDescription(description)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secundaryImages)
            .setCategories(categories)
            .setOffers(offers)
            .setRatingReviews(ratingsReviews)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String scrapDescription(JSONObject json) {
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setProxyservice(List.of(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
         ))
         .build();

      Response response = dataFetcher.get(session, request);
      Document htmlProduct = Jsoup.parse(response.getBody());

      String htmlDescription = CrawlerUtils.scrapElementsDescription(htmlProduct, Arrays.asList("div.info h2", "div.info p", "div.info ol li"));
      String htmlInformation = CrawlerUtils.scrapSimpleDescription(htmlProduct, Arrays.asList("div.module-simple li"));
      String htmlTechnique = CrawlerUtils.scrapElementsDescription(htmlProduct, Arrays.asList("div.container-data-sheet .item .description"));
      String jsonDescription = json.optString("largeDescription");

      return jsonDescription + htmlDescription + htmlInformation + htmlTechnique;
   }

   private CategoryCollection scrapCategories(JSONObject product) {
      CategoryCollection categories = new CategoryCollection();

      String department = JSONUtils.getValueRecursive(product, "departments.0", String.class, "");
      String category = JSONUtils.getValueRecursive(product, "categorie", String.class, "");
      String subcategory = JSONUtils.getValueRecursive(product, "subCategory", String.class, "");

      categories.add(department);
      categories.add(category);
      categories.add(subcategory);

      return categories;
   }

   private List<String> scrapImages(JSONObject json) {
      JSONArray imagesJson = json.optJSONArray("listUrlImages");
      List<String> images = new ArrayList<>();
      if (imagesJson != null) {
         for (int i = 0; i < imagesJson.length(); i++) {
            images.add(imagesJson.optString(i));
         }
      }
      return images;
   }

   private boolean isProductPage(JSONObject json) {
      return !json.keySet().isEmpty();
   }

   private String scrapName(JSONObject json) {
      String name = JSONUtils.getStringValue(json, "name");
      String description = JSONUtils.getStringValue(json, "description");

      if (name != null && description != null) {
         name += " - " + JSONUtils.getStringValue(json, "description");
      }

      return name;
   }

   private Offers scrapOffers(JSONObject product) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);


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

   private Pricing scrapPricing(JSONObject product) throws MalformedPricingException {
      Double priceFrom = null;
      Double spotlightPrice = null;
      String priceFromStr = JSONUtils.getValueRecursive(product, "offers.price", String.class);
      String priceStr = JSONUtils.getValueRecursive(product, "offers.lowPrice", String.class);

      if (priceStr != null && priceFromStr != null) {
         spotlightPrice = Double.parseDouble(priceStr);
         priceFrom = Double.parseDouble(priceFromStr);
      } else if (priceFromStr != null) {
         spotlightPrice = Double.parseDouble(priceFromStr);
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build())
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create().setInstallmentNumber(1).setInstallmentPrice(spotlightPrice).build());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create().setBrand(card).setInstallments(installments).setIsShopCard(false).build());
      }

      return creditCards;
   }

   private RatingsReviews scrapRatingsReviews(String productId) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      ratingsReviews.setDate(session.getDate());

      String urlApi = RATING_API_URL + "?PassKey=" + RATING_API_KEY + "&productid=" + productId +
         "&contentType=reviews,questions&reviewDistribution=primaryRating,recommended&rev=0&contentlocale=es*,es_CO";

      Request request = RequestBuilder.create()
         .setUrl(urlApi)
         .setProxyservice(List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.BUY))
         .build();
      JSONObject reviewJson = CrawlerUtils.stringToJson(new FetcherDataFetcher().get(session, request).getBody());
      reviewJson = JSONUtils.getJSONValue(reviewJson, "reviewSummary");

      if (!reviewJson.keySet().isEmpty()) {
         Integer totalRating = JSONUtils.getIntegerValueFromJSON(reviewJson, "numReviews", 0);
         Double avgRating = JSONUtils.getDoubleValueFromJSON(JSONUtils.getJSONValue(reviewJson, "primaryRating"), "average", true);
         AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(reviewJson);

         ratingsReviews.setTotalRating(totalRating);
         ratingsReviews.setTotalWrittenReviews(totalRating);
         ratingsReviews.setAverageOverallRating(avgRating == null ? 0.0f : avgRating);
         ratingsReviews.setAdvancedRatingReview(advancedRatingReview);
      }

      return ratingsReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject json) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      json = JSONUtils.getJSONValue(json, "primaryRating");

      JSONArray distribution = JSONUtils.getJSONArrayValue(json, "distribution");
      for (Object obj : distribution) {
         if (obj instanceof JSONObject) {
            JSONObject starJson = (JSONObject) obj;

            Integer star = JSONUtils.getIntegerValueFromJSON(starJson, "key", -1);
            switch (star) {
               case 1:
                  star1 = JSONUtils.getIntegerValueFromJSON(starJson, "count", 0);
                  break;
               case 2:
                  star2 = JSONUtils.getIntegerValueFromJSON(starJson, "count", 0);
                  break;
               case 3:
                  star3 = JSONUtils.getIntegerValueFromJSON(starJson, "count", 0);
                  break;
               case 4:
                  star4 = JSONUtils.getIntegerValueFromJSON(starJson, "count", 0);
                  break;
               case 5:
                  star5 = JSONUtils.getIntegerValueFromJSON(starJson, "count", 0);
                  break;
            }
         }
      }

      return new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();
   }
}
