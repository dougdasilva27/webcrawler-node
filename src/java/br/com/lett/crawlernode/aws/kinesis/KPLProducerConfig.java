package br.com.lett.crawlernode.aws.kinesis;

public class KPLProducerConfig {
	public static final long KPL_MAX_CONNECTIONS = 1;
	public static final long KPL_REQUEST_TIMEOUT = 120 * 1000; // 2 minutes
	public static final long RECORD_TTL = 300 * 1000; // 5 minutes
	public static final long RECORD_MAX_BUFFERED_TIME = 30 * 1000; // 30 seconds
	
	public static final String REGION = "us-east-1";
}
