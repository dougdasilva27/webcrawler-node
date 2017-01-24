package br.com.lett.crawlernode.core.server.endpoints;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import br.com.lett.crawlernode.core.server.Server;

public class CrawlerHealthEndpoint {
	
	protected static final Logger logger = LoggerFactory.getLogger(CrawlerHealthEndpoint.class);

	public static String perform(HttpExchange t) throws IOException {
		String response = Server.MSG_SERVER_HEALTH_OK;
		t.sendResponseHeaders(Server.HTTP_STATUS_CODE_OK, response.length());

		return response;
	}

}
