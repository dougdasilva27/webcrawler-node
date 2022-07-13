package br.com.lett.crawlernode.aws.dynamodb;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import dbmodels.Tables;
import models.Processed;
import org.jooq.Field;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

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


   public static JSONArray fetchObjectDynamo(String url, int marketId) {

      JSONArray jsonArray = new JSONArray();
      try {

         Table table = dynamoDB.getTable("capture_job");
         QuerySpec spec = new QuerySpec()
         //   .withKeyConditionExpression("market_id_url_md5 = 2407_7f2c6b5dc3d3a27c8073c08c417b9a31")
            .withKeyConditionExpression("market_id_url_md5 = :market_id_url_md5")
            //  .withFilterExpression("scheduled_at > :now")
            .withProjectionExpression("grid_internal_id, grid_internal_pid")
            .withValueMap(new ValueMap()
               .withString(":market_id_url_md5", "2407_7f2c6b5dc3d3a27c8073c08c417b9a31"))
            .withConsistentRead(true);


         ItemCollection<QueryOutcome> items = table.query(spec);

         Iterator<Item> iterator = items.iterator();
         Item item = null;
         while (iterator.hasNext()) {
            item = iterator.next();
            System.out.println(item.toJSONPretty());
            jsonArray.put(item.toJSONPretty());

         }


      } catch (Exception e) {
         Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
         return null;
      }

      return jsonArray;


   }


   public static List<Processed> fetchProcesseds(String url, int market, Session session) {
      List<Processed> processedIds = new ArrayList<>();
      dbmodels.tables.Processed processed = Tables.PROCESSED;

      List<Field<?>> fields = new ArrayList<>();
      fields.add(processed.ID);
      fields.add(processed.MASTER_ID);
      fields.add(processed.STATUS);
      fields.add(processed.URL);
      fields.add(processed.LRT);
      fields.add(processed.ORIGINAL_NAME);
      fields.add(processed.PRICE);
      fields.add(processed.AVAILABLE);
      fields.add(processed.PIC);

      JSONArray fetchObjectDynamo = fetchObjectDynamo(url, market);

//         for (Record record : records) {
//            Processed p = new Processed();
//            Long masterId = record.get(processed.MASTER_ID);
//            p.setVoid(record.get(processed.STATUS).equalsIgnoreCase("void"));
//            p.setUrl(record.get(processed.URL));
//            p.setLrt(record.get(processed.LRT));
//            p.setOriginalName(record.get(processed.ORIGINAL_NAME));
//            p.setPrice(record.get(processed.PRICE) != null ? record.get(processed.PRICE).floatValue() : null);
//            p.setAvailable(record.get(processed.AVAILABLE));
//            p.setPic(record.get(processed.PIC));
//
//            if (masterId != null) {
//               p.setId(record.get(processed.MASTER_ID));
//            } else {
//               p.setId(record.get(processed.ID));
//            }
//            processedIds.add(p);
//         }
//
//         JSONObject apacheMetadata = new JSONObject().put("postgres_elapsed_time", System.currentTimeMillis() - queryStartTime)
//            .put("query_type", "ranking_fetch_processed_product_with_internalpid");
//
//         Logging.logInfo(logger, session, apacheMetadata, "POSTGRES TIMING INFO");
//
//      } catch (Exception e) {
//         Logging.printLogError(logger, CommonMethods.getStackTrace(e));
//      } finally {
//         JdbcConnectionFactory.closeResource(rs);
//         JdbcConnectionFactory.closeResource(sta);
//         JdbcConnectionFactory.closeResource(conn);
//      }

      return processedIds;
   }


}
