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

import br.com.lett.crawlernode.core.server.endpoints.CrawlerTaskEndpoint;
import br.com.lett.crawlernode.core.server.request.ImageCrawlerRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.server.request.checkers.CrawlerTaskRequestChecker;
import br.com.lett.crawlernode.queue.QueueName;
import br.com.lett.crawlernode.util.Logging;

public class ServerHandler implements HttpHandler {

	protected static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

	public static final String POST = "POST";
	public static final String GET = "GET";

	private static final String MSG_ATTR_HEADER_PREFIX = "X-aws-sqsd-attr-";

	private static final String MSG_ATTR_MARKET_ID = "marketId";
	private static final String MSG_ATTR_PROCESSED_ID = "processedId";
	private static final String MSG_ATTR_INTERNAL_ID = "internalId";
	private static final String MSG_ATTR_IMG_NUMBER = "number";
	private static final String MSG_ATTR_IMG_TYPE = "type";

	private static final String MSG_ID_HEADER = "X-aws-sqsd-msgid";
	private static final String SQS_NAME_HEADER = "X-aws-sqsd-queue";

	@Override
	public void handle(HttpExchange t) throws IOException {		
		String endpoint = t.getHttpContext().getPath();
		String response;

		// handle request on the task endpoint
		if (Server.ENDPOINT_TASK.equals(endpoint)) {
			Logging.printLogDebug(logger, "Received a request on TASK_ENDPOINT.");
			
			Logging.printLogDebug(logger, "parsing request....");
			Request request = parseRequest(t);
			Logging.printLogDebug(logger, request.toString());

			if (CrawlerTaskRequestChecker.checkRequestMethod(request)) {
				if (CrawlerTaskRequestChecker.checkRequest(request)) {
					response = CrawlerTaskEndpoint.perform(t, request);
				} else {
					response = Server.MSG_BAD_REQUEST;
					t.sendResponseHeaders(Server.HTTP_STATUS_CODE_BAD_REQUEST, response.length());
				}
			} else {
				response = Server.MSG_METHOD_NOT_ALLOWED;
				t.sendResponseHeaders(Server.HTTP_STATUS_CODE_METHOD_NOT_ALLOWED, response.length());
			}
		}

		// handle request on the health check endpoint
		else {
			Logging.printLogDebug(logger, "Received a request on HEALTH_CHECK_ENDPOINT.");
			
			response = Server.MSG_SERVER_HEALTH_OK;
			t.sendResponseHeaders(Server.HTTP_STATUS_CODE_OK, response.length());
		}

		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}

	private Request parseRequest(HttpExchange t) throws IOException {
		Headers headers = t.getRequestHeaders();
		String queueName = headers.getFirst(SQS_NAME_HEADER);
		Request request;
		
		if (QueueName.IMAGES.equals(queueName)) {
			request = new ImageCrawlerRequest();
		} else {
			request = new Request();
		}

		request.setRequestMethod(t.getRequestMethod().toUpperCase());

		request.setMessageId(headers.getFirst(MSG_ID_HEADER));
		request.setInternalId(headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_INTERNAL_ID));
		
		String marketIdString = headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_MARKET_ID);
		if (marketIdString != null) {
			request.setMarketId(Integer.parseInt(marketIdString));
		}

		String processedIdString = headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_PROCESSED_ID);
		if (processedIdString != null) {
			request.setProcessedId(Long.parseLong(processedIdString));
		}

		request.setMessageBody(getRequestBody(t));			
		request.setQueueName(queueName);
		
		if (request instanceof ImageCrawlerRequest) {
			((ImageCrawlerRequest) request).setImageNumber(Integer.parseInt(headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_IMG_NUMBER)));
			((ImageCrawlerRequest) request).setImageType(headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_IMG_TYPE));
		}

		return request;
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
