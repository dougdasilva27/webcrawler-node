package br.com.lett.crawlernode.core.task.impl;

import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.aws.sqs.QueueService;
import br.com.lett.crawlernode.core.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Ranking;
import br.com.lett.crawlernode.core.models.RankingProducts;
import br.com.lett.crawlernode.core.models.RankingStatistics;
import br.com.lett.crawlernode.core.models.RequestMethod;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.core.session.ranking.EqiRankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.RankingDiscoverSession;
import br.com.lett.crawlernode.core.session.ranking.RankingSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingSession;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.integration.redis.CrawlerCache;
import br.com.lett.crawlernode.integration.redis.RedisClient;
import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageBatchResultEntry;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import enums.QueueName;
import enums.ScrapersTypes;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import models.Processed;
import org.apache.http.cookie.Cookie;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;

public abstract class CrawlerRanking extends Task {

   protected FetchMode fetchMode;
   protected DataFetcher dataFetcher;

   private Logger logger;

   protected List<RankingProducts> arrayProducts = new ArrayList<>();

   private Map<String, String> mapUrlMessageId = new HashMap<>();

   private Map<String, Map<String, MessageAttributeValue>> messages = new HashMap<>();

   protected int productsLimit;
   protected int pageLimit;

   protected List<Cookie> cookies = new ArrayList<>();

   protected CrawlerWebdriver webdriver;

   protected int pageSize = 0;
   protected int position = 0;
   protected int totalProducts = 0;

   protected Document currentDoc;
   protected int currentPage;

   protected int marketId;
   protected String location;
   private String rankType;

   private Integer doubleCheck;

   private Map<Integer, String> screenshotsAddress = new HashMap<>();

   private static final CrawlerCache cacheClient = CrawlerCache.INSTANCE;

   // variável que identifica se há resultados na página
   protected boolean result;

   public List<RankingProducts> getArrayProducts() {
      return arrayProducts;
   }

   public CrawlerRanking(Session session, String rankType, Logger logger) {
      this.session = session;

      this.logger = logger;
      this.marketId = session.getMarket().getNumber();
      this.rankType = rankType;

      if (session instanceof RankingDiscoverSession) {
         productsLimit = 2000;
         pageLimit = 250;
      } else if (session instanceof RankingSession || session instanceof TestRankingSession) {
         productsLimit = 300;
         pageLimit = 35;
      }

      this.result = true;
      this.fetchMode = FetchMode.STATIC;
   }

   /**
    * Overrides the run method that will perform a task within a thread. The actual thread performs it's computation controlled by an Executor, from Java's Executors Framework.
    */
   @Override
   public void processTask() {
      extractProducts();

      this.log("Foram " + this.arrayProducts.size() + " lidos");
   }

   @Override
   protected void onStart() {
      super.onStart();
   }

   @Override
   protected void onFinish() {
      super.onFinish();

      if (!(session instanceof TestRankingKeywordsSession)) {
         S3Service.uploadCrawlerSessionContentToAmazon(session);
      }

      if (session instanceof TestCrawlerSession) {
         RedisClient.INSTANCE.shutdown();
      }

      // close the webdriver
      if (webdriver != null) {
         Logging.printLogDebug(logger, session, "Terminating Chromium instance...");
         webdriver.terminate();
      }

      List<SessionError> errors = session.getErrors();

      // errors collected manually
      // they can be exceptions or business logic errors
      // and are all gathered inside the session
      if (!errors.isEmpty()) {
         Logging.printLogWarn(logger, session, "Task failed [" + session.getOriginalURL() + "]");

         for (SessionError error : errors) {
            Logging.printLogError(logger, session, error.getErrorContent());
         }

         session.setTaskStatus(Task.STATUS_FAILED);
      }

      // and if we are not testing, because when testing there is no message processing
      else if (session instanceof RankingSession || session instanceof RankingDiscoverSession) {
         Logging.printLogDebug(logger, session, "Task completed.");
         session.setTaskStatus(Task.STATUS_COMPLETED);
      }

      Logging.logInfo(logger, session, new JSONObject().put("elapsed_time", System.currentTimeMillis() - session.getStartTime()), "END");
   }


