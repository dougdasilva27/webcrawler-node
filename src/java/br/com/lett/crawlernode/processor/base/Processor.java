package br.com.lett.crawlernode.processor.base;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jooq.Condition;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.processor.controller.ResultManager;
import br.com.lett.crawlernode.processor.models.ProcessedModel;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.DateConstants;
import br.com.lett.crawlernode.util.Logging;
import dbmodels.Tables;
import dbmodels.tables.Processed;

public class Processor {

	private static final Logger logger = LoggerFactory.getLogger(Processor.class);

	/**
	 * Process the product and create a new ProcessedModel based on the crawled product.
	 * 
	 * @param product
	 * @param session
	 * @return a new ProcessedModel or null in case the Product model has invalid informations
	 */
	public static ProcessedModel createProcessed(
			Product product, 
			Session session, 
			ProcessedModel previousProcessedProduct, 
			ResultManager processorResultManager) {

		Logging.printLogDebug(logger, session, "Creating processed product...");

		String nowISO = new DateTime(DateConstants.timeZone).toString("yyyy-MM-dd HH:mm:ss.SSS");

		ProcessedModel newProcessedProduct = null;

		// get crawled information
		boolean available = product.getAvailable();
		String url = product.getUrl();
		String internalId = product.getInternalId();
		String internalPid = product.getInternalPid();
		String name = product.getName();
		Float price = product.getPrice();
		Prices prices = product.getPrices();
		String cat1 = product.getCategory1();
		String cat2 = product.getCategory2();
		String cat3 = product.getCategory3();
		String foto = product.getPrimaryImage();
		String secondaryPics = product.getSecondaryImages();
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
		secondaryPics = sanitizeBeforePersist(secondaryPics);
		description = sanitizeBeforePersist(description);
		internalId = sanitizeBeforePersist(internalId);
		internalPid = sanitizeBeforePersist(internalPid);


		// checking fields
		boolean checkResult = checkFields(price, available, internalId, url, name, session);
		if (!checkResult) {
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
				newProcessedProduct.setPrices(prices);
				
				newProcessedProduct.setCat1(cat1);
				newProcessedProduct.setCat2(cat2);
				newProcessedProduct.setCat3(cat3);
				
				newProcessedProduct.setSecondary_pics(secondaryPics);
				newProcessedProduct.setCat1(cat1);
				newProcessedProduct.setCat2(cat2);
				newProcessedProduct.setCat3(cat3);
				newProcessedProduct.setOriginalName(name);
				newProcessedProduct.setOriginalDescription(description);
				newProcessedProduct.setInternalPid(internalPid);

			}

			// if the product doesn't exists yet, then we must create a new processed model
			if(newProcessedProduct == null) {
				newProcessedProduct = new ProcessedModel(
						null, 
						internalId, 
						internalPid, 
						name, 
						null, 
						null, 
						null, 
						null, 
						null, 
						null, 
						null, 
						foto, 
						secondaryPics, 
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
						prices,
						null, 
						null, 
						null,
						false, 
						false, 
						stock, 
						null, 
						marketplace);
			}

			// run the processor for the new model
			processorResultManager.processProduct(newProcessedProduct, session);

			// update availability
			newProcessedProduct.setAvailable(available);

			// update LRT
			newProcessedProduct.setLrt(nowISO);

			// update void
			newProcessedProduct.setVoid(false);

			// update LAT
			if(available){
				newProcessedProduct.setLat(nowISO);
			}

			// update status
			updateStatus(newProcessedProduct);

			// update LMS
			updateLMS(newProcessedProduct, previousProcessedProduct, nowISO);

			// update changes
			updateChanges(newProcessedProduct, previousProcessedProduct);

			// update LMT
			updateLMT(newProcessedProduct, nowISO);

			// Retirando price = 0
			if(newProcessedProduct.getPrice() != null && newProcessedProduct.getPrice() == 0.0) {
				newProcessedProduct.setPrice(null);
			}

			// update behavior
			updateBehavior(newProcessedProduct, nowISO, stock, available, price, marketplace, session);

			Logging.printLogDebug(logger, session, "Produto processado:" + "\n" + newProcessedProduct.toString());

		} catch (Exception e2) {
			Logging.printLogError(logger, session, "Erro ao tentar processar produto.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e2));
		}

		return newProcessedProduct;
	}

