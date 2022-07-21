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
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.*;
import models.Processed;
import org.apache.commons.lang.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static br.com.lett.crawlernode.util.JSONUtils.stringToJson;
import static com.amazonaws.auth.policy.actions.DynamoDBv2Actions.UpdateItem;

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


   public static JSONObject fetchObjectDynamo(String url, int marketId) {

      String md5 = convertUrlInMD5(url, marketId);

      try {
//         Map<String, AttributeMap> attributeMapMap = new HashMap<>();
//         attributeMapMap.put(":finished_at", new AttributeMap().withS.("")

         Table table = dynamoDB.getTable("capture_job");
         QuerySpec spec = new QuerySpec()
            .withKeyConditionExpression("market_id_url_md5 = :market_id_url_md5")
            .withProjectionExpression("found_skus, finished_at, scheduled_at")
            .withValueMap(new ValueMap()
               .withString(":market_id_url_md5", md5))
            .withConsistentRead(true);

         ItemCollection<QueryOutcome> items = table.query(spec);

         Iterator<Item> iterator = items.iterator();
         Item item;
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

   public static String fetchObjectDynamoScheduledAt(String md5) {

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

            return item.getString("scheduled_at");
         }

      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
      }

      return "";

   }


   public static JSONObject scheduledTime(RankingProduct product) {

      String md5 = convertUrlInMD5(product.getUrl(), product.getMarketId());
      Date oneHour = DateUtils.addHours(new Date(), +1);

      String dateOneHour = new SimpleDateFormat(
         "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(oneHour);

      try {

         Table table = dynamoDB.getTable("capture_job");
         QuerySpec spec = new QuerySpec()
            .withKeyConditionExpression("market_id_url_md5 = :market_id_url_md5 and scheduled_at > :dateOneHour")
            .withProjectionExpression("scheduled_at, finished_at")
            .withValueMap(new ValueMap()
               .withString(":market_id_url_md5", md5)
               .withString(":dateOneHour", dateOneHour))
            .withConsistentRead(true);

         ItemCollection<QueryOutcome> items = table.query(spec);

         Iterator<Item> iterator = items.iterator();
         Item item;
         while (iterator.hasNext()) {
            item = iterator.next();
            System.out.println(item.toJSONPretty());
            JSONObject jsonObject = stringToJson(item.toJSONPretty());

            return jsonObject;
         }

      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
      }

      return new JSONObject();

   }

   public static boolean scheduledMoreThanOneHour(String scheduled, Session session) {
      if (scheduled != null && !scheduled.isEmpty()) {
         try {

            Date scheduledDate = new SimpleDateFormat(
               "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(scheduled);
            Date oneHour = DateUtils.addHours(new Date(), +1);
            Logging.printLogInfo(logger, session, scheduled + " - " + oneHour + " has more than 1 hours: " + (scheduledDate.after(oneHour)));

           // return scheduledDate.after(oneHour);
            return true;

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


   public static void updateObjectDynamo(List<Product> products) {

      JSONObject productDynamo = new JSONObject();
      String scheduledAt = "";

      JSONArray foundSkus = new JSONArray();
      String md5 = "";
      for (Product p : products) {
         if (md5.isEmpty()) {
            md5 = convertUrlInMD5(p.getUrl(), p.getMarketId());
         }
         if (productDynamo.isEmpty()){
            productDynamo = fetchObjectDynamo(p.getUrl(), p.getMarketId());
            scheduledAt = productDynamo.getString("scheduled_at");
         }
         if (productDynamo != null && !productDynamo.isEmpty()) {
            JSONArray skus = productDynamo.optJSONArray("found_skus");

            for (Object o : skus) {
               if (o instanceof JSONObject) {
                  JSONObject sku = (JSONObject) o;
                  if(sku.optString("internal_id").equals(p.getInternalId())){
                     foundSkus.put(sku);
                     JSONObject product = new JSONObject();
                     product.put("internal_id", p.getInternalId());
                     product.put("event_timestamp", p.getTimestamp());
                     product.put("status", p.getStatus());
                     if (sku.optString("status").equals(p.getStatus())){
                        product.put("last_modified_status_timestamp", p.getTimestamp());
                     }
                     foundSkus.put(product);

                  }
               }

            }
         } else {
            JSONObject product = new JSONObject();
            product.put("internal_id", p.getInternalId());
            product.put("event_timestamp", p.getTimestamp());
            product.put("status", p.getStatus());
            product.put("last_modified_status_timestamp", p.getTimestamp());

            foundSkus.put(product);
         }


      }

      try {
         Table table = dynamoDB.getTable("capture_job");
         UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(new PrimaryKey("market_id_url_md5", md5, "scheduled_at", scheduledAt))
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

   public static void updateObjectDynamo(RankingProduct p, String scheduledAt) {
      String md5 = convertUrlInMD5(p.getUrl(), p.getMarketId());
      String internalId = p.getInternalId() != null ? p.getInternalId() : "";
      String internalPid = p.getInternalPid() != null ? p.getInternalPid() : "";

      Map<String, AttributeValue> attributeValues = new HashMap<>();
      attributeValues.put("#grid_internal_pid", new AttributeValue().withS("555"));
      attributeValues.put("#grid_internal_id", new AttributeValue().withS(internalId));

      try {
        Table table = dynamoDB.getTable("capture_job");

         Map<String, String> expressionAttributeNames = new HashMap<String, String>();
         expressionAttributeNames.put("#scheduled_at", "scheduled_at");

         Map<String, Object> expressionAttributeValues = new HashMap<String, Object>();
         expressionAttributeValues.put(":scheduled_at", getCurrentTime());
         UpdateItemSpec updateItemSpec = new UpdateItemSpec()
           .withPrimaryKey(new PrimaryKey("market_id_url_md5", md5, "scheduled_at", scheduledAt))
            .withUpdateExpression("set #scheduled_at = :scheduled_at")
            .withNameMap(expressionAttributeNames)
            .withValueMap(expressionAttributeValues)
            .withReturnValues(ReturnValue.ALL_NEW);

         UpdateItemOutcome outcome =  table.updateItem(updateItemSpec);
         System.out.println("Displaying updated item...");
         System.out.println(outcome.getItem().toJSONPretty());

      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
      }
   }


//   Map<String, AttributeValue> key = new HashMap<>();
//key.put("market_id_url_md5", new AttributeValue().withS(md5));
//   Map<String, AttributeValue> attributeValues = new HashMap<>();
//attributeValues.put(":scheduled_at", new AttributeValue().withS(getCurrentTime()));
//
//   UpdateItemRequest updateItemRequest = new UpdateItemRequest()
//      .withTableName("capture_job")
//      .withKey(key)
//      .withUpdateExpression("set scheduled_at = :scheduled_at")
//      .withExpressionAttributeValues(attributeValues);
//   UpdateItemResult updateItemResult = dynamoDB.client.updateItem(updateItemRequest);


   public static void deleteDynamo() {
      try {
         Table table = dynamoDB.getTable("capture_job");
         DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
            .withPrimaryKey("market_id_url_md5", "2950_d32195353c0450a54b66b011d08383e6");
         DeleteItemOutcome outcome = table.deleteItem(deleteItemSpec);

         // Confirm
         System.out.println("Displaying deleted item...");
         System.out.println(outcome.getItem().toJSONPretty());
      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
      }
   }

}
