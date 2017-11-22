package br.com.lett.crawlernode.core.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Interval;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;


public class ProxyCollection {

	private static final Logger logger = LoggerFactory.getLogger(ProxyCollection.class);

	public static final String HA_PROXY_HTTP = "191.235.90.114:3333";
	public static final String HA_PROXY_HTTPS = "191.235.90.114:3333";

	public static final String BUY = "buy";
	public static final String BONANZA = "bonanza";
	public static final String STORM = "storm";
	public static final String STORM_RESIDENTIAL_US = "storm_residential_us";
	public static final String NO_PROXY = "no_proxy";
	public static final String LUMINATI_SERVER_BR = "luminati_server_br";
	public static final String LUMINATI_RESIDENTIAL_BR = "luminati_residential_br";
	public static final String LUMINATI_RESIDENTIAL_AR = "luminati_residential_ar";
	public static final String LUMINATI_RESIDENTIAL_MX = "luminati_residential_mx";
	public static final String FETCHER = "fetcher";

	public static final int MAX_ATTEMPTS_FETCHER = 1;
	public static final int MAX_ATTEMPTS_BUY = 2;
	public static final int MAX_ATTEMPTS_BONANZA = 3;

	public static final int MAX_ATTEMPTS_LUMINATI_SERVER_BR = 2;
	public static final int MAX_ATTEMPTS_LUMINATI_RESIDENTIAL_BR = 2;
	public static final int MAX_ATTEMPTS_LUMINATI_RESIDENTIAL_MX = 2;
	public static final int MAX_ATTEMPTS_LUMINATI_RESIDENTIAL_AR = 2;

	public static final int MAX_ATTEMPTS_STORM = 3;
	public static final int MAX_ATTEMPTS_STORM_RESIDENTIAL_US = 2;
	public static final int MAX_ATTEMPTS_NO_PROXY = 1;

	/** Intervals used to select proxy service when running normal information extraction */
	public Map<Integer, List<Interval<Integer>>> intervalsMarketsMapWebcrawler; // global information

	/** Intervals used to select proxy service when downloading images */
	public Map<Integer, List<Interval<Integer>>> intervalsMarketsMapImages; // global information

	public static Map<String, Integer> proxyMaxAttempts; // global information
	public Map<String, List<LettProxy>> proxyMap; // global information


	public ProxyCollection(Markets markets) {
		proxyMap = new HashMap<>();

		proxyMaxAttempts = new HashMap<>();
		proxyMaxAttempts.put(BUY, MAX_ATTEMPTS_BUY);
		proxyMaxAttempts.put(BONANZA, MAX_ATTEMPTS_BONANZA);
		proxyMaxAttempts.put(LUMINATI_SERVER_BR, MAX_ATTEMPTS_LUMINATI_SERVER_BR);
		proxyMaxAttempts.put(LUMINATI_RESIDENTIAL_BR, MAX_ATTEMPTS_LUMINATI_RESIDENTIAL_BR);
		proxyMaxAttempts.put(LUMINATI_RESIDENTIAL_MX, MAX_ATTEMPTS_LUMINATI_RESIDENTIAL_MX);
		proxyMaxAttempts.put(LUMINATI_RESIDENTIAL_AR, MAX_ATTEMPTS_LUMINATI_RESIDENTIAL_AR);
		proxyMaxAttempts.put(STORM, MAX_ATTEMPTS_STORM);
		proxyMaxAttempts.put(STORM_RESIDENTIAL_US, MAX_ATTEMPTS_STORM_RESIDENTIAL_US);
		proxyMaxAttempts.put(NO_PROXY, MAX_ATTEMPTS_NO_PROXY);
		proxyMaxAttempts.put(FETCHER, MAX_ATTEMPTS_FETCHER);

		intervalsMarketsMapWebcrawler = new HashMap<>();
		intervalsMarketsMapImages = new HashMap<>();

		assembleIntervalsWebcrawler(markets);
		assembleIntervalsImages(markets);

		setDefaultProxy();
		setBonanzaProxies();
		setBuyProxies();
		setStormProxies();
		setStormResidentialUSProxy();
		setLuminatiServerBrProxy();
		setLuminatiResidentialBrProxy();
		setLuminatiResidentialMxProxy();
		setLuminatiResidentialArProxy();
		setFetcherProxy();
	}

