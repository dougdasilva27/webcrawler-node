package br.com.lett.crawlernode.database;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Table;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoException;
import com.mongodb.MongoWriteConcernException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.RatingReviewsCrawlerSession;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.processor.models.ProcessedModel;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import dbmodels.Tables;
import dbmodels.tables.Crawler;
import dbmodels.tables.CrawlerOld;
import dbmodels.tables.Processed;
import generation.PostgresJSONGsonBinding;

public class Persistence {
	private static final Logger logger = LoggerFactory.getLogger(Persistence.class);

	public static final String MONGO_TASKS_COLLECTION = "Task";

	public static final String MONGO_TASK_COLLECTION_PROCESSEDID_FIELD 	= "processed_id";
	public static final String MONGO_TASK_COLLECTION_FOUND_SKUS_FIELD 	= "found_skus";
	public static final String MONGO_TASK_COLLECTION_NEW_SKUS_FIELD		= "new_skus";
	public static final String MONGO_TASK_COLLECTION_STATUS_FIELD 		= "status";

	public static final String MONGO_TASK_STATUS_DONE 	= "done";
	public static final String MONGO_TASK_STATUS_FAILED = "failed";

	// Class generated in project DB to convert an object to gson because dialect postgres not accepted this type
	private static final PostgresJSONGsonBinding CONVERT_STRING_GSON = new PostgresJSONGsonBinding();

