package br.com.lett.crawlernode.database;

import java.util.ArrayList;
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

			Result<Record> records = (Result<Record>) databaseManager.connectionPostgreSQL.runSelect(marketTable, fields, conditions);
			
			for(Record r : records) {
				// get the proxies used in this market
				ArrayList<String> proxies = new ArrayList<>();
				JSONArray proxiesJSONArray = new JSONArray(r.getValue(marketTable.PROXIES));
				for (int i = 0; i < proxiesJSONArray.length(); i++) {
					proxies.add( proxiesJSONArray.getString(i) );
				}
				
				// get the proxies used for images download in this market
				ArrayList<String> imageProxies = new ArrayList<>();
				JSONArray imageProxiesJSONArray = new JSONArray(r.getValue(marketTable.PROXIES_IMAGES));
				for (int i = 0; i < imageProxiesJSONArray.length(); i++) {
					imageProxies.add( imageProxiesJSONArray.getString(i) );
				}
				
				// create market
				return new Market(
						r.getValue(marketTable.ID).intValue(), 
						r.getValue(marketTable.CITY), 
						r.getValue(marketTable.NAME),
						proxies,
						imageProxies);
			}

		} catch (Exception e) {
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
	@SuppressWarnings("unchecked")
	public static List<Long> fetchProcessedsFromCrawlerRanking(String location, int market, String today, String yesterday) {
		List<Long> processeds = new ArrayList<>();
		
		try {
			String sql = "SELECT processed_id FROM crawler_ranking WHERE location = '"+ location +"' AND "
					+ "processed_id IN (SELECT id FROM processed WHERE market = "+ market +") "
					+ "AND date BETWEEN '"+ yesterday +"' AND '"+ today +"'";
			
			Result<Record> records = (Result<Record>) Main.dbManager.connectionPostgreSQL.runSqlSelectJooq(sql);
			
			for(Record r : records) {
				processeds.add((Long) r.getValue("processed_id"));
			}
			
		} catch(Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTrace(e));
		}
		
		return processeds;
	}
}
