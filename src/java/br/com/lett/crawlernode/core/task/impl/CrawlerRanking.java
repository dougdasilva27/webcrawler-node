package br.com.lett.crawlernode.core.task.impl;

import br.com.lett.crawlernode.aws.dynamodb.Dynamo;
import br.com.lett.crawlernode.aws.kinesis.KPLProducer;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.aws.sqs.QueueService;
import br.com.lett.crawlernode.core.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.*;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Ranking;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingStatistics;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.ranking.*;
import br.com.lett.crawlernode.core.task.Scheduler;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.ScraperInformation;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageBatchResultEntry;
import enums.QueueName;
import enums.ScrapersTypes;
import models.Processed;
import org.apache.commons.lang.time.DateUtils;
import org.apache.http.cookie.Cookie;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import static br.com.lett.crawlernode.main.GlobalConfigurations.executionParameters;

public abstract class CrawlerRanking extends Task {

   protected FetchMode fetchMode;

   protected DataFetcher dataFetcher;

   private final Logger logger;

   protected List<RankingProduct> arrayProducts = new ArrayList<>();

   private final Map<String, String> mapUrlMessageId = new HashMap<>();

   private final List<String> messages = new ArrayList<>();

   protected int productsLimit;

   protected int pageLimit;

   protected List<Cookie> cookies = new ArrayList<>();

   protected Set<org.openqa.selenium.Cookie> cookiesWD = new HashSet<>();


   protected CrawlerWebdriver webdriver;

   protected int pageSize = 0;
   protected int position = 0;
   protected int totalProducts = 0;

   protected Document currentDoc;
   protected int currentPage;

   protected int marketId;
   protected String location;
   protected Long locationId;
   private final String rankType;

   private Integer doubleCheck;


   // variável que identifica se há resultados na página
   protected boolean result;

   public List<RankingProduct> getArrayProducts() {
      return arrayProducts;
   }

   protected CrawlerRanking(Session session, String rankType, Logger logger) {
      this.session = session;

      this.logger = logger;
      this.marketId = session.getMarket().getNumber();
      this.rankType = rankType;

      if (session instanceof RankingDiscoverSession) {
         productsLimit = 1000;
         pageLimit = 125;
      } else if (session instanceof RankingSession || session instanceof TestRankingSession) {
         productsLimit = 60;
         pageLimit = 35;
      }

      this.result = true;
      this.fetchMode = FetchMode.STATIC;
   }

   /**
    * Overrides the run method that will perform a task within a thread. The actual thread performs
    * it's computation controlled by an Executor, from Java's Executors Framework.
    */
   @Override
   public void processTask() {
      extractProducts();

      Logging.printLogInfo(logger, "Extracted " + arrayProducts.size() + " products from " + session.getMarket().getName());
   }

   @Override
   protected void onFinish() {
      super.onFinish();

      if (!(session instanceof TestRankingKeywordsSession)) {
         S3Service.uploadCrawlerSessionContentToAmazon(session);
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
            if (session instanceof RankingSession || session instanceof RankingDiscoverSession) {

               sendMessagesToQueue();
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

         logInfo("Total products: " + this.totalProducts);

         // função para popular os dados no banco
         if (session instanceof RankingSession) {
            persistRankingData();
         }
      } catch (Exception e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
         SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTrace(e));
         session.registerError(error);
      }
   }

   private void setDataFetcher() {
      if (this.fetchMode == FetchMode.STATIC) {
         dataFetcher = Boolean.TRUE.equals(executionParameters.getUseFetcher()) ? new FetcherDataFetcher() : new ApacheDataFetcher();
      } else if (this.fetchMode == FetchMode.APACHE) {
         dataFetcher = new ApacheDataFetcher();
      } else if (this.fetchMode == FetchMode.JAVANET) {
         dataFetcher = new JavanetDataFetcher();
      } else if (this.fetchMode == FetchMode.FETCHER) {
         dataFetcher = new FetcherDataFetcher();
      } else if (this.fetchMode == FetchMode.JSOUP) {
         dataFetcher = new JsoupDataFetcher();
      }
   }

