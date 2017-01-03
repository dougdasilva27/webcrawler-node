package br.com.lett.crawlernode.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jooq.Condition;
import org.jooq.Field;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.models.Market;
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
//			StringBuilder stringBuilder = new StringBuilder();
//
//			stringBuilder.append("SELECT id, city, name, proxies, proxies_images FROM market WHERE ");
//			stringBuilder.append("name=" + "'" + marketName + "'" + " AND ");
//			stringBuilder.append("city=" + "'" + marketCity + "'");
			
			dbmodels.tables.Market marketTable = Tables.MARKET;
			
			List<Field<?>> fields = new ArrayList<>();
			fields.add(marketTable.ID);
			fields.add(marketTable.CITY);
			fields.add(marketTable.NAME);
			fields.add(marketTable.PROXIES);
			fields.add(marketTable.PROXIES_IMAGES);
			
			List<Condition> conditions = new ArrayList<>();
			conditions.add(marketTable.NAME.equal(marketName).and(marketTable.CITY.equal(marketCity)));

			//ResultSet rs = databaseManager.runSqlConsult(stringBuilder.toString());
			ResultSet rs = databaseManager.runSelectJooq(marketTable, fields, conditions);
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

}
