package br.com.lett.crawlernode.aws.dynamodb;

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
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import models.Processed;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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


   public static JSONObject fetchObjectDynamo(String url, int marketId) {

      String md5 = marketId + "_" + convertUrlInMD5(url);

      try {

         Table table = dynamoDB.getTable("capture_job");
         QuerySpec spec = new QuerySpec()
            .withKeyConditionExpression("market_id_url_md5 = :market_id_url_md5")
            //  .withFilterExpression("scheduled_at > :now")
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

   public static String convertUrlInMD5(String url) {
      StringBuilder s = new StringBuilder();
      try {
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

      JSONObject result = fetchObjectDynamo(url, market);

      JSONArray foundSkus = result.optJSONArray("found_skus");

         for (Object o : foundSkus) {
            if (o instanceof JSONObject){
            Processed p = new Processed();
            JSONObject product = (JSONObject) o;
            p.setVoid(product.optString("status").equalsIgnoreCase("void"));
            p.setLrt(product.optString("event_timestamp"));
            p.setId(Persistence.fetchProcessedIdWithInternalId(product.optString("internal_id"), market, session));

            processeds.add(p);
         }

      }

      return processeds;
   }


}
