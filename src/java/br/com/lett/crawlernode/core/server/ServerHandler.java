package br.com.lett.crawlernode.core.server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionFactory;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.base.TaskFactory;
import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.queue.QueueName;
import br.com.lett.crawlernode.queue.QueueService;
import br.com.lett.crawlernode.util.Logging;

public class ServerHandler implements HttpHandler {
	
	protected static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

	private static final String POST = "POST";
	private static final String GET = "GET";
	
	public static final String TASK_ENDPOINT = "/crawler-task";
	public static final String HEALTH_CHECK_ENDPOINT = "/health-check";

	private static final int HTTP_STATUS_CODE_OK = 200;
	private static final int HTTP_STATUS_CODE_SERVER_ERROR = 500;
	private static final int HTTP_STATUS_CODE_BAD_REQUEST = 400;
	private static final int HTTP_STATUS_CODE_METHOD_NOT_ALLOWED = 402;
	private static final int HTTP_STATUS_CODE_NOT_FOUND = 404;

	private static final String MSG_ATTR_HEADER_PREFIX = "X-aws-sqsd-attr-";

	private static final String MSG_ATTR_MARKET = "market";
	private static final String MSG_ATTR_CITY = "city";
	private static final String MSG_ATTR_PROCESSED_ID = "processedId";
	private static final String MSG_ATTR_INTERNAL_ID = "internalId";

	private static final String MSG_ID_HEADER = "X-aws-sqsd-msgid";
	private static final String SQS_NAME_HEADER = "X-aws-sqsd-queue";

	@Override
	public void handle(HttpExchange t) throws IOException {		
		String endpoint = t.getHttpContext().getPath();
		String requestMethod = t.getRequestMethod().toUpperCase();
		String response;
		
		// handle request on the task endpoint
		if (TASK_ENDPOINT.equals(endpoint)) {
			Logging.printLogDebug(logger, "Received a request on TASK_ENDPOINT.");
			
			if (POST.equals(requestMethod)) {
				Request request = parseRequest(t);

				Markets markets = Main.markets;

				// discover the type of task
				// done internally on the following method
				Session session = SessionFactory.createSession(request, request.getQueueName(), markets);

				// create the task
				Task task = TaskFactory.createTask(session);

				// perform the task
				task.process();

				// check final task status
				
				if (Task.STATUS_COMPLETED.equals(session.getTaskStatus())) {
					response = "task completed";
					t.sendResponseHeaders(HTTP_STATUS_CODE_OK, response.length());				
				} else {
					response = "task failed";
					t.sendResponseHeaders(HTTP_STATUS_CODE_SERVER_ERROR, response.length());
				}
			}
			else {
				response = "request method not allowed";
				t.sendResponseHeaders(HTTP_STATUS_CODE_METHOD_NOT_ALLOWED, response.length());
			}			
		}
		
		// handle request on the health check endpoint
		else if (HEALTH_CHECK_ENDPOINT.equals(endpoint)) {
			Logging.printLogDebug(logger, "Received a request on HEALTH_CHECK_ENDPOINT.");
			response = "the server is fine";
			t.sendResponseHeaders(HTTP_STATUS_CODE_OK, response.length());
		}
		
		// not found
		else {
			response = "endpoint not found";
			t.sendResponseHeaders(HTTP_STATUS_CODE_NOT_FOUND, response.length());
		}
		
		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}

	private Request parseRequest(HttpExchange t) throws IOException {
		Request request = new Request();
		Headers headers = t.getRequestHeaders();

		request.setMessageId(headers.getFirst(MSG_ID_HEADER));
		request.setCityName(headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_CITY));
		request.setMarketName(headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_MARKET));
		request.setInternalId(headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_INTERNAL_ID));

		String processedIdString = headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_PROCESSED_ID);
		if (processedIdString != null) {
			request.setProcessedId(Long.parseLong(processedIdString));
		}

		request.setMessageBody(getRequestBody(t));			
		request.setQueueName(headers.getFirst(SQS_NAME_HEADER));

		return request;
	}
	
	/**
	 * 
	 * @param request
	 * @return
	 */
	private boolean checkRequest(Request request) {
		if (QueueName.IMAGES.equals(request.getQueueName())) {
			return checkImageTaskRequest(request);
		}
		
		if (request.getMarketName() == null) {
			Logging.printLogError(logger, "Request is missing field market name");
			return false;
		}
		if (request.getCityName() == null) {
			Logging.printLogError(logger, "Request is missing field city");
			return false;
		}
		
		if (QueueName.INSIGHTS.equals(request.getQueueName())) {
			if (Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION)) {
				if (request.getProcessedId() == null) {
					Logging.printLogError(logger, "Request is missing processed id");
					return false;
				}
				if (request.getInternalId() == null) {
					Logging.printLogError(logger, "Request is missing internal id");
					return false;
				}
			}
		}
		
		return true;
	}
	
	private static boolean checkImageTaskRequest(Request request) {
		if (request.getMarketName() == null) {
			Logging.printLogError(logger, "Request is missing market name");
			return false;
		}
		if (request.getType() == null) {
			Logging.printLogError(logger, "Request is missing image type");
			return false;
		}
		if (request.getCityName() == null) {
			Logging.printLogError(logger, "Request is missing city");
			return false;
		}
		if (request.getProcessedId() == null) {
			Logging.printLogError(logger, "Request is missing processed id");
			return false;
		}
		if (request.getInternalId() == null) {
			Logging.printLogError(logger, "Request is missing internal id");
			return false;
		}
		if (request.getNumber() == null) {
			Logging.printLogError(logger, "Request is missing image number");
			return false;
		}

		return true;
	}

	private String getRequestBody(HttpExchange t) throws IOException {
		InputStreamReader isr =  new InputStreamReader(t.getRequestBody(), "utf-8");
		BufferedReader br = new BufferedReader(isr);

		int b;
		StringBuilder buf = new StringBuilder(512);
		while ((b = br.read()) != -1) {
			buf.append((char) b);
		}

		br.close();
		isr.close();

		return buf.toString();
	}
}
