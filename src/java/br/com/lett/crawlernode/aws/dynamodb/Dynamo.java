package br.com.lett.crawlernode.aws.dynamodb;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import models.Processed;
import org.apache.commons.lang.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static br.com.lett.crawlernode.util.JSONUtils.stringToJson;

public class Dynamo {

   protected static final Logger logger = LoggerFactory.getLogger(Dynamo.class);

   private static DynamoDB dynamoDB;
   private static final int AWS_CONNECTION_TIMEOUT = Math.toIntExact(TimeUnit.MINUTES.toMillis(20));


   static {
      ClientConfiguration clientConfiguration = new ClientConfiguration();
      clientConfiguration.setConnectionTimeout(AWS_CONNECTION_TIMEOUT);

      AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder
         .standard()
         .withRegion(Regions.US_EAST_1)
         .withClientConfiguration(clientConfiguration)
         //  .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("https://dynamodb.us-east-1.amazonaws.com", "us-east-1"))
         .withCredentials(new DefaultAWSCredentialsProviderChain())
         .build();

      dynamoDB = new DynamoDB(amazonDynamoDB);

   }


   public static JSONObject fetchObjectDynamo(String url, int marketId, Session session) {

      String md5 = convertUrlInMD5(url, marketId);

      try {
//         Map<String, AttributeMap> attributeMapMap = new HashMap<>();
//         attributeMapMap.put(":finished_at", new AttributeMap().withS.("")

         Table table = dynamoDB.getTable("capture_job");
         QuerySpec spec = new QuerySpec()
            .withKeyConditionExpression("market_id_url_md5 = :market_id_url_md5")
            //  .withFilterExpression("scheduled_at > :now")
            //    .withKeyConditionExpression("finished_at = :finished_at")
            .withProjectionExpression("found_skus, finished_at")
            .withValueMap(new ValueMap()
               .withString(":market_id_url_md5", md5))
            .withConsistentRead(true);


         ItemCollection<QueryOutcome> items = table.query(spec);

         Iterator<Item> iterator = items.iterator();
         Item item = null;
         while (iterator.hasNext()) {
            item = iterator.next();
            System.out.println(item.toJSONPretty());

            return stringToJson(item.toJSONPretty());
         }

      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
      }

      return new JSONObject();

   }


   public static String scheduledTime(RankingProduct product) {

      String md5 = convertUrlInMD5(product.getUrl(), product.getMarketId());

      try {

         Table table = dynamoDB.getTable("capture_job");
         QuerySpec spec = new QuerySpec()
            .withKeyConditionExpression("market_id_url_md5 = :market_id_url_md5")
            .withProjectionExpression("scheduled_at")
            .withValueMap(new ValueMap()
               .withString(":market_id_url_md5", md5))
            .withConsistentRead(true);


         ItemCollection<QueryOutcome> items = table.query(spec);

         Iterator<Item> iterator = items.iterator();
         Item item;
         while (iterator.hasNext()) {
            item = iterator.next();
            System.out.println(item.toJSONPretty());
            JSONObject jsonObject = stringToJson(item.toJSONPretty());

            return jsonObject.optString("scheduled_at");

         }

      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
      }

      return "";

   }

