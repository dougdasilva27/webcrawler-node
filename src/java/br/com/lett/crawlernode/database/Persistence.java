package br.com.lett.crawlernode.database;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import br.com.lett.crawlernode.core.models.Categories;
import br.com.lett.crawlernode.core.models.CategoriesRanking;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.Ranking;
import br.com.lett.crawlernode.core.models.RankingProducts;
import br.com.lett.crawlernode.core.models.RatingsReviews;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.crawler.RatingReviewsCrawlerSession;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import comunication.MongoDB;
import dbmodels.Tables;
import dbmodels.tables.Crawler;
import dbmodels.tables.CrawlerCategories;
import dbmodels.tables.CrawlerOld;
import dbmodels.tables.CrawlerRanking;

import generation.PostgresJSONGsonBinding;
import models.Behavior;
import models.Marketplace;
import models.Processed;
import models.prices.Prices;

public class Persistence {
	private static final Logger logger = LoggerFactory.getLogger(Persistence.class);

	public static final String MONGO_TASKS_COLLECTION = "Task";

	public static final String MONGO_TASK_COLLECTION_PROCESSEDID_FIELD 	= "processed_id";
	public static final String MONGO_TASK_COLLECTION_FOUND_SKUS_FIELD 	= "found_skus";
	public static final String MONGO_TASK_COLLECTION_NEW_SKUS_FIELD		= "new_skus";
	public static final String MONGO_TASK_COLLECTION_STATUS_FIELD 		= "status";

	private static final String MONGO_COLLECTION_CATEGORIES = "Categories";
	private static final String MONGO_COLLECTION_DISCOVER_STATS = "RankingDiscoverStats";
	private static final String MONGO_COLLECTION_TASK = "Task";

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
		JSONObject prices = product.getPrices() == null ? null : product.getPrices().toJSON();
		String cat1 = product.getCategory1();
		String cat2 = product.getCategory2();
		String cat3 = product.getCategory3();
		String primaryPic = product.getPrimaryImage();
		String secondaryPics = product.getSecondaryImages();
		String description = product.getDescription();
		Marketplace marketplace = product.getMarketplace();
		Integer stock = product.getStock();

		String marketplaceString = null;

		if ( marketplace != null && !marketplace.isEmpty() ) {
			marketplaceString = marketplace.toString();
		}


