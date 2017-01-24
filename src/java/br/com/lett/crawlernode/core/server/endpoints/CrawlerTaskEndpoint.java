package br.com.lett.crawlernode.core.server.endpoints;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import br.com.lett.crawlernode.core.server.Server;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionFactory;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.base.TaskFactory;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.Logging;

public class CrawlerTaskEndpoint {
	
	protected static final Logger logger = LoggerFactory.getLogger(CrawlerTaskEndpoint.class);

	public static String perform(HttpExchange t, Request request) throws IOException {
		String response;

		Logging.printLogDebug(logger, "creating session....");
		Session session = SessionFactory.createSession(request, Main.markets);

		// create the task
		Logging.printLogDebug(logger, session, "creating task for " + session.getOriginalURL());
		Task task = TaskFactory.createTask(session);
		Logging.printLogDebug(logger, session, "created task " + task.getClass().getSimpleName());

		// perform the task
		Logging.printLogDebug(logger, session, "processing task....");
		task.process();

		// check final task status
		if (Task.STATUS_COMPLETED.equals(session.getTaskStatus())) {
			response = Server.MSG_TASK_COMPLETED;
			t.sendResponseHeaders(Server.HTTP_STATUS_CODE_OK, response.length());
			Main.server.incrementSucceededTasks();
		} else {
			response = Server.MSG_TASK_FAILED;
			t.sendResponseHeaders(Server.HTTP_STATUS_CODE_SERVER_ERROR, response.length());
			Main.server.incrementFailedTasks();
		}

		return response;
	}

}
