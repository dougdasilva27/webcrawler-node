package br.com.lett.crawlernode.core.task;

import br.com.lett.crawlernode.aws.sqs.QueueHandler;
import br.com.lett.crawlernode.aws.sqs.QueueService;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.SeedCrawlerSession;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.ScraperInformation;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import enums.QueueName;
import models.Processed;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static br.com.lett.crawlernode.util.CrawlerUtils.stringToJson;


public class Scheduler {

   protected static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

   public static void scheduleImages(Session session, QueueHandler queueHandler, Processed processed, Long processedId) throws SQLException {
      Logging.printLogDebug(LOGGER, session, "Scheduling images to be downloaded...");

      List<SendMessageBatchRequestEntry> entries = new ArrayList<>(); // send messages batch to Amazon SQS

      Integer counter = 0;

      Market market = session.getMarket();
      String primaryPic = processed.getPic();
      String internalId = processed.getInternalId();
      String url = processed.getPic();

      try {

         // assemble the primary image message
         if (primaryPic != null && !primaryPic.isEmpty()) {
            JSONObject attrPrimary = assembleImageMessageAttributes(internalId, processedId, url, market, QueueService.PRIMARY_IMAGE_TYPE_MESSAGE_ATTR,session);

            String body = removesUselessCharacters(attrPrimary.toString());

            SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
            entry.setId(String.valueOf(counter)); // the id must be unique in the batch
            entry.setMessageBody(body);

            entries.add(entry);
            counter++;

            // when the batch reaches size 10, we send them all to sqs and empty the list
            if (entries.size() == 10) {
               Logging.printLogDebug(LOGGER, session, "Sending batch of " + entries.size() + " messages...");

               // send the batch
               SendMessageBatchResult result;
               if(session instanceof SeedCrawlerSession){
                  Logging.printLogInfo(LOGGER, session, "Sending to queue: " + QueueName.PRODUCT_IMAGE_DOWNLOAD_SEED.toString());
                  result = QueueService.sendBatchMessages(queueHandler.getSqs(), QueueName.PRODUCT_IMAGE_DOWNLOAD_SEED.toString(), entries);
               }else{
                  Logging.printLogInfo(LOGGER, session, "Sending to queue: " + QueueName.PRODUCT_IMAGE_DOWNLOAD.toString());
                  result = QueueService.sendBatchMessages(queueHandler.getSqs(), QueueName.PRODUCT_IMAGE_DOWNLOAD.toString(), entries);
               }

               // get send request results
               result.getSuccessful();
               entries.clear();
            }
         }

         if (entries.size() > 0) { // the left over
            Logging.printLogDebug(LOGGER, session, "Sending remaining batch of " + entries.size() + " messages...");

            SendMessageBatchResult result = null;
            if(session instanceof SeedCrawlerSession){
               Logging.printLogInfo(LOGGER, session, "Sending to queue: " + QueueName.PRODUCT_IMAGE_DOWNLOAD_SEED.toString());
               result = QueueService.sendBatchMessages(queueHandler.getSqs(), QueueName.PRODUCT_IMAGE_DOWNLOAD_SEED.toString(), entries);
            }else{
               Logging.printLogInfo(LOGGER, session, "Sending to queue: " + QueueName.PRODUCT_IMAGE_DOWNLOAD.toString());
               result = QueueService.sendBatchMessages(queueHandler.getSqs(), QueueName.PRODUCT_IMAGE_DOWNLOAD.toString(), entries);
            }

            result.getSuccessful();

            entries.clear();
         }

      } catch (Exception e) {
         Logging.printLogError(LOGGER, "Error during sqs query execution.");
         Logging.printLogError(LOGGER, CommonMethods.getStackTraceString(e));
      }
   }

   /*
    This is an example of the message format that is sent to the Download queue

   {
    "processed_id": 35253474,
    "images": [
        {
            "position": 1,
            "url": "https://images-na.ssl-images-amazon.com/images/I/81pULMGKjsL._AC_SL1500_.jpg"
        }
    ],
    "internal_id": "2063",
    "download_config": {
        "headers": {
            "Accept": "*"
        },
        "proxies": [
            "luminati_server_br",
            "bonanza",
            "no_proxy"
        ]
    },
    "type": "primary",
    "market_code": "market_nVR500va435qyD"
}
   */

   private static JSONObject assembleImageMessageAttributes(String internalId, Long processedId, String url, Market market, String type,Session session) {

      String market_code = market.getCode();

      JSONObject download_config = new JSONObject();
      JSONObject headers = new JSONObject();
      headers.put("Accept", "*");

      download_config.put("headers", headers);
      download_config.put("proxies", session.getImageProxies());

      JSONObject body = new JSONObject();
      body.put("processed_id", processedId);
      body.put("type", type);
      body.put("market_code", market_code);
      body.put("internal_id", internalId);
      body.put("images", imagesTypes(type ,url));
      body.put("download_config", download_config);

      return body;
   }

   private static String removesUselessCharacters (String string) {
      String body;

      if (string != null && string.contains("\\\"")){
         body = string.replace("\\\"", "");
      } else {
         body = string;
      }
      return  body;
   }
   private static List<JSONObject> imagesTypes (String type, String url) {
      ArrayList<JSONObject> imagesArr = new ArrayList<>();

      if(type.equals("primary")){
         JSONObject image = new JSONObject();
         image.put("url", url);
         image.put("position", 1);
         imagesArr.add(image);
      }
      return imagesArr;
   }


   public static JSONObject mountMessageToSendToQueue(String parameters, Market market , ScraperInformation scraper, String scraperType) {

      JSONObject jsonToSendToCrawler = new JSONObject();
      String sessionId = UUID.randomUUID().toString();
      JSONObject marketInfo = new JSONObject();
      marketInfo.put("code", market.getCode());
      marketInfo.put("regex", market.getFirstPartyRegex());
      marketInfo.put("fullName", market.getFullName());
      marketInfo.put("marketId", market.getId());
      marketInfo.put("use_browser", scraper.isUseBrowser());
      marketInfo.put("name", market.getName());
      jsonToSendToCrawler.put("sessionId", sessionId);
      jsonToSendToCrawler.put("type", scraperType);
      jsonToSendToCrawler.put("options", scraper.getOptions());
      jsonToSendToCrawler.put("market", marketInfo);
      jsonToSendToCrawler.put("className", scraper.getClassName());
      jsonToSendToCrawler.put("parameters", parameters);

      return jsonToSendToCrawler;

   }



}
