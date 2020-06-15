package br.com.lett.crawlernode.processor;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.database.DiscordMessages;
import br.com.lett.crawlernode.database.JdbcConnectionFactory;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.DateUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.IllegalBehaviorElementValueException;
import exceptions.MalformedPricesException;
import exceptions.MalformedRatingModel;
import models.Behavior;
import models.BehaviorElement;
import models.BehaviorElement.BehaviorElementBuilder;
import models.Marketplace;
import models.Offers;
import models.Processed;
import models.RatingsReviews;
import models.Seller;
import models.Util;
import models.prices.Prices;

public class Processor {

   private static final Logger logger = LoggerFactory.getLogger(Processor.class);

   /**
    * Process the product and create a new ProcessedModel based on the crawled product.
    * 
    * @param product
    * @param session
    * @return a new ProcessedModel or null in case the Product model has invalid informations
    */
   public static Processed createProcessed(Product product, Session session, Processed previousProcessedProduct,
         ResultManager processorResultManager) {

      Logging.printLogDebug(logger, session, "Creating processed product ...");

      String nowISO = new DateTime(DateUtils.timeZone).toString("yyyy-MM-dd HH:mm:ss.SSS");

      Processed newProcessedProduct = null;

      // get crawled information
      boolean available = product.getAvailable();
      String url = product.getUrl();
      String internalId = product.getInternalId();
      String internalPid = product.getInternalPid();
      String name = product.getName();
      Float price = product.getPrice();
      Prices prices = product.getPrices();
      String cat1 = product.getCategory1();
      String cat2 = product.getCategory2();
      String cat3 = product.getCategory3();
      String foto = product.getPrimaryImage();
      String secondaryPics = product.getSecondaryImages();
      String description = product.getDescription();
      Marketplace marketplace = product.getMarketplace();
      Offers offers = product.getOffers();
      Integer stock = product.getStock();
      RatingsReviews ratingsReviews = product.getRatingReviews();

      List<String> eans = null;

      List<String> crawledEans = product.getEans();
      if (crawledEans != null) {
         for (String eanTmp : crawledEans) {
            if (eanTmp != null && !eanTmp.isEmpty()) {
               if (eans == null)
                  eans = new ArrayList<>();
               eans.add(eanTmp);
            }
         }
      }

      // checking fields
      boolean checkResult = checkFields(price, prices, available, internalId, url, name, session);
      if (!checkResult) {
         return null;
      }

      if (price != null && price == 0.0) {
         price = null;
      }

      try {

         // if the processed model already exists
         // clone it and update with the current crawled values
         if (previousProcessedProduct != null) {

            // clone it, creating a new processed model
            newProcessedProduct = previousProcessedProduct.clone();

            // update fields with new values
            if (url != null) {
               newProcessedProduct.setUrl(url);
            }

            if (stock != null) {
               newProcessedProduct.setStock(stock);
            }

            if (marketplace != null && !marketplace.isEmpty()) {
               newProcessedProduct.setMarketplace(marketplace);
            } else {
               newProcessedProduct.setMarketplace(null);
            }

            if (offers != null && !offers.isEmpty()) {
               newProcessedProduct.setOffers(offers);
            } else {
               newProcessedProduct.setOffers(null);
            }

            newProcessedProduct.setPic(foto);
            newProcessedProduct.setPrice(price);
            newProcessedProduct.setPrices(prices);

            newProcessedProduct.setCat1(cat1);
            newProcessedProduct.setCat2(cat2);
            newProcessedProduct.setCat3(cat3);

            newProcessedProduct.setSecondaryImages(secondaryPics);
            newProcessedProduct.setCat1(cat1);
            newProcessedProduct.setCat2(cat2);
            newProcessedProduct.setCat3(cat3);
            newProcessedProduct.setOriginalName(name);
            newProcessedProduct.setOriginalDescription(description);
            newProcessedProduct.setInternalPid(internalPid);
            newProcessedProduct.setEans(eans);
            newProcessedProduct.setRatingsReviews(ratingsReviews);

         }

         // if the product doesn't exists yet, then we must create a new processed model
         if (newProcessedProduct == null) {
            newProcessedProduct = new Processed(null, internalId, internalPid, name, null, null, null, null, null, null, null, foto, secondaryPics, cat1,
                  cat2, cat3, url, session.getMarket().getNumber(), nowISO, nowISO, null, nowISO, nowISO, null, null, description, price, prices, null,
                  null, null, false, false, stock, new Behavior(), // behavior - will be update in the updateBehavior method just below
                  marketplace, null, eans, offers, ratingsReviews);
         }

         // run the processor for the new model
         processorResultManager.processProduct(newProcessedProduct, session);

         // update availability
         newProcessedProduct.setAvailable(available);
         newProcessedProduct.setLrt(nowISO);
         newProcessedProduct.setVoid(false);
         if (available) {
            newProcessedProduct.setLat(nowISO);
         }

         // update status
         updateStatus(newProcessedProduct);
         updateLMS(newProcessedProduct, previousProcessedProduct, nowISO);
         updateChanges(newProcessedProduct, previousProcessedProduct, session);
         updateLMT(newProcessedProduct, nowISO);

         // Retirando price = 0
         if (newProcessedProduct.getPrice() != null && newProcessedProduct.getPrice() == 0.0) {
            newProcessedProduct.setPrice(null);
         }

         // update behavior
         updateBehavior(newProcessedProduct, nowISO, stock, available, newProcessedProduct.getStatus(), price, newProcessedProduct.getPrices(),
               marketplace, session);

         Logging.printLogDebug(logger, session, "Produto processado.");
      } catch (Exception e2) {
         Logging.printLogError(logger, session, "Error processing product.");
         Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e2));
      }

      return newProcessedProduct;
   }

   private static boolean checkFields(Float price, Prices prices, boolean available, String internalId, String url, String name, Session session) {
      if ((price == null || price.equals(0f) || prices.isEmpty()) && available) {
         Logging.printLogError(logger, session, "Error creating ProcessedModel." + "[" + "product available but 'price' is empty" + "]");
         return false;
      } else if (internalId == null || internalId.isEmpty()) {
         Logging.printLogError(logger, session, "Error creating ProcessedModel." + "[" + "internalId is null or empty" + "]");
         return false;
      } else if (session.getMarket().getNumber() == 0) {
         Logging.printLogError(logger, session, "Error creating ProcessedModel." + "[" + "marketId is 0");
         return false;
      } else if (url == null || url.isEmpty()) {
         Logging.printLogError(logger, session, "Error creating ProcessedModel." + "[" + "url is empty" + "]");
         return false;
      } else if (name == null || name.isEmpty()) {
         Logging.printLogError(logger, session, "Error creating ProcessedModel." + "[" + "name is null or empty" + "]");
         return false;
      }

      return true;
   }

   /**
    * Scan means the same as BehaviorElement. Updates the intra day behavior of the product. When the
    * processed is being created (has just been discovered), the current behavior will be empty. <br>
    * In this case, this method creates the first behavior element ever for this behavior, which is the
    * crawler scanned data that happened just seconds ago. It won't have an artificial scan of the
    * first moment of day, because this artificial scan is nothing more than the last scan of the
    * previous day. <br>
    * So, if the product has just been discovered, it won't have this. On the other side, if the
    * product already exists, then we will only update the behavior with the last scan data (add a new
    * behavior element), while preserving the other behavior elements, but in this case we also look
    * for the first scan of the day. <br>
    * If we can't find it, then we look for the last scan of the previous, and set it as the first scan
    * of the current day.
    * 
    * @param newProcessedProduct
    * @param nowISO
    * @param stock
    * @param available
    * @param price
    * @param marketplace
    * @param session
    */
   public void updateBehaviorTest(Processed newProcessedProduct, String nowISO, Integer stock, boolean available, String status, Float price,
         Prices prices, Marketplace marketplace, Session session) {

      DateTime startOfDay = new DateTime(DateUtils.timeZone).withTimeAtStartOfDay();
      String startOfDayISO = new DateTime(DateUtils.timeZone).withTimeAtStartOfDay().plusSeconds(1).toString("yyyy-MM-dd HH:mm:ss.SSS");

      // Get the previous behavior object
      Behavior oldBehaviour;
      if (newProcessedProduct.getBehaviour() == null) {
         oldBehaviour = new Behavior();
      } else {
         oldBehaviour = newProcessedProduct.getBehaviour().clone();
      }

      Behavior newBehavior = new Behavior();

      // Order the old behavior by date ascending
      oldBehaviour.orderByDateAsc();

      // Add the first behavior element of this day
      // which is the last behavior element from yesterday
      addFirstBehaviorElementOfDay(oldBehaviour, newBehavior, startOfDay, startOfDayISO, session);

      // Create the new BehaviorElement
      try {
         BehaviorElement behaviorElement = createNewBehaviorElement(nowISO, stock, available, status, price, prices, marketplace);

         newBehavior.add(behaviorElement); // add the behavior from the last crawler that occurred just a few seconds ago
      } catch (Exception e) {
         Logging.printLogError(logger, session, Util.getStackTraceString(e));
      }

      // pegando behavior elements apenas com as datas de hoje e
      // que possuem os campos obrigatorios
      List<BehaviorElement> filteredBehaviorElements = oldBehaviour.filterAfter(startOfDay);

      for (BehaviorElement be : filteredBehaviorElements) {
         newBehavior.add(be);
      }

      newBehavior.orderByDateAsc();

      newProcessedProduct.setBehaviour(newBehavior);
   }

   /**
    * Scan means the same as BehaviorElement. Updates the intra day behavior of the product. When the
    * processed is being created (has just been discovered), the current behavior will be empty. <br>
    * In this case, this method creates the first behavior element ever for this behavior, which is the
    * crawler scanned data that happened just seconds ago. It won't have an artificial scan of the
    * first moment of day, because this artificial scan is nothing more than the last scan of the
    * previous day. <br>
    * So, if the product has just been discovered, it won't have this. On the other side, if the
    * product already exists, then we will only update the behavior with the last scan data (add a new
    * behavior element), while preserving the other behavior elements, but in this case we also look
    * for the first scan of the day. <br>
    * If we can't find it, then we look for the last scan of the previous, and set it as the first scan
    * of the current day.
    * 
    * @param newProcessedProduct
    * @param nowISO
    * @param stock
    * @param available
    * @param price
    * @param marketplace
    * @param session
    */
   public static void updateBehavior(Processed newProcessedProduct, String nowISO, Integer stock, boolean available, String status, Float price,
         Prices prices, Marketplace marketplace, Session session) {

      DateTime startOfDay = new DateTime(DateUtils.timeZone).withTimeAtStartOfDay();
      String startOfDayISO = new DateTime(DateUtils.timeZone).withTimeAtStartOfDay().plusSeconds(1).toString("yyyy-MM-dd HH:mm:ss.SSS");

      // Get the previous behavior object
      Behavior oldBehaviour;
      if (newProcessedProduct.getBehaviour() == null) {
         oldBehaviour = new Behavior();
      } else {
         oldBehaviour = newProcessedProduct.getBehaviour().clone();
      }

      Behavior newBehavior = new Behavior();

      // Order the old behavior by date ascending
      oldBehaviour.orderByDateAsc();

      // Add the first behavior element of this day
      // which is the last behavior element from yesterday
      addFirstBehaviorElementOfDay(oldBehaviour, newBehavior, startOfDay, startOfDayISO, session);

      // Create the new BehaviorElement
      try {
         BehaviorElement behaviorElement = createNewBehaviorElement(nowISO, stock, available, status, price, prices, marketplace);

         newBehavior.add(behaviorElement); // add the behavior from the last crawler that occurred just a few seconds ago
      } catch (Exception e) {
         Logging.printLogError(logger, session, Util.getStackTraceString(e));
      }

      // pegando behavior elements apenas com as datas de hoje e
      // que possuem os campos obrigatorios
      List<BehaviorElement> filteredBehaviorElements = oldBehaviour.filterAfter(startOfDay);

      for (BehaviorElement be : filteredBehaviorElements) {
         newBehavior.add(be);
      }

      newBehavior.orderByDateAsc();

      newProcessedProduct.setBehaviour(newBehavior);
   }

   /**
    * This method selects the last behavior element from the previous day (selected from the
    * oldBehavior). Then, this behavior element is added to the newBehavior.
    * 
    * @param oldBehaviour the previous behavior
    * @param newBehavior the behavior being assembled now
    * @param startOfDay DateTime of the start of the current day.
    * @param startDayBehaviorElementDate the date of the start of the current day plus 1 second, parsed
    *        to String
    * @param session
    */
   private static void addFirstBehaviorElementOfDay(Behavior oldBehaviour, Behavior newBehavior, DateTime startOfDay,
         String startDayBehaviorElementDate, Session session) {

      BehaviorElement lastBehaviorBeforeToday = oldBehaviour.getFloor(startOfDay);

      // Criando behavior do início de hoje (supostamente)
      if (lastBehaviorBeforeToday != null
            && (!oldBehaviour.contains(startDayBehaviorElementDate) || oldBehaviour.get(startDayBehaviorElementDate).getStatus() == null)) {

         BehaviorElementBuilder builder = new BehaviorElement.BehaviorElementBuilder();

         // Date
         builder.setDate(startDayBehaviorElementDate);

         // Real Date
         builder.setRealDate(lastBehaviorBeforeToday.getDate());

         // Status
         if (lastBehaviorBeforeToday.getStatus() == null) {
            builder.setStatus("void");
         } else {
            builder.setStatus(lastBehaviorBeforeToday.getStatus());
         }

         // Price
         if (lastBehaviorBeforeToday.getPrice() != null && Double.compare(lastBehaviorBeforeToday.getPrice(), 0.0) <= 0) {
            builder.setPrice(null);
         } else {
            builder.setPrice(lastBehaviorBeforeToday.getPrice());
         }

         // Prices
         builder.setPrices(lastBehaviorBeforeToday.getPrices());

         // Available
         if ("available".equals(lastBehaviorBeforeToday.getStatus())) {
            builder.setAvailable(true);
         } else {
            builder.setAvailable(false);
         }

         // Marketplace
         if (lastBehaviorBeforeToday.getMarketplace() != null) {
            builder.setMarketplace(lastBehaviorBeforeToday.getMarketplace().clone());
         }

         // Stock
         if (lastBehaviorBeforeToday.getStock() != null) {
            builder.setStock(lastBehaviorBeforeToday.getStock());
         }

         // Add the first behavior of this day
         try {
            BehaviorElement be = builder.build();
            newBehavior.add(be);
         } catch (Exception e) {
            Logging.printLogError(logger, session, "Error creating first behavior element from the day.");
            Logging.printLogError(logger, session, Util.getStackTraceString(e));
         }
      }
   }

   private static BehaviorElement createNewBehaviorElement(String nowISO, Integer stock, Boolean available, String status, Float price, Prices prices,
         Marketplace marketplace) throws IllegalBehaviorElementValueException {

      BehaviorElementBuilder builder =
            new BehaviorElementBuilder().setDate(nowISO).setStock(stock).setAvailable(available).setStatus(status).setMarketplace(marketplace);

      if (price != null)
         builder.setPrice(price.doubleValue());
      if (prices != null)
         builder.setPrices(prices);

      return builder.build();
   }

   private static void updateStatus(Processed newProcessedProduct) {
      String newStatus = "available";
      if (!newProcessedProduct.getAvailable()) {
         if (newProcessedProduct.getMarketplace() != null && newProcessedProduct.getMarketplace().size() > 0) {
            newStatus = "only_marketplace";
         } else {
            newStatus = "unavailable";
         }
      }
      newProcessedProduct.setStatus(newStatus);
   }

   private static void updateChanges(Processed newProcessedProduct, Processed previousProcessedProduct, Session session) {

      // detect and register changes
      // an instance of mongo panel must be passed, so we can schedule url to take screenshot
      newProcessedProduct.registerChanges(previousProcessedProduct);

      if (newProcessedProduct.getPrice() != null &&
            previousProcessedProduct != null &&
            previousProcessedProduct.getPrice() != null &&
            newProcessedProduct.getPrice() < previousProcessedProduct.getPrice()) {

         Float discount = 100f - ((newProcessedProduct.getPrice() / previousProcessedProduct.getPrice()) * 100f);

         Boolean shouldSend = false;
         String quote = null;
         String author = null;
         String avatar = null;

         if (discount > 60) {
            shouldSend = true;
            author = "Jacquin";
            avatar = "https://www.azulis.com.br/wp-content/uploads/2019/10/jac-1-770x471.jpg";
            String[] quotes = {
                     "Desligan o freezer a notch!",
                     "esse cheddár tem um gost esquisit",
                     "O que é isso?",
                     "Eu que deciso se isso é bom",
                     "Voce está brincando comig? é pegadinha?",
                     "Problema é seu",
                     "É conta isso para os outros e nao para mim",
                     "Voces me deixa trablar aqui! Que iss gent!",
                     "Eu quer que todo mund venha me ouvi",
                     "No quero mas ti ver hoj!",
                     "Voce vai precisar de uma consultoria, viu",
                     "Eu acho que está tudo muito ruim",
                     "Me senti dentro do filme misson impossive",
                     "Quê?",
                     "Mes parabens gente está horrivell",
                     "Não da pra perdoar non",
                     "Está feliz mesmo?",
                     "Ele aprendeu a economizar 50% na conta de luz",
                     "Cala sua boca!"
            };
            quote = quotes[new Random().nextInt(quotes.length)];
         } else if (discount > 20 && newProcessedProduct.getPrice() > 50) {
            shouldSend = true;
            author = "Julius";
            avatar = "https://i.imgur.com/T65AjlE.png";
            String[] quotes = {
                     "Se você não comprar o desconto é maior",
                     "Esta mensagem custou 12 centavos de energia",
                     "Pra que gastar, se temos opção de não gastar",
                     "Você nem precisa disso..."
            };
            quote = quotes[new Random().nextInt(quotes.length)];
         }

         if (shouldSend) {
            String DISCORD_MSG_TEMPLATE = ("*{0}*\n" +
                  ":shopping_bags: **{1}**\n" +
                  ":moneybag: ~~{2}~~ **{3}** (-{4}%) [ℹ️](http://localhost?processed:{5}/session:{6}/)\n" +
                  ":link: {7}");
            DecimalFormat priceFormat = new DecimalFormat("#,##0.00");
            String msg = MessageFormat.format(DISCORD_MSG_TEMPLATE, quote,
                  newProcessedProduct.getOriginalName(),
                  priceFormat.format(previousProcessedProduct.getPrice()),
                  priceFormat.format(newProcessedProduct.getPrice()),
                  MathUtils.normalizeTwoDecimalPlaces(discount),
                  String.valueOf(newProcessedProduct.getId()),
                  session.getSessionId(),
                  newProcessedProduct.getUrl());
            DiscordMessages.reportPriceChanges(session, msg, author, avatar);
         }
      }
   }

   private static void updateLMS(Processed newProcessedProduct, Processed previousProcessedProduct, String nowISO) {

      // get previous status to verify change
      String oldStatus = "void";
      if (previousProcessedProduct != null) {
         oldStatus = previousProcessedProduct.getStatus();
      }

      // update lms in case we had a status change
      if (oldStatus == null || !newProcessedProduct.getStatus().equals(oldStatus)) {
         newProcessedProduct.setLms(nowISO);
      }
   }

   private static void updateLMT(Processed newProcessedProduct, String nowISO) {
      if (newProcessedProduct.getChanges() != null
            && (newProcessedProduct.getChanges().has("pic") || newProcessedProduct.getChanges().has("originals"))) {
         newProcessedProduct.setLmt(nowISO);
      }
   }

   /**
    * Fetch from database the current ProcessedModel from processed table.
    * 
    * @param product
    * @param session
    * @return the current ProcessedModel stored on database, or null if the product doesn't yet exists
    *         on processed table.
    * @throws MalformedPricesException
    */
   public Processed fetchPreviousProcessed(Product product, Session session) {
      Logging.printLogInfo(logger, session, "Fetching previous processed product ...");

      Processed actualProcessedProduct;

      /*
       * If we are running a test for new crawlers, it may occur cases where the internalId in the product
       * is null, because of a fail in crawling logic for example. In the case a product is void for
       * example, it also may occur to not find the internalId, so we must get what was passed via
       * CrawlerSession. But there are some cases where we don't have the internalId in the session, but
       * the product have it, in case of a product crawled from a URL scheduled by the crawler discover
       * for example.
       */
      String internalId = product.getInternalId();
      if (internalId == null || internalId.isEmpty()) {
         internalId = session.getInternalId();
      }

      // sanitize
      internalId = sanitizeBeforePersist(internalId);

      if (internalId != null && !internalId.isEmpty()) {

         StringBuilder query = new StringBuilder();
         query.append("SELECT * FROM processed WHERE market = ");
         query.append(session.getMarket().getNumber());
         query.append(" AND internal_id = '");
         query.append(internalId);
         query.append("' LIMIT 1");

         Connection conn = null;
         Statement sta = null;
         ResultSet rs = null;

         long queryStartTime = System.currentTimeMillis();

         try {

            conn = JdbcConnectionFactory.getInstance().getConnection();
            sta = conn.createStatement();
            rs = sta.executeQuery(query.toString());

            while (rs.next()) {

               JSONObject digitalContent;
               if (rs.getString("digital_content") != null) {
                  try {
                     digitalContent = new JSONObject(rs.getString("digital_content"));
                  } catch (JSONException e) {
                     digitalContent = null;
                  }
               } else {
                  digitalContent = null;
               }

               JSONObject changes;
               if (rs.getString("changes") != null) {
                  try {
                     changes = new JSONObject(rs.getString("changes"));
                  } catch (JSONException e) {
                     changes = null;
                  }
               } else {
                  changes = null;
               }

               JSONArray similars;
               if (rs.getString("similars") != null) {
                  try {
                     similars = new JSONArray(rs.getString("similars"));
                  } catch (JSONException e) {
                     similars = null;
                  }
               } else {
                  similars = null;
               }

               /*
                * Behavior
                */
               String behaviorJSONArrayString = rs.getString("behaviour");
               JSONArray behaviorJSONArray;
               if (behaviorJSONArrayString != null) {
                  try {
                     behaviorJSONArray = new JSONArray(behaviorJSONArrayString);
                  } catch (JSONException e) {
                     behaviorJSONArray = null;
                  }
               } else {
                  behaviorJSONArray = null;
               }

               Behavior behavior = new Behavior();
               if (behaviorJSONArray != null) {
                  for (int i = 0; i < behaviorJSONArray.length(); i++) {
                     JSONObject behaviorElementJSON = behaviorJSONArray.getJSONObject(i);

                     try {
                        BehaviorElement behaviorElement = new BehaviorElement(behaviorElementJSON);
                        behavior.add(behaviorElement);
                     } catch (Exception e) {
                        Logging.printLogError(logger, session, Util.getStackTraceString(e));
                     }
                  }
               }

               /*
                * Marketplace
                * 
                * get the JSON representation from database if the JSON is null, then an empty model instance is
                * created. Each seller is individually added to the marketplace model, so we can analyze for errors
                * in each of one separately and consider only those seller that are free of errors.
                */
               JSONArray actualMarketplaceJSONArray;
               if (rs.getString("marketplace") != null) {
                  try {
                     actualMarketplaceJSONArray = new JSONArray(rs.getString("marketplace"));
                  } catch (JSONException e) {
                     actualMarketplaceJSONArray = null;
                  }
               } else {
                  actualMarketplaceJSONArray = null;
               }

               Marketplace actualMarketplace = createMarketplace(actualMarketplaceJSONArray, session);

               /*
                * Prices
                * 
                * get the JSON representation from database if the JSON is null, then an empty model instance is
                * crated if any model creating error occurs, an emtpy model is created and the error is registered
                * on the session and logged.
                */
               JSONObject actualPricesJson;
               if (rs.getString("prices") != null) {
                  try {
                     actualPricesJson = new JSONObject(rs.getString("prices"));
                  } catch (JSONException e) {
                     actualPricesJson = null;
                  }
               } else {
                  actualPricesJson = null;
               }

               Prices actualPrices;
               try {
                  actualPrices = new Prices(actualPricesJson);
               } catch (Exception e) {
                  actualPrices = new Prices();

                  Logging.printLogError(logger, session, Util.getStackTraceString(e));
               }

               /*
                * Offers
                * 
                * get the JSON representation from database if the JSON is null, then an empty model instance is
                * created. Each seller is individually added to the marketplace model, so we can analyze for errors
                * in each of one separately and consider only those seller that are free of errors.
                */
               JSONArray actualOffersJSONArray;
               if (rs.getString("offers") != null) {
                  try {
                     actualOffersJSONArray = new JSONArray(rs.getString("offers"));
                  } catch (JSONException e) {
                     actualOffersJSONArray = null;
                  }
               } else {
                  actualOffersJSONArray = null;
               }

               Offers offers;
               try {
                  offers = new Offers(actualOffersJSONArray);
               } catch (Exception e) {
                  offers = new Offers();

                  Logging.printLogError(logger, session, Util.getStackTraceString(e));
               }

               /*
                * Rating Reviews
                * 
                */

               JSONObject ratingReviewsJson;
               RatingsReviews ratingReviews;
               if (rs.getString("rating") != null) {
                  try {
                     ratingReviewsJson = new JSONObject(rs.getString("rating"));
                     try {
                        ratingReviews = new RatingsReviews(ratingReviewsJson);
                     } catch (MalformedRatingModel e) {
                        ratingReviews = new RatingsReviews();
                     }
                  } catch (JSONException e) {
                     ratingReviews = new RatingsReviews();
                  }
               } else {
                  ratingReviews = new RatingsReviews();
               }


               /*
                * Stock
                */
               Integer actualStock = rs.getInt("stock");
               if (actualStock == 0) {
                  actualStock = null;
               }

               /*
                * Price
                */
               Float actualPrice = rs.getFloat("price");
               if (actualPrice == 0) {
                  actualPrice = null;
               }

               /*
                * Array of eans
                */
               List<String> eans = null;
               Array eansArray = rs.getArray("eans");
               if (eansArray != null) {
                  eans = Arrays.asList((String[]) eansArray.getArray());
               }

               JSONObject postgresMetadata = new JSONObject()
                     .put("postgres_elapsed_time", System.currentTimeMillis() - queryStartTime)
                     .put("query_type", "fetch_previous_processed");

               Logging.logInfo(logger, session, postgresMetadata, "POSTGRES TIMING INFO");

               /*
                * Create the Processed model
                */
               actualProcessedProduct = new Processed(rs.getLong("id"), rs.getString("internal_id"), rs.getString("internal_pid"),
                     rs.getString("original_name"), rs.getString("class"), rs.getString("brand"), rs.getString("recipient"), rs.getDouble("quantity"),
                     rs.getInt("multiplier"), rs.getString("unit"), rs.getString("extra"), rs.getString("pic"), rs.getString("secondary_pics"),
                     rs.getString("cat1"), rs.getString("cat2"), rs.getString("cat3"), rs.getString("url"), rs.getInt("market"), rs.getString("ect"),
                     rs.getString("lmt"), rs.getString("lat"), rs.getString("lrt"), rs.getString("lms"), rs.getString("status"), changes,
                     rs.getString("original_description"), actualPrice, actualPrices, digitalContent,
                     rs.getObject("lett_id") instanceof Long ? rs.getLong("lett_id") : null,
                     rs.getObject("master_id") instanceof Long ? rs.getLong("master_id") : null, similars, rs.getBoolean("available"), rs.getBoolean("void"),
                     actualStock, behavior, actualMarketplace, rs.getString("ean"), eans, offers, ratingReviews);

               return actualProcessedProduct;

            }

         } catch (SQLException e) {
            Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
         } finally {
            JdbcConnectionFactory.closeResource(rs);
            JdbcConnectionFactory.closeResource(sta);
            JdbcConnectionFactory.closeResource(conn);
         }
      }

      return null;
   }

   /**
    * Get each JSONObject representing a seller from the marketplaceJSONArray and creates an instance
    * of Seller model using the corresponding Seller JSON. If any error occurs during the creation of
    * one seller, the error is logged and this seller is not added to the marketplace model instance.
    * 
    * @param marketplaceJSONArray
    * @param session
    * @return
    */
   private static Marketplace createMarketplace(JSONArray marketplaceJSONArray, Session session) {
      Marketplace actualMarketplace = new Marketplace();
      try {
         if (marketplaceJSONArray != null && marketplaceJSONArray.length() > 0) {
            for (int i = 0; i < marketplaceJSONArray.length(); i++) {
               JSONObject sellerJSON = marketplaceJSONArray.getJSONObject(i);
               try {
                  Seller seller = new Seller(sellerJSON);
                  actualMarketplace.add(seller);
               } catch (Exception e) {
                  Logging.printLogError(logger, session, Util.getStackTraceString(e));
               }
            }
         }
      } catch (Exception e) {
         Logging.printLogError(logger, session, Util.getStackTraceString(e));
      }

      return actualMarketplace;
   }

   private static String sanitizeBeforePersist(String field) {
      if (field == null) {
         return null;
      } else {
         return field.replace("'", "''").trim();
      }
   }

}
