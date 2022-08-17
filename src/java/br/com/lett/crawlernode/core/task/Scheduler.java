package br.com.lett.crawlernode.core.task;

import br.com.lett.crawlernode.aws.sqs.QueueHandler;
import br.com.lett.crawlernode.aws.sqs.QueueService;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Product;
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

      if (scraper.isMiranha()) {
         jsonToSendToCrawler.put("proxies", scraper.getProxies());
      }

      return jsonToSendToCrawler;

   }


}
