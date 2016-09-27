package br.com.lett.crawlernode.core.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import br.com.lett.crawlernode.database.DatabaseManager;

public class Markets {
	
	private List<Market> markets;
	private DatabaseManager dbManager;
	
	public Markets(DatabaseManager dbManager) {
		this.markets = new ArrayList<Market>();
		this.dbManager = dbManager;
		
		init();
	}
	
	public void init() {
		try {
			ResultSet rs = dbManager.runSqlConsult("SELECT id, city, name, proxies FROM market");
			while(rs.next()) {
				int marketId = rs.getInt("id");
				String city = rs.getString("city");
				String name = rs.getString("name");
				ArrayList<String> proxies = new ArrayList<String>();
				
				// get the array from postgres, as a json array
				JSONArray proxiesJSONArray = new JSONArray(rs.getString("proxies"));
				
				// populate the proxies array list
				for (int i = 0; i < proxiesJSONArray.length(); i++) {
					proxies.add( proxiesJSONArray.getString(i) );
				}				
				
				Market market = new Market(
						marketId, 
						city, 
						name,
						proxies);

				markets.add(market);				
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Search for a market in the markets array.
	 * 
	 * @param city
	 * @param name
	 * @return the desired market. If none is found, the method returns null.
	 */
	public Market getMarket(String city, String name) {
		for (Market m : markets) {
			if (m.getCity().equals(city) && m.getName().equals(name)) {
				return m;
			}
		}
		
		return null;
	}

	public List<Market> getMarkets() {
		return markets;
	}

	public void setMarkets(List<Market> markets) {
		this.markets = markets;
	}
	
	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		for (Market market : this.markets) {
			stringBuilder.append(market.toString());
		}
		return stringBuilder.toString();		
	}

}
