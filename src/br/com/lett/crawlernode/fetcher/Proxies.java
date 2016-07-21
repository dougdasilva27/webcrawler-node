package br.com.lett.crawlernode.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.util.Logging;

public class Proxies {

	private static final Logger logger = LoggerFactory.getLogger(Proxies.class);

	/**
	 * Shader
	 */
	public ArrayList<LettProxy> premiumProxies;

	/**
	 * ByProxies
	 */
	public ArrayList<LettProxy> regularProxies;
	
	/**
	 * None
	 */
	public ArrayList<LettProxy> semiPremiumProxies;
	

	public Proxies() {
		this.premiumProxies = new ArrayList<LettProxy>();
		this.regularProxies = new ArrayList<LettProxy>();
		this.semiPremiumProxies = new ArrayList<LettProxy>();
	}


	public void fetchRegularProxies() {
		try {
			Logging.printLogDebug(logger, "Fetching regular proxies...");

			String url = "http://api.buyproxies.org/?a=showProxies&pid=40833&key=80069a39926fb5a7cbc4a684092572b0";

			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setReadTimeout(10000);

			con.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {

				regularProxies.add(new LettProxy("buyproxies.org", 
						inputLine.split(":")[0], 
						55555, 
						"USA", 
						inputLine.split(":")[2], 
						inputLine.split(":")[3]));

				response.append(inputLine);
			}
			in.close();

			Logging.printLogDebug(logger, regularProxies.size() + " regular proxies fetched!");
		}
		catch (Exception e) {

		}

	}

	public void fetchPremiumProxies() {
		try {
			
			premiumProxies.add(new LettProxy("shader", "138.99.122.129", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.131", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.135", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.137", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.141", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.143", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.147", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.149", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.153", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.155", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.159", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.161", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.165", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.167", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.171", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.173", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.177", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.179", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.183", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.185", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.189", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.191", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.195", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.197", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.201", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.203", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.207", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.209", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.213", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.215", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.217", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.219", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.221", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.223", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.225", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.227", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.229", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.231", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.233", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.235", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.237", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.239", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.241", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.243", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.245", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.247", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.249", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.251", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.253", 60000, "Brasil", "lett", "hQEu5srTMe"));
			premiumProxies.add(new LettProxy("shader", "138.99.122.255", 60000, "Brasil", "lett", "hQEu5srTMe"));
			
		} catch(Exception e) {

		}
	}

}
