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

import br.com.lett.crawlernode.core.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Ranking;
import br.com.lett.crawlernode.core.models.RankingDiscoverStats;
import br.com.lett.crawlernode.core.models.RankingProducts;
import br.com.lett.crawlernode.core.models.RankingProductsDiscover;
import br.com.lett.crawlernode.core.models.RankingStatistics;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.ranking.RankingDiscoverSession;
import br.com.lett.crawlernode.core.session.ranking.RankingSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingSession;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.database.DBSlack;
import br.com.lett.crawlernode.database.DatabaseDataFetcher;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.queue.QueueName;
import br.com.lett.crawlernode.queue.QueueService;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONObjectIgnoreDuplicates;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;

public abstract class CrawlerRanking extends Task {

	private Logger logger;

	protected List<RankingProducts> arrayProducts = new ArrayList<>();
	
	private Map<String,String> mapUrlMessageId = new HashMap<>();

	private Map<String, Map<String, MessageAttributeValue>> messages = new HashMap<>(); 

	protected int productsLimit;
	protected int pageLimit;

	protected CrawlerWebdriver webdriver;

	protected int pageSize 	 = 0;
	protected int position	 = 0;
	protected int totalProducts = 0;

	protected int marketId;

	protected Document currentDoc;
	protected int currentPage;
	
	protected String location;
	private String rankType;
	private String schedulerNameDiscoverProducts;

	private Integer doubleCheck;

	//variável que identifica se há resultados na página
	protected boolean result;

