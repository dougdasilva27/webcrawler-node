package br.com.lett.crawlernode.processor.base;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.processor.controller.ResultManager;
import br.com.lett.crawlernode.processor.models.ProcessedModel;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class Processor {

	private static final Logger logger = LoggerFactory.getLogger(Processor.class);

	/**
	 * Process the product and create a new ProcessedModel based on the crawled product.
	 * @param product
	 * @param session
	 * @return a new ProcessedModel or null in case the Product model has invalid informations
	 */
	public static ProcessedModel createProcessed(
			Product product, 
			CrawlerSession session, 
			ProcessedModel previousProcessedProduct, 
			ResultManager processorResultManager) {

		Logging.printLogDebug(logger, session, "Creating processed product...");

		String nowISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss.SSS");

		ProcessedModel newProcessedProduct = null;

		// get crawled information
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

		// sanitize
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
			Logging.printLogError(logger, session, "Erro tentando criar ProcessedModel de leitura de produto disponível mas com campo vazio: price");
			return null;
		} else if(internal_id == null || internal_id.isEmpty()) {
			Logging.printLogError(logger, session, "Erro tentando criar ProcessedModel de leitura de produto com campo vazio: internal_id");
			return null;
		} else if(session.getMarket().getNumber() == 0) {
			Logging.printLogError(logger, session, "Erro tentando criar ProcessedModel de leitura de produto com campo vazio: [marketId] ... aborting ...");
			return null;
		} else if(url == null || url.isEmpty()) {
			Logging.printLogError(logger, session, "Erro tentando criar ProcessedModel de leitura de produto com campo vazio: [url] ... aborting ...");
			return null;
		} else if(name == null || name.isEmpty()) {
			Logging.printLogError(logger, session, "Erro tentando criar ProcessedModel de leitura de produto com campo vazio: [name] ... aborting ...");
			return null;
		}

		if(price != null && price == 0.0) {
			price = null;
		}

		try {

			// if the processed model already exists
			if (previousProcessedProduct != null) {

				// clone it, creating a new processed model
				newProcessedProduct = previousProcessedProduct.clone();

				// update fields with new values
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

			// if the product doesn't exists yet, then we must create a new processed model
			if(newProcessedProduct == null) {
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

			processorResultManager.processProduct(newProcessedProduct, session);

			// Atualizando disponibilidade
			newProcessedProduct.setAvailable(available);

			// Atualizando LRT
			newProcessedProduct.setLrt(nowISO);

			// Atualizando VOID
			newProcessedProduct.setVoid(false);

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
			if(previousProcessedProduct != null) oldStatus = previousProcessedProduct.getStatus();

			// Atualizando LMS se status mudou
			if(oldStatus == null || !newStatus.equals(oldStatus)) newProcessedProduct.setLms(nowISO);

			// Detectando e registrando mudanças
			// Recebe o banco Panel do Mongo porque grava urls que deverão ter um screenshot capturado
			if (session.getType().equals(CrawlerSession.TEST_TYPE)) {
				newProcessedProduct.registerChanges(previousProcessedProduct, br.com.lett.crawlernode.test.Tester.dbManager.mongoBackendPanel);
			} else {
				newProcessedProduct.registerChanges(previousProcessedProduct, Main.dbManager.mongoBackendPanel);
			}

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
					Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e1));
				}

			}

			newProcessedProduct.setBehaviour(newBehaviour);

			Logging.printLogDebug(logger, session, "Produto processado:" + "\n" + newProcessedProduct.toString());

		} catch (Exception e2) {
			Logging.printLogError(logger, session, "Erro ao tentar processar produto [seed:" + session.getSeedId() + "]");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e2));
		}

		return newProcessedProduct;
	}

	/**
	 * Fetch from database the current ProcessedModel from processed table.
	 * 
	 * @param product
	 * @param session
	 * @return the current ProcessedModel stored on database, or null if the product doesn't yet exists on processed table.
	 */
	public static ProcessedModel fetchPreviousProcessed(Product product, CrawlerSession session) {
		Logging.printLogDebug(logger, session, "Fetching previous processed product...");

		ProcessedModel actualProcessedProduct = null;

		/*
		 * If we are running a test for new crawlers, it may occur cases where the internalId
		 * in the product is null, because of a fail in crawling logic for example. In the case
		 * a product is void for example, it also may occur to not find the internalId, so we must get
		 * what was passed via CrawlerSession.
		 * But there are some cases where we don't have the internalId in the session, but the product
		 * have it, in case of a product crawled from a URL scheduled by the crawler discover for example.
		 */
		String internal_id = null;
		if (session.getType().equals(CrawlerSession.TEST_TYPE)) {
			internal_id = product.getInternalId();
		} else {
			internal_id = session.getInternalId();
		}

		// sanitize
		internal_id = sanitizeBeforePersist(internal_id);

		try {

			// reading current information of processed product
			StringBuilder query = new StringBuilder();
			query.append("SELECT * FROM processed WHERE market = ");
			query.append(session.getMarket().getNumber());
			query.append(" AND internal_id = '");
			query.append(internal_id);
			query.append("'");

			ResultSet rs = null;

			if (session.getType().equals(CrawlerSession.TEST_TYPE)) {
				rs = br.com.lett.crawlernode.test.Tester.dbManager.runSqlConsult(query.toString());
			} else {
				rs = Main.dbManager.runSqlConsult(query.toString());
			}

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

				actualProcessedProduct = new ProcessedModel(
						rs.getLong("id"), 
						rs.getString("internal_id"), 
						rs.getString("internal_pid"), 
						rs.getString("original_name"), 
						rs.getString("class"), 
						rs.getString("brand"), 
						rs.getString("recipient"),
						rs.getDouble("quantity"), 
						rs.getInt("multiplier"), 
						rs.getString("unit"), 
						rs.getString("extra"), 
						rs.getString("pic"), 
						rs.getString("secondary_pics"), 
						rs.getString("cat1"), 
						rs.getString("cat2"),
						rs.getString("cat3"), 
						rs.getString("url"), 
						rs.getInt("market"), 
						rs.getString("ect"), 
						rs.getString("lmt"), 
						rs.getString("lat"), 
						rs.getString("lrt"), 
						rs.getString("lms"), 
						rs.getString("status"), 
						changes,
						rs.getString("original_description"), 
						actual_price, 
						digitalContent, 
						rs.getLong("lett_id"), 
						similars, 
						rs.getBoolean("available"), 
						rs.getBoolean("void"), 
						actual_stock, 
						behaviour, 
						actual_marketplace);


				return actualProcessedProduct;

			}

		} catch (SQLException e) {
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
		}

		return actualProcessedProduct;
	}

	private static String sanitizeBeforePersist(String field) {
		if(field == null) {
			return null;
		} else {
			return field.replace("'", "''").trim();
		}
	}

}