	/**
	 * Persist the product crawled informations on tables crawler and crawler_old
	 * 
	 * @param product
	 * @param session
	 */
	public static void persistProduct(Product product, Session session) {
		Logging.printLogDebug(logger, session, "Persisting crawled product...");

		// get crawled information
		boolean available = product.getAvailable();
		String url = product.getUrl();
		String internalId = product.getInternalId();
		String internalPid = product.getInternalPid();
		String name = product.getName();
		Float price = product.getPrice();
		JSONObject prices = product.getPrices() == null ? null : product.getPrices().getPricesJson();
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

		String marketplaceString = null;

		if(marketplace != null && marketplace.length() > 0) {
			marketplaceString = sanitizeBeforePersist(marketplace.toString());
		}


		// checking fields
		if((price == null || price.equals(0f)) && available) {
			Logging.printLogError(logger, session, "Erro tentando inserir leitura de produto disponível mas com campo vazio: price");
			return;
		} else if(internalId == null || internalId.isEmpty()) {
			Logging.printLogError(logger, session, "Erro tentando inserir leitura de produto com campo vazio: internal_id");
			return;
		} else if(session.getMarket().getNumber() == 0) {
			Logging.printLogError(logger, session, "Erro tentando inserir leitura de produto com campo vazio: [marketId] ... aborting ...");
			return;
		} else if(url == null || url.isEmpty()) {
			Logging.printLogError(logger, session, "Erro tentando inserir leitura de produto com campo vazio: [url] ... aborting ...");
			return;
		} else if(name == null || name.isEmpty()) {
			Logging.printLogError(logger, session, "Erro tentando inserir leitura de produto com campo vazio: [name] ... aborting ...");
			return;
		}

		if(price != null && price == 0.0) {
			price = null;
		}

		// persisting on crawler and crawler_old
		try {

			Logging.printLogDebug(logger, session, "Crawled product persisted with success.");

			// Create a fields and values of crawler
			Crawler crawler = Tables.CRAWLER;

			List<Field<?>> fieldsCrawler = new ArrayList<>();
			List<Object> valuesCrawler = new ArrayList<>();

			fieldsCrawler.add(crawler.AVAILABLE);
			valuesCrawler.add(available);

			fieldsCrawler.add(crawler.MARKET);
			valuesCrawler.add(session.getMarket().getNumber());

			fieldsCrawler.add(crawler.INTERNAL_ID);
			valuesCrawler.add(internalId);

			fieldsCrawler.add(crawler.INTERNAL_PID);
			valuesCrawler.add(internalPid);

			fieldsCrawler.add(crawler.URL);
			valuesCrawler.add(url);

			fieldsCrawler.add(crawler.STOCK);
			valuesCrawler.add(stock);

			fieldsCrawler.add(crawler.NAME);
			valuesCrawler.add(name);

			fieldsCrawler.add(crawler.PIC);
			valuesCrawler.add(foto);

			fieldsCrawler.add(crawler.SECONDARY_PICS);
			valuesCrawler.add(secondaryPics);

			fieldsCrawler.add(crawler.CAT1);
			valuesCrawler.add(cat1);

			fieldsCrawler.add(crawler.CAT2);
			valuesCrawler.add(cat2);

			fieldsCrawler.add(crawler.CAT3);
			valuesCrawler.add(cat3);

			if(price != null) {
				fieldsCrawler.add(crawler.PRICE);
				valuesCrawler.add(MathCommonsMethods.normalizeTwoDecimalPlaces(price.doubleValue()));
			}

			if(prices != null) {
				fieldsCrawler.add(crawler.PRICES);
				valuesCrawler.add(CONVERT_STRING_GSON.converter().from(prices.toString()));
			}

			if(description != null && !Jsoup.parse(description).text().replace("\n", "").replace(" ", "").trim().isEmpty()) {
				fieldsCrawler.add(crawler.DESCRIPTION);
				valuesCrawler.add(description);
			}

			if(marketplaceString != null) {
				fieldsCrawler.add(crawler.MARKETPLACE);
				valuesCrawler.add(marketplaceString);
			}
			
			// Create a fields and values of crawler_old
			CrawlerOld crawlerOld = Tables.CRAWLER_OLD;

			List<Field<?>> fieldsCrawlerOld = new ArrayList<>();
			List<Object> valuesCrawlerOld = new ArrayList<>();

			fieldsCrawlerOld.add(crawlerOld.AVAILABLE);
			valuesCrawlerOld.add(available);

			fieldsCrawlerOld.add(crawlerOld.MARKET);
			valuesCrawlerOld.add(session.getMarket().getNumber());

			fieldsCrawlerOld.add(crawlerOld.INTERNAL_ID);
			valuesCrawlerOld.add(internalId);

			fieldsCrawlerOld.add(crawlerOld.INTERNAL_PID);
			valuesCrawlerOld.add(internalPid);

			fieldsCrawlerOld.add(crawlerOld.URL);
			valuesCrawlerOld.add(url);

			fieldsCrawlerOld.add(crawlerOld.STOCK);
			valuesCrawlerOld.add(stock);

			fieldsCrawlerOld.add(crawlerOld.NAME);
			valuesCrawlerOld.add(name);

			fieldsCrawlerOld.add(crawlerOld.PIC);
			valuesCrawlerOld.add(foto);

			fieldsCrawlerOld.add(crawlerOld.SECONDARY_PICS);
			valuesCrawlerOld.add(secondaryPics);

			fieldsCrawlerOld.add(crawlerOld.CAT1);
			valuesCrawlerOld.add(cat1);

			fieldsCrawlerOld.add(crawlerOld.CAT2);
			valuesCrawlerOld.add(cat2);

			fieldsCrawlerOld.add(crawlerOld.CAT3);
			valuesCrawlerOld.add(cat3);

			if(price != null) {
				fieldsCrawlerOld.add(crawlerOld.PRICE);
				valuesCrawlerOld.add(MathCommonsMethods.normalizeTwoDecimalPlaces(price.doubleValue()));
			}

			if(prices != null) {
				fieldsCrawlerOld.add(crawlerOld.PRICES);
				valuesCrawlerOld.add(CONVERT_STRING_GSON.converter().from(prices.toString()));
			}

			if(description != null && !Jsoup.parse(description).text().replace("\n", "").replace(" ", "").trim().isEmpty()) {
				fieldsCrawlerOld.add(crawlerOld.DESCRIPTION);
				valuesCrawlerOld.add(description);
			}

			if(marketplaceString != null) {
				fieldsCrawlerOld.add(crawlerOld.MARKETPLACE);
				valuesCrawlerOld.add(marketplaceString);
			}
			
			// List of tables for batch insert
			List<Table<?>> tables = new ArrayList<>();
			tables.add(crawler);
			tables.add(crawlerOld);
			
			// Map of Table - FieldsOfTable
			Map<Table<?>,List<Field<?>>> fieldsMap = new HashMap<>();
			fieldsMap.put(crawler, fieldsCrawler);
			fieldsMap.put(crawlerOld, fieldsCrawlerOld);
			
			// Map of Table - ValuesOfTable
			Map<Table<?>,List<Object>> valuesMap = new HashMap<>();
			valuesMap.put(crawler, valuesCrawler);
			valuesMap.put(crawlerOld, valuesCrawlerOld);

			Main.dbManager.runBatchInsertJooq(tables, fieldsMap, valuesMap);

		} catch (Exception e) {
			Logging.printLogError(logger, session, "Error inserting product on database!");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));

