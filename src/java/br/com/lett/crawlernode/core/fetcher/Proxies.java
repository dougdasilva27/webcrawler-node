package br.com.lett.crawlernode.core.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONObject;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Interval;
import br.com.lett.crawlernode.util.Logging;


public class Proxies {

	private static final Logger logger = LoggerFactory.getLogger(Proxies.class);

	public static final String BUY 		= "buy";
	public static final String BONANZA 	= "bonanza";
	public static final String STORM 	= "storm";
	public static final String NO_PROXY = "no_proxy";
	public static final String CHARITY 	= "charity";
	public static final String AZURE 	= "azure";

	public static final int MAX_ATTEMPTS_BUY 		= 2;
	public static final int MAX_ATTEMPTS_BONANZA 	= 2;
	public static final int MAX_ATTEMPTS_CHARITY 	= 2;
	public static final int MAX_ATTEMPTS_AZURE 		= 2;
	public static final int MAX_ATTEMPTS_STORM		= 2;
	public static final int MAX_ATTEMPTS_NO_RPOXY 	= 2;

	/** Intervals used to select proxy service when running normal information extraction */
	public Map<Integer, List<Interval<Integer>>> intervalsMarketsMapWebcrawler; // global information
	
	/** Intervals used to select proxy service when downloading images */
	public Map<Integer, List<Interval<Integer>>> intervalsMarketsMapImages; // global information
	
	private Map<String, Integer> proxyMaxAttempts; // global information
	public Map<String, List<LettProxy>> proxyMap; // global information


	public Proxies(Markets markets) {
		this.proxyMap = new HashMap<String, List<LettProxy>>();

		this.proxyMaxAttempts = new HashMap<String, Integer>();
		this.proxyMaxAttempts.put(BUY, MAX_ATTEMPTS_BUY);
		this.proxyMaxAttempts.put(BONANZA, MAX_ATTEMPTS_BONANZA);
		this.proxyMaxAttempts.put(CHARITY, MAX_ATTEMPTS_CHARITY);
		this.proxyMaxAttempts.put(AZURE, MAX_ATTEMPTS_AZURE);
		this.proxyMaxAttempts.put(STORM, MAX_ATTEMPTS_STORM);
		this.proxyMaxAttempts.put(NO_PROXY, MAX_ATTEMPTS_NO_RPOXY);

		this.proxyMap.put(NO_PROXY, new ArrayList<LettProxy>());
		
		this.intervalsMarketsMapWebcrawler = new HashMap<Integer, List<Interval<Integer>>>();
		this.intervalsMarketsMapImages = new HashMap<Integer, List<Interval<Integer>>>();
		
		this.assembleIntervalsWebcrawler(markets);
		this.assembleIntervalsImages(markets);
	}

	public void setCharityProxy() {
		List<LettProxy> charity = new ArrayList<LettProxy>();
		charity.add(new LettProxy(CHARITY, "workdistribute.charityengine.com", 20000, "world", "", "ff548a45065c581adbb23bbf9253de9b"));
		this.proxyMap.put(CHARITY, charity);
	}

	public void setAzureProxy() {
		List<LettProxy> azure = new ArrayList<LettProxy>();
		azure.add(new LettProxy(AZURE, "191.235.90.114", 3333, "brazil", "", "5RXsOBETLoWjhdM83lDMRV3j335N1qbeOfMoyKsD"));
		this.proxyMap.put(AZURE, azure);
	}

	public void setStormProxies() {
		List<LettProxy> storm = new ArrayList<LettProxy>();
		storm.add(new LettProxy("storm", "37.48.118.90", 13012, "worldwide", "lett", ""));
		this.proxyMap.put(STORM, storm);
	}

