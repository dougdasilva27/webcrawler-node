package br.com.lett.crawlernode.database;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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

import org.bson.Document;

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

public class Persistence {
	private static final Logger logger = LoggerFactory.getLogger(Persistence.class);

	public static final String MONGO_TASKS_COLLECTION = "Task";

	public static final String MONGO_TASK_COLLECTION_PROCESSEDID_FIELD 	= "processed_id";
	public static final String MONGO_TASK_COLLECTION_FOUND_SKUS_FIELD 	= "found_skus";
	public static final String MONGO_TASK_COLLECTION_NEW_SKUS_FIELD		= "new_skus";
	public static final String MONGO_TASK_COLLECTION_STATUS_FIELD 		= "status";

	public static final String MONGO_TASK_STATUS_DONE 	= "done";
	public static final String MONGO_TASK_STATUS_FAILED = "failed";

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
		String internal_id = product.getInternalId();
		String internal_pid = product.getInternalPid();
		String name = product.getName();
		Float price = product.getPrice();
		JSONObject prices = (product.getPrices() == null ? null : product.getPrices().getPricesJson());
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
			Logging.printLogError(logger, session, "Erro tentando inserir leitura de produto disponível mas com campo vazio: price");
			return;
		} else if(internal_id == null || internal_id.isEmpty()) {
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

			// Montando string de lista de campos da query
			StringBuilder fields = new StringBuilder();
			fields.append("available");
			fields.append(", market");
			fields.append(", internal_id");			
			fields.append(", internal_pid"); 
			fields.append(", url");
			fields.append(", price");
			fields.append(", prices");
			fields.append(", stock");
			fields.append(", name");
			fields.append(", pic");
			fields.append(", secondary_pics");
			fields.append(", cat1");
			fields.append(", cat2");
			fields.append(", cat3");

			// Montando string de lista de valores da query
			String values = "";

			values += available;
			values += ", " + session.getMarket().getNumber();
			values += ", '" + internal_id + "'"; 
			values += (internal_pid == null ? ", NULL" : ", '" + internal_pid + "'");
			values += ", '" + url + "'";			
			values += ", " + price;
			values += (prices == null ? ", NULL" : ", " + "'" + prices.toString() + "'" + "::json");
			values += (stock == null ? ", NULL" : ", " + stock);
			values += ", '" + name + "'";
			values += (foto == null ? ", NULL" : ", '" + foto + "'");
			values += (secondary_pics == null ? ", NULL" : ", '" + secondary_pics + "'");
			values += (cat1 == null ? ", NULL" : ", '" + cat1 + "'");
			values += (cat2 == null ? ", NULL" : ", '" + cat2 + "'");
			values += (cat3 == null ? ", NULL" : ", '" + cat3 + "'");

			if(description != null && !Jsoup.parse(description).text().replace("\n", "").replace(" ", "").trim().isEmpty()) {
				fields.append(", description");
				values = values + ", '" + description + "'";
			}

			if(marketplace_string != null) {
				fields.append(", marketplace");
				values = values + ", '" + marketplace_string + "'";
			}

			// store data on crawler and crawler_old tables
			StringBuilder sqlExecuteCrawler = new StringBuilder();

			sqlExecuteCrawler.append("INSERT INTO crawler ");
			sqlExecuteCrawler.append("( ");
			sqlExecuteCrawler.append(fields.toString());
			sqlExecuteCrawler.append(") ");

			sqlExecuteCrawler.append("VALUES ");
			sqlExecuteCrawler.append("( ");
			sqlExecuteCrawler.append(values);
			sqlExecuteCrawler.append("); ");
			
			sqlExecuteCrawler.append("INSERT INTO crawler_old ");
			sqlExecuteCrawler.append("( ");
			sqlExecuteCrawler.append(fields.toString());
			sqlExecuteCrawler.append(") ");

			sqlExecuteCrawler.append("VALUES ");
			sqlExecuteCrawler.append("(");
			sqlExecuteCrawler.append(values);
			sqlExecuteCrawler.append(");");

			Main.dbManager.runSqlExecute(sqlExecuteCrawler.toString());

			Logging.printLogDebug(logger, session, "Crawled product persisted with success.");

		} catch (SQLException e) {
			Logging.printLogError(logger, session, "Error inserting product on database!");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
			
			session.registerError( new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );
		}
	}
	
	public static void updateRating(RatingsReviews ratingReviews, Session session) {
		String query = "UPDATE processed SET "
				+ "rating=" + (ratingReviews == null ? "NULL" : "'" + ratingReviews.getJSON().toString() + "'" + "::json") + " "
				+ "WHERE id = " + ((RatingReviewsCrawlerSession)session).getProcessedId();
		
		try {
			Main.dbManager.runSqlExecute(query);
			Logging.printLogDebug(logger, session, "Processed product rating updated with success.");

		} catch(SQLException e) {
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
		Long id = null;

		String query = "";
		
		JSONObject prices = (newProcessedProduct.getPrices() == null ? null : newProcessedProduct.getPrices().getPricesJson());

		if(newProcessedProduct.getId() == null) {
			query = "INSERT INTO processed("
					+ "internal_id, internal_pid, original_name, class, brand, recipient, quantity,"
					+ "unit, extra, pic, url, market, ect, lmt, lat, lrt, lms, status, available, void, cat1, cat2, cat3, "
					+ "multiplier, original_description, price, prices, stock, secondary_pics, changes, digital_content, marketplace, behaviour, similars)"
					+ "VALUES ("
					+ "'" + newProcessedProduct.getInternalId() + "', "
					+ (newProcessedProduct.getInternalPid() 		== null ? "NULL" : "'" + newProcessedProduct.getInternalPid()  + "'") + ", "
					+ "'" + newProcessedProduct.getOriginalName() + "', "
					+ (newProcessedProduct.get_class() 				== null ? "null" : "'" + newProcessedProduct.get_class()  + "'" ) + ", "
					+ (newProcessedProduct.getBrand() 				== null ? "null" : "'" + newProcessedProduct.getBrand()  + "'" ) + ", "
					+ (newProcessedProduct.getRecipient() 			== null ? "null" : "'" + newProcessedProduct.getRecipient()  + "'" ) + ", "
					+ (newProcessedProduct.getQuantity() 			== null ? "null" : newProcessedProduct.getQuantity() ) + ", "
					+ (newProcessedProduct.getUnit() 				== null ? "null" : "'" + newProcessedProduct.getUnit()  + "'" ) + ", "
					+ (newProcessedProduct.getExtra() 				== null ? "null" : "'" + newProcessedProduct.getExtra()  + "'" ) + ", "
					+ (newProcessedProduct.getPic() 				== null ? "null" : "'" + newProcessedProduct.getPic()  + "'" ) + ", "
					+ "'" + newProcessedProduct.getUrl() + "', "
					+ newProcessedProduct.getMarket() + ", "
					+ "'" + newProcessedProduct.getEct() + "', "
					+ "'" + newProcessedProduct.getLmt() + "', "
					+ (newProcessedProduct.getLat() == null ? "null" : "'" + newProcessedProduct.getLat()  + "'" ) + ", "
					+ "'" + newProcessedProduct.getLrt() + "', "
					+ "'" + newProcessedProduct.getLms() + "', "
					+ (newProcessedProduct.getStatus() == null ? "null" : "'" + newProcessedProduct.getStatus()  + "'" ) + ", "
					+ newProcessedProduct.getAvailable() + ", "
					+ newProcessedProduct.getVoid() + ", "
					+ (newProcessedProduct.getCat1() == null ? "null" : "'" + newProcessedProduct.getCat1()  + "'" ) + ", "
					+ (newProcessedProduct.getCat2() == null ? "null" : "'" + newProcessedProduct.getCat2()  + "'" ) + ", "
					+ (newProcessedProduct.getCat3() == null ? "null" : "'" + newProcessedProduct.getCat3()  + "'" ) + ", "
					+ (newProcessedProduct.getMultiplier() == null ? "null" : newProcessedProduct.getMultiplier() ) + ", "
					+ (newProcessedProduct.getOriginalDescription() == null ? "null" : "'" + newProcessedProduct.getOriginalDescription()  + "'" ) + ", "
					+ newProcessedProduct.getPrice() + ", "
					+ (prices == null ? "NULL" : "'" + prices.toString() + "'" + "::json") + ", "
					+ newProcessedProduct.getStock() + ", "
					+ (newProcessedProduct.getSecondary_pics() == null ? "null" : "'" + newProcessedProduct.getSecondary_pics()  + "'" ) + ", "
					+ (newProcessedProduct.getChanges() == null ? "null" : "'" + newProcessedProduct.getChanges().toString().replace("'","''")  + "'" ) + ", "
					+ ((newProcessedProduct.getDigitalContent() == null || newProcessedProduct.getDigitalContent().length() == 0) ? "null" : "'" + newProcessedProduct.getDigitalContent().toString().replace("'","''")  + "'" ) + ", "
					+ ((newProcessedProduct.getMarketplace() == null || newProcessedProduct.getMarketplace().length() == 0) ? "null" : "'" + newProcessedProduct.getMarketplace().toString().replace("'","''")  + "'" ) + ", "
					+ ((newProcessedProduct.getBehaviour() == null || newProcessedProduct.getBehaviour().length() == 0) ? "null" : "'" + newProcessedProduct.getBehaviour().toString().replace("'","''")  + "'" ) + ", "
					+ ((newProcessedProduct.getSimilars() == null || newProcessedProduct.getSimilars().length() == 0) ? "null" : "'" + newProcessedProduct.getSimilars().toString().replace("'","''")  + "'" ) + " "
					+ ") RETURNING id";
		} else {

			query = "UPDATE processed SET "
					+ "internal_id=" 		+ "'" + newProcessedProduct.getInternalId() + "', "
					+ "internal_pid=" 		+ (newProcessedProduct.getInternalPid() == null ? "NULL" : "'" + newProcessedProduct.getInternalPid()  + "'") + ", "
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
					+ "void=" 		+ newProcessedProduct.getVoid() + ", "
					+ "cat1=" 		+ (newProcessedProduct.getCat1() == null ? "null" : "'" + newProcessedProduct.getCat1()  + "'" ) + ", "
					+ "cat2=" 		+ (newProcessedProduct.getCat2() == null ? "null" : "'" + newProcessedProduct.getCat2()  + "'" ) + ", "
					+ "cat3=" 		+ (newProcessedProduct.getCat3() == null ? "null" : "'" + newProcessedProduct.getCat3()  + "'" ) + ", "
					+ "multiplier=" 		+ (newProcessedProduct.getMultiplier() == null ? "null" : newProcessedProduct.getMultiplier() ) + ", "
					+ "original_description=" 		+ (newProcessedProduct.getOriginalDescription() == null ? "null" : "'" + newProcessedProduct.getOriginalDescription()  + "'" ) + ", "
					+ "price=" 		+ newProcessedProduct.getPrice() + ", "
					+ "prices=" + (prices == null ? "NULL" : "'" + prices.toString() + "'" + "::json") + ", "
					+ "stock=" 		+ newProcessedProduct.getStock() + ", "
					+ "secondary_pics=" 		+ (newProcessedProduct.getSecondary_pics() == null ? "null" : "'" + newProcessedProduct.getSecondary_pics()  + "'" ) + ", "
					+ "changes=" 		+ (newProcessedProduct.getChanges() == null ? "null" : "'" + newProcessedProduct.getChanges().toString().replace("'","''")  + "'" ) + ", "
					+ "digital_content=" 		+ ((newProcessedProduct.getDigitalContent() == null || newProcessedProduct.getDigitalContent().length() == 0) ? "null" : "'" + newProcessedProduct.getDigitalContent().toString().replace("'","''")  + "'" ) + ", "
					+ "marketplace=" 	+ ((newProcessedProduct.getMarketplace() == null || newProcessedProduct.getMarketplace().length() == 0) ? "null" : "'" + newProcessedProduct.getMarketplace().toString().replace("'","''")  + "'" ) + ", "
					+ "behaviour=" 		+ ((newProcessedProduct.getBehaviour() == null || newProcessedProduct.getBehaviour().length() == 0) ? "null" : "'" + newProcessedProduct.getBehaviour().toString().replace("'","''")  + "'" ) + ", "
					+ "similars=" 		+ ((newProcessedProduct.getSimilars() == null || newProcessedProduct.getSimilars().length() == 0) ? "null" : "'" + newProcessedProduct.getSimilars().toString().replace("'","''")  + "'" ) + " "
					+ "WHERE id = " + newProcessedProduct.getId();

			// get the id of the processed product that already exists
			id = newProcessedProduct.getId();
			
			if (persistenceResult instanceof ProcessedModelPersistenceResult) {
				((ProcessedModelPersistenceResult)persistenceResult).addModifiedId(id);
			}
		}

		try {
			
			if (id == null) { //  a new processed was created
				ResultSet generatedKeys = Main.dbManager.runSqlConsult(query);
				if (generatedKeys.next()) {
					id = generatedKeys.getLong(1);
					if (persistenceResult instanceof ProcessedModelPersistenceResult) {
						((ProcessedModelPersistenceResult)persistenceResult).addCreatedId(id);
					}
				}
			}
			else { // the processed already exists
				Main.dbManager.runSqlExecute(query);
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
		StringBuilder query = new StringBuilder();

		query.append("UPDATE processed SET void=true, ");
		query.append("available=false, ");
		query.append("status=" + "'void', ");
		query.append("marketplace=NULL, ");
		query.append("price=NULL ");
		query.append("WHERE internal_id=" + "'" + session.getInternalId() + "'" + " ");
		query.append("AND ");
		query.append("market=" + session.getMarket().getNumber());

		try {
			Main.dbManager.runSqlExecute(query.toString());
			Logging.printLogDebug(logger, session, "Processed product void value updated with success.");

		} catch(SQLException e) {
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
		StringBuilder query = new StringBuilder();

		query.append("UPDATE processed set lrt=" + "'" + nowISO + "'" + " ");
		query.append("WHERE internal_id=" + "'" + session.getInternalId() + "'" + " ");
		query.append("AND ");
		query.append("market=" + session.getMarket());

		try {
			Main.dbManager.runSqlExecute(query.toString());
			Logging.printLogDebug(logger, session, "Processed product LRT updated with success.");

		} catch(SQLException e) {
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
		StringBuilder query = new StringBuilder();

		query.append("UPDATE processed set lmt=" + "'" + nowISO + "'" + " ");
		query.append("WHERE internal_id=" + "'" + session.getInternalId() + "'" + " ");
		query.append("AND ");
		query.append("market=" + session.getMarket());

		try {
			Main.dbManager.runSqlExecute(query.toString());
			Logging.printLogDebug(logger, session, "Processed product LMT updated with success.");

		} catch(SQLException e) {
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

			File file = new File(Main.executionParameters.getTmpImageFolder() + "/" + m.getCity());
			if (!file.exists()) {
				if (file.mkdir()) {
					Logging.printLogInfo(logger, "Directory " + file.getAbsolutePath() + " created!");
				} else {
					Logging.printLogError(logger, "Failed to create " + file.getAbsolutePath() + " directory!");
				}
			} else {
				Logging.printLogDebug(logger, "Directory " + file.getAbsolutePath() + " was already created...");
			}


			Logging.printLogInfo(logger, "Processing " + m.getCity() + " -> " + m.getName());
			file = new File(Main.executionParameters.getTmpImageFolder() + "/" + m.getCity() + "/" + m.getName());
			if (!file.exists()) {
				if (file.mkdir()) {
					Logging.printLogInfo(logger, "Directory " + file.getAbsolutePath() + " created!");
				} else {
					Logging.printLogError(logger, "Failed to create " + file.getAbsolutePath() + " directory!");
				}
			} else {
				Logging.printLogDebug(logger, "Directory " + file.getAbsolutePath() + " was already created...");
			}

			for(String folder: subdirectories) {

				file = new File(Main.executionParameters.getTmpImageFolder() + "/" + m.getCity() + "/" + m.getName() + "/" + folder);
				if (!file.exists()) {
					if (file.mkdir()) {
						Logging.printLogInfo(logger, "Directory " + file.getAbsolutePath() + " created!");
					} else {
						Logging.printLogError(logger, "Failed to create " + file.getAbsolutePath() + " directory!");
					}
				} else {
					Logging.printLogDebug(logger, "Directory " + file.getAbsolutePath() + " was already created...");
				}
			}



		}

	}

}
