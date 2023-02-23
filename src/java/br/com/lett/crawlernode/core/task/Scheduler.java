package br.com.lett.crawlernode.core.task;

import br.com.lett.crawlernode.aws.sqs.QueueService;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.ScraperInformation;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageBatchResultEntry;
import enums.QueueName;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static br.com.lett.crawlernode.main.GlobalConfigurations.executionParameters;


public class Scheduler {

   protected static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);


   public static JSONObject mountMessageToSendToQueue(String parameters, Market market, ScraperInformation scraper, String scraperType) {

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

      if (scraper.isMiranha()) {
         jsonToSendToCrawler.put("proxies", scraper.getProxies());
      }

      return jsonToSendToCrawler;

   }

   public static JSONObject mountMessageToSendToQueue(Session session) {

      JSONObject jsonToSendToCrawler = new JSONObject();
      JSONObject marketInfo = new JSONObject();
      marketInfo.put("code", session.getMarket().getCode());
      marketInfo.put("regex", session.getMarket().getFirstPartyRegex());
      marketInfo.put("fullName", session.getMarket().getFullName());
      marketInfo.put("marketId", session.getMarket().getId());
      marketInfo.put("use_browser", session.isWebDriver());
      marketInfo.put("name", session.getMarket().getName());
      jsonToSendToCrawler.put("sessionId", session.getSessionId());
      jsonToSendToCrawler.put("internalId", session.getInternalId());
      jsonToSendToCrawler.put("type", session.getScraperType());
      jsonToSendToCrawler.put("options", session.getOptions());
      jsonToSendToCrawler.put("market", marketInfo);
      jsonToSendToCrawler.put("className", session.getClassName());
      jsonToSendToCrawler.put("parameters", session.getOriginalURL());
      jsonToSendToCrawler.put("readingNumber", session.getReadingNumber());
      jsonToSendToCrawler.put("isVoid", session.scheduledProductIsVoid());

      return jsonToSendToCrawler;

   }

   public static void sendMessagesToQueue(JSONObject jsonToSentToQueue, boolean isMiranha, boolean isWebDriver, Session session) {

      List<SendMessageBatchRequestEntry> entries = new ArrayList<>();

      long sendMessagesStartTime = System.currentTimeMillis();

      SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
      entry.setId(session.getSessionId()); // the id must be unique in the batch

      entry.setMessageBody(jsonToSentToQueue.toString());

      entries.add(entry);

      populateMessagesInToQueue(entries, isMiranha, isWebDriver);

      JSONObject apacheMetadata = new JSONObject().put("aws_elapsed_time", System.currentTimeMillis() - sendMessagesStartTime)
         .put("aws_type", "sqs");

      Logging.logInfo(LOGGER, session, apacheMetadata, "AWS TIMING INFO");

   }

   public static void populateMessagesInToQueue(List<SendMessageBatchRequestEntry> entries, boolean isMiranha, boolean isWebDriver) {
      Map<String, String> mapUrlMessageId = new HashMap<>();
      String queueName;

      if (isMiranha) {
         if (executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_DEVELOPMENT)) {
            queueName = QueueName.WEB_SCRAPER_MIRANHA_CAPTURE_DEV.toString();
         } else {
            queueName = QueueName.WEB_SCRAPER_MIRANHA_DELAY_ATTEMPT.toString();
         }
      } else {
         if (executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_DEVELOPMENT)) {
            queueName = QueueName.WEB_SCRAPER_PRODUCT_DEV.toString();
         } else {
            queueName = isWebDriver ? QueueName.WEB_SCRAPER_PRODUCT_DELAY_ATTEMPT_WD.toString() : QueueName.WEB_SCRAPER_PRODUCT_DELAY_ATTEMPT.toString();
         }
      }

      SendMessageBatchResult messagesResult = QueueService.sendBatchMessages(Main.queueHandler.getSqs(), queueName, entries);

      // get send request results
      List<SendMessageBatchResultEntry> successResultEntryList = messagesResult.getSuccessful();

      if (!successResultEntryList.isEmpty()) {
         for (int i = 0; i < successResultEntryList.size(); i++) { // the successfully
            // sent messages

            // the _id field in the document will be the message id, which is the session id in the
            // crawler
            String messageId = successResultEntryList.get(i).getMessageId();
            mapUrlMessageId.put(entries.get(i).getMessageBody(), messageId);
         }

         Logging.printLogInfo(LOGGER, successResultEntryList.size() + " messages sended to " + queueName);
      }

   }


}