			session.registerError( new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );
		}
	}


	public static void updateRating(RatingsReviews ratingReviews, Session session) {
		Processed processedTable = Tables.PROCESSED;

		Map<Field<?>, Object> updateSets = new HashMap<>();
		
		if(ratingReviews != null){
			updateSets.put(processedTable.RATING, CONVERT_STRING_GSON.converter().from(ratingReviews.getJSON().toString()));
		} else {
			updateSets.put(processedTable.RATING, null);
		}

		List<Condition> conditions = new ArrayList<>();
		conditions.add(processedTable.ID.equal(((RatingReviewsCrawlerSession)session).getProcessedId()));

		try {
			Main.dbManager.runUpdateJooq(processedTable, updateSets, conditions);
			Logging.printLogDebug(logger, session, "Processed product rating updated with success.");

		} catch(Exception e) {
			Logging.printLogError(logger, session, "Error updating processed product rating.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			session.registerError( new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );
		}

	}

	/**
	 * 
	 * @param newProcessedProduct
	 * @param session
	 * @return
	 */
	public static PersistenceResult persistProcessedProduct(ProcessedModel newProcessedProduct, Session session) {
		Logging.printLogDebug(logger, session, "Persisting processed product...");

		PersistenceResult persistenceResult = new ProcessedModelPersistenceResult();
		Long id;

		String query = "";

		JSONObject prices = newProcessedProduct.getPrices() == null ? null : newProcessedProduct.getPrices().getPricesJson();

		Processed processedTable = Tables.PROCESSED;

		try {
			if(newProcessedProduct.getId() == null) {
				List<Field<?>> fields = new ArrayList<>();

				fields.add(processedTable.INTERNAL_ID);
				fields.add(processedTable.INTERNAL_PID);
				fields.add(processedTable.ORIGINAL_NAME);
				fields.add(processedTable.CLASS);
				fields.add(processedTable.BRAND);
				fields.add(processedTable.RECIPIENT);
				fields.add(processedTable.QUANTITY);
				fields.add(processedTable.UNIT);
				fields.add(processedTable.EXTRA);
				fields.add(processedTable.PIC);
				fields.add(processedTable.URL);
				fields.add(processedTable.MARKET);
				fields.add(processedTable.ECT);
				fields.add(processedTable.LMT);
				fields.add(processedTable.LAT);
				fields.add(processedTable.LRT);
				fields.add(processedTable.LMS);
				fields.add(processedTable.STATUS);
				fields.add(processedTable.AVAILABLE);
				fields.add(processedTable.VOID);
				fields.add(processedTable.CAT1);
				fields.add(processedTable.CAT2);
				fields.add(processedTable.CAT3);
				fields.add(processedTable.MULTIPLIER);
				fields.add(processedTable.ORIGINAL_DESCRIPTION);
				fields.add(processedTable.PRICE);
				fields.add(processedTable.PRICES);
				fields.add(processedTable.STOCK);
				fields.add(processedTable.SECONDARY_PICS);
				fields.add(processedTable.CHANGES);
				fields.add(processedTable.DIGITAL_CONTENT);
				fields.add(processedTable.MARKETPLACE);
				fields.add(processedTable.BEHAVIOUR);
				fields.add(processedTable.SIMILARS);
				
				List<Object> values = new ArrayList<>();

				values.add(newProcessedProduct.getInternalId());
				values.add(newProcessedProduct.getInternalPid());
				values.add(newProcessedProduct.getOriginalName());
				values.add(newProcessedProduct.get_class());
				values.add(newProcessedProduct.getBrand());
				values.add(newProcessedProduct.getRecipient());
				values.add(newProcessedProduct.getQuantity());
				values.add(newProcessedProduct.getUnit());
				values.add(newProcessedProduct.getExtra());
				values.add(newProcessedProduct.getPic());
				values.add(newProcessedProduct.getUrl());
				values.add(newProcessedProduct.getMarket());
				values.add(newProcessedProduct.getEct());
				values.add(newProcessedProduct.getLmt());
				values.add(newProcessedProduct.getLat());
				values.add(newProcessedProduct.getLrt());
				values.add(newProcessedProduct.getLms());
				values.add(newProcessedProduct.getStatus());
				values.add(newProcessedProduct.getAvailable());
				values.add(newProcessedProduct.getVoid());
				values.add(newProcessedProduct.getCat1());
				values.add(newProcessedProduct.getCat2());
				values.add(newProcessedProduct.getCat3());
				values.add(newProcessedProduct.getMultiplier());
				values.add(newProcessedProduct.getOriginalDescription());
				values.add(newProcessedProduct.getPrice());
				
				if(prices != null) {
					values.add(CONVERT_STRING_GSON.converter().from(prices.toString()));
				} else {
					values.add(null);
				}
				
				values.add(newProcessedProduct.getStock());
				values.add(newProcessedProduct.getSecondary_pics());
				
				if(newProcessedProduct.getChanges() != null){
					values.add(newProcessedProduct.getChanges().toString().replace("'","''"));
				} else {
					values.add(null);
				}
	
				if(newProcessedProduct.getDigitalContent() != null){
					values.add(newProcessedProduct.getDigitalContent().toString().replace("'","''"));
				} else {
					values.add(null);
				}
				
				if(newProcessedProduct.getMarketplace() != null){
					values.add(newProcessedProduct.getMarketplace().toString().replace("'","''"));
				} else {
					values.add(null);
				}
				
				if(newProcessedProduct.getBehaviour() != null){
					values.add(newProcessedProduct.getBehaviour().toString().replace("'","''"));
				} else {
					values.add(null);
				}
				
				if(newProcessedProduct.getSimilars() != null){
					values.add(newProcessedProduct.getSimilars().toString().replace("'","''"));
				} else {
					values.add(null);
				}
				
				// get processeed id of new processed product
				id = Main.dbManager.runInsertJooqReturningID(processedTable, fields, values, processedTable.ID);
				if (persistenceResult instanceof ProcessedModelPersistenceResult && id != 0) {
					((ProcessedModelPersistenceResult)persistenceResult).addCreatedId(id);
				}

			} else {
				Map<Field<?>, Object> updateMap = new HashMap<>();

				//			  				 Column					Value
				updateMap.put(processedTable.INTERNAL_ID, 			newProcessedProduct.getInternalId());
				updateMap.put(processedTable.INTERNAL_PID, 			newProcessedProduct.getInternalPid());
				updateMap.put(processedTable.ORIGINAL_NAME, 		newProcessedProduct.getOriginalName());
				updateMap.put(processedTable.CLASS, 				newProcessedProduct.get_class());
				updateMap.put(processedTable.BRAND, 				newProcessedProduct.getBrand());
				updateMap.put(processedTable.RECIPIENT,				newProcessedProduct.getRecipient());
				updateMap.put(processedTable.QUANTITY, 				newProcessedProduct.getQuantity());
				updateMap.put(processedTable.UNIT, 					newProcessedProduct.getUnit());
				updateMap.put(processedTable.EXTRA, 				newProcessedProduct.getExtra());
				updateMap.put(processedTable.PIC, 					newProcessedProduct.getPic());
				updateMap.put(processedTable.URL, 					newProcessedProduct.getUrl());
				updateMap.put(processedTable.MARKET, 				newProcessedProduct.getMarket());
				updateMap.put(processedTable.ECT, 					newProcessedProduct.getEct());
				updateMap.put(processedTable.LMT, 					newProcessedProduct.getLmt());
				updateMap.put(processedTable.LAT, 					newProcessedProduct.getLat());
				updateMap.put(processedTable.LRT, 					newProcessedProduct.getLrt());
				updateMap.put(processedTable.LMS, 					newProcessedProduct.getLms());
				updateMap.put(processedTable.STATUS, 				newProcessedProduct.getStatus());
				updateMap.put(processedTable.AVAILABLE, 			newProcessedProduct.getAvailable());
				updateMap.put(processedTable.VOID, 					newProcessedProduct.getVoid());
				updateMap.put(processedTable.CAT1, 					newProcessedProduct.getCat1());
				updateMap.put(processedTable.CAT2, 					newProcessedProduct.getCat2());
				updateMap.put(processedTable.CAT3, 					newProcessedProduct.getCat3());
				updateMap.put(processedTable.MULTIPLIER, 			newProcessedProduct.getMultiplier());
				updateMap.put(processedTable.ORIGINAL_DESCRIPTION, 	newProcessedProduct.getOriginalDescription());
				updateMap.put(processedTable.PRICE, 				newProcessedProduct.getPrice());
				updateMap.put(processedTable.STOCK, 				newProcessedProduct.getStock());
				updateMap.put(processedTable.SECONDARY_PICS, 		newProcessedProduct.getSecondary_pics());
				
				if(prices != null) {
					updateMap.put(processedTable.PRICES, CONVERT_STRING_GSON.converter().from(prices.toString()));
				} else {
					updateMap.put(processedTable.PRICES, null);
				}
				
				if(newProcessedProduct.getChanges() != null){
					updateMap.put(processedTable.CHANGES, newProcessedProduct.getChanges().toString().replace("'","''"));
				} else {
					updateMap.put(processedTable.CHANGES, null);
				}
	
				if(newProcessedProduct.getDigitalContent() != null){
					updateMap.put(processedTable.DIGITAL_CONTENT, newProcessedProduct.getDigitalContent().toString().replace("'","''") );
				} else {
					updateMap.put(processedTable.DIGITAL_CONTENT, null);
				}
				
				if(newProcessedProduct.getMarketplace() != null){
					updateMap.put(processedTable.MARKETPLACE,  newProcessedProduct.getMarketplace().toString().replace("'","''"));
				} else {
					updateMap.put(processedTable.MARKETPLACE,  null);
				}
				
				if(newProcessedProduct.getBehaviour() != null){
					updateMap.put(processedTable.BEHAVIOUR, newProcessedProduct.getBehaviour().toString().replace("'","''"));
				} else {
					updateMap.put(processedTable.BEHAVIOUR, null);
				}
				
				if(newProcessedProduct.getSimilars() != null){
					updateMap.put(processedTable.SIMILARS, newProcessedProduct.getSimilars().toString().replace("'","''"));
				} else {
					updateMap.put(processedTable.SIMILARS, null);
				}
				
				// get the id of the processed product that already exists
				id = newProcessedProduct.getId();
				
				List<Condition> conditions = new ArrayList<>();
				conditions.add(processedTable.ID.equal(id));

				if (persistenceResult instanceof ProcessedModelPersistenceResult) {
					((ProcessedModelPersistenceResult)persistenceResult).addModifiedId(id);
				}

				Main.dbManager.runUpdateJooq(processedTable, updateMap, conditions);
			}

			Logging.printLogDebug(logger, session, "Processed product persisted with success.");

		} catch (SQLException e) {
			Logging.printLogError(logger, session, "Error updating processed product.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			session.registerError( new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );

			return null;
		}

		return persistenceResult;
	}

	/**
	 * Set void value of a processed model.
	 * @param processed
	 * @param voidValue A boolean indicating whether the processed product void must be set to true or false
	 * @param session
	 */
	public static void setProcessedVoidTrue(ProcessedModel processed, Session session) {
		Processed processedTable = Tables.PROCESSED;

		Map<Field<?>, Object> updateSets = new HashMap<>();

		updateSets.put(processedTable.AVAILABLE, false);
		updateSets.put(processedTable.STATUS, "void");
		updateSets.put(processedTable.MARKETPLACE, null);
		updateSets.put(processedTable.PRICE, null);

		List<Condition> conditions = new ArrayList<>();

		conditions.add(processedTable.INTERNAL_ID.equal(session.getInternalId()));
		conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber()));

		try {
			Main.dbManager.runUpdateJooq(processedTable, updateSets, conditions);
			Logging.printLogDebug(logger, session, "Processed product void value updated with success.");

		} catch(Exception e) {
			Logging.printLogError(logger, session, "Error updating processed product void.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			session.registerError( new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );
		}
	}

	/**
	 * Updates processed LastReadTime on processed table.
	 * @param processed
	 * @param nowISO
	 * @param session
	 */
	public static void updateProcessedLRT(ProcessedModel processed, String nowISO, Session session) {
		Processed processedTable = Tables.PROCESSED;

		Map<Field<?>, Object> updateSets = new HashMap<>();
		updateSets.put(processedTable.LRT, nowISO);

		List<Condition> conditions = new ArrayList<>();
		conditions.add(processedTable.INTERNAL_ID.equal(session.getInternalId()));
		conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber()));

		try {
			Main.dbManager.runUpdateJooq(processedTable, updateSets, conditions);
			Logging.printLogDebug(logger, session, "Processed product LRT updated with success.");

		} catch(Exception e) {
			Logging.printLogError(logger, session, "Error updating processed product LRT.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			session.registerError( new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );
		}
	}

	/**
	 * Updates processed LastModifiedTime on processed table.
	 * @param processed
	 * @param nowISO
	 * @param session
	 */
	public static void updateProcessedLMT(ProcessedModel processed, String nowISO, Session session) {
		Processed processedTable = Tables.PROCESSED;

		Map<Field<?>, Object> updateSets = new HashMap<>();
		updateSets.put(processedTable.LMT, nowISO);

		List<Condition> conditions = new ArrayList<>();
		conditions.add(processedTable.INTERNAL_ID.equal(session.getInternalId()));
		conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber()));

		try {
			Main.dbManager.runUpdateJooq(processedTable, updateSets, conditions);
			Logging.printLogDebug(logger, session, "Processed product LRT updated with success.");

		} catch(Exception e) {
			Logging.printLogError(logger, session, "Error updating processed product LMT.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			session.registerError( new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );
		}

	}

	/**
	 * Set the status field of the task document.
	 * 
	 * @param status
	 * @param session
	 * @param mongoDatabase
	 */
	public static void setTaskStatusOnMongo(String status, Session session, MongoDatabase mongoDatabase) {
		try {
			if (mongoDatabase != null) {
				MongoCollection<Document> taskCollection = mongoDatabase.getCollection(MONGO_TASKS_COLLECTION);
				String documentId = String.valueOf(session.getSessionId());
				taskCollection.updateOne(
						new Document("_id", documentId),
						new Document("$set", new Document(MONGO_TASK_COLLECTION_STATUS_FIELD, status))
						);
			} else {
				Logging.printLogError(logger, session, "Mongo database is null.");
			}
		} catch (MongoWriteException mongoWriteException) {
			Logging.printLogError(logger, session, CommonMethods.getStackTrace(mongoWriteException));
		} catch (MongoWriteConcernException mongoWriteConcernException) {
			Logging.printLogError(logger, session, CommonMethods.getStackTrace(mongoWriteConcernException));
		} catch (MongoException mongoException) {
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(mongoException));
		}
	}

	/**
	 * 
	 * @param processedId
	 * @param session
	 * @param mongoDatabase
	 */
	public static void appendProcessedIdOnMongo(Long processedId, Session session, MongoDatabase mongoDatabase) {
		try {
			if (mongoDatabase != null) {
				MongoCollection<Document> taskCollection = mongoDatabase.getCollection(MONGO_TASKS_COLLECTION);
				String documentId = String.valueOf(session.getSessionId());

				Document search = new Document("_id", documentId);
				Document modification = new Document("$push", new Document(MONGO_TASK_COLLECTION_FOUND_SKUS_FIELD, processedId));

				taskCollection.updateOne(search, modification);

				Logging.printLogDebug(logger, session, "Mongo task document updated with success!");
			}
		} catch (MongoException mongoException) {
			Logging.printLogError(logger, session, "Error updating collection on Mongo.");
			Logging.printLogError(logger, session, CommonMethods.getStackTrace(mongoException));

		}
	}

	/**
	 * 
	 * @param processedId
	 * @param session
	 * @param mongoDatabase
	 */
	public static void appendCreatedProcessedIdOnMongo(Long processedId, Session session, MongoDatabase mongoDatabase) {
		try {
			if (mongoDatabase != null) {
				MongoCollection<Document> taskCollection = mongoDatabase.getCollection(MONGO_TASKS_COLLECTION);
				String documentId = String.valueOf(session.getSessionId());

				Document search = new Document("_id", documentId);
				Document modification = new Document("$push", new Document(MONGO_TASK_COLLECTION_NEW_SKUS_FIELD, processedId));

				taskCollection.updateOne(search, modification);

				Logging.printLogDebug(logger, session, "Mongo task document updated with success!");
			}
		} catch (MongoException mongoException) {
			Logging.printLogError(logger, session, "Error updating collection on Mongo.");
			Logging.printLogError(logger, session, CommonMethods.getStackTrace(mongoException));

		}
	}

	/**
	 * 
	 * @param session
	 * @param mongoDatabase
	 */
	public static void insertProcessedIdOnMongo(Session session, MongoDatabase mongoDatabase) {
		try {
			if (mongoDatabase != null) {
				MongoCollection<Document> taskCollection = mongoDatabase.getCollection(MONGO_TASKS_COLLECTION);
				String documentId = String.valueOf(session.getSessionId());

				// if processedId inside the session is null, it means
				// we are processing a task that was inserted manually
				// or a task from an URL scheduled by the webcrawler discover
				// in these two cases, the field processedId on Mongo must be null, because it can
				// get more than one product during extraction
				if (session.getProcessedId() == null) {
					taskCollection.updateOne(
							new Document("_id", documentId),
							new Document("$set", new Document(MONGO_TASK_COLLECTION_PROCESSEDID_FIELD, null))
							);
				}

				// in this case we are processing a task from insights queue
				else {
					taskCollection.updateOne(
							new Document("_id", documentId),
							new Document("$set", new Document(MONGO_TASK_COLLECTION_PROCESSEDID_FIELD, session.getProcessedId()))
							);
				}
			} else {
				Logging.printLogError(logger, session, "MongoDatabase is null.");
			}
		} catch (MongoException mongoException) {
			Logging.printLogError(logger, session, "Error updating collection on Mongo.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(mongoException));
		}
	}


	private static String sanitizeBeforePersist(String field) {
		if(field == null) {
			return null;
		} else {
			return field.replace("'", "''").trim();
		}
	}

	/**
	 * 
	 * @param markets
	 */
	public static void initializeImagesDirectories(Markets markets) {
		Logging.printLogDebug(logger, "Initializing temp directory at:" + Main.executionParameters.getTmpImageFolder() + "...");

		List<Market> marketsList = markets.getMarkets();

		String[] subdirectories = new String[]{"images"};

		// create folder for each market
		for(Market m: marketsList) {
			Logging.printLogInfo(logger, "Processing " + m.getCity());
			printLogDirectory(m.getCity(), null, null);

			Logging.printLogInfo(logger, "Processing " + m.getCity() + " -> " + m.getName());
			printLogDirectory(m.getCity(), m.getName(), null);

			for(String folder: subdirectories) {
				printLogDirectory(m.getCity(), m.getName(), folder);
			}

		}
	}
	
	/**
	 * Directory creation log
	 * @param city
	 * @param name
	 * @param folder
	 */
	static void printLogDirectory(String city, String name, String folder){
		File file;
		
		if(name == null) {
			file = new File(Main.executionParameters.getTmpImageFolder() + "/" + city);
		} else if(folder == null) {
			file = new File(Main.executionParameters.getTmpImageFolder() + "/" + city + "/" + name);
		} else {
			file = new File(Main.executionParameters.getTmpImageFolder() + "/" + city + "/" + name + "/" + folder);
		}
		
		if (!file.exists()) {
			if (file.mkdir()) {
				Logging.printLogInfo(logger, createDefaultMessageInitializeImagesSuccess(file));
			} else {
				Logging.printLogError(logger, createDefaultMessageInitializeImagesFailed(file));
			}
		} else {
			Logging.printLogDebug(logger, createDefaultMessageInitializeImagesAlreadyCreated(file));
		}
	}
	
	/**
	 * Default messages to directoty creation
	 * @param file
	 * @return
	 */
	
	private static String createDefaultMessageInitializeImagesSuccess(File file){
		return "Directory " + file.getAbsolutePath() + " created!";
	}
	
	private static String createDefaultMessageInitializeImagesFailed(File file){
		return "Failed to create " + file.getAbsolutePath() + " directory!";
	}
	
	private static String createDefaultMessageInitializeImagesAlreadyCreated(File file){
		return  "Directory " + file.getAbsolutePath() + " was already created...";
	}

}