   public static boolean scheduledMoreThanTwelveHours(String scheduled, Session session) {
      if (scheduled != null && !scheduled.isEmpty()) {
         try {

            Date scheduledDate = new SimpleDateFormat(
               "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(scheduled);
            Date twelveHours = DateUtils.addHours(new Date(), -12);
            Logging.printLogDebug(logger, session, scheduled + " - " + twelveHours + " has more than 12 hours: " + (scheduledDate.after(twelveHours)));
            return scheduledDate.before(twelveHours);

         } catch (Exception e) {
            Logging.printLogError(logger, session, "Error parsing lrt date: " + scheduled);
         }
      }

      return false;
   }

   public static String convertUrlInMD5(String url, int marketId) {
      StringBuilder s = new StringBuilder();
      try {
         s.append(marketId).append("_");
         MessageDigest mgs = MessageDigest.getInstance("MD5");
         byte[] hash = mgs.digest(url.getBytes(StandardCharsets.UTF_8));
         for (byte b : hash) {
            s.append(String.format("%02x", b));
         }
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      }

      return s.toString();


   }

   public static List<Processed> fetchProducts(String url, int market, Session session) {
      List<Processed> processeds = new ArrayList<>();

      JSONObject result = fetchObjectDynamo(url, market, session);

      JSONArray foundSkus = result.optJSONArray("found_skus");

      if (foundSkus != null) {

         for (Object o : foundSkus) {
            if (o instanceof JSONObject) {
               Processed p = new Processed();
               JSONObject product = (JSONObject) o;
               p.setVoid(product.optString("status").equalsIgnoreCase("void"));
               p.setLrt(product.optString("event_timestamp"));
               p.setId(Persistence.fetchProcessedIdWithInternalId(product.optString("internal_id"), market, session));

               processeds.add(p);
            }

         }
      }

      return processeds;
   }

   public static String getCurrentTime() {
      LocalDateTime datetime1 = LocalDateTime.now();
      DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
      return datetime1.format(format);
   }

   public static void insertObjectDynamo(RankingProduct product) {
      String md5 = convertUrlInMD5(product.getUrl(), product.getMarketId());
      String internalId = product.getInternalId() != null ? product.getInternalId() : "";
      String internalPid = product.getInternalPid() != null ? product.getInternalPid() : "";

      try {

         Table table = dynamoDB.getTable("capture_job");
         Item item = new Item()
            .withPrimaryKey("market_id_url_md5", md5)
            .withString("grid_internal_id", internalId)
            .withString("grid_internal_pid", internalPid)
            .withString("scheduled_at", getCurrentTime());
         table.putItem(item);

      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
      }
   }

   public static boolean sendToQueue(RankingProduct product, Session session) {
      boolean mustSend;

      String scheduled = scheduledTime(product);
      if (scheduled == null || scheduled.isEmpty()) {
         insertObjectDynamo(product);
         mustSend = true;
      } else {
         if (!scheduledMoreThanTwelveHours(scheduled, session)) {
            mustSend = true;
            updateObjectDynamo(product);
         } else {
            mustSend = false;
         }

      }
      return mustSend;

   }

   public static void updateObjectDynamo(List<Product> products) {
      JSONArray foundSkus = new JSONArray();
      String md5 = "";
      for (Product p : products) {
         JSONObject product = new JSONObject();
         product.put("internal_id", p.getInternalId());
         product.put("event_timestamp", p.getTimestamp());
         product.put("status", p.getStatus());
         product.put("last_modified_status_timestamp", p.getInternalPid());

         foundSkus.put(product);
         if (md5.isEmpty()) {
            md5 = convertUrlInMD5(p.getUrl(), p.getMarketId());
         }
      }

      try {
         Table table = dynamoDB.getTable("capture_job");
         UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey("market_id_url_md5", md5)
            .withUpdateExpression("set found_skus = :found_skus, finished_at = :finished_at")
            .withValueMap(new ValueMap()
               .withList(":found_skus", foundSkus)
               .withString(":finished_at", getCurrentTime()));
         UpdateItemOutcome outcome = table.updateItem(updateItemSpec);

         // Confirm
         System.out.println("Displaying updated item...");
         System.out.println(outcome.getItem().toJSONPretty());
      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
      }
   }

   public static void updateObjectDynamo(RankingProduct p) {
      String md5 = convertUrlInMD5(p.getUrl(), p.getMarketId());
      String internalId = p.getInternalId() != null ? p.getInternalId() : "";
      String internalPid = p.getInternalPid() != null ? p.getInternalPid() : "";

      try {
         Table table = dynamoDB.getTable("capture_job");
         UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey("market_id_url_md5", md5)
            .withUpdateExpression("set grid_internal_id = :grid_internal_id,grid_internal_pid = :grid_internal_pid, scheduled_at = :scheduled_at")
            .withValueMap(new ValueMap()
               .withString(":scheduled_at", getCurrentTime())
               .withString(":grid_internal_id", internalId)
               .withString(":grid_internal_pid", internalPid))
            .withReturnValues(ReturnValue.ALL_NEW);
         UpdateItemOutcome outcome = table.updateItem(updateItemSpec);

         // Confirm
         System.out.println("Displaying updated item...");
         System.out.println(outcome.getItem().toJSONPretty());
      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
      }
   }


}
