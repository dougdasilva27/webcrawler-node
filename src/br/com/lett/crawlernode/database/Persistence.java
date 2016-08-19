package br.com.lett.crawlernode.database;

import java.sql.SQLException;

import org.json.JSONArray;
import org.jsoup.Jsoup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;

import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.processor.models.ProcessedModel;
import br.com.lett.crawlernode.util.Logging;

public class Persistence {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

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
			
			Logging.printLogDebug(logger, session, "Crawled product persisted with success.");

		} catch (SQLException e) {
			Logging.printLogError(logger, session, "Error inserting product on database! [" + e.getMessage() + "]");
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

		try {
			Main.dbManager.runSqlExecute(query);
			Logging.printLogDebug(logger, session, "Processed product persisted with success.");
			
		} catch (SQLException e) {
			Logging.printLogError(logger, session, "Error updating processed product " + "[seedId: " + session.getSeedId() + "]");
			Logging.printLogError(logger, session, e.getMessage());
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
