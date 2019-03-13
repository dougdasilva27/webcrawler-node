package br.com.lett.crawlernode.core.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import br.com.lett.crawlernode.database.DatabaseManager;

public class Markets {

  private List<Market> marketsList;
  private DatabaseManager dbManager;

  public Markets(DatabaseManager dbManager) {
    this.marketsList = new ArrayList<>();
    this.dbManager = dbManager;

    init();
  }

  public void init() {
    try (ResultSet rs = this.dbManager.connectionPostgreSQL.createStatement()
        .executeQuery("SELECT id, city, name, crawler_webdriver, proxies, proxies_images FROM market")) {

      while (rs.next()) {
        int marketId = rs.getInt("id");
        String city = rs.getString("city");
        String name = rs.getString("name");
        boolean crawlerWebdriver = rs.getBoolean("crawler_webdriver");
        ArrayList<String> proxies = new ArrayList<>();
        ArrayList<String> imageProxies = new ArrayList<>();

        // get the array of proxies from postgres, as a json array
        JSONArray proxiesJSONArray = new JSONArray(rs.getString("proxies"));

        // populate the proxies array list
        for (int i = 0; i < proxiesJSONArray.length(); i++) {
          proxies.add(proxiesJSONArray.getString(i));
        }

        // get the array of image proxies from postgres
        JSONArray imageProxiesJSONArray = new JSONArray(rs.getString("proxies_images"));
        for (int i = 0; i < imageProxiesJSONArray.length(); i++) {
          imageProxies.add(imageProxiesJSONArray.getString(i));
        }

        Market market = new Market(marketId, city, name, proxies, imageProxies);

        market.setMustUseCrawlerWebdriver(crawlerWebdriver);

        marketsList.add(market);
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
    for (Market m : marketsList) {
      if (m.getCity().equals(city) && m.getName().equals(name)) {
        return m;
      }
    }

    return null;
  }

  public Market getMarket(int marketId) {
    for (Market m : marketsList) {
      if (m.getNumber() == marketId) {
        return m;
      }
    }

    return null;
  }

  public List<Market> getMarkets() {
    return marketsList;
  }

  public void setMarkets(List<Market> markets) {
    this.marketsList = markets;
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (Market market : this.marketsList) {
      stringBuilder.append(market.toString());
      stringBuilder.append("\n");
    }
    return stringBuilder.toString();
  }

}
