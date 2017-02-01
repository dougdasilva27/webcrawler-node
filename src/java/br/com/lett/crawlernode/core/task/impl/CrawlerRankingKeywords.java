package br.com.lett.crawlernode.core.task.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.cookie.Cookie;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.AmazonSQS;
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
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Ranking;
import br.com.lett.crawlernode.core.models.RankingDiscoverUrls;
import br.com.lett.crawlernode.core.models.RankingProducts;
import br.com.lett.crawlernode.core.models.RankingStatistics;
import br.com.lett.crawlernode.core.session.InsightsCrawlerSession;
import br.com.lett.crawlernode.core.session.RankingKeywordsSession;
import br.com.lett.crawlernode.core.session.TestCrawlerSession;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.queue.QueueHandler;
import br.com.lett.crawlernode.queue.QueueName;
import br.com.lett.crawlernode.queue.QueueService;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONObjectIgnoreDuplicates;
import br.com.lett.crawlernode.util.Logging;

public abstract class CrawlerRankingKeywords extends Task{

	private static Logger logger = LoggerFactory.getLogger(CrawlerRankingKeywords.class);

	protected List<RankingProducts> arrayProducts = new ArrayList<>();
	protected Set<RankingDiscoverUrls> arrayRankingUrls = new HashSet<>();

	private Map<String, Map<String, MessageAttributeValue>> messages = new HashMap<>(); 

	protected int productsLimit;
	protected int pageLimit;
	
	private static final String RANK_TYPE = "keywords";

	public static final String SCHEDULER_NAME_DISCOVER_KEYWORDS = "discover_keywords";
	
	protected CrawlerWebdriver webdriver;

	private static QueueHandler	queueHandler;
	public static AmazonSQS queue;
	
	protected int pageSize 	 = 0;
	protected int position	 = 0;
	protected int totalBusca = 0;

	protected int marketId;
	private String marketName;
	private String marketCity;
	protected String proxies;

	protected Document currentDoc;
	protected int currentPage;
	protected String keywordEncoded;
	protected String location;

	private Integer doubleCheck;

	//variável que identifica se há resultados na página
	protected boolean result;

	/**
	 * Overrides the run method that will perform a task within a thread.
	 * The actual thread performs it's computation controlled by an Executor, from
	 * Java's Executors Framework.
	 */
	@Override 
	public void processTask() {
		extractProducts();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		// create a queue handler that will contain an Amazon SQS instance
		queueHandler = new QueueHandler();
		queue = queueHandler.getSqs();
	}
	
