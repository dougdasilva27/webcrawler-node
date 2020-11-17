package br.com.lett.crawlernode.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Resources;
import br.com.lett.crawlernode.core.session.Session;

public class TestHtmlBuilder {

   private static final Logger logger = LoggerFactory.getLogger(TestHtmlBuilder.class);

   private static final String PRODUCT_URL = "url";
   private static final String INTERNAL_ID = "internalId";
   private static final String INTERNAL_PID = "internalPid";
   private static final String NAME = "name";
   private static final String PRICE = "price";
   private static final String STOCK = "stock";
   private static final String AVAILABLE = "available";
   private static final String DESCRIPTION = "description";
   private static final String CAT1 = "category1";
   private static final String CAT2 = "category2";
   private static final String CAT3 = "category3";
   private static final String PRIMARY_IMAGE = "primaryImage";
   private static final String SECONDARY_IMAGES = "secondaryImages";
   private static final String MARKETPLACE = "marketplace";
   private static final String OFFERS = "offers";
   private static final String PRICES = "prices";
   private static final String BANK_TICKET = "bank_ticket";
   private static final String FROM = "from";
   private static final String EANS = "eans";
   private static final String RATING = "rating";
   private static final String OVERALL = "overall";
   private static final String REVIEWS = "reviews";
   private static final String WRITTEN = "written";

   private static final String STAR1 = "star1";
   private static final String STAR2 = "star2";
   private static final String STAR3 = "star3";
   private static final String STAR4 = "star4";
   private static final String STAR5 = "star5";

   public static String buildProductHtml(JSONObject productJson, String pathWrite, Session session) {
      MustacheFactory mustacheFactory = new DefaultMustacheFactory();
      File file = new File(Resources.getResource("productTemplate.html").getFile());

      Mustache mustache = null;
      try {
         mustache = mustacheFactory.compile(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), file.getName());
      } catch (FileNotFoundException e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
      }

      if (mustache != null) {
         // Map to replace all var to product informations in html
         Map<String, Object> scopes = new HashMap<>();

         // Put url in map
         putProductUrl(productJson, scopes);

         // Put eans on map
         putProductEans(productJson, scopes);

         // Put internalId in map
         putInternalId(productJson, scopes);

         // Put internalPid in map
         putInternalPid(productJson, scopes);

         // Put name in map
         putName(productJson, scopes);

         // Put name in map
         putPrice(productJson, scopes);

         // Put name in map
         putAvailable(productJson, scopes);

         // Put name in map
         putStock(productJson, scopes);

         // Put description in map
         putDescription(productJson, scopes);

         // Put cat1 in map
         putCat1(productJson, scopes);

         // Put cat2 in map
         putCat2(productJson, scopes);

         // Put cat1 in map
         putCat3(productJson, scopes);

         // Put primaryImage in map
         putPrimaryImage(productJson, scopes);

         // Put secondaryImages in map
         putSecondaryImages(productJson, scopes);

         // Put marketplace in map
         putMarketplaces(productJson, scopes);

         // Put offers on map
         putOffers(productJson, scopes);

         // Put prices in map
         putPrices(productJson, scopes);

         // Put ratings in map
         putRatings(productJson, scopes);

         // Execute replace in html
         StringWriter writer = new StringWriter();
         mustache.execute(writer, scopes);

         try (PrintWriter out = new PrintWriter(pathWrite + session.getMarket().getName() + "-" + scopes.get(INTERNAL_ID) + ".html")) {
            out.println(writer.toString());
         } catch (FileNotFoundException e) {
            Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
         }

         return writer.toString();
      }

