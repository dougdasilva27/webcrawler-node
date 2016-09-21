package br.com.lett.crawlernode.core.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONObject;

import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class Proxies {

	private static final Logger logger = LoggerFactory.getLogger(Proxies.class);

	public static final String SHADER = "shader";
	public static final String BUY = "buy";
	public static final String BONANZA = "bonanza";
	public static final String STORM = "storm";
	public static final String NO_PROXY = "no_proxy";
	public static final String CHARITY = "charity";

	public static final String DEFAULT = SHADER;

	/**
	 * Shader
	 */
	public ArrayList<LettProxy> shaderProxies;

	/**
	 * ByProxies
	 */
	public ArrayList<LettProxy> buyProxies;

	/**
	 * Proxy bonanza
	 */
	public ArrayList<LettProxy> bonanzaProxies;

	/**
	 * Storm
	 */
	public ArrayList<LettProxy> storm;
	
	/**
	 * Charity
	 */
	public ArrayList<LettProxy> charity;


	public Proxies() {
		this.shaderProxies = new ArrayList<LettProxy>();
		this.buyProxies = new ArrayList<LettProxy>();
		this.bonanzaProxies = new ArrayList<LettProxy>();
		this.storm = new ArrayList<LettProxy>();
		this.charity = new ArrayList<LettProxy>();
	}
	
	public void setCharityProxy() {
		charity.add(new LettProxy(CHARITY, "workdistribute.charityengine.com", 20000, "world", "", "ff548a45065c581adbb23bbf9253de9b"));
	}

	public void setBuyProxies() {
		try {
			Logging.printLogDebug(logger, "Fetching buyProxies proxies...");

			String url = "http://api.buyproxies.org/?a=showProxies&pid=40833&key=80069a39926fb5a7cbc4a684092572b0";

			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setReadTimeout(10000);

			con.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {

				buyProxies.add(new LettProxy(BUY, 
						inputLine.split(":")[0], 
						55555, 
						"USA", 
						inputLine.split(":")[2], 
						inputLine.split(":")[3]));

				response.append(inputLine);
			}
			in.close();

			Logging.printLogDebug(logger, "Buy proxies fetched with success! [" + this.buyProxies.size() + " proxies fetched]");
		}
		catch (Exception e) {
			Logging.printLogError(logger, "Error fetching buy proxies");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

	}

	public void setShaderProxies() {					
		Logging.printLogDebug(logger, "Fetching Shader proxies...");

		shaderProxies.add(new LettProxy(SHADER, "138.99.122.129", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.131", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.135", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.137", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.141", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.143", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.147", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.149", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.153", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.155", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.159", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.161", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.165", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.167", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.171", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.173", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.177", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.179", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.183", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.185", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.189", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.191", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.195", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.197", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.201", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.203", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.207", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.209", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.213", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.215", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.217", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.219", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.221", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.223", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.225", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.227", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.229", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.231", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.233", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.235", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.237", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.239", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.241", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.243", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.245", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.247", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.249", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.251", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.253", 60000, "Brasil", "lett", "hQEu5srTMe"));
		shaderProxies.add(new LettProxy(SHADER, "138.99.122.255", 60000, "Brasil", "lett", "hQEu5srTMe"));

		Logging.printLogDebug(logger, "Shader proxies fetched with success! [" + this.shaderProxies.size() + " proxies fetched]");

	}

	public void setStormProxies() {
		Logging.printLogDebug(logger, "Fetching Storm proxies...");
		
		storm.add(new LettProxy("storm", "37.48.118.90", 13012, "worldwide", "lett", ""));
	}

	public void setBonanzaProxies() {
		try {

			Logging.printLogDebug(logger, "Fetching bonanza proxies...");

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
				bonanzaProxies.add(new LettProxy(BONANZA, 
						proxies.getJSONObject(i).getString("ip"), 
						proxies.getJSONObject(i).getInt("port_http"), 
						proxies.getJSONObject(i).getJSONObject("proxyserver").getJSONObject("georegion").getString("name"), 
						username, 
						pass));
			}

			Logging.printLogDebug(logger, "Bonanza proxies fetched with success! [" + this.bonanzaProxies.size() + " proxies fetched]");

		} catch (Exception e) {
			Logging.printLogError(logger, "Error fetching bonanza proxies.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}

}