   public void setProductsLimit(int productsLimit) {
      this.productsLimit = productsLimit;
   }

   /**
    * Função checa de 4 formas se existe proxima pagina
    * <p>
    * 1 - Se o limite de produtos não foi atingido (this.arrayProducts.size() < productsLimit) 2 - Se
    * naquele market foi identificado se há proxima pagina (hasNextPage()) 3 - Se naquele market obteve
    * resultado para aquela location (this.result) 4 - A variável doubleCheck armazena todos os
    * produtos pegos até aquela página, caso na próxima página o número de produtos se manter, é
    * identificado que não há próxima página devido algum erro naquele market.
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
   protected abstract void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException;

   /**
    * função que retorna se há ou não uma próxima página default: total de produtos maior que os
    * produtos pegos até agora
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
    * @param internalId id unico do produto
    * @param pid        id do produto
    * @param url        url do produto
    * @deprecated Novos campos devem ser capturados pelo ranking. Utilizar a função {@link #saveDataProduct(RankingProduct)}
    */
   @Deprecated
   protected void saveDataProduct(String internalId, String pid, String url) {
      saveDataProduct(internalId, pid, url, position);
   }

   /**
    * Salva os dados do produto e chama a função que salva a url para mandar pra fila
    *
    * @param internalId id unico do produto
    * @param pid        id do produto
    * @param url        url do produto
    * @deprecated Novos campos devem ser capturados pelo ranking. Utilizar a função {@link #saveDataProduct(RankingProduct)}
    */
   @Deprecated
   protected void saveDataProduct(String internalId, String pid, String url, int position) {
      RankingProduct rankingProducts = new RankingProduct();

      rankingProducts.setInternalId(internalId);
      rankingProducts.setInteranlPid(pid);
      rankingProducts.setUrl(url);
      saveDataProduct(rankingProducts);
   }


   public static List<Processed> createProcesseds(JSONObject result, int market, Session session) {
      List<Processed> processeds = new ArrayList<>();

      if (result != null) {
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
      }

      return processeds;
   }

   public static boolean isVoid(JSONObject result) {
      boolean isVoid = false;

      if (result != null) {
         JSONArray foundSkus = result.optJSONArray("found_skus");

         if (foundSkus != null) {

            for (Object o : foundSkus) {
               if (o instanceof JSONObject) {
                  JSONObject product = (JSONObject) o;
                  if (product.optString("status").equalsIgnoreCase("void")) {
                     isVoid = true;
                     break;
                  }

               }

            }
         }
      }

      return isVoid;
   }


   /**
    * Salva os dados do produto e chama a função que salva a url para mandar pra fila
    *
    * @param product produto
    */
   protected void saveDataProduct(RankingProduct product) {
      this.position++;

      if (product.getPosition() == 0) {
         product.setPosition(this.position);
      }
      product.setPageNumber(this.currentPage);
      product.setMarketId(session.getMarket().getId());

      JSONObject metadataJson = new JSONObject();
      metadataJson.put("keyword", this.location);
      metadataJson.put("position", product.getPosition());
      metadataJson.put("url", product.getUrl());

      Logging.logDebug(logger, session, metadataJson, "Keyword= " + this.location + "," + product);


       if (!(session instanceof TestRankingSession) && !(session instanceof EqiRankingDiscoverKeywordsSession)) {
          JSONObject resultJson = Dynamo.fetchObjectDynamo(product.getUrl(), product.getMarketId());
         //this is legacy architecture, when none app using anymore we can remove
         List<Long> processedIds = new ArrayList<>();

         if (resultJson.has("finished_at")) {
            List<Processed> processeds = createProcesseds(resultJson, product.getMarketId(), session); //todo: remover processed assim que tudo migrar
            for (Processed p : processeds) {
               processedIds.add(p.getId());
            }
            if (isVoid(resultJson) && hasReadBeforeOneMonth(resultJson.optString("finished_at"))) {
               Logging.printLogDebug(logger, session, "Product already discovered but it was void in the last month - " + product.getUrl());
               saveProductUrlToQueue(product, resultJson);

            } else {
               Logging.printLogDebug(logger, session, "Product already discoverer " + product.getUrl());
            }

         } else if (product.getUrl() != null) {

            saveProductUrlToQueue(product, resultJson);
         }

         product.setProcessedIds(processedIds);
      }

      if (product.getUrl() != null && session instanceof EqiRankingDiscoverKeywordsSession) {
         JSONObject resultJson = Dynamo.fetchObjectDynamo(product.getUrl(), product.getMarketId());
         if (resultJson.isEmpty() || !resultJson.has("finished_at")){
            saveProductUrlToQueue(product, resultJson);
         }
      }

      this.arrayProducts.add(product);
   }