		// checking fields
		if((price == null || price.equals(0f)) && available) {
			Logging.printLogError(logger, session, "Erro tentando inserir leitura de produto dispon�vel mas com campo vazio: price");
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

			Map<Field<?>, Object> insertMapCrawler= new HashMap<>();

			insertMapCrawler.put(crawler.AVAILABLE, available);
			insertMapCrawler.put(crawler.MARKET, session.getMarket().getNumber());
			insertMapCrawler.put(crawler.INTERNAL_ID, internalId);
			insertMapCrawler.put(crawler.INTERNAL_PID, internalPid);
			insertMapCrawler.put(crawler.URL, url);
			insertMapCrawler.put(crawler.STOCK, stock);
			insertMapCrawler.put(crawler.NAME, name);
			insertMapCrawler.put(crawler.SECONDARY_PICS, secondaryPics);
			insertMapCrawler.put(crawler.CAT1, cat1);
			insertMapCrawler.put(crawler.CAT2, cat2);
			insertMapCrawler.put(crawler.CAT3, cat3);
			insertMapCrawler.put(crawler.PIC, primaryPic);

			if(price != null) {
				insertMapCrawler.put(crawler.PRICE, MathCommonsMethods.normalizeTwoDecimalPlaces(price.doubleValue()));
			}

			if(prices != null) {
				insertMapCrawler.put(crawler.PRICES, CONVERT_STRING_GSON.converter().from(prices.toString()));
			}

			if(description != null && !Jsoup.parse(description).text().replace("\n", "").replace(" ", "").trim().isEmpty()) {
				insertMapCrawler.put(crawler.DESCRIPTION, description);
			}

			if(marketplaceString != null) {
				insertMapCrawler.put(crawler.MARKETPLACE, marketplaceString);
			}

			// Create a fields and values of crawler_old
			CrawlerOld crawlerOld = Tables.CRAWLER_OLD;

			Map<Field<?>, Object> insertMapCrawlerOld = new HashMap<>();

			insertMapCrawlerOld.put(crawlerOld.AVAILABLE, available);
			insertMapCrawlerOld.put(crawlerOld.MARKET, session.getMarket().getNumber());
			insertMapCrawlerOld.put(crawlerOld.INTERNAL_ID, internalId);
			insertMapCrawlerOld.put(crawlerOld.INTERNAL_PID, internalPid);
			insertMapCrawlerOld.put(crawlerOld.URL, url);
			insertMapCrawlerOld.put(crawlerOld.STOCK, stock);
			insertMapCrawlerOld.put(crawlerOld.NAME, name);
			insertMapCrawlerOld.put(crawlerOld.SECONDARY_PICS, secondaryPics);
			insertMapCrawlerOld.put(crawlerOld.CAT1, cat1);
			insertMapCrawlerOld.put(crawlerOld.CAT2, cat2);
			insertMapCrawlerOld.put(crawlerOld.CAT3, cat3);
			insertMapCrawlerOld.put(crawlerOld.PIC, primaryPic);

			if(price != null) {
				insertMapCrawlerOld.put(crawlerOld.PRICE, MathCommonsMethods.normalizeTwoDecimalPlaces(price.doubleValue()));
			}

			if(prices != null) {
				insertMapCrawlerOld.put(crawlerOld.PRICES, CONVERT_STRING_GSON.converter().from(prices.toString()));
			}

			if(description != null && !Jsoup.parse(description).text().replace("\n", "").replace(" ", "").trim().isEmpty()) {
				insertMapCrawlerOld.put(crawlerOld.DESCRIPTION, description);
			}

			if(marketplaceString != null) {
				insertMapCrawlerOld.put(crawlerOld.MARKETPLACE, marketplaceString);
			}

			// List of tables for batch insert
			List<Table<?>> tables = new ArrayList<>();
			tables.add(crawler);
			tables.add(crawlerOld);

			// Map of Table - FieldsOfTable
			Map<Table<?>,Map<Field<?>, Object>> tablesMap = new HashMap<>();
			tablesMap.put(crawler, insertMapCrawler);
			tablesMap.put(crawlerOld, insertMapCrawlerOld);

			Main.dbManager.connectionPostgreSQL.runBatchInsertWithNTables(tables, tablesMap);

		} catch (Exception e) {
			Logging.printLogError(logger, session, "Error inserting product on database!");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			session.registerError( new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );
		}
	}


	public static void updateRating(RatingsReviews ratingReviews, Session session) {
		dbmodels.tables.Processed processedTable = Tables.PROCESSED;

		Map<Field<?>, Object> updateSets = new HashMap<>();

		if(ratingReviews != null){
			updateSets.put(processedTable.RATING, CONVERT_STRING_GSON.converter().from(ratingReviews.getJSON().toString()));
		} else {
			updateSets.put(processedTable.RATING, null);
		}

		List<Condition> conditions = new ArrayList<>();
		conditions.add(processedTable.ID.equal(((RatingReviewsCrawlerSession)session).getProcessedId()));

		try {
			Main.dbManager.connectionPostgreSQL.runUpdate(processedTable, updateSets, conditions);
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
	public static PersistenceResult persistProcessedProduct(Processed newProcessedProduct, Session session) {
		Logging.printLogDebug(logger, session, "Persisting processed product...");

		PersistenceResult persistenceResult = new ProcessedModelPersistenceResult();
		Long id;

		Prices prices = newProcessedProduct.getPrices();		

		dbmodels.tables.Processed processedTable = Tables.PROCESSED;

		try {
			if(newProcessedProduct.getId() == null) {

				Map<Field<?>, Object> insertMap = new HashMap<>();

				// 				 			 Column					Value
				insertMap.put(processedTable.INTERNAL_ID, 			newProcessedProduct.getInternalId());
				insertMap.put(processedTable.INTERNAL_PID, 			newProcessedProduct.getInternalPid());
				insertMap.put(processedTable.ORIGINAL_NAME, 		newProcessedProduct.getOriginalName());
				insertMap.put(processedTable.CLASS, 				newProcessedProduct.getProcessedClass());
				insertMap.put(processedTable.BRAND, 				newProcessedProduct.getBrand());
				insertMap.put(processedTable.RECIPIENT, 			newProcessedProduct.getRecipient());
				insertMap.put(processedTable.QUANTITY, 				newProcessedProduct.getQuantity());
				insertMap.put(processedTable.UNIT, 					newProcessedProduct.getUnit());
				insertMap.put(processedTable.EXTRA, 				newProcessedProduct.getExtra());
				insertMap.put(processedTable.PIC, 					newProcessedProduct.getPic());
				insertMap.put(processedTable.URL, 					newProcessedProduct.getUrl());
				insertMap.put(processedTable.MARKET, 				newProcessedProduct.getMarket());
				insertMap.put(processedTable.ECT, 					newProcessedProduct.getEct());
				insertMap.put(processedTable.LMT, 					newProcessedProduct.getLmt());
				insertMap.put(processedTable.LAT, 					newProcessedProduct.getLat());
				insertMap.put(processedTable.LRT, 					newProcessedProduct.getLrt());
				insertMap.put(processedTable.LMS, 					newProcessedProduct.getLms());
				insertMap.put(processedTable.STATUS, 				newProcessedProduct.getStatus());
				insertMap.put(processedTable.AVAILABLE, 			newProcessedProduct.getAvailable());
				insertMap.put(processedTable.VOID, 					newProcessedProduct.isVoid());
				insertMap.put(processedTable.CAT1, 					newProcessedProduct.getCat1());
				insertMap.put(processedTable.CAT2, 					newProcessedProduct.getCat2());
				insertMap.put(processedTable.CAT3, 					newProcessedProduct.getCat3());
				insertMap.put(processedTable.MULTIPLIER, 			newProcessedProduct.getMultiplier());
				insertMap.put(processedTable.ORIGINAL_DESCRIPTION, 	newProcessedProduct.getOriginalDescription());
				insertMap.put(processedTable.PRICE, 				newProcessedProduct.getPrice());
				insertMap.put(processedTable.STOCK, 				newProcessedProduct.getStock());
				insertMap.put(processedTable.SECONDARY_PICS,		newProcessedProduct.getSecondaryImages());

				if (prices != null) {
					insertMap.put(processedTable.PRICES, CONVERT_STRING_GSON.converter().from(prices.toJSON()));
				} else {
					insertMap.put(processedTable.PRICES, null);
				}

				if (newProcessedProduct.getChanges() != null) {
					insertMap.put(processedTable.CHANGES, newProcessedProduct.getChanges().toString());
				} else {
					insertMap.put(processedTable.CHANGES, null);
				}

				if (newProcessedProduct.getDigitalContent() != null) {
					insertMap.put(processedTable.DIGITAL_CONTENT, newProcessedProduct.getDigitalContent().toString());
				} else {
					insertMap.put(processedTable.DIGITAL_CONTENT, null);
				}

				if (newProcessedProduct.getMarketplace() != null && !newProcessedProduct.getMarketplace().isEmpty()) {
					insertMap.put(processedTable.MARKETPLACE, newProcessedProduct.getMarketplace().toString());
				} else {
					insertMap.put(processedTable.MARKETPLACE, null);
				}

				if (newProcessedProduct.getBehaviour() != null) {
					insertMap.put(processedTable.BEHAVIOUR, newProcessedProduct.getBehaviour().toString());
				} else {
					insertMap.put(processedTable.BEHAVIOUR, null);
				}

				if (newProcessedProduct.getSimilars() != null) {
					insertMap.put(processedTable.SIMILARS, newProcessedProduct.getSimilars().toString());
				} else {
					insertMap.put(processedTable.SIMILARS, null);
				}

				// get processeed id of new processed product
				Record recordId = Main.dbManager.connectionPostgreSQL.runInsertReturningID(processedTable, insertMap, processedTable.ID);

				if(recordId != null) {
					id = recordId.get(processedTable.ID);
				} else {
					id = (long) 0;
				}

				if (persistenceResult instanceof ProcessedModelPersistenceResult && id != 0) {
					((ProcessedModelPersistenceResult)persistenceResult).addCreatedId(id);
				}

			} else {
				Map<Field<?>, Object> updateMap = new HashMap<>();

				//			  				 Column					Value
				updateMap.put(processedTable.INTERNAL_ID, 			newProcessedProduct.getInternalId());
				updateMap.put(processedTable.INTERNAL_PID, 			newProcessedProduct.getInternalPid());
				updateMap.put(processedTable.ORIGINAL_NAME, 		newProcessedProduct.getOriginalName());
				updateMap.put(processedTable.CLASS, 				newProcessedProduct.getProcessedClass());
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
				updateMap.put(processedTable.VOID, 					newProcessedProduct.isVoid());
				updateMap.put(processedTable.CAT1, 					newProcessedProduct.getCat1());
				updateMap.put(processedTable.CAT2, 					newProcessedProduct.getCat2());
				updateMap.put(processedTable.CAT3, 					newProcessedProduct.getCat3());
				updateMap.put(processedTable.MULTIPLIER, 			newProcessedProduct.getMultiplier());
				updateMap.put(processedTable.ORIGINAL_DESCRIPTION, 	newProcessedProduct.getOriginalDescription());
				updateMap.put(processedTable.PRICE, 				newProcessedProduct.getPrice());
				updateMap.put(processedTable.STOCK, 				newProcessedProduct.getStock());
				updateMap.put(processedTable.SECONDARY_PICS, 		newProcessedProduct.getSecondaryImages());

				if (prices != null) {
					updateMap.put(processedTable.PRICES, CONVERT_STRING_GSON.converter().from(prices.toJSON()));
				} else {
					updateMap.put(processedTable.PRICES, null);
				}

				if (newProcessedProduct.getChanges() != null) {
					updateMap.put(processedTable.CHANGES, newProcessedProduct.getChanges().toString());
				} else {
					updateMap.put(processedTable.CHANGES, null);
				}

				if (newProcessedProduct.getDigitalContent() != null) {
					updateMap.put(processedTable.DIGITAL_CONTENT, newProcessedProduct.getDigitalContent().toString());
				} else {
					updateMap.put(processedTable.DIGITAL_CONTENT, null);
				}

				if (newProcessedProduct.getMarketplace() != null && !newProcessedProduct.getMarketplace().isEmpty()) {
					updateMap.put(processedTable.MARKETPLACE,  newProcessedProduct.getMarketplace().toString());
				} else {
					updateMap.put(processedTable.MARKETPLACE,  null);
				}

				if (newProcessedProduct.getBehaviour() != null) {
					updateMap.put(processedTable.BEHAVIOUR, newProcessedProduct.getBehaviour().toString());
				} else {
					updateMap.put(processedTable.BEHAVIOUR, null);
				}

				if (newProcessedProduct.getSimilars() != null) {
					updateMap.put(processedTable.SIMILARS, newProcessedProduct.getSimilars().toString());
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

				Main.dbManager.connectionPostgreSQL.runUpdate(processedTable, updateMap, conditions);
			}

			Logging.printLogDebug(logger, session, "Processed product persisted with success.");

		} catch (Exception e) {
			Logging.printLogError(logger, session, "Error updating processed product.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			session.registerError( new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );

			return null;
		}

		return persistenceResult;
	}
	/**
	 * Updates processed Behaviour on processed table.
	 * This method is used in active void to include the behavior of void status.
	 * 
	 * @param newBehaviour
	 * @param session
	 */
	public static void updateProcessedBehaviour(Behavior newBehaviour, Session session) {
		dbmodels.tables.Processed processedTable = Tables.PROCESSED;

		Map<Field<?>, Object> updateSets = new HashMap<>();

		if(newBehaviour != null){
			updateSets.put(processedTable.BEHAVIOUR, newBehaviour.toString());
		} else {
			updateSets.put(processedTable.BEHAVIOUR, null);
		}

		List<Condition> conditions = new ArrayList<>();
		conditions.add(processedTable.INTERNAL_ID.equal(session.getInternalId()));
		conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber()));

		try {
			Main.dbManager.connectionPostgreSQL.runUpdate(processedTable, updateSets, conditions);
			Logging.printLogDebug(logger, session, "Processed product behaviour updated with success.");

		} catch(Exception e) {
			Logging.printLogError(logger, session, "Error updating processed product behaviour.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			session.registerError( new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );
		}
	}

	/**
	 * Set void value of a processed model.
	 * This method sets the following values:
	 * <ul>
	 * <li>available = false</li>
	 * <li>status = "void"</li>
	 * <li>void = true</li>
	 * <li>marketplace = null</li>
	 * <li>price = null</li>
	 * <li>prices = new Prices() which is an empty prices model</li>
	 * </ul>
	 * @param processed
	 * @param voidValue A boolean indicating whether the processed product void must be set to true or false
	 * @param session
	 */
	public static void setProcessedVoidTrue(Session session) {
		dbmodels.tables.Processed processedTable = Tables.PROCESSED;

		Map<Field<?>, Object> updateSets = new HashMap<>();

		updateSets.put(processedTable.AVAILABLE, false);
		updateSets.put(processedTable.STATUS, "void");
		updateSets.put(processedTable.VOID, true);
		updateSets.put(processedTable.MARKETPLACE, null);
		updateSets.put(processedTable.PRICE, null);
		updateSets.put(processedTable.PRICES, CONVERT_STRING_GSON.converter().from(new Prices().toJSON()));

		List<Condition> conditions = new ArrayList<>();

		conditions.add(processedTable.INTERNAL_ID.equal(session.getInternalId()));
		conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber()));

		try {
			Main.dbManager.connectionPostgreSQL.runUpdate(processedTable, updateSets, conditions);
			Logging.printLogDebug(logger, session, "Processed product void value updated with success.");

		} catch(Exception e) {
			Logging.printLogError(logger, session, "Error updating processed product void.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			session.registerError( new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );
		}
	}

	/**
	 * Updates processed LastReadTime on processed table.
	 * 
	 * @param nowISO
	 * @param session
	 */
	public static void updateProcessedLRT(String nowISO, Session session) {
		dbmodels.tables.Processed processedTable = Tables.PROCESSED;

		Map<Field<?>, Object> updateSets = new HashMap<>();
		updateSets.put(processedTable.LRT, nowISO);

		List<Condition> conditions = new ArrayList<>();
		conditions.add(processedTable.INTERNAL_ID.equal(session.getInternalId()));
		conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber()));

		try {
			Main.dbManager.connectionPostgreSQL.runUpdate(processedTable, updateSets, conditions);
			Logging.printLogDebug(logger, session, "Processed product LRT updated with success.");

		} catch(Exception e) {
			Logging.printLogError(logger, session, "Error updating processed product LRT.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			session.registerError( new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );
		}
	}

	/**
	 * Updates processed LastModifiedTime on processed table.
	 * 
	 * @param nowISO
	 * @param session
	 */
	public static void updateProcessedLMT(String nowISO, Session session) {
		dbmodels.tables.Processed processedTable = Tables.PROCESSED;

		Map<Field<?>, Object> updateSets = new HashMap<>();
		updateSets.put(processedTable.LMT, nowISO);

		List<Condition> conditions = new ArrayList<>();
		conditions.add(processedTable.INTERNAL_ID.equal(session.getInternalId()));
		conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber()));

		try {
			Main.dbManager.connectionPostgreSQL.runUpdate(processedTable, updateSets, conditions);
			Logging.printLogDebug(logger, session, "Processed product LMT updated with success.");

		} catch(Exception e) {
			Logging.printLogError(logger, session, "Error updating processed product LMT.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

			session.registerError( new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );
		}

	}

	/**
	 * Updates processed LastModifiedStatus on processed table.
	 * 
	 * @param nowISO
	 * @param session
	 */
	public static void updateProcessedLMS(String nowISO, Session session) {
		dbmodels.tables.Processed processedTable = Tables.PROCESSED;

		Map<Field<?>, Object> updateSets = new HashMap<>();
		updateSets.put(processedTable.LMS, nowISO);

		List<Condition> conditions = new ArrayList<>();
		conditions.add(processedTable.INTERNAL_ID.equal(session.getInternalId()));
		conditions.add(processedTable.MARKET.equal(session.getMarket().getNumber()));

		try {
			Main.dbManager.connectionPostgreSQL.runUpdate(processedTable, updateSets, conditions);
			Logging.printLogDebug(logger, session, "Processed product LMS updated with success.");

		} catch(Exception e) {
			Logging.printLogError(logger, session, "Error updating processed product LMS.");
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
	public static void setTaskStatusOnMongo(String status, Session session, MongoDB panelDatabase) {
		try {
			if (panelDatabase != null) {
				String documentId = String.valueOf(session.getSessionId());
				panelDatabase.updateOne(
						new Document("_id", documentId),
						new Document("$set", new Document(MONGO_TASK_COLLECTION_STATUS_FIELD, status)),
						MONGO_TASKS_COLLECTION
						);
			} else {
				Logging.printLogError(logger, session, "Mongo database is null.");
			}
		} catch (Exception e) {
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
		}
	}

	/**
	 * 
	 * @param processedId
	 * @param session
	 * @param mongoDatabase
	 */
	public static void appendProcessedIdOnMongo(Long processedId, Session session, MongoDB panelDatabase) {
		try {
			if (panelDatabase != null) {
				String documentId = String.valueOf(session.getSessionId());

				Document search = new Document("_id", documentId);
				Document modification = new Document("$addToSet", new Document(MONGO_TASK_COLLECTION_FOUND_SKUS_FIELD, processedId));

				panelDatabase.updateOne(search, modification, MONGO_TASKS_COLLECTION);

				Logging.printLogDebug(logger, session, "Mongo task document updated with success!");
			}
		} catch (Exception e) {
			Logging.printLogError(logger, session, "Error updating collection on Mongo.");
			Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));

		}
	}

	/**
	 * 
	 * @param processedId
	 * @param session
	 * @param mongoDatabase
	 */
	public static void appendCreatedProcessedIdOnMongo(Long processedId, Session session, MongoDB panelDatabase) {
		try {
			if (panelDatabase != null) {
				String documentId = String.valueOf(session.getSessionId());

				Document search = new Document("_id", documentId);
				Document modification = new Document("$push", new Document(MONGO_TASK_COLLECTION_NEW_SKUS_FIELD, processedId));

				panelDatabase.updateOne(search, modification, MONGO_TASKS_COLLECTION);

				Logging.printLogDebug(logger, session, "Mongo task document updated with success!");
			}
		} catch (Exception e) {
			Logging.printLogError(logger, session, "Error updating collection on Mongo.");
			Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
		}
	}

	/**
	 * 
	 * @param session
	 * @param mongoDatabase
	 */
	public static void insertProcessedIdOnMongo(Session session, MongoDB panelDatabase) {
		try {
			if (panelDatabase != null) {
				String documentId = String.valueOf(session.getSessionId());

				// if processedId inside the session is null, it means
				// we are processing a task that was inserted manually
				// or a task from an URL scheduled by the webcrawler discover
				// in these two cases, the field processedId on Mongo must be null, because it can
				// get more than one product during extraction
				if (session.getProcessedId() == null) {
					panelDatabase.updateOne(
							new Document("_id", documentId),
							new Document("$set", new Document(MONGO_TASK_COLLECTION_PROCESSEDID_FIELD, null)),
							MONGO_TASKS_COLLECTION
							);
				}

				// in this case we are processing a task from insights queue
				else {
					panelDatabase.updateOne(
							new Document("_id", documentId),
							new Document("$set", new Document(MONGO_TASK_COLLECTION_PROCESSEDID_FIELD, session.getProcessedId())),
							MONGO_TASKS_COLLECTION
							);
				}
			} else {
				Logging.printLogError(logger, session, "MongoDatabase is null.");
			}
		} catch (Exception e) {
			Logging.printLogError(logger, session, "Error updating collection on Mongo.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
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
			processDirectory(m.getCity(), null, null);

			Logging.printLogInfo(logger, "Processing " + m.getCity() + " -> " + m.getName());
			processDirectory(m.getCity(), m.getName(), null);

			for(String folder: subdirectories) {
				processDirectory(m.getCity(), m.getName(), folder);
			}

		}
	}

	/**
	 * Directory creation.
	 * 
	 * @param city
	 * @param name
	 * @param folder
	 */
	private static void processDirectory(String city, String name, String folder) {
		File file;

		if (name == null) {
			file = new File(Main.executionParameters.getTmpImageFolder() + "/" + city);
		} else if (folder == null) {
			file = new File(Main.executionParameters.getTmpImageFolder() + "/" + city + "/" + name);
		} else {
			file = new File(Main.executionParameters.getTmpImageFolder() + "/" + city + "/" + name + "/" + folder);
		}

		if (!file.exists()) {
			boolean fileWasCreated = file.mkdir();

			if (fileWasCreated) {
				Logging.printLogInfo(logger, "Directory " + file.getAbsolutePath() + " created!");
			} else {
				Logging.printLogError(logger, "Failed to create " + file.getAbsolutePath() + " directory!");
			}

		} else {
			Logging.printLogDebug(logger, "Directory " + file.getAbsolutePath() + " already exists.");
		}
	}


	/*********************************Ranking*****************************************************/


	//busca dados no postgres
	public static CategoriesRanking fecthCategories(int id) {		
		try {
			CrawlerCategories crawlerCategories = Tables.CRAWLER_CATEGORIES;

			List<Field<?>> fields = new ArrayList<>();
			fields.add(crawlerCategories.CAT1);
			fields.add(crawlerCategories.CAT2);
			fields.add(crawlerCategories.CAT3);
			fields.add(crawlerCategories.URL);

			List<Condition> conditions = new ArrayList<>();
			conditions.add(crawlerCategories.ID.equal((long) id));

			Result<Record> results = Main.dbManager.connectionPostgreSQL.runSelect(crawlerCategories, fields, conditions);

			CategoriesRanking cat = new CategoriesRanking();

			for(Record record : results) {
				cat.setCat1(record.get(crawlerCategories.CAT1));
				cat.setCat2(record.get(crawlerCategories.CAT2));
				cat.setCat3(record.get(crawlerCategories.CAT3));
				cat.setUrl(record.get(crawlerCategories.URL));
			}

			return cat;

		} catch (Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

		return null;
	}

	//busca dados no postgres
	public static List<Long> fetchProcessedIdsWithInternalPid(String pid, int market) {
		List<Long> processedIds = new ArrayList<> ();

		try {
			dbmodels.tables.Processed processed = Tables.PROCESSED;

			List<Field<?>> fields = new ArrayList<>();
			fields.add(processed.ID);
			fields.add(processed.MASTER_ID);

			List<Condition> conditions = new ArrayList<>();
			conditions.add(processed.MARKET.equal(market));
			conditions.add(processed.INTERNAL_PID.equal(pid));

			Result<Record> results = Main.dbManager.connectionPostgreSQL.runSelect(processed, fields, conditions);

			for(Record record : results) {
				Long masterId = record.get(processed.MASTER_ID);
				
				if(masterId != null) {
					processedIds.add(record.get(processed.MASTER_ID));
				} else {
					processedIds.add(record.get(processed.ID));
				}
			}


		} catch (Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

		return processedIds;
	}



	public static List<Long> fetchProcessedIdsWithUrl(String url, int market) {
		List<Long> processedIds = new ArrayList<> ();

		try {
			dbmodels.tables.Processed processed = Tables.PROCESSED;

			List<Field<?>> fields = new ArrayList<>();
			fields.add(processed.ID);
			fields.add(processed.MASTER_ID);

			List<Condition> conditions = new ArrayList<>();
			conditions.add(processed.MARKET.equal(market));
			conditions.add(processed.URL.equal(url));

			Result<Record> results = Main.dbManager.connectionPostgreSQL.runSelect(processed, fields, conditions);

			for(Record record : results) {
				Long masterId = record.get(processed.MASTER_ID);
				
				if(masterId != null) {
					processedIds.add(record.get(processed.MASTER_ID));
				} else {
					processedIds.add(record.get(processed.ID));
				}
			}


		} catch (Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

		return processedIds;
	}

	//busca dados no postgres
	public static List<Long> fetchProcessedIdsWithInternalId(String id, int market) {
		List<Long> processedIds = new ArrayList<>();

		try {
			dbmodels.tables.Processed processed = Tables.PROCESSED;

			List<Field<?>> fields = new ArrayList<>();
			fields.add(processed.ID);
			fields.add(processed.MASTER_ID);

			List<Condition> conditions = new ArrayList<>();
			conditions.add(processed.MARKET.equal(market));
			conditions.add(processed.INTERNAL_ID.equal(id));

			Result<Record> results = Main.dbManager.connectionPostgreSQL.runSelect(processed, fields, conditions);

			for(Record record : results) {
				Long masterId = record.get(processed.MASTER_ID);
				
				if(masterId != null) {
					processedIds.add(record.get(processed.MASTER_ID));
				} else {
					processedIds.add(record.get(processed.ID));
				}
			}


		} catch (Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

		return processedIds;
	}

	public static void insertProductsRanking(Ranking ranking){
		try {
			List<Query> queries = new ArrayList<>();

			CrawlerRanking crawlerRanking = Tables.CRAWLER_RANKING;

			List<RankingProducts> products = ranking.getProducts();

			for(RankingProducts rankingProducts : products){	
				List<Long> processedIds = rankingProducts.getProcessedIds();

				for(Long processedId : processedIds){

					Map<Field<?>, Object> mapInsert = new HashMap<>();

					mapInsert.put(crawlerRanking.RANK_TYPE, 	ranking.getRankType());
					mapInsert.put(crawlerRanking.DATE, 			ranking.getDate());
					mapInsert.put(crawlerRanking.LOCATION, 		ranking.getLocation());
					mapInsert.put(crawlerRanking.POSITION, 		rankingProducts.getPosition());
					mapInsert.put(crawlerRanking.PAGE_SIZE, 	ranking.getStatistics().getPageSize());
					mapInsert.put(crawlerRanking.PROCESSED_ID, 	processedId);
					mapInsert.put(crawlerRanking.TOTAL_SEARCH, 	ranking.getStatistics().getTotalSearch());
					mapInsert.put(crawlerRanking.TOTAL_FETCHED, ranking.getStatistics().getTotalFetched());


					queries.add(Main.dbManager.connectionPostgreSQL.createQueryInsert(crawlerRanking, mapInsert));
				}
			}

			Main.dbManager.connectionPostgreSQL.runBatchInsert(queries);

			Logging.printLogDebug(logger, "Produtos cadastrados no postgres.");

		} catch(Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}


	/**
	 * Queries in database panel
	 */

	//insere dados do ranking no mongo
	public static void persistDiscoverStats(Ranking r) {
		SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd");

		//se não conseguir inserir tenta atualizar
		try{
			Document filter = new Document("location", r.getLocation()).append("market", r.getMarketId()).append("rank_type", 
					r.getRankType()).append("date", ft.format(new Date()));

			if(Main.dbManager.connectionFrozen.countFind(filter, MONGO_COLLECTION_DISCOVER_STATS) > 0) {

				Document update =  new Document("$set", new Document(r.getDocumentUpdate()));
				Main.dbManager.connectionFrozen.updateOne(filter, update, MONGO_COLLECTION_DISCOVER_STATS);
				Logging.printLogDebug(logger, "Dados atualizados com sucesso!");

			} else {

				Main.dbManager.connectionFrozen.insertOne(r.getDocument(),MONGO_COLLECTION_DISCOVER_STATS);
				Logging.printLogDebug(logger, "Dados cadastrados com sucesso!");

			}

		} catch(Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}

	//insere dados do categories no mongo
	public void insertPanelCategories(Categories cg) {
		Document categories = new Document();
		categories.put("market", cg.getMarket());
		categories.put("cat1", cg.getCat1());
		categories.put("cat1_name", cg.getCat1Name());

		if(cg.getCat2() != null) {
			categories.put("cat2", cg.getCat2());
			categories.put("cat2_name", cg.getCat2Name());

			if(cg.getCat3() != null) {
				categories.put("cat3", cg.getCat3());
				categories.put("cat3_name", cg.getCat3Name());
			}
		}

		categories.put("url", cg.getUrl());
		categories.put("ect", cg.getDataCreated());
		categories.put("lmt", cg.getDataUpdated());

		try{
			Main.dbManager.connectionPanel.insertOne(categories, MONGO_COLLECTION_CATEGORIES);
			Logging.printLogDebug(logger, "Dados cadastrados com sucesso!");
		} catch(Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}

	//insere dados do categories no mongo		
	public int updatePanelCategories(Categories cg)	{		
		Document filter = new Document("cat1", cg.getCat1()).append("cat2", cg.getCat2()).append("cat3", cg.getCat3()).append("market", cg.getMarket());

		String nowISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss");
		Document update =  new Document("$set", new Document("lmt", nowISO).append("url", cg.getUrl()));

		try {
			return (int) Main.dbManager.connectionPanel.updateMany(filter, update, MONGO_COLLECTION_CATEGORIES).getModifiedCount();
		} catch (Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

		return 0;
	}

	//insere as categorias no mongo
	public Set<CategoriesRanking> extractCategories(String id) {
		Set<CategoriesRanking> arrayCategories = new HashSet<> ();

		try {
			FindIterable<Document> iterable = Main.dbManager.connectionPanel.runFind(Filters.and(Filters.eq("_id", new ObjectId(id))), MONGO_COLLECTION_CATEGORIES);

			for(Document e: iterable){
				CategoriesRanking categories = new CategoriesRanking();

				categories.setCat1(e.getString("cat1"));
				categories.setCat2(e.getString("cat2"));
				categories.setCat3(e.getString("cat3"));
				categories.setUrl(e.getString("url"));

				arrayCategories.add(categories);
			}
		} catch (Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
		return arrayCategories;
	}

	//insere dados da task
	public static void insertPanelTask(String sessionId, String schedullerName, int marketID, String url, String location) {
		String nowISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss.SSS");

		Document taskDocument = new Document()
				.append("_id", sessionId)
				.append("scheduler", schedullerName)
				.append("date", nowISO)
				.append("status", "queued")
				.append("url", url)
				.append("market", marketID)
				.append("location", location)
				//.append("processed_id", null)
				.append("found_skus", new ArrayList<String>());

		try{
			Main.dbManager.connectionPanel.insertOne(taskDocument, MONGO_COLLECTION_TASK);
		} catch(Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}
}
