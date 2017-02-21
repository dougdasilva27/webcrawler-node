package br.com.lett.crawlernode.core.task.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageBatchResultEntry;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import br.com.lett.crawlernode.core.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.models.Ranking;
import br.com.lett.crawlernode.core.models.RankingDiscoverStats;
import br.com.lett.crawlernode.core.models.RankingProducts;
import br.com.lett.crawlernode.core.models.RankingProductsDiscover;
import br.com.lett.crawlernode.core.models.RankingStatistics;
import br.com.lett.crawlernode.core.session.DiscoverKeywordsSession;
import br.com.lett.crawlernode.core.session.RankingKeywordsSession;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.TestRankingKeywordsSession;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.queue.QueueName;
import br.com.lett.crawlernode.queue.QueueService;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONObjectIgnoreDuplicates;
import br.com.lett.crawlernode.util.Logging;

public abstract class CrawlerRankingKeywords extends Task {

	private static Logger logger = LoggerFactory.getLogger(CrawlerRankingKeywords.class);

	protected List<RankingProducts> arrayProducts = new ArrayList<>();
	
	private Map<String,String> mapUrlMessageId = new HashMap<>();

	private Map<String, Map<String, MessageAttributeValue>> messages = new HashMap<>(); 

	protected int productsLimit;
	protected int pageLimit;

	private static final String RANK_TYPE = "keywords";

	public static final String SCHEDULER_NAME_DISCOVER_KEYWORDS = "discover_keywords";

	protected CrawlerWebdriver webdriver;

	protected int pageSize 	 = 0;
	protected int position	 = 0;
	protected int totalBusca = 0;

	protected int marketId;

	protected Document currentDoc;
	protected int currentPage;
	protected String keywordEncoded;
	protected String location;

	private Integer doubleCheck;

	//variável que identifica se há resultados na página
	protected boolean result;

	public CrawlerRankingKeywords(Session session) {
		this.session = session;

		//market
		this.marketId = session.getMarket().getNumber();

		if(session instanceof RankingKeywordsSession) {
			this.location = ((RankingKeywordsSession)session).getKeyword();
		} else if(session instanceof TestRankingKeywordsSession) {
			this.location = ((TestRankingKeywordsSession)session).getKeyword();
		} else if(session instanceof DiscoverKeywordsSession) {
			this.location = ((DiscoverKeywordsSession)session).getKeyword();
		}

		if(!"mexico".equals(session.getMarket().getCity())) {
			this.location = CommonMethods.removeAccents(this.location.replaceAll("/", " ").replaceAll("\\.", ""));
		}

		try {
			this.keywordEncoded = URLEncoder.encode(location, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTrace(e));
			session.registerError(error);
		}

		if(session instanceof DiscoverKeywordsSession){
			productsLimit = 2000;
			pageLimit = 250;
		} else if(session instanceof RankingKeywordsSession || session instanceof TestRankingKeywordsSession) {
			productsLimit = 300;
			pageLimit = 35;
		}


		this.result = true;
	}

	/**
	 * Overrides the run method that will perform a task within a thread.
	 * The actual thread performs it's computation controlled by an Executor, from
	 * Java's Executors Framework.
	 */
	@Override 
	public void processTask() {
		int products = extractProducts();
		
		this.log("Foram " + products + " lidos");
	}

	@Override
	protected void onStart() {
		super.onStart();
		this.log("Iniciando o crawler ranking ...");
	}

	@Override
	protected void onFinish() {
		super.onFinish();
		List<SessionError> errors = session.getErrors();

		Logging.printLogDebug(logger, session, "Finalizing session of type [" + session.getClass().getSimpleName() + "]");

		// errors collected manually
		// they can be exceptions or business logic errors
		// and are all gathered inside the session
		if (!errors.isEmpty()) {
			Logging.printLogError(logger, session, "Task failed [" + session.getOriginalURL() + "]");
			
			for(SessionError error : errors) {
				Logging.printLogError(logger, error.getErrorContent());
			}

			Persistence.setTaskStatusOnMongo(Persistence.MONGO_TASK_STATUS_FAILED, session, Main.dbManager.connectionPanel);

			session.setTaskStatus(Task.STATUS_FAILED);
		}

		// only remove the task from queue if it was flawless
		// and if we are not testing, because when testing there is no message processing
		else if (session instanceof RankingKeywordsSession || session instanceof DiscoverKeywordsSession) {
			Logging.printLogDebug(logger, session, "Task completed.");

			Persistence.setTaskStatusOnMongo(Persistence.MONGO_TASK_STATUS_DONE, session, Main.dbManager.connectionPanel);

			session.setTaskStatus(Task.STATUS_COMPLETED);
		}
		
		Logging.printLogDebug(logger, session, "END");
	}


