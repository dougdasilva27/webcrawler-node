package br.com.lett.crawlernode.core.session;

import java.util.ArrayList;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.queue.QueueName;

public class TestCrawlerSession extends Session {
	
	/** Number of truco checks */
	private int trucoAttemptsCounter;

	/** Number of readings to prevent a void status */
	private int voidAttemptsCounter;
	
	private TestType type;
	
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
	
	
	public TestCrawlerSession(Request request, Market market) {
		super();
		
		if (QueueName.DISCOVER.equals(request.getQueueName())) {
			type = TestType.DISCOVER;
		} else if (QueueName.IMAGES.equals(request.getQueueName())) {
			type = TestType.IMAGE;
		} else if (QueueName.INSIGHTS.equals(request.getQueueName())) {
			type = TestType.INSIGHTS;
		} else if (QueueName.RATING.equals(request.getQueueName())) {
			type = TestType.RATING;
		}
		else {
			type = TestType.SEED;
		}

		// initialize counters
		this.trucoAttemptsCounter = 0;
		this.voidAttemptsCounter = 0;

		// creating the errors list
		this.crawlerSessionErrors = new ArrayList<SessionError>();

		// setting session id
		this.sessionId = request.getMessageId();
		
		// setting Market
		this.market = market;

		// setting URL and originalURL
		this.originalURL = request.getMessageBody();
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