	public void setBuyProxies() {
		try {
			Logging.printLogDebug(logger, "Fetching buyProxies proxies...");

			List<LettProxy> buy = new ArrayList<LettProxy>();

			String url = "http://api.buyproxies.org/?a=showProxies&pid=40833&key=80069a39926fb5a7cbc4a684092572b0";

			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setReadTimeout(10000);

			con.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {

				buy.add(new LettProxy("buyproxies.org", 
						inputLine.split(":")[0], 
						55555, 
						"USA", 
						inputLine.split(":")[2], 
						inputLine.split(":")[3]));

				response.append(inputLine);
			}
			in.close();

			this.proxyMap.put(BUY, buy);

			Logging.printLogDebug(logger, "Buy proxies fetched with success! [" + buy.size() + " proxies fetched]");
		}
		catch (Exception e) {
			Logging.printLogError(logger, "Error fetching buy proxies");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

	}


	public void setBonanzaProxies() {
		try {

			Logging.printLogDebug(logger, "Fetching bonanza proxies...");

			List<LettProxy> bonanza = new ArrayList<LettProxy>();

			String url = "https://api.proxybonanza.com/v1/userpackages/41202.json";

			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setRequestMethod("GET");
			con.setRequestProperty("Authorization", "BxcANHYTx3fRlGDKXGjAsTz6MbaZaY68ufUrSMr81yLyvGcJfe!40284");

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

			for(int i=0; i < proxies.length(); i++) {
				bonanza.add(new LettProxy("proxybonanza", 
						proxies.getJSONObject(i).getString("ip"), 
						proxies.getJSONObject(i).getInt("port_http"), 
						proxies.getJSONObject(i).getJSONObject("proxyserver").getJSONObject("georegion").getString("name"), 
						username, 
						pass));
			}

			this.proxyMap.put(BONANZA, bonanza);

			Logging.printLogDebug(logger, "Bonanza proxies fetched with success! [" + bonanza.size() + " proxies fetched]");

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
	 * @return an ArrayList containing all the proxy units for a service. Returns an empty array if the service name was not found.
	 */
	public List<LettProxy> getProxy(String serviceName) {
		if (this.proxyMap.containsKey(serviceName)) {
			return this.proxyMap.get(serviceName);
		} 

		Logging.printLogDebug(logger, "Proxy service not found...returning empty array");

		return new ArrayList<LettProxy>();		
	}
	
	/**
	 * Get the maximum number of attempts allowed with this proxy service.
	 * If the proxy service is not found on the map, the method returns 0 attempts.
	 * 
	 * @param serviceName
	 * @return
	 */
	public Integer getProxyMaxAttempts(String serviceName) {
		if (this.proxyMaxAttempts.containsKey(serviceName)) {
			return this.proxyMaxAttempts.get(serviceName);
		}
		return 0;
	}
	
	private void assembleIntervalsWebcrawler(Markets markets) {
		List<Market> marketList = markets.getMarkets();
		for (Market m : marketList) {
			List<Interval<Integer>> intervals = new ArrayList<Interval<Integer>>();
			ArrayList<String> proxies = m.getProxies();
			int index = 1;
			for (int i = 0; i < proxies.size(); i++) {
				if (proxyMaxAttempts.get(proxies.get(i)) != null) {
					intervals.add( new Interval<Integer>(proxies.get(i), index, index + proxyMaxAttempts.get(proxies.get(i)) - 1) );
					index = index + proxyMaxAttempts.get(proxies.get(i));
				}
			}
			this.intervalsMarketsMapWebcrawler.put(m.getNumber(), intervals);
		}		
	}
	
	private void assembleIntervalsImages(Markets markets) {
		List<Market> marketList = markets.getMarkets();
		for (Market m : marketList) {
			List<Interval<Integer>> intervals = new ArrayList<Interval<Integer>>();
			ArrayList<String> proxies = m.getImageProxies();
			int index = 1;
			for (int i = 0; i < proxies.size(); i++) {
				if (proxyMaxAttempts.get(proxies.get(i)) != null) {
					intervals.add( new Interval<Integer>(proxies.get(i), index, index + proxyMaxAttempts.get(proxies.get(i)) - 1) );
					index = index + proxyMaxAttempts.get(proxies.get(i));
				}
			}
			this.intervalsMarketsMapImages.put(m.getNumber(), intervals);
		}		
	}
	
	
	
	/**
	 * Select a proxy service to be used, given the number of attempt.
	 * To solve this, we create a list of intervals from the maximmum number
	 * of attempts per proxy. The list contains all intervals ordered and disjoints.
	 * Thus, the problem is: given a a list of ordered and disjoint sets, select the one
	 * in which a point is.
	 * 
	 * e.g:
	 * buy[1, 1]
	 * bonanza[2, 3]
	 * attempt = 1
	 * result = buy
	 * 
	 * @param market
	 * @param webcrawler true if we must select a proxy from the normal crawling proxies, or false if we want to select proxies for image download.
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
		Interval<Integer> interval = CommonMethods.findInterval(intervals, attempt);
		if (interval != null) return interval.getName();
		return null;
	}

}