   public boolean hasReadBeforeOneMonth(String lrt) {
      if (lrt != null && !lrt.isEmpty()) {
         try {

            Date lrtDate = new SimpleDateFormat(
               "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(lrt);
            Date oneMonthAgo = DateUtils.addMonths(new Date(), -1);
            Logging.printLogDebug(logger, session, lrt + " - " + oneMonthAgo + " has more than one month: " + (lrtDate.before(oneMonthAgo)));
            return lrtDate.before(oneMonthAgo);

         } catch (Exception e) {
            Logging.printLogError(logger, session, "Error parsing lrt date: " + lrt);
         }
      }

      return false;
   }

   private void completeRankingProduct(RankingProduct product, List<Processed> processeds) {
      if (!processeds.isEmpty()) {
         Processed processed = processeds.get(0);
         product.setName(processed.getOriginalName());
         product.setPriceInCents(processed.getPrice() != null ? getPriceInCents(processed) : null);
         product.setIsAvailable(processed.getAvailable());
         product.setImageUrl(processed.getPic());
      }
   }

   private Integer getPriceInCents(Processed processed) {
      Float price = processed.getPrice();
      if (price != null) {
         return (int) (price * 100);
      }
      return null;
   }


   protected void saveProductUrlToQueue(RankingProduct product, JSONObject result) {
      if (mustSendProductToQueue(product, result)) {
         this.messages.add(product.getUrl());
      }

   }

   protected boolean mustSendProductToQueue(RankingProduct product, JSONObject result) {
      boolean sendToQueue = true;
      if (result == null || result.isEmpty()) {
         Dynamo.insertObjectDynamo(product);
         Logging.printLogDebug(logger, session, "Product new:  " + product.getUrl() + " insert in dynamo and saved to queue" );
      } else if (Dynamo.scheduledMoreThanOneHour(result.optString("scheduled_at"), session)) {
         Logging.printLogDebug(logger, session, "Update product " + product.getUrl() + " in dynamo and saved to queue");
         Dynamo.updateScheduledObjectDynamo(product, result.optString("created_at"));
      } else {
         sendToQueue = false;
         Logging.printLogDebug(logger, session, "Product already send to queue less than one hour ago url: " + product.getUrl());
      }

      return sendToQueue;
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
         ranking.setLocationId(this.locationId);

         RankingStatistics statistics = new RankingStatistics();

         statistics.setPageSize(this.pageSize);
         statistics.setTotalFetched(this.arrayProducts.size());
         statistics.setTotalSearch(this.totalProducts);

         ranking.setStatistics(statistics);

         // insere dados no postgres
         Persistence.insertProductsRanking(ranking, session); //todo: remover isso

         // persiste os dados no elasticsearch
         KPLProducer.getInstance().put(ranking, session);

      } else {
         this.log("Nothing to persist, because there are no crawled products.");
      }
   }

