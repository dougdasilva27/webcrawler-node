package br.com.lett.crawlernode.aws.kinesis;

public class KPLProducerConfig {
	
	public static final long KPL_MAX_CONNECTIONS = 1;
	public static final long KPL_REQUEST_TIMEOUT = 120000;
	public static final long RECORD_TTL = 300000;
	public static final long RECORD_MAX_BUFFERED_TIME = 30000; // 30 seconds
	
	public static final String REGION = "us-east-1";
	public static final String STREAM_NAME = "sku-core-crawler-kinesis-stream";
	

}
