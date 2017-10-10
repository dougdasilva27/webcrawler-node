package br.com.lett.crawlernode.aws.sqs;

public class QueueName {
		
	public static final String RATING						= "crawler-rating";
	public static final String INSIGHTS_DEVELOPMENT 		= "crawler-insights-development";
	public static final String INSIGHTS_TRY_AGAIN			= "crawler-insights-try-again";
	public static final String INSIGHTS 					= "crawler-insights";
	public static final String DISCOVER 					= "crawler-discover";
	public static final String DISCOVER_WEBDRIVER			= "crawler-discover-webdriver";
	public static final String SEED 						= "crawler-seed";
	public static final String IMAGES 						= "crawler-images";
	public static final String RANKING_KEYWORDS 			= "crawler-ranking-keywords";
	public static final String DISCOVER_KEYWORDS 			= "crawler-discover-keywords";
	public static final String RANKING_CATEGORIES			= "crawler-ranking-categories";
	public static final String DISCOVER_CATEGORIES			= "crawler-discover-categories";
	public static final String INTEREST_PROCESSED 			= "interest-processed";
	public static final String TEST_PHANTOMJS				= "crawler-insights-webdriver";
	
	public static final String RATING_WEBDRIVER	= "crawler-rating-webdriver";
	public static final String WEBDRIVER = "crawler-insights-webdriver";
	public static final String LAMBDA	= "lambda-test";
	public static final String DEVELOPMENT	= "development-test";
	public static final String RANKING_KEYWORDS_WEBDRIVER = "crawler-ranking-keywords-webdriver";
	public static final String DISCOVER_KEYWORDS_WEBDRIVER = "crawler-discover-keywords-webdriver";
	
	private QueueName() {
		super();
	}
	
}