	private void setDefaultProxy() {
		proxyMap.put(NO_PROXY, new ArrayList<LettProxy>());
	}

	private void setFetcherProxy() {
		List<LettProxy> fetcher = new ArrayList<>();
		fetcher.add(new LettProxy(FETCHER, "127.0.0.1", 8080, "brazil", "", ""));
		proxyMap.put(FETCHER, fetcher);
	}

	private void setLuminatiServerBrProxy() {
		List<LettProxy> luminati = new ArrayList<>();
		luminati.add(new LettProxy(LUMINATI_SERVER_BR, "zproxy.luminati.io", 22225, "brazil",
				"lum-customer-lettinsights-zone-static_shared_br", "72nxUzRANwPf3tYcekwii"));
		proxyMap.put(LUMINATI_SERVER_BR, luminati);
	}

	private void setLuminatiResidentialBrProxy() {
		List<LettProxy> luminatiBr = new ArrayList<>();
		luminatiBr.add(new LettProxy(LUMINATI_RESIDENTIAL_BR, "zproxy.luminati.io", 22225, "brazil",
				"lum-customer-lettinsights-zone-residential_br-country-br", "bKhgwEQijyG92jR9kvBPw"));
		proxyMap.put(LUMINATI_RESIDENTIAL_BR, luminatiBr);
	}

	private void setLuminatiResidentialMxProxy() {
		List<LettProxy> luminatiResidentialMx = new ArrayList<>();
		luminatiResidentialMx
				.add(new LettProxy(LUMINATI_RESIDENTIAL_MX, "zproxy.luminati.io", 22225, "mexico",
						"lum-customer-lettinsights-zone-residential_br-country-mx", "bKhgwEQijyG92jR9kvBPw"));
		proxyMap.put(LUMINATI_RESIDENTIAL_MX, luminatiResidentialMx);
	}

	private void setLuminatiResidentialArProxy() {
		List<LettProxy> luminatiResidentialAr = new ArrayList<>();
		luminatiResidentialAr
				.add(new LettProxy(LUMINATI_RESIDENTIAL_AR, "zproxy.luminati.io", 22225, "argentina",
						"lum-customer-lettinsights-zone-residential_br-country-ar", "bKhgwEQijyG92jR9kvBPw"));
		proxyMap.put(LUMINATI_RESIDENTIAL_AR, luminatiResidentialAr);
	}

	private void setStormProxies() {
		List<LettProxy> storm = new ArrayList<>();
		storm.add(new LettProxy("storm", "37.48.118.90", 13012, "worldwide", "lett", ""));
		proxyMap.put(STORM, storm);
	}

	private void setStormResidentialUSProxy() {
		List<LettProxy> storm = new ArrayList<>();
		storm.add(new LettProxy(STORM_RESIDENTIAL_US, "104.245.96.241", 19008, "worldwide", "", ""));
		storm.add(new LettProxy(STORM_RESIDENTIAL_US, "199.168.141.147", 19014, "worldwide", "", ""));
		storm.add(new LettProxy(STORM_RESIDENTIAL_US, "104.193.9.41", 19006, "worldwide", "", ""));
		storm.add(new LettProxy(STORM_RESIDENTIAL_US, "142.54.179.98", 19006, "worldwide", "", ""));
		storm.add(new LettProxy(STORM_RESIDENTIAL_US, "198.204.229.194", 19007, "worldwide", "", ""));
		storm.add(new LettProxy(STORM_RESIDENTIAL_US, "104.245.96.241", 19017, "worldwide", "", ""));
		storm.add(new LettProxy(STORM_RESIDENTIAL_US, "104.255.66.35", 19001, "worldwide", "", ""));
		storm.add(new LettProxy(STORM_RESIDENTIAL_US, "69.30.199.122", 19012, "worldwide", "", ""));
		storm.add(new LettProxy(STORM_RESIDENTIAL_US, "104.245.96.105", 19016, "worldwide", "", ""));
		storm.add(new LettProxy(STORM_RESIDENTIAL_US, "199.168.141.132", 19011, "worldwide", "", ""));

		proxyMap.put(STORM_RESIDENTIAL_US, storm);
	}