   // função para extrair produtos do market
   public void extractProducts() {
      this.setDataFetcher();
      try {

         Logging.printLogInfo(logger, session, "Initiate crawler ranking for this location: " + this.location);

         // Processe implementado pelas classes filhas para executar antes de rodar a categorie
         this.processBeforeFetch();

         // É chamada a função que extrai os produtos da pagina atual enquanto os produtos não
         // atingirem a 100 e houver próxima página
         do {
            this.currentPage = this.currentPage + 1;

            extractProductsFromCurrentPage();

            // mandando possíveis urls de produtos não descobertos pra amazon e pro mongo
            if (session instanceof RankingSession || session instanceof RankingDiscoverSession || session instanceof EqiRankingDiscoverKeywordsSession
               && GlobalConfigurations.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION) && (session instanceof TestRankingKeywordsSession)) {

               sendMessagesToAmazonAndMongo();
            }

            // caso cehgue no limite de páginas pré estabelecido, é finalizada a categorie.
            if (this.currentPage >= pageLimit) {
               this.log("Page limit has been reached");
               break;
            }

            this.log("End of page.");

         } while (checkIfHasNextPage());

         // Total de produtos retornados pelo site
         if (this.totalProducts < 1 && !this.arrayProducts.isEmpty()) {
            setTotalProducts();
         }

         if (this.position == productsLimit) {
            log(productsLimit + " reached products!");
         } else if (!this.result) {
            log("End of pages!");
         }

         // função para popular os dados no banco
         if (session instanceof RankingSession) {
            persistRankingData();
            // persistDiscoverData();
         } else if (session instanceof RankingDiscoverSession) {
            // persistDiscoverData();
         }
      } catch (Exception e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
         SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTrace(e));
         session.registerError(error);
      }
   }

   private void setDataFetcher() {
      if (this.fetchMode == FetchMode.STATIC) {
         dataFetcher = GlobalConfigurations.executionParameters.getUseFetcher() ? new FetcherDataFetcher() : new ApacheDataFetcher();
      } else if (this.fetchMode == FetchMode.APACHE) {
         dataFetcher = new ApacheDataFetcher();
      } else if (this.fetchMode == FetchMode.JAVANET) {
         dataFetcher = new JavanetDataFetcher();
      } else if (this.fetchMode == FetchMode.FETCHER) {
         dataFetcher = new FetcherDataFetcher();
      }
   }

   protected <T> T cache(String key, int ttl, RequestMethod requestMethod, Request request, Function<Response, T> function) {
      String component = getClass().getSimpleName() + ":" + key;
      return cacheClient.getPutCache(component, ttl, requestMethod, request, function, dataFetcher, session);
   }

   protected <T> T cache(String key, RequestMethod requestMethod, Request request, Function<Response, T> function) {
      String component = getClass().getSimpleName() + ":" + key;
      return cacheClient.getPutCache(component, requestMethod, request, function, dataFetcher, session);
   }

   public void setProductsLimit(int productsLimit) {
      this.productsLimit = productsLimit;
   }

   /**
    * Função checa de 4 formas se existe proxima pagina
    * <p>
    * 1 - Se o limite de produtos não foi atingido (this.arrayProducts.size() < productsLimit) 2 - Se naquele market foi identificado se há proxima pagina (hasNextPage()) 3 - Se naquele market obteve
    * resultado para aquela location (this.result) 4 - A variável doubleCheck armazena todos os produtos pegos até aquela página, caso na próxima página o número de produtos se manter, é identificado
    * que não há próxima página devido algum erro naquele market.
    *
    * @return
    */
   protected boolean checkIfHasNextPage() {
      if (this.position < productsLimit && hasNextPage() && this.result) {
         if (doubleCheck == null || position > doubleCheck) {
            doubleCheck = this.position;
         } else {
            return false;
         }

         return true;
      }

      return false;
   }

   // função para setar cookies
   protected void processBeforeFetch() {
   }

   // função que extrai os produtos da página atual
   protected abstract void extractProductsFromCurrentPage() throws UnsupportedEncodingException;

   /**
    * função que retorna se há ou não uma próxima página default: total de produtos maior que os produtos pegos até agora
    *
    * @return
    */
   protected boolean hasNextPage() {
      return this.totalProducts > this.arrayProducts.size();
   }

   // função que seta o Total de busca de cada categoria
   protected void setTotalProducts() {
      if (this.totalProducts < 1 && this.arrayProducts.size() < productsLimit) {
         this.totalProducts = this.position;

         this.log("Total: " + this.totalProducts);
      }
   }

   /**
    * Salva os dados do produto e chama a função que salva a url para mandar pra fila
    *
    * @param internalId
    * @param pid
    * @param url
    */
   protected void saveDataProduct(String internalId, String pid, String url) {
      this.position++;
      saveDataProduct(internalId, pid, url, position);
   }

   /**
    * Salva os dados do produto e chama a função que salva a url para mandar pra fila
    *
    * @param internalId
    * @param pid
    * @param url
    */
   protected void saveDataProduct(String internalId, String pid, String url, int position) {
      RankingProducts rankingProducts = new RankingProducts();

      List<Long> processedIds = new ArrayList<>();

      rankingProducts.setInteranlPid(pid);
      rankingProducts.setUrl(url);
      rankingProducts.setPosition(position);

      if (!screenshotsAddress.isEmpty()) {
         switch (this.currentPage) {
            case 1:
               if (screenshotsAddress.containsKey(1)) {
                  rankingProducts.setScreenshot(screenshotsAddress.get(1));
               }
               break;

            case 2:
               if (screenshotsAddress.containsKey(2)) {
                  rankingProducts.setScreenshot(screenshotsAddress.get(2));
               }
               break;

            default:
               break;
         }
      }

      if (!(session instanceof TestRankingSession) && !(session instanceof EqiRankingDiscoverKeywordsSession)) {
         List<Processed> processeds = new ArrayList<>();
         if (internalId != null) {
            processeds = Persistence.fetchProcessedIdsWithInternalId(internalId.trim(), this.marketId, session);
         } else if (pid != null) {
            processeds = Persistence.fetchProcessedIdsWithInternalPid(pid, this.marketId, session);
         } else if (url != null) {
            Logging.printLogWarn(logger, session, "Searching for processed with url and market.");
            processedIds = Persistence.fetchProcessedIdsWithUrl(url, this.marketId, session);
         }

         if (!processeds.isEmpty()) {
            for (Processed p : processeds) {
               processedIds.add(p.getId());

               if (p.isVoid() && url != null && !p.getUrl().equals(url)) {
                  saveProductUrlToQueue(url);
                  Logging.printLogWarn(logger, session, "Processed " + p.getId() + " with suspected of url change: " + url);
               }
            }
         }

         if (url != null && processedIds.isEmpty()) {
            saveProductUrlToQueue(url);
         }

         rankingProducts.setProcessedIds(processedIds);
      }

      if (url != null && session instanceof EqiRankingDiscoverKeywordsSession) {
         saveProductUrlToQueue(url);
      }

      this.arrayProducts.add(rankingProducts);
   }


   /**
    * @param url
    */
   protected void saveProductUrlToQueue(String url) {
      Map<String, MessageAttributeValue> attr = new HashMap<>();
      attr.put(QueueService.MARKET_ID_MESSAGE_ATTR,
         new MessageAttributeValue().withDataType(QueueService.QUEUE_DATA_TYPE_STRING).withStringValue(String.valueOf(this.marketId)));

      String scraperType = session instanceof EqiRankingDiscoverKeywordsSession ? ScrapersTypes.EQI.toString() : ScrapersTypes.DISCOVERER.toString();

      attr.put(QueueService.SCRAPER_TYPE_MESSAGE_ATTR, new MessageAttributeValue().withDataType(QueueService.QUEUE_DATA_TYPE_STRING)
         .withStringValue(String.valueOf(scraperType)));

      this.messages.put(url.trim(), attr);
   }


   /**
    * Insert all data on table Ranking in Postgres
    */
   protected void persistRankingData() {
      // se houver 1 ou mais produtos, eles serão cadastrados no banco
      if (!this.arrayProducts.isEmpty()) {
         Ranking ranking = new Ranking();

         String nowISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss.mmm");
         Timestamp ts = Timestamp.valueOf(nowISO);

         ranking.setMarketId(this.marketId);
         ranking.setDate(ts);
         ranking.setLmt(nowISO);
         ranking.setRankType(this.rankType);
         ranking.setLocation(this.location);
         ranking.setProducts(this.arrayProducts);

         RankingStatistics statistics = new RankingStatistics();

         statistics.setPageSize(this.pageSize);
         statistics.setTotalFetched(this.arrayProducts.size());
         statistics.setTotalSearch(this.totalProducts);

         ranking.setStatistics(statistics);

         // insere dados no postgres
         Persistence.insertProductsRanking(ranking, session);

      } else {
         this.log("Nothing to persist, because there are no crawled products.");
      }
   }

   /**
    * Create message and call function to send messages
    */
   protected void sendMessagesToAmazonAndMongo() {
      List<SendMessageBatchRequestEntry> entries = new ArrayList<>();

      int counter = 0;

      this.log(this.messages.size() + " possible new products to send to SQS.");

      long sendMessagesStartTime = System.currentTimeMillis();

      for (Entry<String, Map<String, MessageAttributeValue>> message : this.messages.entrySet()) {
         SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
         entry.setId(String.valueOf(counter)); // the id must be unique in the batch
         entry.setMessageAttributes(message.getValue());
         entry.setMessageBody(message.getKey());

         entries.add(entry);
         counter++;

         if (entries.size() > 9 || this.messages.size() == counter) {

            // Aqui se envia 10 mensagens para serem enviadas pra amazon e no mongo
            populateMessagesInMongoAndAmazon(entries);
            entries.clear();

            JSONObject apacheMetadata = new JSONObject().put("aws_elapsed_time", System.currentTimeMillis() - sendMessagesStartTime)
               .put("aws_type", "sqs")
               .put("sqs_queue", "ws-discoverer");

            Logging.logInfo(logger, session, apacheMetadata, "AWS TIMING INFO");
         }
      }

      this.messages.clear();
   }

   /**
    * @param entries
    */
   private void populateMessagesInMongoAndAmazon(List<SendMessageBatchRequestEntry> entries) {
      String queueName;

      if (session instanceof EqiRankingDiscoverKeywordsSession) {
         queueName = session.getMarket().mustUseCrawlerWebdriver() ? QueueName.CORE_EQI_WEBDRIVER.toString() : QueueName.CORE_EQI.toString();
      } else {
         queueName = session.getMarket().mustUseCrawlerWebdriver() ? QueueName.DISCOVERER_WEBDRIVER.toString() : QueueName.DISCOVERER.toString();
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
            this.mapUrlMessageId.put(entries.get(count).getMessageBody(), messageId);
            count++;
         }

         this.log(successResultEntryList.size() + " messages sended.");
      }

   }

   /**
    * Fetch Document
    *
    * @param url
    * @return
    */
   protected Document fetchDocument(String url) {
      return fetchDocument(url, cookies);
   }

   /**
    * Fetch Document eith cookies
    *
    * @param url
    * @param cookies
    * @return
    */
   protected Document fetchDocument(String url, List<Cookie> cookies) {
      this.currentDoc = new Document(url);

      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }

      Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).build();
      Response response = dataFetcher.get(session, request);

      Document doc = Jsoup.parse(response.getBody());

      // Screenshot
      takeAScreenshot(url, cookies);

      return doc;
   }

   /**
    * Fetch Map of Cookies
    *
    * @param url
    * @return
    */
   protected List<Cookie> fetchCookies(String url) {
      return fetchCookies(url, cookies);
   }

   /**
    * Fetch Map of Cookies
    *
    * @param url
    * @param cookies
    * @return
    */
   protected List<Cookie> fetchCookies(String url, List<Cookie> cookies) {
      this.currentDoc = new Document(url);

      Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).build();
      Response response = dataFetcher.get(session, request);

      // faz a conexão na url baixando o document html
      return response.getCookies();
   }

   /**
    * Fetch jsonObject(deprecated) Use fetchJSONObject(String url, List<Cookie> cookies)
    *
    * @param url
    * @return
    */
   protected JSONObject fetchJSONObject(String url) {
      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }

      return fetchJSONObject(url, cookies);
   }

   /**
    * Fetch String with Get Request
    *
    * @param url
    * @param cookies
    * @return
    */
   protected String fetchGETString(String url, List<Cookie> cookies) {
      Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).build();
      Response response = dataFetcher.get(session, request);

      return response.getBody();
   }

   /**
    * Fetch jsonObject
    *
    * @param url
    * @return
    */
   protected JSONObject fetchJSONObject(String url, List<Cookie> cookies) {
      this.currentDoc = new Document(url);

      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }

      Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).build();
      Response response = dataFetcher.get(session, request);

      return CrawlerUtils.stringToJson(response.getBody());
   }

   /**
    * Fetch google Json
    *
    * @param url
    * @return
    */
   protected JsonObject fetchJsonObjectGoogle(String url) {
      this.currentDoc = new Document(url);

      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }

      Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).build();
      Response response = dataFetcher.get(session, request);

      JsonObject jobj;
      try {
         jobj = new Gson().fromJson(response.getBody(), JsonObject.class);
      } catch (JsonSyntaxException e) {
         jobj = new JsonObject();
         this.logError(CommonMethods.getStackTraceString(e));
      }

      return jobj;
   }

   /**
    * Fetch String with Post Request
    *
    * @param url
    * @param payload
    * @param headers
    * @param cookies
    * @return
    */
   protected String fetchStringPOST(String url, String payload, Map<String, String> headers, List<Cookie> cookies) {
      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }

      Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).setPayload(payload).setHeaders(headers).build();
      Response response = dataFetcher.post(session, request);

      return response.getBody();
   }

   /**
    * Fetch String with Post Request in FETCHER
    *
    * @param url
    * @param payload
    * @param headers
    * @param cookies
    * @return
    */
   protected String fetchPostFetcher(String url, String payload, Map<String, String> headers, List<Cookie> cookies) {
      Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).setPayload(payload).setHeaders(headers).build();
      Response response = new FetcherDataFetcher().post(session, request);

      return response.getBody();
   }

   /**
    * Fetch String with Post Request in FETCHER
    *
    * @param url
    * @param payload
    * @param headers
    * @param cookies
    * @return
    */
   protected String fetchGetFetcher(String url, String payload, Map<String, String> headers, List<Cookie> cookies) {
      Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).setPayload(payload).setHeaders(headers).build();
      Response response = new FetcherDataFetcher().get(session, request);

      return response.getBody();
   }

   /**
    * Fetch Cookies with Post Request
    *
    * @param url
    * @param payload
    * @param headers
    * @param cookies
    * @return
    */
   protected List<Cookie> fetchCookiesPOST(String url, String payload, Map<String, String> headers, List<Cookie> cookies) {
      Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).setPayload(payload).setHeaders(headers).build();
      Response response = dataFetcher.post(session, request);

      return response.getCookies();
   }

   /**
    * Inicia o webdriver
    *
    * @param url
    */
   protected CrawlerWebdriver startWebDriver(String url) {
      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }

      return DynamicDataFetcher.fetchPageWebdriver(url, ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY, false, session);
   }

   /**
    * Inicia o webdriver
    *
    * @param url
    * @param proxy
    */
   protected CrawlerWebdriver startWebDriver(String url, String proxy) {
      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }

      return DynamicDataFetcher.fetchPageWebdriver(url, proxy, session);
   }

   /**
    * Conecta url com webdriver
    *
    * @param url
    * @return
    */
   protected Document fetchDocumentWithWebDriver(String url) {
      return fetchDocumentWithWebDriver(url, null);
   }

   /**
    * Conecta url com webdriver
    *
    * @param url
    * @param timeout
    * @return
    */
   protected Document fetchDocumentWithWebDriver(String url, Integer timeout, String proxy) {
      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }

      // se o webdriver não estiver iniciado, inicio ele
      if (this.webdriver == null) {
         Document doc = new Document(url);
         this.webdriver = startWebDriver(url, proxy);

         if (this.webdriver != null) {
            if (timeout != null) {
               this.webdriver.waitLoad(timeout);
            }

            String html = this.webdriver.getCurrentPageSource();
            session.addRedirection(url, webdriver.getCurURL());

            if (html != null) {
               doc = Jsoup.parse(html);
            }
         }

         return doc;
      }

      return DynamicDataFetcher.fetchPage(this.webdriver, url, this.session);
   }

   protected Document fetchDocumentWithWebDriver(String url, Integer timeout) {
      return fetchDocumentWithWebDriver(url, timeout, ProxyCollection.BUY_HAPROXY);
   }

   protected void takeAScreenshot(String url) {
      takeAScreenshot(url, this.currentPage, null);
   }

   protected void takeAScreenshot(String url, List<Cookie> cookies) {
      takeAScreenshot(url, this.currentPage, cookies);
   }

   /**
    * Take a screenshot for audit only the first 2 pages
    *
    * @param url
    */
   protected void takeAScreenshot(String url, int page, List<Cookie> cookies) {
      // if (session instanceof RankingSession && page <= 2 && ((RankingSession)
      // session).mustTakeAScreenshot()) {
      // String printUrl = URLBox.takeAScreenShot(url, session, page, cookies);
      //
      // switch (this.currentPage) {
      // case 1:
      // this.screenshotsAddress.put(1, printUrl);
      // break;
      //
      // case 2:
      // this.screenshotsAddress.put(2, printUrl);
      // break;
      //
      // default:
      // break;
      // }
      // }
   }

   public void log(String message) {
      Logging.printLogDebug(logger, session, message);
   }

   public void logError(String message) {
      Logging.printLogError(logger, session, message);
   }

   public void logError(String message, Throwable e) {
      Logging.printLogError(logger, session, message);
      SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTrace(e));
      session.registerError(error);
   }
}
