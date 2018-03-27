package br.com.lett.crawlernode.core.fetcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.database.DatabaseDataFetcher;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.util.Interval;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;


public class ProxyCollection {

  private static final Logger logger = LoggerFactory.getLogger(ProxyCollection.class);

  public static final String BUY = "buy";
  public static final String BONANZA = "bonanza";
  public static final String STORM = "storm";
  public static final String STORM_RESIDENTIAL_US = "storm_residential_us";
  public static final String STORM_RESIDENTIAL_EU = "storm_residential_eu";
  public static final String NO_PROXY = "no_proxy";
  public static final String LUMINATI_SERVER_BR = "luminati_server_br";
  public static final String LUMINATI_RESIDENTIAL_BR = "luminati_residential_br";
  public static final String LUMINATI_RESIDENTIAL_AR = "luminati_residential_ar";
  public static final String LUMINATI_RESIDENTIAL_MX = "luminati_residential_mx";

  public static final int MAX_ATTEMPTS_BUY = 2;
  public static final int MAX_ATTEMPTS_BONANZA = 3;

  public static final int MAX_ATTEMPTS_LUMINATI_SERVER_BR = 2;
  public static final int MAX_ATTEMPTS_LUMINATI_RESIDENTIAL_BR = 2;
  public static final int MAX_ATTEMPTS_LUMINATI_RESIDENTIAL_MX = 2;
  public static final int MAX_ATTEMPTS_LUMINATI_RESIDENTIAL_AR = 2;

  public static final int MAX_ATTEMPTS_STORM = 3;
  public static final int MAX_ATTEMPTS_STORM_RESIDENTIAL_US = 2;
  public static final int MAX_ATTEMPTS_STORM_RESIDENTIAL_EU = 2;
  public static final int MAX_ATTEMPTS_NO_PROXY = 1;

  /** Intervals used to select proxy service when running normal information extraction */
  protected Map<Integer, List<Interval<Integer>>> intervalsMarketsMapWebcrawler = new HashMap<>();
  /** Intervals used to select proxy service when downloading images */
  protected Map<Integer, List<Interval<Integer>>> intervalsMarketsMapImages = new HashMap<>();

  protected static Map<String, Integer> proxyMaxAttempts = new HashMap<>();
  protected Map<String, List<LettProxy>> proxyMap = new HashMap<>();


  public ProxyCollection(Markets markets, DatabaseManager databaseManager) {
    DatabaseDataFetcher dbFetcher = new DatabaseDataFetcher(databaseManager);
    Logging.printLogDebug(logger, "Fetching proxies in Mongo Fetcher...");
    proxyMap = dbFetcher.fetchProxiesFromMongoFetcher();
    Logging.printLogDebug(logger, proxyMap.size() + " proxies services returned from Mongo Fetcher.");
    proxyMap.put(NO_PROXY, new ArrayList<LettProxy>());

    proxyMaxAttempts.put(BUY, MAX_ATTEMPTS_BUY);
    proxyMaxAttempts.put(BONANZA, MAX_ATTEMPTS_BONANZA);
    proxyMaxAttempts.put(LUMINATI_SERVER_BR, MAX_ATTEMPTS_LUMINATI_SERVER_BR);
    proxyMaxAttempts.put(LUMINATI_RESIDENTIAL_BR, MAX_ATTEMPTS_LUMINATI_RESIDENTIAL_BR);
    proxyMaxAttempts.put(LUMINATI_RESIDENTIAL_MX, MAX_ATTEMPTS_LUMINATI_RESIDENTIAL_MX);
    proxyMaxAttempts.put(LUMINATI_RESIDENTIAL_AR, MAX_ATTEMPTS_LUMINATI_RESIDENTIAL_AR);
    proxyMaxAttempts.put(STORM, MAX_ATTEMPTS_STORM);
    proxyMaxAttempts.put(STORM_RESIDENTIAL_US, MAX_ATTEMPTS_STORM_RESIDENTIAL_US);
    proxyMaxAttempts.put(STORM_RESIDENTIAL_EU, MAX_ATTEMPTS_STORM_RESIDENTIAL_EU);
    proxyMaxAttempts.put(NO_PROXY, MAX_ATTEMPTS_NO_PROXY);

    assembleIntervalsWebcrawler(markets);
    assembleIntervalsImages(markets);
  }