   /**
    * Create message and call function to send messages
    */
   protected void sendMessagesToQueue() {
      List<SendMessageBatchRequestEntry> entries = new ArrayList<>();

      int counter = 0;

      this.logInfo(this.messages.size() + " possible new products to send to SQS.");

      long sendMessagesStartTime = System.currentTimeMillis();

      ScraperInformation scraperInformation = Persistence.fetchScraperInfoToOneMarket(session.getMarket().getNumber());
      String scraperType = session instanceof EqiRankingDiscoverKeywordsSession ? ScrapersTypes.EQI.toString() : ScrapersTypes.DISCOVERER.toString();

      if (scraperInformation != null) {
         for (String menssage : this.messages) {

            SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
            entry.setId(String.valueOf(counter)); // the id must be unique in the batch

            JSONObject jsonToSentToQueue = Scheduler.mountMessageToSendToQueue(menssage, session.getMarket(), scraperInformation, scraperType);

            entry.setMessageBody(jsonToSentToQueue.toString());

            entries.add(entry);
            counter++;

            if (entries.size() > 9 || this.messages.size() == counter) {
               populateMessagesInToQueue(entries, scraperInformation.isUseBrowser());
               entries.clear();

               JSONObject apacheMetadata = new JSONObject().put("aws_elapsed_time", System.currentTimeMillis() - sendMessagesStartTime)
                  .put("aws_type", "sqs")
                  .put("sqs_queue", "web-scraper-discoverer");

               Logging.logInfo(logger, session, apacheMetadata, "AWS TIMING INFO");

            }
         }
      }
      this.messages.clear();
   }

   /**
    * Send messages to SQS
    *
    * @param entries    entries
    * @param isWebDrive is true if use web drive
    */
   private void populateMessagesInToQueue(List<SendMessageBatchRequestEntry> entries, boolean isWebDrive) {
      String queueName;


      if (executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_DEVELOPMENT)) {
         queueName = QueueName.WEB_SCRAPER_PRODUCT_DEV.toString();
      } else {
         if (session instanceof EqiRankingDiscoverKeywordsSession) {
            queueName = isWebDrive ? QueueName.WEB_SCRAPER_PRODUCT_EQI_WEBDRIVER.toString() : QueueName.WEB_SCRAPER_PRODUCT_EQI.toString();
         } else {
            queueName = isWebDrive ? QueueName.WEB_SCRAPER_DISCOVERER_WEBDRIVER.toString() : QueueName.WEB_SCRAPER_DISCOVERER.toString();
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
            this.mapUrlMessageId.put(entries.get(count).getMessageBody(), messageId);
            count++;
         }

         this.log(successResultEntryList.size() + " messages sended to " + queueName);
      }

   }

   /**
    * Fetch Document
    */
   protected Document fetchDocument(String url) {
      return fetchDocument(url, cookies);
   }

   /**
    * Fetch Document with cookies
    */
   protected Document fetchDocument(String url, List<Cookie> cookies) {
      this.currentDoc = new Document(url);

      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }

      Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).build();
      Response response = dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }

   /**
    * Fetch Map of Cookies
    */
   protected List<Cookie> fetchCookies(String url) {
      return fetchCookies(url, cookies);
   }

   /**
    * Fetch Map of Cookies
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
    */
   protected JSONObject fetchJSONObject(String url) {
      if (this.currentPage == 1) {
         this.session.setOriginalURL(url);
      }

      return fetchJSONObject(url, cookies);
   }

   /**
    * Fetch String with Get Request
    */
   protected String fetchGETString(String url, List<Cookie> cookies) {
      Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).build();
      Response response = dataFetcher.get(session, request);

      return response.getBody();
   }

   /**
    * Fetch jsonObject
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
    * Fetch String with Post Request
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
    */
   protected String fetchPostFetcher(String url, String payload, Map<String, String> headers, List<Cookie> cookies) {
      Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).setPayload(payload).setHeaders(headers).build();
      Response response = new FetcherDataFetcher().post(session, request);

      return response.getBody();
   }

   /**
    * Fetch String with Post Request in FETCHER
    */
   protected String fetchGetFetcher(String url, String payload, Map<String, String> headers, List<Cookie> cookies) {
      Request request = RequestBuilder.create().setCookies(cookies).setUrl(url).setPayload(payload).setHeaders(headers).build();
      Response response = new FetcherDataFetcher().get(session, request);

      return response.getBody();
   }

   /**
    * Inicia o webdriver
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
    */


   /**
    * Conecta url com webdriver
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

   protected Document fetchDocumentWithWebDriver(String url) {
      return fetchDocumentWithWebDriver(url, null, ProxyCollection.BUY_HAPROXY);
   }


   public void log(String message) {
      Logging.printLogDebug(logger, session, message);
   }

   public void logInfo(String message) {
      Logging.printLogInfo(logger, session, message);
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
