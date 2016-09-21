package br.com.lett.crawlernode.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

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
			StringBuilder stringBuilder = new StringBuilder();

			stringBuilder.append("SELECT id, city, name, proxies FROM market WHERE ");
			stringBuilder.append("name=" + "'" + marketName + "'" + " AND ");
			stringBuilder.append("city=" + "'" + marketCity + "'");

			ResultSet rs = databaseManager.runSqlConsult(stringBuilder.toString());
			if(rs.next()) {
				
				// get the proxies used in this market
				String proxiesCharacterVarying = rs.getString("proxies").replace("[", "").replace("]", "").trim();
				ArrayList<String> proxies = new ArrayList<String>();
				String[] tokens = proxiesCharacterVarying.split(",");
				for (String token : tokens) {
					proxies.add(token.replaceAll("\"", "").trim());
				}
				
				// create market
				Market market = new Market(
						rs.getInt("id"), 
						rs.getString("city"), 
						rs.getString("name"),
						proxies);

				return market;				
			}

		} catch (SQLException e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
		return null;
	}

}
