package br.com.lett.crawlernode.core.session;

import java.util.ArrayList;

import br.com.lett.crawlernode.core.models.Market;

public class TestCrawlerSession extends Session {
	
	/** Number of truco checks */
	private int trucoAttemptsCounter;

	/** Number of readings to prevent a void status */
	private int voidAttemptsCounter;
	
	public TestCrawlerSession(String url, Market market) {
		super();

		// initialize counters
		this.trucoAttemptsCounter = 0;
		this.voidAttemptsCounter = 0;

		// creating the errors list
		this.crawlerSessionErrors = new ArrayList<SessionError>();

		// setting session id
		this.sessionId = "test";
		
		// setting Market
		this.market = market;

		// setting URL and originalURL
		this.originalURL = url;
	}
	
	public int getTrucoAttempts() {
		return trucoAttemptsCounter;
	}
	
	public int getVoidAttempts() {
		return voidAttemptsCounter;
	}

	public void setVoidAttempts(int voidAttempts) {
		this.voidAttemptsCounter = voidAttempts;
	}

	public void incrementTrucoAttemptsCounter() {
		this.trucoAttemptsCounter++;
	}

	public void incrementVoidAttemptsCounter() {
		this.voidAttemptsCounter++;
	}

}
