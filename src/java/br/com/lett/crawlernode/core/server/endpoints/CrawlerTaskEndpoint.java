package br.com.lett.crawlernode.core.server.endpoints;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.ServerConstants;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionFactory;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.base.TaskFactory;
import br.com.lett.crawlernode.main.Main;

public class CrawlerTaskEndpoint {

	public static String perform(HttpExchange t, Request request) throws IOException {
		String response;

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
			response = ServerConstants.MSG_TASK_COMPLETED;
			t.sendResponseHeaders(ServerConstants.HTTP_STATUS_CODE_OK, response.length());				
		} else {
			response = ServerConstants.MSG_TASK_FAILED;
			t.sendResponseHeaders(ServerConstants.HTTP_STATUS_CODE_SERVER_ERROR, response.length());
		}

		return response;
	}

}