	//função para extrair produtos do market
	public int extractProducts() {

		// essa função simula o construtor dessa clase
		this.simulateConstructor(location, session.getMarket());

		this.log("Iniciando o crawler ranking ...");

		// Processe implementado pelas classes filhas para executar antes de rodar a keyword
		this.processBeforeFetch();

		//É chamada a função que extrai os produtos da pagina atual enquanto os produtos não atingirem a 100 e houver próxima página
		do {
			this.currentPage = this.currentPage + 1;	

			this.extractProductsFromCurrentPage();

			// mandando possíveis urls de produtos não descobertos pra amazon e pro mongo
			if(session instanceof RankingKeywordsSession){
				sendMessagesToAmazonAndMongo();
			}

			// caso cehgue no limite de páginas pré estabelecido, é finalizada a keyword.
			if(this.currentPage >= pageLimit) {
				this.log("Atingi o limite de páginas.");
				break;
			}
			
		} while (checkIfHasNextPage());

		if(this.arrayProducts.size() == productsLimit){
			log(productsLimit + " produtos atingidos!");
		} else if(this.result) {
			log("Fim das páginas!");
		}

		// função para popular os dados no banco
		populateData();

		// Número de produtos crawleados
		int numberOfProductsCrawled = this.arrayProducts.size();

		//aqui se limpa as variáveis
		this.clearVar();

		return numberOfProductsCrawled;
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
	
	/**
	 * Construtor simulado
	 * @param location
	 * @param market
	 * @param cacheUrls
	 */
	private void simulateConstructor(String location, Market market){
		//market
		this.marketId 	= market.getNumber();
		this.marketName = market.getName();
		this.marketCity = market.getCity();

		if(!"mexico".equals(this.marketCity)){
			this.location = CommonMethods.removeAcentos(location.replaceAll("/", " ").replaceAll("\\.", ""));
		} else {
			this.location = location;
		}

		try {
			this.keywordEncoded = URLEncoder.encode(location, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			this.logError(CommonMethods.getStackTraceString(e));
		}

		if(!(session instanceof RankingKeywordsSession)){
			productsLimit = 2000;
			pageLimit = 250;
		} else {
			productsLimit = 300;
			pageLimit = 35;
		}


		this.result = true;
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

	protected Document fetchDocument(String url) {
		return fetchDocument(url, null);
	}

	protected Document fetchDocument(String url, List<Cookie> cookies) {
		this.currentDoc = new Document(url);	
		
		if(cookies != null){
			this.log("Cookies sendo usados: ");
			for(Cookie cookie : cookies){
				this.log("Cookie: " + cookie.getName() + " Value: " + cookie.getValue());
			}
		}
				
		return DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
	}

	protected Map<String,String> fetchCookies(String url) {
		this.currentDoc = new Document(url);		

		//faz a conexão na url baixando o document html
		return DataFetcher.fetchCookies(session, url, null, 1);
	}

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
		rankingProducts.setPosition(position);

		List<Long> processedIds = new ArrayList<>();

		if( internalId  != null ){
			processedIds.addAll(Persistence.fetchProcessedIds(internalId.trim(), this.marketId));
			rankingProducts.setProcessedIds(processedIds);
		} else {
			if(pid != null){
				processedIds = Persistence.fetchProcessedIdsWithPid(pid, this.marketId);
				rankingProducts.setProcessedIds(processedIds);
			} else if(url != null){
				processedIds = Persistence.fetchProcessedIdsWithUrl(url, this.marketId);
				rankingProducts.setProcessedIds(processedIds);
			}
		}

		if(pid != null){
			rankingProducts.setInteranlPid(pid);
		}

		if(url != null){
			rankingProducts.setUrl(url);
			saveProductUrlToQueue(processedIds, url);
		}

		this.arrayProducts.add(rankingProducts);
	}

	/**
	 * Salva as urls que serão enviadas para a fila na amazon
	 * @param internalIds
	 * @param url
	 */
	protected void saveProductUrlToQueue(List<Long> internalIds, String url) {
		if(internalIds.isEmpty()) {
			Map<String, MessageAttributeValue> attr = new HashMap<>();

			String dataType = "String";

			attr.put(QueueService.MARKET_ID_MESSAGE_ATTR, new MessageAttributeValue().withDataType(dataType).withStringValue(String.valueOf(this.marketId)));
			
			this.messages.put(url.trim(), attr);
		} 

	}


	private void populateData(){
		//se houver 1 ou mais produtos, eles serão cadastrados no banco
		if(!this.arrayProducts.isEmpty() && session instanceof RankingKeywordsSession) {
			this.log("Vou persistir " + this.arrayProducts.size() + " posições de produtos...");

			Ranking ranking = populateRanking(location);
			
			//insere os dados no mongo
			//Main.monggodbManager.insertPanelRanking(ranking);
			
			//insere dados no postgres
			//Main.postgresManager.insertProductsRanking(ranking);

		} else {		
			this.log("Não vou persistir nada pois não achei nada");
		}
	}

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
				populateMessagesInMongoAndAmazon(entries, message.getKey());
				entries.clear();
			}
		}

		this.messages.clear();
	}	


	private void populateMessagesInMongoAndAmazon(List<SendMessageBatchRequestEntry> entries, String url) {

		// send the batch
		SendMessageBatchResult messagesResult = QueueService.sendBatchMessages(queue, QueueName.DISCOVER, entries);

		// get send request results
		List<SendMessageBatchResultEntry> successResultEntryList = messagesResult.getSuccessful();

		this.log("Estou enviando " + successResultEntryList.size() + " mensagens para a Task e o SQS.");

		if(!successResultEntryList.isEmpty()){			
			for (SendMessageBatchResultEntry resultEntry : successResultEntryList) { // the successfully sent messages

				// the _id field in the document will be the message id, which is the session id in the crawler
				String messageId = resultEntry.getMessageId();

				//Main.monggodbManager.insertPanelTask(messageId, SCHEDULER_NAME_DISCOVER_KEYWORDS, this.marketId, url, this.location);
			}

			this.log("Mensagens enviadas com sucesso.");
		}

	}


	//popula o ranking
	private Ranking populateRanking(String location) {
		Ranking ranking = new Ranking();

		List<RankingProducts> productsResult = new ArrayList<>();

		for(RankingProducts r : this.arrayProducts){			
			productsResult.add(r);
		}

		String nowISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss");
		Timestamp ts = Timestamp.valueOf(nowISO);
		
		ranking.setMarketId(this.marketId);
		ranking.setDate(ts);
		ranking.setLmt(nowISO);
		ranking.setRankType(RANK_TYPE);
		ranking.setLocation(location);
		ranking.setProducts(productsResult);
		ranking.setStatistics(populateRankingStatistics());

		return ranking;
	}

	//popula as estatísticas do ranking
	private RankingStatistics populateRankingStatistics(){
		RankingStatistics statistics = new RankingStatistics();

		statistics.setPageSize(this.pageSize);
		statistics.setTotalFetched(this.arrayProducts.size());
		statistics.setTotalSearch(this.totalBusca);

		return statistics;
	}

	private void clearVar() {
		this.arrayProducts.clear();
		this.currentPage = 0;
		this.position    = 0;
		this.totalBusca  = 0;
		this.pageSize = 0;
		this.arrayRankingUrls.clear();
		this.messages.clear();
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