	public CrawlerRanking(Session session, String rankType, String schedulerName, Logger logger) {
		this.session = session;
		
		this.logger = logger;
		this.schedulerNameDiscoverProducts = schedulerName;
		this.marketId = session.getMarket().getNumber();
		this.rankType = rankType;
		
		if(session instanceof RankingDiscoverSession){
			productsLimit = 2000;
			pageLimit = 250;
		} else if(session instanceof RankingSession || session instanceof TestRankingSession) {
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
		if(session instanceof RankingSession) {
			// Identify anomalies
			anomalyDetector(this.location, this.session.getMarket(), this.rankType);
		}
		
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
		else if (session instanceof RankingSession || session instanceof RankingDiscoverSession) {
			Logging.printLogDebug(logger, session, "Task completed.");

			Persistence.setTaskStatusOnMongo(Persistence.MONGO_TASK_STATUS_DONE, session, Main.dbManager.connectionPanel);

			session.setTaskStatus(Task.STATUS_COMPLETED);
		}
		
		Logging.printLogDebug(logger, session, "END");
	}


	//função para extrair produtos do market
	public void extractProducts() {
		try {
			
			Logging.printLogDebug(logger, "Initiate crawler ranking for this location: " + this.location);
			
			// Processe implementado pelas classes filhas para executar antes de rodar a categorie
			this.processBeforeFetch();
	
			//É chamada a função que extrai os produtos da pagina atual enquanto os produtos não atingirem a 100 e houver próxima página
			do {
				this.currentPage = this.currentPage + 1;	
	
				extractProductsFromCurrentPage();
	
				// mandando possíveis urls de produtos não descobertos pra amazon e pro mongo
				if(			session instanceof RankingSession 
						|| 	session instanceof RankingDiscoverSession
						&& 	Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION)) {
					
					sendMessagesToAmazonAndMongo();
				}
	
				// caso cehgue no limite de páginas pré estabelecido, é finalizada a categorie.
				if(this.currentPage >= pageLimit) {
					this.log("Page limit has been reached");
					break;
				}
	
				this.log("End of page.");
				
			} while (checkIfHasNextPage());
	
			if(this.arrayProducts.size() == productsLimit){
				log(productsLimit + " reached products!");
			} else if(this.result) {
				log("End of pages!");
			}
	
			// função para popular os dados no banco
			if(session instanceof RankingSession) {
				persistRankingData();
				persistDiscoverData();
			} else if(session instanceof RankingDiscoverSession) {
				persistDiscoverData();
			}

		} catch (Exception e) {
			SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTrace(e));
			session.registerError(error);
		}
	}
	/**
	 * Função checa de 4 formas se existe proxima pagina
	 * 
	 * 1 - Se o limite de produtos não foi atingido (this.arrayProducts.size() < productsLimit)
	 * 2 - Se naquele market foi identificado se há proxima pagina (hasNextPage())
	 * 3 - Se naquele market obteve resultado para aquela categorie (this.result)
	 * 4 - A variável doubleCheck armazena todos os produtos pegos até aquela página, caso na próxima página o número de produtos se manter,
	 * é identificado que não há próxima página devido algum erro naquele market.
	 * 
	 * @return
	 */
	protected boolean checkIfHasNextPage() {
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
	protected void setTotalProducts(){
		if(this.arrayProducts.size() < productsLimit){
			this.totalProducts = this.arrayProducts.size();
		}

		this.log("Total da busca: "+ this.totalProducts);
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

		if(!(session instanceof TestRankingSession)) {
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
	protected void persistRankingData(){
		//se houver 1 ou mais produtos, eles serão cadastrados no banco
		if(!this.arrayProducts.isEmpty()) {
			this.log("Vou persistir " + this.arrayProducts.size() + " posições de produtos...");

			Ranking ranking = new Ranking();
			
			String nowISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss");
			Timestamp ts = Timestamp.valueOf(nowISO);

			ranking.setMarketId(this.marketId);
			ranking.setDate(ts);
			ranking.setLmt(nowISO);
			ranking.setRankType(this.rankType);
			ranking.setLocation(location);
			ranking.setProducts(this.arrayProducts);
			
			RankingStatistics statistics = new RankingStatistics();

			statistics.setPageSize(this.pageSize);
			statistics.setTotalFetched(this.arrayProducts.size());
			statistics.setTotalSearch(this.totalProducts);
			
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
	protected void persistDiscoverData(){
		List<RankingProductsDiscover> products = sanitizedRankingProducts(this.mapUrlMessageId);
		
		//se houver 1 ou mais produtos, eles serão cadastrados no banco
		if(!products.isEmpty()) {
			this.log(products.size() + " products will be persisted");

			RankingDiscoverStats ranking = new RankingDiscoverStats();

			String nowISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss");
			Timestamp ts = Timestamp.valueOf(nowISO);

			ranking.setMarketId(this.marketId);
			ranking.setDate(ts);
			ranking.setLmt(nowISO);
			ranking.setLocation(location);
			ranking.setProductsDiscover(products);
			ranking.setRankType(rankType);
			
			RankingStatistics statistics = new RankingStatistics();

			statistics.setPageSize(this.pageSize);
			statistics.setTotalFetched(this.arrayProducts.size());
			statistics.setTotalSearch(this.totalProducts);
			
			ranking.setStatistics(statistics);
			
			//insere dados no mongo
			Persistence.persistDiscoverStats(ranking);

		} else {		
			this.log("No product was found.");
		}
	}

	/**
	 * Create message and call function to send messages
	 */
	protected void sendMessagesToAmazonAndMongo(){
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
	 *
	 * @param entries
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
				
				Persistence.insertPanelTask(messageId, this.schedulerNameDiscoverProducts, this.marketId, entries.get(count).getMessageBody(), this.location);
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

		if(cookies != null){
			StringBuilder string = new StringBuilder();
			string.append("Cookies been used: ");
			
			for(Cookie cookie : cookies){
				string.append("\nCookie: " + cookie.getName() + " Value: " + cookie.getValue());
			}
			
			this.log(string.toString());
		}
		
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
		return POSTFetcher.fetchPagePOSTWithHeaders(url, session, payload, cookies, 1, headers);
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

	
	/***************************************************************************************************************************
	* ANOMALIAS DE SHARE OF SEARCH
	* 
	* O crawler ranking roda todos os dias geralmente de 05:00 as 06:45 da manhã.
	* 
	* Em alguns casos em determinadas categories, o resultado pode vir diferente ou sequer nem vir caso o site mude
	* ou ocorra algum erro nos crawlers.
	* 
	* Por isso foi desenvolvido essa funcionalidade para detectar alguns tipos de anomalias como:
	* 
	* 1- Caso o número de produtos capturados hoje seja 20% maior ou menor que ontem
	* 2- Caso os produtos capturados ontem não estejam em pelo menos 50% do share de determinada categorie hoje.
	* 
	* Com essas duas regras conseguimos identificar se uma categorie em determinado market rodou ou mesmo se um site mudou.
	* 
	****************************************************************************************************************************/
	private void anomalyDetector(String location, Market market, String rankType) {
		Map<String,String> anomalies = new HashMap<>();
		
		Logging.printLogDebug(logger, session, "Searching for anomalies ...");

		String nowISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd");
		String yesterdayISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).minusDays(1).toString("yyyy-MM-dd");
		
		List<Long> yesterdayProcesseds = DatabaseDataFetcher.fetchProcessedsFromCrawlerRanking(location, market.getNumber(), nowISO, yesterdayISO);
		
		Logging.printLogDebug(logger, session, "Yesterday products: " + yesterdayProcesseds.size());
		
		int countToday = this.arrayProducts.size();
		int countYesterday = yesterdayProcesseds.size();
	
		if(countToday > 0 && countYesterday > 0) {
			analyzeCrawledProducts(yesterdayProcesseds, yesterdayISO, anomalies, rankType);
		}
		
		if(anomalies.size() > 0) {
			Logging.printLogDebug(logger, "Was identified " + anomalies.size() + " anomalies for this " + rankType + ".");
			
			for(Entry<String, String> entry : anomalies.entrySet()) {
				DBSlack.reportErrorRanking("webcrawler-node", entry.getKey(), entry.getValue(), location, market.getName(), "Categoria");
			}
		} else {
			Logging.printLogDebug(logger, session, "No anomaly was identified.");
		}
	}
	
	@SuppressWarnings("unchecked")
	private void analyzeCrawledProducts(List<Long> yesterdayProcesseds, String yesterdayISO, Map<String,String> anomalies, String rankType) {
		List<Long> todayProcesseds = new ArrayList<>();
		
		for(RankingProducts r : this.arrayProducts) {
			todayProcesseds.addAll(r.getProcessedIds());
		}
		
		List<Long> intersection = (List<Long>) CommonMethods.getIntersectionOfTwoArrays(yesterdayProcesseds, todayProcesseds);
		
		int countYesterday = yesterdayProcesseds.size();
		int countIntersection = intersection.size();
		
		StringBuilder str = new StringBuilder();
		
		str.append("*" + yesterdayISO + "*: " + Integer.toString(countYesterday) + "\n");
		str.append("*Interseção de hoje e ontem*: " + Integer.toString(countIntersection) + "\n");
		
		if(countYesterday > countIntersection) {
			Float percentage = MathCommonsMethods.normalizeTwoDecimalPlaces(((float)countIntersection / (float)countYesterday) * 100f);
			
			if(percentage <= 20) {
				String text = "O crawler ranking capturou apenas cerca de *" + percentage + "%* dos produtos capturados nessa " + rankType + " em relação a ontem."
						+ "\n\n *Session*: " + session.getSessionId();
				
				Logging.printLogDebug(logger, session, "Anomaly was identified: \n" + text.replaceAll("\\*", "") + "\n" + str.toString().replaceAll("\\*", ""));
				
				anomalies.put(text,  str.toString());
			}
		}
	}
	
}