	//função para extrair produtos do market
	public int extractProducts() {
		try {
			// Processe implementado pelas classes filhas para executar antes de rodar a keyword
			this.processBeforeFetch();
	
			//É chamada a função que extrai os produtos da pagina atual enquanto os produtos não atingirem a 100 e houver próxima página
			do {
				this.currentPage = this.currentPage + 1;	
	
				extractProductsFromCurrentPage();
	
				// mandando possíveis urls de produtos não descobertos pra amazon e pro mongo
				if(			session instanceof RankingKeywordsSession 
						|| 	session instanceof DiscoverKeywordsSession
						&& 	Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION)) {
					
					sendMessagesToAmazonAndMongo();
				}
	
				// caso cehgue no limite de páginas pré estabelecido, é finalizada a keyword.
				if(this.currentPage >= pageLimit) {
					this.log("Atingi o limite de páginas.");
					break;
				}
	
				this.log("Fim de página");
				
			} while (checkIfHasNextPage());
	
			if(this.arrayProducts.size() == productsLimit){
				log(productsLimit + " produtos atingidos!");
			} else if(this.result) {
				log("Fim das páginas!");
			}
	
			// função para popular os dados no banco
			if(session instanceof RankingKeywordsSession) {
				persistRankingData();
			} else if(session instanceof DiscoverKeywordsSession) {
				persistDiscoverData();
			}

		} catch (Exception e) {
			SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTrace(e));
			session.registerError(error);
		}

		return this.arrayProducts.size();
	}

	/**
	 * Função checa de 4 forma se existe proxima pagina
	 * 
	 * 1 - Se o limite de produtos não foi atingido (this.arrayProducts.size() < productsLimit)
	 * 2 - Se naquele market foi identificado se há proxima pagina (hasNextPage())
	 * 3 - Se naquele market obteve resultado para aquela keyword (this.result)
	 * 4 - A variável doubleCheck armazena todos os produtos pegos até aquela página, caso na próxima página o número de produtos se manter,
	 * é identificado que não há próxima página devido algum erro naquele market.
	 * 
	 * @return
	 */
	private boolean checkIfHasNextPage() {
		if(this.arrayProducts.size() < productsLimit && hasNextPage() && this.result) {
			if(doubleCheck == null || this.arrayProducts.size() > doubleCheck) {
				doubleCheck = this.arrayProducts.size();
			} else {
				return false;
			}

			return true;
		}

		return false;
	}

	// função para setar cookies
	protected void processBeforeFetch(){}

	//função que extrai os produtos da página atual
	protected abstract void extractProductsFromCurrentPage();

	//função que retorna se há ou não uma próxima página
	protected abstract boolean hasNextPage();

	//função que seta o Total de busca de cada categoria
	protected void setTotalBusca(){
		if(this.arrayProducts.size() < productsLimit){
			this.totalBusca = this.arrayProducts.size();
		}

		this.log("Total da busca: "+ this.totalBusca);
	}

	/**
	 * Salva os dados do produto e chama a função
	 * que salva a url para mandar pra fila
	 * @param internalId
	 * @param pid
	 * @param url
	 */
	protected void saveDataProduct(String internalId, String pid, String url) {
		RankingProducts rankingProducts = new RankingProducts();

		this.position++;
		List<Long> processedIds = new ArrayList<>();
		
		rankingProducts.setInteranlPid(pid);
		rankingProducts.setUrl(url);
		rankingProducts.setPosition(position);

		if(!(session instanceof TestRankingKeywordsSession)) {
			if( internalId  != null ){
				processedIds.addAll(Persistence.fetchProcessedIdsWithInternalId(internalId.trim(), this.marketId));
			} else if(pid != null){
				processedIds = Persistence.fetchProcessedIdsWithInternalPid(pid, this.marketId);
			} else if(url != null){
				processedIds = Persistence.fetchProcessedIdsWithUrl(url, this.marketId);
			}
			
			rankingProducts.setProcessedIds(processedIds);
			
			if(url != null && processedIds.isEmpty()) {
				saveProductUrlToQueue(url);
			}
			
		}

		this.arrayProducts.add(rankingProducts);
	}

	/**
	 * Salva as urls que serão enviadas para a fila na amazon
	 * @param processedIds
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
	private void persistRankingData(){
		//se houver 1 ou mais produtos, eles serão cadastrados no banco
		if(!this.arrayProducts.isEmpty()) {
			this.log("Vou persistir " + this.arrayProducts.size() + " posições de produtos...");

			Ranking ranking = new Ranking();
			
			String nowISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss");
			Timestamp ts = Timestamp.valueOf(nowISO);

			ranking.setMarketId(this.marketId);
			ranking.setDate(ts);
			ranking.setLmt(nowISO);
			ranking.setRankType(RANK_TYPE);
			ranking.setLocation(location);
			ranking.setProducts(this.arrayProducts);
			
			RankingStatistics statistics = new RankingStatistics();

			statistics.setPageSize(this.pageSize);
			statistics.setTotalFetched(this.arrayProducts.size());
			statistics.setTotalSearch(this.totalBusca);
			
			ranking.setStatistics(statistics);
			
			//insere dados no postgres
			Persistence.insertProductsRanking(ranking);

		} else {		
			this.log("Não vou persistir nada pois não achei nada");
		}
	}
	
	/**
	 * Insert all data on table Ranking in Postgres
	 */
	private void persistDiscoverData(){
		List<RankingProductsDiscover> products = sanitizedRankingProducts(this.mapUrlMessageId);
		
		//se houver 1 ou mais produtos, eles serão cadastrados no banco
		if(!products.isEmpty()) {
			this.log("Vou persistir " + products + " produtos...");

			RankingDiscoverStats ranking = new RankingDiscoverStats();

			String nowISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss");
			Timestamp ts = Timestamp.valueOf(nowISO);

			ranking.setMarketId(this.marketId);
			ranking.setDate(ts);
			ranking.setLmt(nowISO);
			ranking.setRankType(RANK_TYPE);
			ranking.setLocation(location);
			ranking.setProductsDiscover(products);
			
			RankingStatistics statistics = new RankingStatistics();

			statistics.setPageSize(this.pageSize);
			statistics.setTotalFetched(this.arrayProducts.size());
			statistics.setTotalSearch(this.totalBusca);
			
			ranking.setStatistics(statistics);
			
			//insere dados no mongo
			Persistence.insertPanelRanking(ranking);

		} else {		
			this.log("Não vou persistir nada pois não achei nada");
		}
	}

	/**
	 * Create message and call function to send messages
	 */
	private void sendMessagesToAmazonAndMongo(){
		List<SendMessageBatchRequestEntry> entries = new ArrayList<>();

		int counter = 0; 

		this.log("Vou enviar " +  this.messages.size() + " mensagens para o sqs.");

		for(Entry<String, Map<String, MessageAttributeValue>> message : this.messages.entrySet()){
			SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
			entry.setId(String.valueOf(counter));	// the id must be unique in the batch
			entry.setMessageAttributes(message.getValue());
			entry.setMessageBody(message.getKey());
			
			entries.add(entry);
			counter++;

			if(entries.size() > 9 || this.messages.size() == counter){

				//Aqui se envia 10 mensagens para serem enviadas pra amazon e no mongo
				populateMessagesInMongoAndAmazon(entries);
				entries.clear();
			}
		}

		this.messages.clear();
	}	

	/**
	 * Send messages to amazon and populate them on collection task
	 * @param entries
	 * @param url
	 */
	private void populateMessagesInMongoAndAmazon(List<SendMessageBatchRequestEntry> entries) {
		SendMessageBatchResult messagesResult = QueueService.sendBatchMessages(Main.queueHandler.getSqs(), QueueName.DISCOVER, entries);

		// get send request results
		List<SendMessageBatchResultEntry> successResultEntryList = messagesResult.getSuccessful();

		this.log("Estou enviando " + successResultEntryList.size() + " mensagens para a Task e o SQS.");
		
		if(!successResultEntryList.isEmpty()){	
			int count = 0;
			for (SendMessageBatchResultEntry resultEntry : successResultEntryList) { // the successfully sent messages

				// the _id field in the document will be the message id, which is the session id in the crawler
				String messageId = resultEntry.getMessageId();
				this.mapUrlMessageId.put(entries.get(count).getMessageBody(), messageId);
				
				Persistence.insertPanelTask(messageId, SCHEDULER_NAME_DISCOVER_KEYWORDS, this.marketId, entries.get(count).getMessageBody(), this.location);
				count++;
			}

			this.log("Mensagens enviadas com sucesso.");
		}

	}

	/**
	 * 
	 * @param mapUrlMessageId
	 * @return
	 */
	private List<RankingProductsDiscover> sanitizedRankingProducts(Map<String,String> mapUrlMessageId) {
		List<RankingProductsDiscover> productsDiscover = new ArrayList<>();
		
		for(RankingProducts product : this.arrayProducts) {
			RankingProductsDiscover productDiscover = new RankingProductsDiscover();
			
			productDiscover.setPosition(product.getPosition());
			productDiscover.setUrl(product.getUrl());
			
			List<Long> processedIds = product.getProcessedIds();
			
			if(processedIds.isEmpty()) {
				productDiscover.setType(RankingProductsDiscover.TYPE_NEW);
				productDiscover.setTaskId(mapUrlMessageId.get(product.getUrl()));
			} else {
				productDiscover.setType(RankingProductsDiscover.TYPE_OLD);
				productDiscover.setProcessedIds(processedIds);
			}
			
			productsDiscover.add(productDiscover);
		}
		
		return productsDiscover;
	}
	
	/**
	 * Fetch Document
	 * @param url
	 * @return
	 */
	protected Document fetchDocument(String url) {
		return fetchDocument(url, null);
	}
	
	/**
	 * Fetch Document eith cookies
	 * @param url
	 * @param cookies
	 * @return
	 */
	protected Document fetchDocument(String url, List<Cookie> cookies) {
		this.currentDoc = new Document(url);	

		StringBuilder string = new StringBuilder();
		string.append("Cookies sendo usados: ");
		if(cookies != null){
			for(Cookie cookie : cookies){
				string.append("Cookie: " + cookie.getName() + " Value: " + cookie.getValue());
			}
		}

		this.log(string.toString());
		
		return DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
	}

	/**
	 * Fetch Map of Cookies
	 * @param url
	 * @return
	 */
	protected Map<String,String> fetchCookies(String url) {
		this.currentDoc = new Document(url);		

		//faz a conexão na url baixando o document html
		return DataFetcher.fetchCookies(session, url, null, 1);
	}

	/**
	 * Fetch jsonObject
	 * @param url
	 * @return
	 */
	protected JSONObject fetchJSONObject(String url) {
		this.currentDoc = new Document(url);	

		//faz a conexão na url baixando o document html
		String json = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, null);

		JSONObject jsonProducts;
		try{
			jsonProducts = new JSONObjectIgnoreDuplicates(json);
		} catch(Exception e){
			jsonProducts = new JSONObject();
			this.logError(CommonMethods.getStackTraceString(e));
		}

		return jsonProducts;
	}

	/**
	 * Fetch google Json
	 * @param url
	 * @return
	 */
	protected JsonObject fetchJsonObjectGoogle(String url) {
		this.currentDoc = new Document(url);

		//faz a conexão na url baixando o document html
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
	 * @param url
	 * @param payload
	 * @param headers
	 * @param cookies
	 * @return
	 */
	protected String fetchStringPOST(String url, String payload, Map<String,String> headers, List<Cookie> cookies){
		return DataFetcher.fetchPagePOSTWithHeaders(url, session, payload, cookies, 1, headers);
	}

	/**
	 * Inicia o webdriver
	 * @param url
	 */
	protected CrawlerWebdriver startWebDriver(String url){
		this.log("Iniciando webdriver");
		return DynamicDataFetcher.fetchPageWebdriver(url, session);
	}

	/**
	 * Conecta url com webdriver
	 * @param url
	 * @return
	 */
	protected Document fetchDocumentWithWebDriver(String url){
		// se o webdriver não estiver iniciado, inicio ele
		if(this.webdriver == null){
			Document doc = new Document(url);
			this.webdriver = startWebDriver(url);

			String html = this.webdriver.getCurrentPageSource();

			if(html != null){
				doc = Jsoup.parse(html);
			}

			return doc;
		}

		return DynamicDataFetcher.fetchPage(this.webdriver, url);
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
		Logging.printLogDebug(logger, session, 
				"Fetching url with "+ requestType +" request using method " + method + ": " + url);
	}

	public void logError(String message) {
		Logging.printLogError(logger, session, message);
	}	

}
