package br.com.lett.crawlernode.aws.dynamodb;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.SkuStatus;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.main.GlobalConfigurations;
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
import org.apache.commons.lang.time.DateUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
         .withCredentials(new DefaultAWSCredentialsProviderChain())
         .build();

      dynamoDB = new DynamoDB(amazonDynamoDB);

   }

   public static JSONObject fetchObjectDynamo(String md5) {

      try {

         Table table = dynamoDB.getTable(GlobalConfigurations.executionParameters.getDynamoTableName());
         QuerySpec spec = new QuerySpec()
            .withKeyConditionExpression("market_id_url_md5 = :market_id_url_md5")
            .withProjectionExpression("found_skus, finished_at, scheduled_at, created_at")
            .withValueMap(new ValueMap()
               .withString(":market_id_url_md5", md5))
            .withConsistentRead(true);

         ItemCollection<QueryOutcome> items = table.query(spec);

         Iterator<Item> iterator = items.iterator();
         Item item;
         while (iterator.hasNext()) {
            item = iterator.next();
            Logging.printLogInfo(logger, "Fetching object in dynamo " + md5); //todo: remove
            Logging.printLogInfo(logger, "Object in dynamo " + item.toJSONPretty()); //todo: remove

            return stringToJson(item.toJSONPretty());
         }

      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
         Logging.printLogError(logger, "Error fetching object from DynamoDB  " + md5);
      }

      return new JSONObject();

   }


   public static JSONObject fetchObjectDynamo(String url, int marketId) {
      return fetchObjectDynamo(convertUrlInMD5(url, marketId));

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

            return scheduledDate.after(oneHour);

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
         Table table = dynamoDB.getTable(GlobalConfigurations.executionParameters.getDynamoTableName());
         Item item = new Item()
            .withPrimaryKey("market_id_url_md5", md5)
            .withString("grid_internal_id", internalId)
            .withString("grid_internal_pid", internalPid)
            .withString("scheduled_at", getCurrentTime())
            .withString("created_at", getCurrentTime());

         Logging.printLogDebug(logger, "Insert item in dynamo " + md5);

         table.putItem(item);

      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
         Logging.printLogError(logger, "Error inserting object in dynamo: " + product.getUrl());
      }
   }


   public static void updateReadByCrawlerObjectDynamo(List<Product> products, Session session, SkuStatus skuStatus) {

      JSONObject productDynamo;
      String createdAt = "";

      List<Map> foundSkus = new ArrayList<>();
      String md5 = convertUrlInMD5(session.getOriginalURL(), session.getMarket().getId());;
      for (Product p : products) {

         if (createdAt.isEmpty()) {
            productDynamo = fetchObjectDynamo(md5);
            if (productDynamo.isEmpty()) {
               Logging.printLogError(logger, "Error fetching object in dynamo: " + p.getUrl());
               return;
            }
            createdAt = productDynamo.optString("created_at");
         }

         Map<String, String> map = new HashMap<>();
         map.put("internal_id", p.getInternalId());
         map.put("event_timestamp", p.getTimestamp());
         map.put("status", skuStatus.toString());

         foundSkus.add(map);
      }

      Map<String, String> expressionAttributeNames = new HashMap<String, String>();
      expressionAttributeNames.put("#found_skus", "found_skus");
      expressionAttributeNames.put("#finished_at", "finished_at");

      Map<String, Object> expressionAttributeValues = new HashMap<String, Object>();
      expressionAttributeValues.put(":found_skus", foundSkus);
      expressionAttributeValues.put(":finished_at", getCurrentTime());

      try {
         Table table = dynamoDB.getTable(GlobalConfigurations.executionParameters.getDynamoTableName());
         UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(new PrimaryKey("market_id_url_md5", md5, "created_at", createdAt))
            .withUpdateExpression("set #found_skus = :found_skus, #finished_at = :finished_at")
            .withNameMap(expressionAttributeNames)
            .withValueMap(expressionAttributeValues);
         table.updateItem(updateItemSpec);

         Logging.printLogDebug(logger, "Updated item in dynamo " + md5);

      } catch (Exception e) {
         Logging.printLogError(logger, CommonMethods.getStackTrace(e));
         Logging.printLogError(logger, "Error updating object in dynamo: " + md5);
      }
   }

   public static void updateScheduledObjectDynamo(RankingProduct p, String createdAt) {
      String md5 = convertUrlInMD5(p.getUrl(), p.getMarketId());
      String internalId = p.getInternalId() != null ? p.getInternalId() : "";
      String internalPid = p.getInternalPid() != null ? p.getInternalPid() : "";

      try {
         Table table = dynamoDB.getTable(GlobalConfigurations.executionParameters.getDynamoTableName());

         Map<String, String> expressionAttributeNames = new HashMap<String, String>();
         expressionAttributeNames.put("#scheduled_at", "scheduled_at");
         expressionAttributeNames.put("#grid_internal_pid", "grid_internal_pid");
         expressionAttributeNames.put("#grid_internal_id", "grid_internal_id");

         Map<String, Object> expressionAttributeValues = new HashMap<String, Object>();
         expressionAttributeValues.put(":scheduled_at", getCurrentTime());
         expressionAttributeValues.put(":grid_internal_pid", internalPid);
         expressionAttributeValues.put(":grid_internal_id", internalId);

         UpdateItemSpec updateItemSpec = new UpdateItemSpec()
            .withPrimaryKey(new PrimaryKey("market_id_url_md5", md5, "created_at", createdAt))
            .withUpdateExpression("set #scheduled_at = :scheduled_at, #grid_internal_pid = :grid_internal_pid, #grid_internal_id = :grid_internal_id")
            .withNameMap(expressionAttributeNames)
            .withValueMap(expressionAttributeValues)
            .withReturnValues(ReturnValue.ALL_NEW);

         UpdateItemOutcome outcome = table.updateItem(updateItemSpec);
         Logging.printLogDebug(logger, "Displaying updated item..." + outcome.getItem().toJSONPretty());

      } catch (Exception e) {
         Logging.printLogError(logger, CommonMethods.getStackTrace(e));
         Logging.printLogError(logger, "Error updating object in dynamo: " + p.getUrl());
      }
   }


}