	private void setBuyProxies() {
		try {
			Logging.printLogDebug(logger, "Fetching buyProxies proxies...");

			List<LettProxy> buy = new ArrayList<>();

			String url =
					"http://api.buyproxies.org/?a=showProxies&pid=40833&key=80069a39926fb5a7cbc4a684092572b0";

			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setReadTimeout(10000);

			con.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {

				buy.add(new LettProxy("buyproxies.org", inputLine.split(":")[0], 55555, "USA",
						inputLine.split(":")[2], inputLine.split(":")[3]));

				response.append(inputLine);
			}
			in.close();

			this.proxyMap.put(BUY, buy);

			Logging.printLogDebug(logger,
					"Buy proxies fetched with success! [" + buy.size() + " proxies fetched]");
		} catch (Exception e) {
			Logging.printLogError(logger, "Error fetching buy proxies");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

	}

	private void setBonanzaProxies() {
		try {

			Logging.printLogDebug(logger, "Fetching bonanza proxies...");

			List<LettProxy> bonanza = new ArrayList<>();

			String url = "https://api.proxybonanza.com/v1/userpackages/41202.json";

			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setRequestMethod("GET");
			con.setRequestProperty("Authorization",
					"BxcANHYTx3fRlGDKXGjAsTz6MbaZaY68ufUrSMr81yLyvGcJfe!40284");

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			JSONObject data = new JSONObject(response.toString()).getJSONObject("data");

			String username = data.getString("login").toString();
			String pass = data.getString("password").toString();

			JSONArray proxies = data.getJSONArray("ippacks");

			for (int i = 0; i < proxies.length(); i++) {
				bonanza.add(new LettProxy("proxybonanza", proxies.getJSONObject(i).getString("ip"),
						proxies.getJSONObject(i).getInt("port_http"), proxies.getJSONObject(i)
								.getJSONObject("proxyserver").getJSONObject("georegion").getString("name"),
						username, pass));
			}

			this.proxyMap.put(BONANZA, bonanza);

			Logging.printLogDebug(logger,
					"Bonanza proxies fetched with success! [" + bonanza.size() + " proxies fetched]");

		} catch (Exception e) {
			Logging.printLogError(logger, "Error fetching bonanza proxies.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}

	/**
	 * Get the array of proxy units corresponding to a proxy service name.
	 * 
	 * @param serviceName the name of the proxy service
	 * @param session the crawler session. Used for logging purposes.
	 * @return an ArrayList containing all the proxy units for a service. Returns an empty array if
	 *         the service name was not found.
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
					intervals.add(new Interval<Integer>(proxies.get(i), index,
							index + proxyMaxAttempts.get(proxies.get(i)) - 1));
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
					intervals.add(new Interval<Integer>(proxies.get(i), index,
							index + proxyMaxAttempts.get(proxies.get(i)) - 1));
					index = index + proxyMaxAttempts.get(proxies.get(i));
				}
			}
			this.intervalsMarketsMapImages.put(m.getNumber(), intervals);
		}
	}



	/**
	 * Select a proxy service to be used, given the number of attempt. To solve this, we create a list
	 * of intervals from the maximmum number of attempts per proxy. The list contains all intervals
	 * ordered and disjoints. Thus, the problem is: given a a list of ordered and disjoint sets,
	 * select the one in which a point is.
	 * 
	 * e.g: buy[1, 1] bonanza[2, 3] attempt = 1 result = buy
	 * 
	 * @param market
	 * @param webcrawler true if we must select a proxy from the normal crawling proxies, or false if
	 *        we want to select proxies for image download.
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
		Interval<Integer> interval = MathCommonsMethods.findInterval(intervals, attempt);
		if (interval != null) {
			return interval.getName();
		}
		return null;
	}

}
