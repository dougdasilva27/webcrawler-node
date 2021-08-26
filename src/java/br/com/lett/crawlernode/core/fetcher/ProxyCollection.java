package br.com.lett.crawlernode.core.fetcher;

import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.database.DatabaseDataFetcher;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.util.Interval;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ProxyCollection {

   private static final Logger logger = LoggerFactory.getLogger(ProxyCollection.class);

   // CORE PROXIES
   public static final String BUY = "buy";
   public static final String BUY_HAPROXY = "buy_haproxy";
   public static final String BONANZA = "bonanza";
   public static final String BONANZA_BELGIUM = "bonanza_server_be";
   public static final String BONANZA_BELGIUM_HAPROXY = "bonanza_be_haproxy";
   public static final String LUMINATI_SERVER_BR = "luminati_server_br";
   public static final String LUMINATI_SERVER_BR_HAPROXY = "luminati_server_haproxy_br";
   public static final String LUMINATI_RESIDENTIAL_BR = "luminati_residential_br";
   public static final String LUMINATI_RESIDENTIAL_BR_HAPROXY = "luminati_residential_br_haproxy";
   public static final String INFATICA_RESIDENTIAL_BR = "infatica_residential_br";
   public static final String NETNUT_RESIDENTIAL_BR = "netnut_residential_br";
   public static final String NETNUT_RESIDENTIAL_BR_HAPROXY = "netnut_residential_br_haproxy";
   public static final String NETNUT_RESIDENTIAL_ES = "netnut_residential_es";
   public static final String NETNUT_RESIDENTIAL_ES_HAPROXY = "netnut_residential_es_haproxy";
   public static final String NETNUT_RESIDENTIAL_MX = "netnut_residential_mx";
   public static final String INFATICA_RESIDENTIAL_BR_HAPROXY = "infatica_residential_br_haproxy";
   public static final String BR_OXYLABS = "br-oxylabs";
   public static final String BE_OXYLABS = "oxylabs_server_be";
   public static final String NO_PROXY = "no_proxy";
   public static final String NETNUT_RESIDENTIAL_CO_HAPROXY = "netnut_residential_co_haproxy";
   public static final String NETNUT_RESIDENTIAL_CH_HAPROXY = "netnut_residential_cl_haproxy";
   public static final String NETNUT_RESIDENTIAL_AR_HAPROXY = "netnut_residential_ar_haproxy";
   public static final String NETNUT_RESIDENTIAL_ANY_HAPROXY = "netnut_residential_any_haproxy";



   // EQI PROXIES
   public static final String INFATICA_RESIDENTIAL_BR_EQI = "infatica_residential_br_eqi";


   public static final int MAX_ATTEMPTS_PER_PROXY = 2;


   protected Map<String, List<LettProxy>> proxyMap;


   public ProxyCollection(DatabaseManager databaseManager) {
      DatabaseDataFetcher dbFetcher = new DatabaseDataFetcher(databaseManager);
      Logging.printLogDebug(logger, "Fetching proxies in Mongo Fetcher...");
      proxyMap = dbFetcher.fetchProxiesFromMongoFetcher();
      Logging.printLogDebug(logger, proxyMap.size() + " proxies services returned from Mongo Fetcher.");
      proxyMap.put(NO_PROXY, new ArrayList<>());
   }

   /**
    * Get the array of proxy units corresponding to a proxy service name.
    *
    * @param serviceName the name of the proxy service
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
      return MAX_ATTEMPTS_PER_PROXY;
   }

   /**
    * Select a proxy service to be used, given the number of attempt. To solve this, we create a list
    * of intervals from the maximmum number of attempts per proxy. The list contains all intervals
    * ordered and disjoints. Thus, the problem is: given a a list of ordered and disjoint sets, select
    * the one in which a point is.
    * <p>
    * e.g: buy[1, 1] bonanza[2, 3] attempt = 1 result = buy
    *
    * @param webcrawler true if we must select a proxy from the normal crawling proxies, or false if we
    *        want to select proxies for image download.
    * @param attempt
    * @return a String representing the name of the proxy service.
    */
   public String selectProxy(Session session, boolean webcrawler, int attempt) {


      List<Interval<Integer>> intervals = new ArrayList<>();
      List<String> proxies = session.getProxies();
      int index = 1;
      for (String proxy : proxies) {
         intervals.add(new Interval<>(proxy, index, index + MAX_ATTEMPTS_PER_PROXY - 1));
         index = index + MAX_ATTEMPTS_PER_PROXY;
      }

      Interval<Integer> interval = MathUtils.findInterval(intervals, attempt);
      if (interval != null) {
         return interval.getName();
      }
      return null;
   }

}
