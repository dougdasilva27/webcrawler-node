package br.com.lett.crawlernode.core.task.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.cookie.Cookie;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageBatchResultEntry;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import br.com.lett.crawlernode.aws.sqs.QueueName;
import br.com.lett.crawlernode.aws.sqs.QueueService;
import br.com.lett.crawlernode.core.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.Ranking;
import br.com.lett.crawlernode.core.models.RankingProducts;
import br.com.lett.crawlernode.core.models.RankingStatistics;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.ranking.RankingDiscoverSession;
import br.com.lett.crawlernode.core.session.ranking.RankingSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingSession;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JsonUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Processed;

public abstract class CrawlerRanking extends Task {

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

  // variável que identifica se há resultados na página
  protected boolean result;

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
  }

  /**
   * Overrides the run method that will perform a task within a thread. The actual thread performs
   * it's computation controlled by an Executor, from Java's Executors Framework.
   */
  @Override
  public void processTask() {
    extractProducts();

    this.log("Foram " + this.arrayProducts.size() + " lidos");
  }

  @Override
  protected void onStart() {
    super.onStart();
    this.log("Iniciando o crawler ranking ...");
  }

  @Override
  protected void onFinish() {
    super.onFinish();
    // if (session instanceof RankingSession) {
    // // Identify anomalies
    // anomalyDetector(this.location, this.session.getMarket(), this.rankType);
    // }

    // close the webdriver
    if (webdriver != null) {
      Logging.printLogDebug(logger, session, "Terminating PhantomJS instance...");
      webdriver.terminate();
    }

    List<SessionError> errors = session.getErrors();

    // errors collected manually
    // they can be exceptions or business logic errors
    // and are all gathered inside the session
    if (!errors.isEmpty()) {
      Logging.printLogError(logger, session, "Task failed [" + session.getOriginalURL() + "]");

      for (SessionError error : errors) {
        Logging.printLogError(logger, error.getErrorContent());
      }

      session.setTaskStatus(Task.STATUS_FAILED);
    }

    // only remove the task from queue if it was flawless
    // and if we are not testing, because when testing there is no message processing
    else if (session instanceof RankingSession || session instanceof RankingDiscoverSession) {
      Logging.printLogDebug(logger, session, "Task completed.");
      session.setTaskStatus(Task.STATUS_COMPLETED);
    }

    Logging.printLogDebug(logger, session, "END");
  }


  // função para extrair produtos do market
  public void extractProducts() {
    try {

      Logging.printLogDebug(logger, "Initiate crawler ranking for this location: " + this.location);

      // Processe implementado pelas classes filhas para executar antes de rodar a categorie
      this.processBeforeFetch();

      // É chamada a função que extrai os produtos da pagina atual enquanto os produtos não
      // atingirem a 100 e houver próxima página
      do {
        this.currentPage = this.currentPage + 1;

        extractProductsFromCurrentPage();

        // mandando possíveis urls de produtos não descobertos pra amazon e pro mongo
        if (session instanceof RankingSession || session instanceof RankingDiscoverSession
            && GlobalConfigurations.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION)) {

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

  /**
   * Função checa de 4 formas se existe proxima pagina
   * 
   * 1 - Se o limite de produtos não foi atingido (this.arrayProducts.size() < productsLimit) 2 - Se
   * naquele market foi identificado se há proxima pagina (hasNextPage()) 3 - Se naquele market obteve
   * resultado para aquela location (this.result) 4 - A variável doubleCheck armazena todos os
   * produtos pegos até aquela página, caso na próxima página o número de produtos se manter, é
   * identificado que não há próxima página devido algum erro naquele market.
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
  protected void processBeforeFetch() {}

  // função que extrai os produtos da página atual
  protected abstract void extractProductsFromCurrentPage();

  /**
   * função que retorna se há ou não uma próxima página default: total de produtos maior que os
   * produtos pegos até agora
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

      this.log("Total da busca: " + this.totalProducts);
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

    if (!(session instanceof TestRankingSession)) {
      List<Processed> processeds = new ArrayList<>();
      if (internalId != null) {
        processeds = Persistence.fetchProcessedIdsWithInternalId(internalId.trim(), this.marketId);
      } else if (pid != null) {
        processeds = Persistence.fetchProcessedIdsWithInternalPid(pid, this.marketId);
      } else if (url != null) {
        Logging.printLogWarn(logger, session, "Searching for processed with url and market.");
        processedIds = Persistence.fetchProcessedIdsWithUrl(url, this.marketId);
      }


      if (!processeds.isEmpty()) {
        for (Processed p : processeds) {
          processedIds.add(p.getId());

          if (p.isVoid() && url != null && !p.getUrl().equals(url)) {
            saveProductUrlToQueue(url);
            Logging.printLogWarn(logger, session, "Processed " + p.getId() + " with suspected of changing url: " + url);
          }
        }
      }

      if (url != null && processedIds.isEmpty()) {
        saveProductUrlToQueue(url);
      }

      rankingProducts.setProcessedIds(processedIds);
    }

    this.arrayProducts.add(rankingProducts);
  }


  /**
   *
   * @param url
   */
  protected void saveProductUrlToQueue(String url) {
    Map<String, MessageAttributeValue> attr = new HashMap<>();
    attr.put(QueueService.MARKET_ID_MESSAGE_ATTR, new MessageAttributeValue().withDataType("String").withStringValue(String.valueOf(this.marketId)));

    this.messages.put(url.trim(), attr);
  }


  /**
   * Insert all data on table Ranking in Postgres
   */
  protected void persistRankingData() {
    // se houver 1 ou mais produtos, eles serão cadastrados no banco
    if (!this.arrayProducts.isEmpty()) {
      this.log("Vou persistir " + this.arrayProducts.size() + " posições de produtos...");

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
      this.log("Não vou persistir nada pois não achei nada");
    }
  }

  // /**
  // * Insert all data on table Ranking in Postgres
  // */
  // protected void persistDiscoverData(){
  // List<RankingProductsDiscover> products = sanitizedRankingProducts(this.mapUrlMessageId);
  //
  // //se houver 1 ou mais produtos, eles serão cadastrados no banco
  // if(!products.isEmpty()) {
  // this.log(products.size() + " products will be persisted");
  //
  // RankingDiscoverStats ranking = new RankingDiscoverStats();
  //
  // String nowISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd
  // HH:mm:ss.mmm");
  // Timestamp ts = Timestamp.valueOf(nowISO);
  //
  // ranking.setMarketId(this.marketId);
  // ranking.setDate(ts);
  // ranking.setLmt(nowISO);
  // ranking.setLocation(location);
  // ranking.setProductsDiscover(products);
  // ranking.setRankType(rankType);
  //
  // RankingStatistics statistics = new RankingStatistics();
  //
  // statistics.setPageSize(this.pageSize);
  // statistics.setTotalFetched(this.arrayProducts.size());
  // statistics.setTotalSearch(this.totalProducts);
  //
  // ranking.setStatistics(statistics);
  //
  // //insere dados no mongo
  // //Persistence.persistDiscoverStats(ranking);
  //
  // } else {
  // this.log("No product was found.");
  // }
  // }

  /**
   * Create message and call function to send messages
   */
  protected void sendMessagesToAmazonAndMongo() {
    List<SendMessageBatchRequestEntry> entries = new ArrayList<>();

    int counter = 0;

    this.log("Vou enviar " + this.messages.size() + " mensagens para o sqs.");

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
      }
    }

    this.messages.clear();
  }

  /**
   *
   * @param entries
   */
  private void populateMessagesInMongoAndAmazon(List<SendMessageBatchRequestEntry> entries) {
    String queueName = session.getMarket().mustUseCrawlerWebdriver() ? QueueName.DISCOVER_WEBDRIVER : QueueName.DISCOVER;

    SendMessageBatchResult messagesResult = QueueService.sendBatchMessages(Main.queueHandler.getSqs(), queueName, entries);

    // get send request results
    List<SendMessageBatchResultEntry> successResultEntryList = messagesResult.getSuccessful();

    this.log("Estou enviando " + successResultEntryList.size() + " mensagens para a fila " + queueName + ".");

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

      this.log("Mensagens enviadas com sucesso.");
    }

  }

  // /**
  // *
  // * @param mapUrlMessageId
  // * @return
  // */
  // private List<RankingProductsDiscover> sanitizedRankingProducts(Map<String,String>
  // mapUrlMessageId) {
  // List<RankingProductsDiscover> productsDiscover = new ArrayList<>();
  //
  // for(RankingProducts product : this.arrayProducts) {
  // RankingProductsDiscover productDiscover = new RankingProductsDiscover();
  //
  // productDiscover.setPosition(product.getPosition());
  // productDiscover.setUrl(product.getUrl());
  //
  // List<Long> processedIds = product.getProcessedIds();
  //
  // if(processedIds.isEmpty()) {
  // productDiscover.setType(RankingProductsDiscover.TYPE_NEW);
  // productDiscover.setTaskId(mapUrlMessageId.get(product.getUrl()));
  // } else {
  // productDiscover.setType(RankingProductsDiscover.TYPE_OLD);
  // productDiscover.setProcessedIds(processedIds);
  // }
  //
  // productsDiscover.add(productDiscover);
  // }
  //
  // return productsDiscover;
  // }

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

    if (cookies != null && !cookies.isEmpty()) {
      StringBuilder string = new StringBuilder();
      string.append("Cookies been used: ");

      for (Cookie cookie : cookies) {
        string.append("\nCookie: " + cookie.getName() + " Value: " + cookie.getValue());
      }

      this.log(string.toString());
    }

    Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);

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
  protected Map<String, String> fetchCookies(String url) {
    return fetchCookies(url, cookies);
  }

  /**
   * Fetch Map of Cookies
   * 
   * @param url
   * @param cookies
   * @return
   */
  protected Map<String, String> fetchCookies(String url, List<Cookie> cookies) {
    this.currentDoc = new Document(url);

    // faz a conexão na url baixando o document html
    return DataFetcher.fetchCookies(session, url, cookies, 1);
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
    return DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);
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

    // faz a conexão na url baixando o document html
    String json = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies).trim();

    JSONObject jsonProducts = new JSONObject();

    if (json.startsWith("{") && json.endsWith("}")) {
      try {
        jsonProducts = new JsonUtils(json);
      } catch (Exception e) {
        this.logError(CommonMethods.getStackTraceString(e));
      }
    }

    return jsonProducts;
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

    // faz a conexão na url baixando o document html
    String json = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, null);

    JsonObject jobj;
    try {
      jobj = new Gson().fromJson(json, JsonObject.class);
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

    return POSTFetcher.fetchPagePOSTWithHeaders(url, session, payload, cookies, 1, headers);
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
    JSONObject res = POSTFetcher.fetcherRequest(url, cookies, headers, payload, DataFetcher.POST_REQUEST, session, false);

    if (res != null && res.has("response")) {
      return res.getJSONObject("response").get("body").toString();
    }

    return null;
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
    JSONObject res = POSTFetcher.fetcherRequest(url, cookies, headers, payload, DataFetcher.GET_REQUEST, session, false);

    if (res != null && res.has("response")) {
      return res.getJSONObject("response").get("body").toString();
    }

    return null;
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
  protected Map<String, String> fetchCookiesPOST(String url, String payload, Map<String, String> headers, List<Cookie> cookies) {
    return POSTFetcher.fetchCookiesPOSTWithHeaders(url, session, payload, cookies, 1, headers);
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

    this.log("Iniciando webdriver");
    return DynamicDataFetcher.fetchPageWebdriver(url, session);
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
  protected Document fetchDocumentWithWebDriver(String url, Integer timeout) {
    if (this.currentPage == 1) {
      this.session.setOriginalURL(url);
    }

    // se o webdriver não estiver iniciado, inicio ele
    if (this.webdriver == null) {
      Document doc = new Document(url);
      this.webdriver = startWebDriver(url);

      if (timeout != null) {
        this.webdriver.waitLoad(timeout);
      }

      String html = this.webdriver.getCurrentPageSource();
      session.addRedirection(url, webdriver.getCurURL());

      if (html != null) {
        doc = Jsoup.parse(html);
      }

      return doc;
    }

    return DynamicDataFetcher.fetchPage(this.webdriver, url, this.session);
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

  /**
   * 
   * @param requestType GET or POST
   * @param method Name of method
   * @param url Url of page
   */
  public void logFetch(String requestType, String method, String url) {
    Logging.printLogDebug(logger, session, "Fetching url with " + requestType + " request using method " + method + ": " + url);
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
