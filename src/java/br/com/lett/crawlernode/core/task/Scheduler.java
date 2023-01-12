package br.com.lett.crawlernode.core.task;

import br.com.lett.crawlernode.aws.sqs.QueueService;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.ranking.EqiRankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.ScraperInformation;
import cdjd.com.dremio.common.JSONOptions;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageBatchResultEntry;
import enums.QueueName;
import enums.ScrapersTypes;
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
      jsonToSendToCrawler.put("type", session.getScraperType());
      jsonToSendToCrawler.put("options", session.getOptions());
      jsonToSendToCrawler.put("market", marketInfo);
      jsonToSendToCrawler.put("className", session.getClassName());
      jsonToSendToCrawler.put("parameters", session.getOriginalURL());

      return jsonToSendToCrawler;

   }

   public static void sendMessagesToQueue(JSONObject jsonToSentToQueue, boolean isMiranha, Session session) {

      List<SendMessageBatchRequestEntry> entries = new ArrayList<>();

      int counter = 0;

      //  LOGGER.info(messages.size() + " possible new products to send to SQS.");

      long sendMessagesStartTime = System.currentTimeMillis();


      SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
      entry.setId(String.valueOf(counter)); // the id must be unique in the batch

      entry.setMessageBody(jsonToSentToQueue.toString());

      entries.add(entry);
      counter++;

      if (entries.size() > 9) {
         populateMessagesInToQueue(entries, isMiranha);
         entries.clear();

         JSONObject apacheMetadata = new JSONObject().put("aws_elapsed_time", System.currentTimeMillis() - sendMessagesStartTime)
            .put("aws_type", "sqs")
            .put("sqs_queue", "web-scraper-discoverer");

         Logging.logInfo(LOGGER, session, apacheMetadata, "AWS TIMING INFO");

      }

   }

   public static void populateMessagesInToQueue(List<SendMessageBatchRequestEntry> entries, boolean isMiranha) {
      Map<String, String> mapUrlMessageId = new HashMap<>();
      String queueName;

      if (isMiranha) {
         if (executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_DEVELOPMENT)) {
            queueName = QueueName.WEB_SCRAPER_MIRANHA_CAPTURE_DEV.toString();
         } else {
            queueName = "??";
         }
      } else {
         if (executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_DEVELOPMENT)) {
            queueName = QueueName.WEB_SCRAPER_PRODUCT_DEV.toString();
         } else {
            queueName = "??";
         }
      }

      SendMessageBatchResult messagesResult = QueueService.sendBatchMessages(Main.queueHandler.getSqs(), queueName, entries);

      // get send request results
      List<SendMessageBatchResultEntry> successResultEntryList = messagesResult.getSuccessful();

      if (!successResultEntryList.isEmpty()) {
         int count = 0;
         for (SendMessageBatchResultEntry resultEntry : successResultEntryList) { // the successfully
            // sent messages

            // the _id field in the document will be the message id, which is the session id in the
            // crawler
            String messageId = resultEntry.getMessageId();
            mapUrlMessageId.put(entries.get(count).getMessageBody(), messageId);
            count++;
         }

         LOGGER.info(successResultEntryList.size() + " messages sended to " + queueName);
      }

   }



}
