package br.com.lett.crawlernode.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.jsoup.Jsoup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;

import br.com.lett.crawlernode.kernel.models.Market;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.processor.models.ProcessedModel;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class Persistence {
	private static final Logger logger = LoggerFactory.getLogger(Persistence.class);

	public static final String MONGO_TASKS_COLLECTION = "Task";
	public static final String MONGO_TASK_COLLECTION_INTERNALID_FIELD = "internal_id";
	public static final String MONGO_TASK_COLLECTION_FOUND_SKUS_FIELD = "found_skus";
	public static final String MONGO_TASK_COLLECTION_STATUS_FIELD = "status";
	public static final String MONGO_TASK_STATUS_DONE = "done";
	public static final String MONGO_TASK_STATUS_FAILED = "failed";

	/**
	 * Persist the product crawled informations on tables crawler and crawler_old
	 * 
	 * @param product
	 * @param session
	 */
	public static void persistProduct(Product product, CrawlerSession session) {
		Logging.printLogDebug(logger, session, "Persisting crawled product...");

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

			// store data on crawler and crawler_old tables
			StringBuilder sqlExecuteCrawler = new StringBuilder();

			sqlExecuteCrawler.append("INSERT INTO crawler ");
			sqlExecuteCrawler.append("( ");
			sqlExecuteCrawler.append(listOfFields);
			sqlExecuteCrawler.append(") ");

			sqlExecuteCrawler.append("VALUES ");
			sqlExecuteCrawler.append("( ");
			sqlExecuteCrawler.append(values);
			sqlExecuteCrawler.append("); ");

			sqlExecuteCrawler.append("INSERT INTO crawler_old ");
			sqlExecuteCrawler.append("( ");
			sqlExecuteCrawler.append(listOfFields);
			sqlExecuteCrawler.append(") ");

			sqlExecuteCrawler.append("VALUES ");
			sqlExecuteCrawler.append("(");
			sqlExecuteCrawler.append(values);
			sqlExecuteCrawler.append(");");

			if (session.getType().equals(CrawlerSession.TEST_TYPE)) {
				br.com.lett.crawlernode.test.Tester.dbManager.runSqlExecute(sqlExecuteCrawler.toString());
			} else {
				Main.dbManager.runSqlExecute(sqlExecuteCrawler.toString());
			}

			Logging.printLogDebug(logger, session, "Crawled product persisted with success.");

		} catch (SQLException e) {
			Logging.printLogError(logger, session, "Error inserting product on database!");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}

	public static void persistProcessedProduct(ProcessedModel newProcessedProduct, CrawlerSession session) {
		Logging.printLogDebug(logger, session, "Persisting processed product...");

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
					+ newProcessedProduct.getVoid() + ", "
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
					+ "void=" 		+ newProcessedProduct.getVoid() + ", "
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

		try {
			if (session.getType().equals(CrawlerSession.TEST_TYPE)) {
				br.com.lett.crawlernode.test.Tester.dbManager.runSqlExecute(query);
			} else {
				Main.dbManager.runSqlExecute(query);
			}
			Logging.printLogDebug(logger, session, "Processed product persisted with success.");

		} catch (SQLException e) {
			Logging.printLogError(logger, session, "Error updating processed product " + "[seedId: " + session.getSeedId() + "]");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
		}

	}

	/**
	 * Set void value of a processed model.
	 * @param processed
	 * @param voidValue A boolean indicating whether the processed product void must be set to true or false
	 * @param session
	 */
	public static void setProcessedVoidTrue(ProcessedModel processed, CrawlerSession session) {
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
			if (session.getType().equals(CrawlerSession.TEST_TYPE)) {
				br.com.lett.crawlernode.test.Tester.dbManager.runSqlExecute(query.toString());
			} else {
				Main.dbManager.runSqlExecute(query.toString());
			}
			Logging.printLogDebug(logger, session, "Processed product void value updated with success.");
		} catch(SQLException e) {
			Logging.printLogError(logger, session, "Error updating processed product void.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
		}
	}

	/**
	 * Updates processed LastReadTime on processed table.
	 * @param processed
	 * @param nowISO
	 * @param session
	 */
	public static void updateProcessedLRT(ProcessedModel processed, String nowISO, CrawlerSession session) {
		StringBuilder query = new StringBuilder();

		query.append("UPDATE processed set lrt=" + "'" + nowISO + "'" + " ");
		query.append("WHERE internal_id=" + "'" + session.getInternalId() + "'" + " ");
		query.append("AND ");
		query.append("market=" + session.getMarket());

		try {
			if (session.getType().equals(CrawlerSession.TEST_TYPE)) {
				br.com.lett.crawlernode.test.Tester.dbManager.runSqlExecute(query.toString());
			} else {
				Main.dbManager.runSqlExecute(query.toString());
			}
			Logging.printLogDebug(logger, session, "Processed product LRT updated with success.");
		} catch(SQLException e) {
			Logging.printLogError(logger, session, "Error updating processed product LRT.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
		}
	}

	/**
	 * Updates processed LastModifiedTime on processed table.
	 * @param processed
	 * @param nowISO
	 * @param session
	 */
	public static void updateProcessedLMT(ProcessedModel processed, String nowISO, CrawlerSession session) {
		StringBuilder query = new StringBuilder();

		query.append("UPDATE processed set lmt=" + "'" + nowISO + "'" + " ");
		query.append("WHERE internal_id=" + "'" + session.getInternalId() + "'" + " ");
		query.append("AND ");
		query.append("market=" + session.getMarket());

		try {
			if (session.getType().equals(CrawlerSession.TEST_TYPE)) {
				br.com.lett.crawlernode.test.Tester.dbManager.runSqlExecute(query.toString());
			} else {
				Main.dbManager.runSqlExecute(query.toString());
			}
			Logging.printLogDebug(logger, session, "Processed product LMT updated with success.");
		} catch(SQLException e) {
			Logging.printLogError(logger, session, "Error updating processed product LMT.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
		}

	}

	/**
	 * Insert the processedId on the task collection.
	 * @param session
	 * @param processedId
	 * @param taskCollection
	 */
	public static void insertProcessedId(CrawlerSession session, Long processedId, MongoCollection<Document> taskCollection) {
		String documentId =  String.valueOf(session.getSessionId());

		if (processedId == null) {
			taskCollection.updateOne(
					new Document("_id", documentId), 
					new Document("$set", new Document(MONGO_TASK_COLLECTION_INTERNALID_FIELD, null))
					);
		} else {
			taskCollection.updateOne(
					new Document("_id", documentId), 
					new Document("$set", new Document(MONGO_TASK_COLLECTION_INTERNALID_FIELD, String.valueOf(processedId)))
					);
		}
	}

	/**
	 * Set the status field of the task document to "done"
	 * @param session
	 * @param taskCollection
	 */
	public static void setTaskStatusOnMongo(String status, CrawlerSession session, MongoDatabase mongoDatabase) {
		if (mongoDatabase != null) {
			MongoCollection<Document> taskCollection = mongoDatabase.getCollection(MONGO_TASKS_COLLECTION);
			String documentId =  String.valueOf(session.getSessionId());
			taskCollection.updateOne(
					new Document("_id", documentId), 
					new Document("$set", new Document(MONGO_TASK_COLLECTION_STATUS_FIELD, status))
					);
		} else {
			Logging.printLogError(logger, session, "Mongo database is null");
		}
	}

	private static String sanitizeBeforePersist(String field) {
		if(field == null) {
			return null;
		} else {
			return field.replace("'", "''").trim();
		}
	}

}
