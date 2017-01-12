package br.com.lett.crawlernode.core.server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionFactory;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.base.TaskFactory;
import br.com.lett.crawlernode.main.Main;

public class ServerHandler implements HttpHandler {

	private static final String POST = "POST";

	private static final int HTTP_STATUS_CODE_OK = 200;
	private static final int HTTP_STATUS_CODE_SERVER_ERROR = 500;

	private static final String MSG_ATTR_HEADER_PREFIX = "X-aws-sqsd-attr-";

	private static final String MSG_ATTR_MARKET = "market";
	private static final String MSG_ATTR_CITY = "city";
	private static final String MSG_ATTR_PROCESSED_ID = "processedId";
	private static final String MSG_ATTR_INTERNAL_ID = "internalId";

	private static final String MSG_ID_HEADER = "X-aws-sqsd-msgid";
	private static final String SQS_NAME_HEADER = "X-aws-sqsd-queue";

	@Override
	public void handle(HttpExchange t) throws IOException {
		
		String requestMethod = t.getRequestMethod().toUpperCase();
		
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
			
			
			//			String taskType = null;
			//			
			//			// Passo 1 - Descobrir tipo da tarefa
			//			// ...
			//			
			//			taskType = "product"; //FIXME
			//			
			//			// Passo 2 - Instanciar tarefa correpsondente
			//			
			//			Task task;
			//			
			//			switch (taskType) {
			//			case "product": task = new ProductTask(); break;
			//			case "image": task = new ImageTask(); break;
			//
			//			default:
			//				break;
			//			}
			//						
			//			// Passo 3 - Executar tarefa
			//			task.execute();
			//			
			//			public void execute() {
			//				
			//				// Passo 1
			//				this.onStart();
			//				
			//				// Passo 2
			//				this.executeTask();
			//				
			//				// Passo 3
			//				this.onFinish();
			//				
			//			}
			//			
			//			try {
			//				System.out.println("waiting 5 seconds...");
			//				Thread.sleep(60000);
			//			} catch (InterruptedException e) {
			//				e.printStackTrace();
			//			}
			//			
			//			
			//			
			//			
			////			String request = getRequestBody(t);
			////			BeanstalkRequest beanstalkRequest = parseBeanstalkRequest(request);
			//			
			//			String response = "Received request: " + t.getRequestBody();
			//			t.sendResponseHeaders(HTTP_STATUS_CODE_OK, response.length());
			//			
			//			OutputStream os = t.getResponseBody();
			//			os.write(response.getBytes());
			//			os.close();
		}		
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