	private static boolean checkFields(
			Float price,
			boolean available,
			String internalId,
			String url,
			String name,
			Session session) {
		if((price == null || price.equals(0f)) && available) {
			Logging.printLogError(logger, session, "Erro tentando criar ProcessedModel de leitura de produto disponível mas com campo vazio: price");
			return false;
		} else if(internalId == null || internalId.isEmpty()) {
			Logging.printLogError(logger, session, "Erro tentando criar ProcessedModel de leitura de produto com campo vazio: internal_id");
			return false;
		} else if(session.getMarket().getNumber() == 0) {
			Logging.printLogError(logger, session, "Erro tentando criar ProcessedModel de leitura de produto com campo vazio: [marketId] ... aborting ...");
			return false;
		} else if(url == null || url.isEmpty()) {
			Logging.printLogError(logger, session, "Erro tentando criar ProcessedModel de leitura de produto com campo vazio: [url] ... aborting ...");
			return false;
		} else if(name == null || name.isEmpty()) {
			Logging.printLogError(logger, session, "Erro tentando criar ProcessedModel de leitura de produto com campo vazio: [name] ... aborting ...");
			return false;
		}

		return true;
	}

	/**
	 * Updates the intra day behavior of the sku.
	 * 
	 * @param newProcessedProduct
	 * @param nowISO
	 * @param stock
	 * @param available
	 * @param price
	 * @param marketplace
	 * @param session
	 */
	private static void updateBehavior(
			ProcessedModel newProcessedProduct,
			String nowISO,
			Integer stock,
			boolean available,
			Float price,
			JSONArray marketplace,
			Session session) {
		
		DateTimeFormatter f = DateTimeFormat
				.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
				.withZone(DateConstants.timeZone);
		
		// get the previous behavior object
		JSONArray oldBehaviour = newProcessedProduct.getBehaviour();
		if(oldBehaviour == null) {
			oldBehaviour = new JSONArray();
		}

		// using a TreeMap to automatically order by the keys
		Map<String, JSONObject> newBehaviorTreeMap = new TreeMap<>();

		JSONObject lastBehaviorBeforeToday = new JSONObject();

		// Populando newBehaviour
		DateTime dateTimeNow = new DateTime(DateConstants.timeZone).withTimeAtStartOfDay();
		
		for (int i = 0; i < oldBehaviour.length(); i++) {

			// Adicionando no mapa (para depois ser filtrado)
			newBehaviorTreeMap.put(oldBehaviour.getJSONObject(i).getString("date"), oldBehaviour.getJSONObject(i));

			// Adicionando primeiro behavior do dia (leitura na hora 00:00:01.000)
			DateTime currentIterationDateTime = null;
			DateTime dateTimeLast = null;

			try {
				currentIterationDateTime = f.parseDateTime(oldBehaviour.getJSONObject(i).getString("date"));
				if (lastBehaviorBeforeToday.has("date")) {
					dateTimeLast = f.parseDateTime(lastBehaviorBeforeToday.getString("date"));
				}

				// Se a data do behavior que estou analisando é anterior à hoje
				if (currentIterationDateTime.isBefore(dateTimeNow)) {

					// Se o candidato atual a primeiro behavior do dia não existe ou é antes do behavior 
					// que estou analisando, então atualizo o candidato à primeiro behavior do dia
					if (dateTimeLast == null || dateTimeLast.isBefore(currentIterationDateTime)) {
						lastBehaviorBeforeToday = oldBehaviour.getJSONObject(i);
					}
				}

			} catch (Exception e) {
				Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			}
		}


		// Criando behaviour do início de hoje (supostamente)
		String startOfDayISO = new DateTime(DateConstants.timeZone).withTimeAtStartOfDay().plusSeconds(1).toString("yyyy-MM-dd HH:mm:ss.SSS");

		if ( lastBehaviorBeforeToday != null && 
			(!newBehaviorTreeMap.containsKey(startOfDayISO) || !newBehaviorTreeMap.get(startOfDayISO).has("status")) ) {
			
			JSONObject behaviourStart = lastBehaviorBeforeToday;
			behaviourStart.put("date", startOfDayISO);
			
			if(!behaviourStart.has("status")) {
				behaviourStart.put("status", "void");
			}

			if(behaviourStart.has("price") && behaviourStart.getDouble("price") == 0.0) {
				behaviourStart.remove("price");
			}

			newBehaviorTreeMap.put(startOfDayISO, behaviourStart);
		}

		// Criando behaviour de agora
		JSONObject behaviour = new JSONObject();
		
		behaviour.put("date", nowISO);
		behaviour.put("stock", stock);
		behaviour.put("available", available);
		behaviour.put("status", newProcessedProduct.getStatus());
		
		if(price != null) {
			behaviour.put("price", price);
		}
		
		if (newProcessedProduct.getPrices() != null) {
			behaviour.put("prices", newProcessedProduct.getPrices().getPricesJson());
		}
		
		if(marketplace != null && marketplace.length() > 0) {
			behaviour.put("marketplace", marketplace);
		}
		
		newBehaviorTreeMap.put(nowISO, behaviour);

		JSONArray newBehaviour = new JSONArray();

		// Criando novo arrray behaviour apenas com as datas de hoje e
		// mantendo apenas os que tem os campos obrigatórios
		for(Entry<String, JSONObject> e: newBehaviorTreeMap.entrySet()) {
			String dateString = e.getKey();
			DateTime dateTime;

			if(!dateString.contains(".")){
				dateString = dateString + ".000";
			}

			try {
				dateTime = f.parseDateTime(dateString);
				DateTime beginingOfDay = new DateTime(DateConstants.timeZone).withTimeAtStartOfDay();
				
				if( dateTime.isAfter(beginingOfDay) && e.getValue().has("status") ) {
					newBehaviour.put(e.getValue());
				}

			} catch (Exception e1) {
				Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e1));
			}
		}

		newProcessedProduct.setBehaviour(newBehaviour);
	}

	private static void updateStatus(ProcessedModel newProcessedProduct) {
		String newStatus = "available";
		if(!newProcessedProduct.getAvailable()) {
			if(newProcessedProduct.getMarketplace() != null && newProcessedProduct.getMarketplace().length() > 0) {
				newStatus = "only_marketplace";
			} else {
				newStatus = "unavailable";
			}
		}
		newProcessedProduct.setStatus(newStatus);
	}

	private static void updateChanges(
			ProcessedModel newProcessedProduct,
			ProcessedModel previousProcessedProduct) {

		// detect and register changes
		// an instance of mongo panel must be passed, so we can schedule url to take screenshot
		newProcessedProduct.registerChanges(previousProcessedProduct, Main.dbManager.mongoBackendPanel);
	}

	private static void updateLMS(
			ProcessedModel newProcessedProduct, 
			ProcessedModel previousProcessedProduct,
			String nowISO) {

		// get previous status to verify change
		String oldStatus = "void";
		if(previousProcessedProduct != null) {
			oldStatus = previousProcessedProduct.getStatus();
		}

		// update lms in case we had a status change
		if(oldStatus == null || !newProcessedProduct.getStatus().equals(oldStatus)) {
			newProcessedProduct.setLms(nowISO);
		}
	}

	private static void updateLMT(ProcessedModel newProcessedProduct, String nowISO) {
		if(newProcessedProduct.getChanges() != null && (newProcessedProduct.getChanges().has("pic") || newProcessedProduct.getChanges().has("originals"))) {
			newProcessedProduct.setLmt(nowISO);
		}
	}

	/**
	 * Fetch from database the current ProcessedModel from processed table.
	 * 
	 * @param product
	 * @param session
	 * @return the current ProcessedModel stored on database, or null if the product doesn't yet exists on processed table.
	 */
	public static ProcessedModel fetchPreviousProcessed(Product product, Session session) {
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
		String internalId = product.getInternalId();
		if (internalId == null) {
			internalId = session.getInternalId();
		}

		// sanitize
		internalId = sanitizeBeforePersist(internalId);

		if (internalId != null) {

			try {
//				Processed processedTable = Tables.PROCESSED;
//				
//				List<Condition> conditions = new ArrayList<>();
//				conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber())
//						.and(processedTable.INTERNAL_ID.equal(internalId)));
				
				// TODO hotfix for query
				// estava falhando aqui
				// voltei do jeito antigo pra apagar o fogo, estava dando problema na fastshop
				// averiguar o motivo
				StringBuilder query = new StringBuilder();
								query.append("SELECT * FROM processed WHERE market = ");
								query.append(session.getMarket().getNumber());
								query.append(" AND internal_id = '");
								query.append(internalId);
								query.append("' LIMIT 1");
				
//				ResultSet rs = Main.dbManager.runSelectJooq(processedTable, null, conditions);
				Logging.printLogDebug(logger, session, "Running query: " + query.toString());
				ResultSet rs = Main.dbManager.runSqlConsult(query.toString());

				while(rs.next()) {
					
					JSONObject digitalContent;
					if(rs.getString("digital_content") != null) {
						try {
							digitalContent = new JSONObject(rs.getString("digital_content"));
						} catch (JSONException e) {
							digitalContent = null;
						}
					} else {
						digitalContent = null;
					}
					
					JSONObject changes;
					if(rs.getString("changes") != null) {
						try {
							changes = new JSONObject(rs.getString("changes"));
						} catch (JSONException e) {
							changes = null;
						}
					} else {
						changes = null;
					}
					

					JSONArray similars;
					if(rs.getString("similars") != null) {
						try {
							similars = new JSONArray(rs.getString("similars"));
						} catch (JSONException e) {
							similars = null;
						}
					} else {
						similars = null;
					}

					JSONArray behaviour;
					if(rs.getString("behaviour") != null) {
						try {
							behaviour = new JSONArray(rs.getString("behaviour"));
						} catch (JSONException e) {
							behaviour = null;
						}
					} else {
						behaviour = null;
					}

					JSONArray actualMarketplace;
					if(rs.getString("marketplace") != null) {
						try {
							actualMarketplace = new JSONArray(rs.getString("marketplace"));
						} catch (JSONException e) {
							actualMarketplace = null;
						}
					} else {
						actualMarketplace = null;
					}
				
					JSONObject actualPricesJson;
					if(rs.getString("prices") != null) {
						try {
							actualPricesJson = new JSONObject(rs.getString("prices"));
						} catch (JSONException e) {
							actualPricesJson = null;
						}
					} else {
						actualPricesJson = null;
					}
			
					Integer actualStock = rs.getInt("stock");
					if(actualStock == 0) {
						actualStock = null;
					}
					
					Float actualPrice = rs.getFloat("price"); 
					if(actualPrice == 0) {
						actualPrice = null;
					}
					
					Prices actualPrices = new Prices();
					actualPrices.setPricesJson(actualPricesJson);

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
							actualPrice,
							actualPrices,
							digitalContent, 
							rs.getLong("lett_id"), 
							similars, 
							rs.getBoolean("available"), 
							rs.getBoolean("void"), 
							actualStock, 
							behaviour, 
							actualMarketplace);

					return actualProcessedProduct;

				}

			} catch (SQLException e) {
				Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
			}
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
