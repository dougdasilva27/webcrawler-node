package br.com.lett.crawlernode.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import dbmodels.Tables;
import dbmodels.tables.CrawlerRanking;

public class DatabaseDataFetcher {
	
	private static final Logger logger = LoggerFactory.getLogger(DatabaseDataFetcher.class);

	private DatabaseManager databaseManager;

	public DatabaseDataFetcher(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
	}
	
	/**
	 * Fetch the desired market from the database.
	 * @param marketCity
	 * @param marketName
	 * @return
	 */
	public Market fetchMarket(String marketCity, String marketName) {
		try {

			dbmodels.tables.Market marketTable = Tables.MARKET;
			
			List<Field<?>> fields = new ArrayList<>();
			fields.add(marketTable.ID);
			fields.add(marketTable.CITY);
			fields.add(marketTable.NAME);
			fields.add(marketTable.PROXIES);
			fields.add(marketTable.PROXIES_IMAGES);
			
			List<Condition> conditions = new ArrayList<>();
			conditions.add(marketTable.NAME.equal(marketName).and(marketTable.CITY.equal(marketCity)));

			ResultSet rs = databaseManager.connectionPostgreSQL.runSelectReturningResultSet(marketTable, fields, conditions);
			
			if(rs.next()) {
				
				// get the proxies used in this market
				ArrayList<String> proxies = new ArrayList<>();
				JSONArray proxiesJSONArray = new JSONArray(rs.getString("proxies"));
				for (int i = 0; i < proxiesJSONArray.length(); i++) {
					proxies.add( proxiesJSONArray.getString(i) );
				}
				
				// get the proxies used for images download in this market
				ArrayList<String> imageProxies = new ArrayList<>();
				JSONArray imageProxiesJSONArray = new JSONArray(rs.getString("proxies_images"));
				for (int i = 0; i < imageProxiesJSONArray.length(); i++) {
					imageProxies.add( imageProxiesJSONArray.getString(i) );
				}
				
				// create market
				return new Market(
						rs.getInt("id"), 
						rs.getString("city"), 
						rs.getString("name"),
						proxies,
						imageProxies);
			
			}

		} catch (SQLException e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
		return null;
	}
	
	/**
	 * Return all processeds from crawler_ranking
	 * for this location
	 * and market
	 * from yesterday
	 * 
	 * @param location
	 * @param market
	 * @param date
	 * @return
	 */
	public static List<Long> fetchProcessedsFromCrawlerRanking(String location, int market, Date date) {
		List<Long> processeds = new ArrayList<>();
		
		try {
			List<Long> allProcessedsThisMarket = new ArrayList<>();
			
			List<Field<?>> fieldsP = new ArrayList<>();
			fieldsP.add(Tables.PROCESSED.ID);
			
			List<Condition> conditionsP = new ArrayList<>();
			conditionsP.add(Tables.PROCESSED.MARKET.equal(market));
			
			Result<Record> products = Main.dbManager.connectionPostgreSQL.runSelect(Tables.PROCESSED, fieldsP, conditionsP);
			
			for(Record r : products) {
				allProcessedsThisMarket.add(r.getValue(Tables.PROCESSED.ID));
			}
			
			
			CrawlerRanking ranking = Tables.CRAWLER_RANKING;
			
			List<Field<?>> fields = new ArrayList<>();
			fields.add(ranking.PROCESSED_ID);
			
		    Timestamp timestamp = new java.sql.Timestamp(date.getTime());
			
			List<Condition> conditions = new ArrayList<>();
			conditions.add(ranking.LOCATION.equal(location));
			conditions.add(ranking.PROCESSED_ID.in(allProcessedsThisMarket));
			conditions.add(ranking.DATE.between(timestamp, new Timestamp(timestamp.getTime() - (24*60*60*1000))));
			
			Result<Record> records = Main.dbManager.connectionPostgreSQL.runSelect(ranking, fields, conditions);
			
			for(Record r : records) {
				processeds.add(r.getValue(ranking.PROCESSED_ID));
			}
			
		} catch(Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTrace(e));
		}
		
		return processeds;
	}
}