  /**
   * Get the array of proxy units corresponding to a proxy service name.
   * 
   * @param serviceName the name of the proxy service
   * @param session the crawler session. Used for logging purposes.
   * @return an ArrayList containing all the proxy units for a service. Returns an empty array if the
   *         service name was not found.
   */
  public List<LettProxy> getProxy(String serviceName) {
    if (proxyMap.containsKey(serviceName)) {
      return proxyMap.get(serviceName);
    }

    Logging.printLogDebug(logger, "Proxy service not found...returning empty array");

    return new ArrayList<>();
  }

  /**
   * Get the maximum number of attempts allowed with this proxy service. If the proxy service is not
   * found on the map, the method returns 0 attempts.
   * 
   * @param serviceName
   * @return
   */
  public Integer getProxyMaxAttempts(String serviceName) {
    if (proxyMaxAttempts.containsKey(serviceName)) {
      return proxyMaxAttempts.get(serviceName);
    }
    return 0;
  }

  private void assembleIntervalsWebcrawler(Markets markets) {
    List<Market> marketList = markets.getMarkets();
    for (Market m : marketList) {
      List<Interval<Integer>> intervals = new ArrayList<>();
      List<String> proxies = m.getProxies();
      int index = 1;
      for (int i = 0; i < proxies.size(); i++) {
        if (proxyMaxAttempts.get(proxies.get(i)) != null) {
          intervals.add(new Interval<Integer>(proxies.get(i), index, index + proxyMaxAttempts.get(proxies.get(i)) - 1));
          index = index + proxyMaxAttempts.get(proxies.get(i));
        }
      }
      this.intervalsMarketsMapWebcrawler.put(m.getNumber(), intervals);
    }
  }

  private void assembleIntervalsImages(Markets markets) {
    List<Market> marketList = markets.getMarkets();
    for (Market m : marketList) {
      List<Interval<Integer>> intervals = new ArrayList<>();
      List<String> proxies = m.getImageProxies();
      int index = 1;
      for (int i = 0; i < proxies.size(); i++) {
        if (proxyMaxAttempts.get(proxies.get(i)) != null) {
          intervals.add(new Interval<Integer>(proxies.get(i), index, index + proxyMaxAttempts.get(proxies.get(i)) - 1));
          index = index + proxyMaxAttempts.get(proxies.get(i));
        }
      }
      this.intervalsMarketsMapImages.put(m.getNumber(), intervals);
    }
  }



  /**
   * Select a proxy service to be used, given the number of attempt. To solve this, we create a list
   * of intervals from the maximmum number of attempts per proxy. The list contains all intervals
   * ordered and disjoints. Thus, the problem is: given a a list of ordered and disjoint sets, select
   * the one in which a point is.
   * 
   * e.g: buy[1, 1] bonanza[2, 3] attempt = 1 result = buy
   * 
   * @param market
   * @param webcrawler true if we must select a proxy from the normal crawling proxies, or false if we
   *        want to select proxies for image download.
   * @param attempt
   * @return a String representing the name of the proxy service.
   */
  public String selectProxy(Market market, boolean webcrawler, int attempt) {
    List<Interval<Integer>> intervals = null;
    if (webcrawler) {
      intervals = this.intervalsMarketsMapWebcrawler.get(market.getNumber());
    } else {
      intervals = this.intervalsMarketsMapImages.get(market.getNumber());
    }
    Interval<Integer> interval = MathUtils.findInterval(intervals, attempt);
    if (interval != null) {
      return interval.getName();
    }
    return null;
  }

}
