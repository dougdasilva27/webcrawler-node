package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

public class PaguemenosCrawler extends VTEXNewScraper {


   public PaguemenosCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.paguemenos.com.br/";
   private static final String API_sha256Has = "add5c41ad4c1edee61c2987c5fc1396ca5cf20cd28035ee0225f9634bbaffdb8";
   private static final List<String> MAIN_SELLERS = Arrays.asList("Farmácias Pague Menos");
   private RatingsReviews rating = new RatingsReviews();
   private Offers offers;
   private Pricing pricing;
   private Product product;

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return MAIN_SELLERS;
   }

   @Override
   protected void processBeforeScrapVariations(Document doc, JSONObject productJson, String internalPid) throws UnsupportedEncodingException {
      super.processBeforeScrapVariations(doc, productJson, internalPid);
      this.rating = scrapRating(internalPid);
   }

   @Override
   protected String scrapName(Document doc, JSONObject productJson, JSONObject jsonSku) {
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1 .vtex-store-components-3-x-productBrand", false);
      return name != null ? (productJson.optString("brand") + " " + name) : null;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return this.rating;
   }

   @Override
   protected Product extractProduct(Document doc, String pidFromApi, CategoryCollection categories, String description, JSONObject jsonSku, JSONObject productJson) throws Exception {
      this.product = super.extractProduct(doc, pidFromApi, categories, description, jsonSku, productJson);
      String internalId = jsonSku.optString("itemId", null);
      boolean showPriceFrom = shouldShowPriceFrom(productJson);
      this.offers = scrapOffer(doc, jsonSku, internalId, pidFromApi, showPriceFrom);
      product.setOffers(this.offers);
      return this.product;
   }

   protected RatingsReviews scrapRating(String internalPid) throws UnsupportedEncodingException {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject jsonRating = crawlPageRatings(internalPid);

      JSONObject element = null;
      Integer totalNumOfEvaluations = null;
      Double avgRating = null;

      if (jsonRating != null) {
         JSONObject data = jsonRating.has("data") ? jsonRating.optJSONObject("data") : new JSONObject();
         JSONObject productReviews = data.has("productReviews") ? data.optJSONObject("productReviews") : new JSONObject();
         element = productReviews.has("Element") ? productReviews.optJSONObject("Element") : new JSONObject();
      }

      if (element != null) {
         totalNumOfEvaluations = element.optInt("TotalRatings", 0);
         avgRating = element.optDouble("Rating", 0.0);
      }

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(scrapAdvancedRating(element));
      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRating(JSONObject reviews) {

      AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview();

      if (reviews != null) {

         JSONArray ratingList = (JSONArray) reviews.optQuery( "/RatingHistogram/RatingList");
         if (ratingList != null) {

            for (int i = 0; i < ratingList.length(); i++) {
               switch (i) {
                  case 0:
                     advancedRatingReview.setTotalStar5(((JSONObject) ratingList.get(i)).optInt("Total", 0));
                     break;
                  case 1:
                     advancedRatingReview.setTotalStar4(((JSONObject) ratingList.get(i)).optInt("Total", 0));
                     break;
                  case 2:
                     advancedRatingReview.setTotalStar3(((JSONObject) ratingList.get(i)).optInt("Total", 0));
                     break;
                  case 3:
                     advancedRatingReview.setTotalStar2(((JSONObject) ratingList.get(i)).optInt("Total", 0));
                     break;
                  case 4:
                     advancedRatingReview.setTotalStar1(((JSONObject) ratingList.get(i)).optInt("Total", 0));
                     break;
                  default:
               }
            }
         }
      }

      return advancedRatingReview;
   }

   private String createVariablesBase64(String internalId) {
      JSONObject variables = new JSONObject();
      variables.put("productId", internalId);
      variables.put("page", 1);
      variables.put("count", 5);
      variables.put("orderBy", 0);
      variables.put("filters", "");

      return Base64.getEncoder().encodeToString(variables.toString().getBytes());
   }

   private JSONObject crawlPageRatings(String internalId) throws UnsupportedEncodingException {

      String query = "{\"persistedQuery\":" +
         "{\"version\":1,\"sha256Hash\":\"" + API_sha256Has + "\",\"sender\":\"yourviews.yourviewsreviews@0.x\"," +
         "\"provider\":\"yourviews.yourviewsreviews@0.x\"}," +
         "\"variables\":\"" + createVariablesBase64(internalId) + "\"}";

      String encodedQuery = URLEncoder.encode(query, "UTF-8");

      StringBuilder url = new StringBuilder();
      url.append("https://www.paguemenos.com.br/_v/public/graphql/v1?workspace=master&maxAge=medium&appsEtag=remove&domain=store&locale=pt-BR&__bindingId=23424e23-86bb-4397-98b0-238d88d7f528&operationName=productReviews&variables=%7B%7D&extensions=");
      url.append(encodedQuery);

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");

      Request request = RequestBuilder.create().setUrl(url.toString()).setHeaders(headers).build();
      String response = this.dataFetcher.get(session, request).getBody();

      return JSONUtils.stringToJson(response);
   }

   private boolean shouldShowPriceFrom(JSONObject productJson) {
      JSONArray showPriceFrom = productJson.optJSONArray("ApresentarPrecoDePor");
      if (showPriceFrom != null && showPriceFrom.length() > 0) {
         return (showPriceFrom.getString(0).equals("Sim"));
      }
      return true;
   }

   private Offers scrapOffer(Document doc, JSONObject jsonSku, String internalId, String internalPid, boolean showPriceFrom) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      JSONArray sellers = jsonSku.optJSONArray("sellers");
      if (sellers != null) {
         int position = 1;
         for (Object o : sellers) {
            JSONObject offerJson = o instanceof JSONObject ? (JSONObject) o
               : new JSONObject();
            JSONObject commertialOffer = offerJson.optJSONObject("commertialOffer");
            String sellerFullName = offerJson.optString("sellerName", null);
            boolean isDefaultSeller = offerJson.optBoolean("sellerDefault", true);

            if (commertialOffer != null && sellerFullName != null) {
               Integer stock = commertialOffer.optInt("AvailableQuantity");

               if (stock > 0) {
                  JSONObject discounts = scrapDiscounts(commertialOffer);

                  String sellerId = offerJson.optString("sellerId", null);
                  boolean isBuyBox = sellers.length() > 1;
                  boolean isMainRetailer = isMainRetailer(sellerFullName);

                  Pricing pricing = scrapPricing(doc, internalId, commertialOffer, discounts, showPriceFrom);
                  List<String> sales = isDefaultSeller ? scrapSales(doc, offerJson, internalId, internalPid, pricing) : new ArrayList<>();

                  offers.add(Offer.OfferBuilder.create()
                     .setInternalSellerId(sellerId)
                     .setSellerFullName(sellerFullName)
                     .setMainPagePosition(position)
                     .setIsBuybox(isBuyBox)
                     .setIsMainRetailer(isMainRetailer)
                     .setPricing(pricing)
                     .setSales(sales)
                     .build());

                  position++;
               }
            }
         }
      }
      return offers;
   }

   @Override
   protected List<String> scrapSales(Document doc, JSONObject offerJson, String internalId, String internalPid, Pricing pricing) {
      List<String> sales = new ArrayList<>();
      if (pricing != null) sales.add(CrawlerUtils.calculateSales(pricing));

      String kitSales = CrawlerUtils.scrapStringSimpleInfo(doc, ".paguemenos-product-details-medicamento-controlado-1-x-containerTexto", false);

      if (kitSales != null && !kitSales.isEmpty()) sales.add(kitSales);

      Object teasers = offerJson.optQuery("/commertialOffer/Teasers");

      if (teasers instanceof JSONArray) {
         Object teaser = ((JSONArray) teasers).optQuery("/0/<Name>k__BackingField");
         if (teaser instanceof String) {
            sales.add((String) teaser);
         }
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc, String internalId, JSONObject comertial, JSONObject discountsJson, boolean showPriceFrom) throws MalformedPricingException {
      Double principalPrice = comertial.optDouble("Price");
      Double priceFrom = comertial.optDouble("ListPrice");

      CreditCards creditCards = scrapCreditCards(comertial, discountsJson, true);
      BankSlip bankSlip = scrapBankSlip(principalPrice, comertial, discountsJson, true);

      Double spotlightPrice = scrapSpotlightPrice(doc, internalId, principalPrice, comertial, discountsJson);
      if (priceFrom != null && spotlightPrice != null && spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      if (!showPriceFrom) priceFrom = null;

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }
}