      return null;
   }

   private static void putProductUrl(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(PRODUCT_URL) && !productJson.isNull(PRODUCT_URL)) {
         scopes.put(PRODUCT_URL, productJson.getString(PRODUCT_URL));
      }
   }

   private static void putProductEans(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(EANS) && !productJson.isNull(EANS)) {
         scopes.put(EANS, productJson.get(EANS).toString().replace("[", "").replace("]", ""));
      }
   }

   private static void putInternalId(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(INTERNAL_ID) && !productJson.isNull(INTERNAL_ID)) {
         scopes.put(INTERNAL_ID, productJson.getString(INTERNAL_ID));
      }
   }

   private static void putInternalPid(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(INTERNAL_PID) && !productJson.isNull(INTERNAL_PID)) {
         scopes.put(INTERNAL_PID, productJson.getString(INTERNAL_PID));
      }
   }

   private static void putName(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(NAME) && !productJson.isNull(NAME)) {
         scopes.put(NAME, productJson.getString(NAME));
      }
   }

   private static void putPrice(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(PRICE) && !productJson.isNull(PRICE)) {
         scopes.put(PRICE, productJson.get(PRICE));
      }
   }

   private static void putAvailable(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(AVAILABLE) && !productJson.isNull(AVAILABLE)) {
         scopes.put(AVAILABLE, productJson.get(AVAILABLE));
      }
   }

   private static void putStock(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(STOCK) && !productJson.isNull(STOCK)) {
         scopes.put(STOCK, productJson.get(STOCK));
      }
   }

   private static void putDescription(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(DESCRIPTION) && !productJson.isNull(DESCRIPTION)) {
         scopes.put(DESCRIPTION, productJson.getString(DESCRIPTION));
      }
   }

   private static void putCat1(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(CAT1) && !productJson.isNull(CAT1)) {
         scopes.put(CAT1, productJson.getString(CAT1));
      }
   }

   private static void putCat2(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(CAT2) && !productJson.isNull(CAT2)) {
         scopes.put(CAT2, productJson.getString(CAT2));
      }
   }

   private static void putCat3(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(CAT3) && !productJson.isNull(CAT3)) {
         scopes.put(CAT3, productJson.getString(CAT3));
      }
   }

   private static void putPrimaryImage(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(PRIMARY_IMAGE) && !productJson.isNull(PRIMARY_IMAGE)) {
         scopes.put(PRIMARY_IMAGE, productJson.getString(PRIMARY_IMAGE));
      }
   }

   private static void putSecondaryImages(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(SECONDARY_IMAGES) && !productJson.isNull(SECONDARY_IMAGES)) {
         List<String> secondaryImages = new ArrayList<>();
         JSONArray imagesArray = productJson.optJSONArray(SECONDARY_IMAGES);

         if (imagesArray != null) {
            for (int i = 0; i < imagesArray.length(); i++) {
               secondaryImages.add(imagesArray.getString(i));
            }
         } else {
            imagesArray = new JSONArray();
         }

         scopes.put(SECONDARY_IMAGES, secondaryImages);
      }
   }

   private static void putMarketplaces(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(MARKETPLACE) && !productJson.isNull(MARKETPLACE)) {
         Multimap<String, Object> marketplaces = ArrayListMultimap.create();
         JSONArray arrayMarketplaces = new JSONArray(productJson.getString(MARKETPLACE));

         for (int i = 0; i < arrayMarketplaces.length(); i++) {
            JSONObject jsonMarketplace = arrayMarketplaces.getJSONObject(i);

            if (jsonMarketplace.has(NAME)) {
               String name = jsonMarketplace.getString(NAME);
               Map<Object, Object> prices = new HashMap<>();

               if (jsonMarketplace.has(PRICES)) {
                  JSONObject pricesJson = jsonMarketplace.getJSONObject(PRICES);

                  prices = getMapPricesFromJson(pricesJson);

                  if (pricesJson.has(BANK_TICKET)) {
                     JSONObject bankTicket = pricesJson.getJSONObject(BANK_TICKET);

                     if (bankTicket.has("1")) {
                        Map<Object, Object> bankTicketPrices = new HashMap<>();
                        bankTicketPrices.put(1, bankTicket.get("1"));
                        prices.put("Boleto", bankTicketPrices.entrySet());
                     }
                  }
               } else if (jsonMarketplace.has(PRICE)) {
                  prices.put("1'", jsonMarketplace.get(PRICE));
               }

               marketplaces.put(name, prices.entrySet());
            }
         }

         scopes.put(MARKETPLACE, marketplaces.entries());
      }
   }

   private static void putOffers(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(OFFERS) && !productJson.isNull(OFFERS)) {
         scopes.put(OFFERS, productJson.get(OFFERS).toString());
      }
   }

   private static void putPrices(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(PRICES) && !productJson.isNull(PRICES)) {
         JSONObject pricesJson = new JSONObject(productJson.getString(PRICES));
         Map<Object, Object> pricesMap = getMapPricesFromJson(pricesJson);

         if (pricesJson.has(BANK_TICKET)) {
            JSONObject bankTicket = pricesJson.getJSONObject(BANK_TICKET);

            if (bankTicket.has("1")) {
               scopes.put(BANK_TICKET, bankTicket.get("1"));
            }
         }

         if (pricesJson.has(FROM)) {
            scopes.put(FROM, pricesJson.get(FROM));
         }

         scopes.put(PRICES, pricesMap.entrySet());
      }
   }

   private static void putRatings(JSONObject productJson, Map<String, Object> scopes) {
      if (productJson.has(RATING) && !productJson.isNull(RATING)) {
         JSONObject ratingJson = new JSONObject(productJson.getString(RATING));

         if (ratingJson.has("average_overall_rating")) {
            scopes.put(OVERALL, ratingJson.get("average_overall_rating"));
         }

         if (ratingJson.has("total_reviews")) {
            scopes.put(REVIEWS, ratingJson.get("total_reviews"));
         }

         if (ratingJson.has("total_written_reviews")) {
            scopes.put(WRITTEN, ratingJson.get("total_written_reviews"));
         }

         if (ratingJson.has("rating_star") && ratingJson.get("rating_star") instanceof JSONObject) {
            JSONObject starJson = ratingJson.getJSONObject("rating_star");

            if (starJson.has("star_1")) scopes.put(STAR1, starJson.get("star_1"));
            if (starJson.has("star_2")) scopes.put(STAR2, starJson.get("star_2"));
            if (starJson.has("star_3")) scopes.put(STAR3, starJson.get("star_3"));
            if (starJson.has("star_4")) scopes.put(STAR4, starJson.get("star_4"));
            if (starJson.has("star_5")) scopes.put(STAR5, starJson.get("star_5"));
         }

         scopes.put(RATING, ratingJson);
      }
   }

   private static Map<Object, Object> getMapPricesFromJson(JSONObject prices) {
      Map<Object, Object> pricesMap = new HashMap<>();

      if (prices.has("card")) {
         JSONObject cardJson = prices.getJSONObject("card");
         Set<String> cards = cardJson.keySet();

         for (String card : cards) {
            Map<Object, Object> cardPrices = new HashMap<>();
            JSONObject installmentsCard = cardJson.getJSONObject(card);

            Set<String> installments = installmentsCard.keySet();

            for (String installment : installments) {
               cardPrices.put(installment, installmentsCard.get(installment));
            }

            pricesMap.put(card, cardPrices.entrySet());
         }
      }

      return pricesMap;
   }
}
