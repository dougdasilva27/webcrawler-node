package br.com.lett.crawlernode.core.server;

public class ServerConstants {
	
	public static final String MSG_TASK_COMPLETED = "task completed";
	public static final String MSG_TASK_FAILED = "task failed";
	public static final String MSG_METHOD_NOT_ALLOWED = "request method not allowed";
	public static final String MSG_SERVER_HEALTH_OK = "the server is fine";
	public static final String MSG_BAD_REQUEST = "bad request";
	
	public static final int HTTP_STATUS_CODE_OK = 200;
	public static final int HTTP_STATUS_CODE_SERVER_ERROR = 500;
	public static final int HTTP_STATUS_CODE_BAD_REQUEST = 400;
	public static final int HTTP_STATUS_CODE_METHOD_NOT_ALLOWED = 402;
	public static final int HTTP_STATUS_CODE_NOT_FOUND = 404;
	
	public static final String ENDPOINT_TASK = "/crawler-task";
	public static final String ENDPOINT_HEALTH_CHECK = "/health-check";

}
