package br.com.lett.crawlernode.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.http.cookie.Cookie;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;

import br.com.lett.crawlernode.base.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.models.CrawlerSession;
import br.com.lett.crawlernode.models.ProcessedModel;
import br.com.lett.crawlernode.models.Product;
import br.com.lett.crawlernode.util.Logging;

public class Persistence {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
	
	public static void persistInformation(Product product, CrawlerSession session) {
		ProcessedModel truco = session.getTruco();		

		// validating truco mode
		if(truco != null) {
			if(session.getOriginalURL() == null) { // TODO conferir o que seria essa originalURL
				Logging.printLogError(logger, "Erro tentando começar modo truco sem enviar a originalUrl");
				return;
			}

			if(truco.getInternalId() == null) {
				Logging.printLogError(logger, "Error: truco with null internalId");
				return;
			}

			// Se estou no modo truco mas o produto sendo verificado não é o mesmo,
			// que acontece no caso de URLs com múltiplos produtos, então paro por aqui.
			if(!truco.getInternalId().equals(product.getInternalId())) {
				Logging.printLogDebug(logger, "Abortando este modo truco pois estou trucando outro produto.");
				return;
			}

		}

		boolean available = product.getAvailable();
		String seedId = product.getSeedId();
		String url = product.getUrl();
		String internal_id = product.getInternalId();
		String internal_pid = product.getInternalPid();
		String name = product.getName();
		Float price = product.getPrice();
		String cat1 = product.getCategory1();
		String cat2 = product.getCategory2();
		String cat3 = product.getCategory3();
		String foto = product.getPrimaryImage();
		String secondary_pics = product.getSecondaryImages();
		String description = product.getDescription();
		JSONArray marketplace = product.getMarketplace();
		Integer stock = product.getStock();

		// Sanitizing before persist
		url = sanitizeBeforePersist(url);
		name = sanitizeBeforePersist(name);
		cat1 = sanitizeBeforePersist(cat1);
		cat2 = sanitizeBeforePersist(cat2);
		cat3 = sanitizeBeforePersist(cat3);
		foto = sanitizeBeforePersist(foto);
		secondary_pics = sanitizeBeforePersist(secondary_pics);
		description = sanitizeBeforePersist(description);
		internal_id = sanitizeBeforePersist(internal_id);
		internal_pid = sanitizeBeforePersist(internal_pid);

		String marketplace_string = null;

		if(marketplace != null && marketplace.length() > 0) {
			marketplace_string = sanitizeBeforePersist(marketplace.toString());
		}


		// checking fields
		if((price == null || price.equals(0f)) && available) {
			Logging.printLogError(logger, "Erro tentando inserir leitura de produto disponível mas com campo vazio: price");
			return;
		} else if(internal_id == null || internal_id.isEmpty()) {
			Logging.printLogError(logger, "Erro tentando inserir leitura de produto com campo vazio: internal_id");
			return;
		} else if(session.getMarket().getNumber() == 0) {
			Logging.printLogError(logger, "Erro tentando inserir leitura de produto com campo vazio: [marketId] ... aborting ...");
			return;
		} else if(url == null || url.isEmpty()) {
			Logging.printLogError(logger, "Erro tentando inserir leitura de produto com campo vazio: [url] ... aborting ...");
			return;
		} else if(name == null || name.isEmpty()) {
			Logging.printLogError(logger, "Erro tentando inserir leitura de produto com campo vazio: [name] ... aborting ...");
			return;
		}

		if(price != null && price == 0.0) {
			price = null;
		}

		// persisting on crawler and crawler_old
		try {

			// Montando string de lista de campos da query
			String listOfFields = "";

			listOfFields += "available";
			listOfFields += ", market";
			listOfFields += ", internal_id";			
			listOfFields += ", internal_pid"; 
			listOfFields += ", url";
			listOfFields += ", price";
			listOfFields += ", stock";
			listOfFields += ", name";
			listOfFields += ", pic";
			listOfFields += ", secondary_pics";
			listOfFields += ", cat1";
			listOfFields += ", cat2";
			listOfFields += ", cat3";

			// Montando string de lista de valores da query
			String values = "";

			values += available;
			values += ", " + session.getMarket().getNumber();
			values += ", '" + internal_id + "'"; 

			if (internal_pid != null) values += ", '" + internal_pid + "'";
			else values += ", NULL";

			values += ", '" + url + "'";			
			values += ", " + price;
			values += ", " + stock;
			values += ", '" + name + "'";

			if (foto != null) values += ", '" + foto + "'";
			else values += ", NULL";

			if (secondary_pics != null) values += ", '" + secondary_pics + "'";
			else values += ", NULL";

			if (cat1 != null) values += ", '" + cat1 + "'";
			else values += ", NULL";

			if (cat2 != null) values += ", '" + cat2 + "'";
			else values += ", NULL";

			if (cat3 != null) values += ", '" + cat3 + "'";
			else values += ", NULL";

			if(description != null && !Jsoup.parse(description).text().replace("\n", "").replace(" ", "").trim().isEmpty()) {
				listOfFields = listOfFields + ", description";
				values = values + ", '" + description + "'";
			}

			if(marketplace_string != null) {
				listOfFields = listOfFields + ", marketplace";
				values = values + ", '" + marketplace_string + "'";
			}

			String sql_crawler = 
					"INSERT INTO crawler ( "
							+ 		listOfFields
							+ ") "
							+ "VALUES ("
							+ 		values
							+ "); "
							+ "INSERT INTO crawler_old ( "
							+ 		listOfFields
							+ ") "
							+ "VALUES ("
							+ 		values
							+ "); ";

			Main.dbManager.runSqlExecute(sql_crawler);

		} catch (SQLException e) {
			Logging.printLogError(logger, "Error inserting producton database! [" + e.getMessage() + "]");
		}
		
		/*
		 * If we are on production environment, we must process the product and update the processed table
		 */
		
		if( Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION) ) {
			try {
				String nowISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss.SSS");

				// reading current information of processed product
				ResultSet rs = Main.dbManager.runSqlConsult("SELECT * FROM processed WHERE market = " + session.getMarket().getNumber() + " AND internal_id = '" + internal_id + "'");

				ProcessedModel actualProcessedProduct = null;
				ProcessedModel newProcessedProduct = null;

				while(rs.next()) {

					JSONObject digitalContent;
					try {
						digitalContent = new JSONObject(rs.getString("digital_content"));
					} catch (Exception e) {	
						digitalContent = null; 
					}

					JSONObject changes;
					try { 	
						changes = new JSONObject(rs.getString("changes"));
					} catch (Exception e) {	
						changes = null; 
					}

					JSONArray similars;
					try { 	
						similars = new JSONArray(rs.getString("similars"));
					} catch (Exception e) {	
						similars = null; 
					}

					JSONArray behaviour;
					try { 	
						behaviour = new JSONArray(rs.getString("behaviour"));
					} catch (Exception e) {	
						behaviour = null; 
					}

					JSONArray actual_marketplace;
					try { 	actual_marketplace = new JSONArray(rs.getString("marketplace"));
					} catch (Exception e) {	
						actual_marketplace = null; 
					}

					Integer actual_stock;
					try { 	
						actual_stock = rs.getInt("stock"); if(actual_stock == 0) actual_stock = null;
					} catch (Exception e) {	
						actual_stock = null; 
					}

					Float actual_price;
					try { 	
						actual_price = rs.getFloat("price"); if(actual_price == 0) actual_price = null;
					} catch (Exception e) {	
						actual_price = null; 
					}

					actualProcessedProduct = new ProcessedModel(rs.getLong("id"), rs.getString("internal_id"), rs.getString("internal_pid"), rs.getString("original_name"), rs.getString("class"), rs.getString("brand"), rs.getString("recipient"),
							rs.getDouble("quantity"), rs.getInt("multiplier"), rs.getString("unit"), rs.getString("extra"), rs.getString("pic"), rs.getString("secondary_pics"), rs.getString("cat1"), rs.getString("cat2"),
							rs.getString("cat3"), rs.getString("url"), rs.getInt("market"), rs.getString("ect"), rs.getString("lmt"), rs.getString("lat"), rs.getString("lrt"), rs.getString("lms"), rs.getString("status"), changes,
							rs.getString("original_description"), actual_price, 
							digitalContent, rs.getLong("lett_id"), similars, rs.getBoolean("available"), rs.getBoolean("void"), actual_stock, behaviour, actual_marketplace);


					// Criando log de produto encontrado para a seed no caso do modo standalone
					foundProductForSeed(session);

					newProcessedProduct = actualProcessedProduct.clone();

					// Atualizando campos com novos valores
					if(url != null) {
						newProcessedProduct.setUrl(url);
					}
					if(stock != null) {
						newProcessedProduct.setStock(stock);
					}
					if(marketplace != null && marketplace.length() > 0) {
						newProcessedProduct.setMarketplace(marketplace);
					} else {
						newProcessedProduct.setMarketplace(null);
					}

					newProcessedProduct.setPic(foto);
					newProcessedProduct.setPrice(price);
					newProcessedProduct.setSecondary_pics(secondary_pics);
					newProcessedProduct.setOriginalName(name);
					newProcessedProduct.setOriginalDescription(description);
				}

				if(newProcessedProduct == null) { // Não achou processed, terá que criar
					newProcessedProduct = new ProcessedModel(null, 
							internal_id, 
							internal_pid, 
							name, 
							null, 
							null, 
							null, 
							null, 
							null, 
							null, 
							null, 
							foto, 
							secondary_pics, 
							cat1, 
							cat2, 
							cat3, 
							url, 
							session.getMarket().getNumber(),
							nowISO,
							nowISO,
							null,
							nowISO,
							nowISO,
							null,
							null,
							description, 
							price, 
							null, 
							null, 
							null, 
							false, 
							false, 
							stock, 
							null, 
							null);
				}

				Main.processorResultManager.processProduct(newProcessedProduct);

				// Atualizando disponibilidade
				newProcessedProduct.setAvailable(available);

				// Atualizando LRT
				newProcessedProduct.setLrt(nowISO);

				// Atualizando VOID
				newProcessedProduct.setVoid_product(false);

				// Atualizando LAT
				if(available) newProcessedProduct.setLat(nowISO);

				// Calculando Status
				String newStatus = "available";
				if(!newProcessedProduct.getAvailable()) {
					if(newProcessedProduct.getMarketplace() != null && newProcessedProduct.getMarketplace().length() > 0) {
						newStatus = "only_marketplace";
					} else {
						newStatus = "unavailable";
					}
				}

				// Atualizando status
				newProcessedProduct.setStatus(newStatus);

				// Pegando status anterior para verificar mudança
				String oldStatus = "void";
				if(actualProcessedProduct != null) oldStatus = actualProcessedProduct.getStatus();

				// Atualizando LMS se status mudou
				if(oldStatus == null || !newStatus.equals(oldStatus)) newProcessedProduct.setLms(nowISO);

				// Detectando e registrando mudanças
				// Recebe o banco Panel do Mongo porque grava urls que deverão ter um screenshot capturado
				newProcessedProduct.registerChanges(actualProcessedProduct, Main.dbManager.mongoBackendPanel);

				// Atualizando LMT
				if(newProcessedProduct.getChanges() != null && (newProcessedProduct.getChanges().has("pic") || newProcessedProduct.getChanges().has("originals"))) {
					newProcessedProduct.setLmt(nowISO);
				}

				// Retirando price = 0
				if(newProcessedProduct.getPrice() != null && newProcessedProduct.getPrice() == 0.0) {
					newProcessedProduct.setPrice(null);
				}

				// Atualiza comportamento intra-day (behaviour)
				JSONArray oldBehaviour = newProcessedProduct.getBehaviour();

				// Instanciamos o Map como TreeMap para que seja ordenado pelas keys automaticamente
				Map<String, JSONObject> newBehaviourMap = new TreeMap<String, JSONObject>();

				if(oldBehaviour == null) oldBehaviour = new JSONArray();

				DateTimeFormatter f = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

				JSONObject lastBehaviorBeforeToday = new JSONObject();

				// Populando newBehaviour
				for(int i=0; i < oldBehaviour.length(); i++) {

					// Adicionando no mapa (para depois ser filtrado)
					newBehaviourMap.put(oldBehaviour.getJSONObject(i).getString("date"), oldBehaviour.getJSONObject(i));

					// Adicionando primeiro behavior do dia (leitura na hora 00:00:01.000)
					DateTime dateTimeFor = null;
					DateTime dateTimeNow = null;
					DateTime dateTimeLast = null;

					try {
						dateTimeFor = f.parseDateTime(oldBehaviour.getJSONObject(i).getString("date"));
						dateTimeNow = new DateTime().withTimeAtStartOfDay();
						if(lastBehaviorBeforeToday.has("date")) dateTimeLast = f.parseDateTime(lastBehaviorBeforeToday.getString("date"));

						// Se a data do behavior que estou analisando é anterior à hoje
						if(dateTimeFor.isBefore(dateTimeNow)) {

							// Se o candidato atual a primeiro behavior do dia não existe ou é antes do behavior 
							// que estou analisando, então atualizo o candidato à primeiro behavior do dia

							if(dateTimeLast == null || dateTimeLast.isBefore(dateTimeFor)) {
								lastBehaviorBeforeToday = oldBehaviour.getJSONObject(i);
							}
						}

					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}


				// Criando behaviour do início de hoje (supostamente)
				String startOfDayISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).withTimeAtStartOfDay().plusSeconds(1).toString("yyyy-MM-dd HH:mm:ss.SSS");

				if(lastBehaviorBeforeToday != null && (!newBehaviourMap.containsKey(startOfDayISO) || !newBehaviourMap.get(startOfDayISO).has("status"))) {

					JSONObject behaviourStart = lastBehaviorBeforeToday;
					behaviourStart.put("date", startOfDayISO);
					if(!behaviourStart.has("status")) behaviourStart.put("status", "void");

					if(behaviourStart.has("price") && behaviourStart.getDouble("price") == 0.0) {
						behaviourStart.remove("price");
					}

					newBehaviourMap.put(startOfDayISO, behaviourStart);

				}

				// Criando behaviour de agora
				JSONObject behaviour = new JSONObject();
				behaviour.put("date", nowISO);
				behaviour.put("stock", stock);
				behaviour.put("available", available);
				behaviour.put("status", newProcessedProduct.getStatus());
				if(price != null) behaviour.put("price", price);
				if(marketplace != null && marketplace.length() > 0) behaviour.put("marketplace", marketplace);
				newBehaviourMap.put(nowISO, behaviour);

				JSONArray newBehaviour = new JSONArray();

				// Criando novo arrray behaviour apenas com as datas de hoje e
				// mantendo apenas os que tem os campos obrigatórios
				for(Entry<String, JSONObject> e: newBehaviourMap.entrySet()) {
					String dateString = e.getKey();
					DateTime dateTime = null;

					if(!dateString.contains(".")) dateString = dateString + ".000";

					try {
						dateTime = f.parseDateTime(dateString);

						if(
								dateTime.isAfter(new DateTime().withTimeAtStartOfDay())
								&&
								e.getValue().has("status")) {

							newBehaviour.put(e.getValue());

						}

					} catch (Exception e1) {
						System.err.println(dateString);
						e1.printStackTrace();
					}

				}

				newProcessedProduct.setBehaviour(newBehaviour);

				Logging.printLogDebug(logger, "Produto processado:"
						+ "\n" + newProcessedProduct.toString());


				boolean mustCheckAgain = false;

				if(actualProcessedProduct != null || truco != null) {
					if(truco != null) {
						mustCheckAgain = newProcessedProduct.compareHugeChanges(truco);
					} else {
						mustCheckAgain = newProcessedProduct.compareHugeChanges(actualProcessedProduct);
					}
				} else {
					mustCheckAgain = false;
				}


				if(mustCheckAgain) {

					checkAgain(session);


				} else {
					try {
						
						persistProcessedProduct(newProcessedProduct);
						
						// TODO increment a counter of stored processed products

					} catch (Exception e) {
						Logging.printLogError(logger, "Erro ao tentar atualizar tabela processed [seed:" + session.getSeedId() + "]");
						Logging.printLogError(logger, e.getMessage());
					}

				}


			} catch (Exception e) {
				Logging.printLogError(logger, "Erro ao tentar processar produto e atualizar tabela processed [seed:" + session.getSeedId() + "]");
				Logging.printLogError(logger, e.getMessage());
			}

		}

	}


	private static void checkAgain(CrawlerSession session) {

//		Logging.printLogInfo(marketCrawlerLogger, city, market, "Iniciando checkAgain na URL: " + originalUrl);
//
//		this.crawlerController.incrementTotalOfTrucosCounter();
//
//		Logging.printLogInfo(marketCrawlerLogger, city, market, "Iniciando checkAgain:");
//		Logging.printLogInfo(marketCrawlerLogger, city, market, "truco: " + truco);
//		Logging.printLogInfo(marketCrawlerLogger, city, market, "originalUrl: " + originalUrl);
//
//		MyPageFetcher myPageFetcher = (MyPageFetcher) this.crawlerController.pageFetcher;
//		List<Cookie> cookies = myPageFetcher.getCookies();
//
//		// Baixa de novo
//		Document document = null;
//		if (cookies.size() == 0) {
//			document = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session.getOriginalURL(), null, this, null);
//		} else {
//			document = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session.getOriginalURL(), null, this, cookies);
//		}
//
//		// Cria objetos do crawler4j
//		WebURL webURL = new WebURL();
//		webURL.setURL(originalUrl);
//		Page page = new Page(webURL);
//		HtmlParseData htmlParseData =  new HtmlParseData();
//		htmlParseData.setHtml(document.html());
//		page.setParseData(htmlParseData);
//
//		// Chama o prepareToExtract
//		this.prepareToExtract(page, truco);

	}
	
	private static void persistProcessedProduct(ProcessedModel newProcessedProduct) throws SQLException {

		String query = "";

		if(newProcessedProduct.getId() == null) {
			query = "INSERT INTO processed("
					+ "internal_id, internal_pid, original_name, class, brand, recipient, quantity,"
					+ "unit, extra, pic, url, market, ect, lmt, lat, lrt, lms, status, available, void, cat1, cat2, cat3, "
					+ "multiplier, original_description, price, stock, secondary_pics, changes, digital_content, marketplace, behaviour, similars)"
					+ "VALUES ("
					+ "'" + newProcessedProduct.getInternalId() + "', "
					+ "'" + newProcessedProduct.getInternalPid() + "', "
					+ "'" + newProcessedProduct.getOriginalName() + "', "
					+ (newProcessedProduct.get_class() == null ? "null" : "'" + newProcessedProduct.get_class()  + "'" ) + ", "
					+ (newProcessedProduct.getBrand() == null ? "null" : "'" + newProcessedProduct.getBrand()  + "'" ) + ", "
					+ (newProcessedProduct.getRecipient() == null ? "null" : "'" + newProcessedProduct.getRecipient()  + "'" ) + ", "
					+ (newProcessedProduct.getQuantity() == null ? "null" : newProcessedProduct.getQuantity() ) + ", "
					+ (newProcessedProduct.getUnit() == null ? "null" : "'" + newProcessedProduct.getUnit()  + "'" ) + ", "
					+ (newProcessedProduct.getExtra() == null ? "null" : "'" + newProcessedProduct.getExtra()  + "'" ) + ", "
					+ (newProcessedProduct.getPic() == null ? "null" : "'" + newProcessedProduct.getPic()  + "'" ) + ", "
					+ "'" + newProcessedProduct.getUrl() + "', "
					+ newProcessedProduct.getMarket() + ", "
					+ "'" + newProcessedProduct.getEct() + "', "
					+ "'" + newProcessedProduct.getLmt() + "', "
					+ (newProcessedProduct.getLat() == null ? "null" : "'" + newProcessedProduct.getLat()  + "'" ) + ", "
					+ "'" + newProcessedProduct.getLrt() + "', "
					+ "'" + newProcessedProduct.getLms() + "', "
					+ (newProcessedProduct.getStatus() == null ? "null" : "'" + newProcessedProduct.getStatus()  + "'" ) + ", "
					+ newProcessedProduct.getAvailable() + ", "
					+ newProcessedProduct.getVoid_product() + ", "
					+ (newProcessedProduct.getCat1() == null ? "null" : "'" + newProcessedProduct.getCat1()  + "'" ) + ", "
					+ (newProcessedProduct.getCat2() == null ? "null" : "'" + newProcessedProduct.getCat2()  + "'" ) + ", "
					+ (newProcessedProduct.getCat3() == null ? "null" : "'" + newProcessedProduct.getCat3()  + "'" ) + ", "
					+ (newProcessedProduct.getMultiplier() == null ? "null" : newProcessedProduct.getMultiplier() ) + ", "
					+ (newProcessedProduct.getOriginalDescription() == null ? "null" : "'" + newProcessedProduct.getOriginalDescription()  + "'" ) + ", "
					+ newProcessedProduct.getPrice() + ", "
					+ newProcessedProduct.getStock() + ", "
					+ (newProcessedProduct.getSecondary_pics() == null ? "null" : "'" + newProcessedProduct.getSecondary_pics()  + "'" ) + ", "
					+ (newProcessedProduct.getChanges() == null ? "null" : "'" + newProcessedProduct.getChanges().toString().replace("'","''")  + "'" ) + ", "
					+ ((newProcessedProduct.getDigitalContent() == null || newProcessedProduct.getDigitalContent().length() == 0) ? "null" : "'" + newProcessedProduct.getDigitalContent().toString().replace("'","''")  + "'" ) + ", "
					+ ((newProcessedProduct.getMarketplace() == null || newProcessedProduct.getMarketplace().length() == 0) ? "null" : "'" + newProcessedProduct.getMarketplace().toString().replace("'","''")  + "'" ) + ", "
					+ ((newProcessedProduct.getBehaviour() == null || newProcessedProduct.getBehaviour().length() == 0) ? "null" : "'" + newProcessedProduct.getBehaviour().toString().replace("'","''")  + "'" ) + ", "
					+ ((newProcessedProduct.getSimilars() == null || newProcessedProduct.getSimilars().length() == 0) ? "null" : "'" + newProcessedProduct.getSimilars().toString().replace("'","''")  + "'" ) + " "
					+ ")";
		} else {

			query = "UPDATE processed SET "
					+ "internal_id=" 		+ "'" + newProcessedProduct.getInternalId() + "', "
					+ "internal_pid=" 		+ "'" + newProcessedProduct.getInternalPid() + "', "
					+ "original_name=" 		+ "'" + newProcessedProduct.getOriginalName() + "', "
					+ "class=" 				+ (newProcessedProduct.get_class() == null ? "null" : "'" + newProcessedProduct.get_class()  + "'" ) + ", "
					+ "brand=" 				+ (newProcessedProduct.getBrand() == null ? "null" : "'" + newProcessedProduct.getBrand()  + "'" ) + ", "
					+ "recipient=" 			+ (newProcessedProduct.getRecipient() == null ? "null" : "'" + newProcessedProduct.getRecipient()  + "'" ) + ", "
					+ "quantity=" 			+ (newProcessedProduct.getQuantity() == null ? "null" : newProcessedProduct.getQuantity() ) + ", "
					+ "unit=" 				+ (newProcessedProduct.getUnit() == null ? "null" : "'" + newProcessedProduct.getUnit()  + "'" ) + ", "
					+ "extra=" 		+ (newProcessedProduct.getExtra() == null ? "null" : "'" + newProcessedProduct.getExtra()  + "'" ) + ", "
					+ "pic=" 		+ (newProcessedProduct.getPic() == null ? "null" : "'" + newProcessedProduct.getPic()  + "'" ) + ", "
					+ "url=" 		+ "'" + newProcessedProduct.getUrl() + "', "
					+ "market=" 		+ newProcessedProduct.getMarket() + ", "
					+ "ect=" 		+ "'" + newProcessedProduct.getEct() + "', "
					+ "lmt=" 		+ "'" + newProcessedProduct.getLmt() + "', "
					+ "lat=" 		+ (newProcessedProduct.getLat() == null ? "null" : "'" + newProcessedProduct.getLat()  + "'" ) + ", "
					+ "lrt=" 		+ "'" + newProcessedProduct.getLrt() + "', "
					+ "lms=" 		+ "'" + newProcessedProduct.getLms() + "', "
					+ "status=" 	+ "'" + newProcessedProduct.getStatus() + "', "
					+ "available=" 	+ newProcessedProduct.getAvailable() + ", "
					+ "void=" 		+ newProcessedProduct.getVoid_product() + ", "
					+ "cat1=" 		+ (newProcessedProduct.getCat1() == null ? "null" : "'" + newProcessedProduct.getCat1()  + "'" ) + ", "
					+ "cat2=" 		+ (newProcessedProduct.getCat2() == null ? "null" : "'" + newProcessedProduct.getCat2()  + "'" ) + ", "
					+ "cat3=" 		+ (newProcessedProduct.getCat3() == null ? "null" : "'" + newProcessedProduct.getCat3()  + "'" ) + ", "
					+ "multiplier=" 		+ (newProcessedProduct.getMultiplier() == null ? "null" : newProcessedProduct.getMultiplier() ) + ", "
					+ "original_description=" 		+ (newProcessedProduct.getOriginalDescription() == null ? "null" : "'" + newProcessedProduct.getOriginalDescription()  + "'" ) + ", "
					+ "price=" 		+ newProcessedProduct.getPrice() + ", "
					+ "stock=" 		+ newProcessedProduct.getStock() + ", "
					+ "secondary_pics=" 		+ (newProcessedProduct.getSecondary_pics() == null ? "null" : "'" + newProcessedProduct.getSecondary_pics()  + "'" ) + ", "
					+ "changes=" 		+ (newProcessedProduct.getChanges() == null ? "null" : "'" + newProcessedProduct.getChanges().toString().replace("'","''")  + "'" ) + ", "
					+ "digital_content=" 		+ ((newProcessedProduct.getDigitalContent() == null || newProcessedProduct.getDigitalContent().length() == 0) ? "null" : "'" + newProcessedProduct.getDigitalContent().toString().replace("'","''")  + "'" ) + ", "
					+ "marketplace=" 	+ ((newProcessedProduct.getMarketplace() == null || newProcessedProduct.getMarketplace().length() == 0) ? "null" : "'" + newProcessedProduct.getMarketplace().toString().replace("'","''")  + "'" ) + ", "
					+ "behaviour=" 		+ ((newProcessedProduct.getBehaviour() == null || newProcessedProduct.getBehaviour().length() == 0) ? "null" : "'" + newProcessedProduct.getBehaviour().toString().replace("'","''")  + "'" ) + ", "
					+ "similars=" 		+ ((newProcessedProduct.getSimilars() == null || newProcessedProduct.getSimilars().length() == 0) ? "null" : "'" + newProcessedProduct.getSimilars().toString().replace("'","''")  + "'" ) + " "
					+ "WHERE id = " + newProcessedProduct.getId();
		}

		Main.dbManager.runSqlExecute(query);

	}
	
	private static void foundProductForSeed(CrawlerSession session) {
		
		// if we are in a standalone task session
//		if(Main.mode.equals(Main.MODE_STANDALONE) && db.mongo_backend_panel != null && seedId != null) {
//			BasicDBObject log = new BasicDBObject("date", new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss"))
//					.append("type", "product")
//					.append("message", processedId);
//
//			db.appendLogToSeedDocument(seedId, log);
//
//		}

	}
	
	private static String sanitizeBeforePersist(String field) {
		if(field == null) {
			return null;
		} else {
			return field.replace("'", "''").trim();
		}
	}

}
