package br.com.lett.crawlernode.core.server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class WebcrawlerServer implements HttpHandler {
	
	private static final String POST = "POST";
	
	private static final int HTTP_STATUS_CODE_OK = 200;
	private static final int HTTP_STATUS_CODE_SERVER_ERROR = 500;
	
	@Override
	public void handle(HttpExchange t) throws IOException {
//		System.out.println("Handling request in thread " + Thread.currentThread().getId() + " ...");
//		
//		String requestMethod = t.getRequestMethod().toUpperCase();
//		if (POST.equals(requestMethod)) {
//			
//			System.out.println("Handling POST request...");
//			
//			
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
//		}		
	}
	
	private BeanstalkRequest parseBeanstalkRequest(String request) throws IOException {
    	JSONObject requestObject = new JSONObject(request);
		BeanstalkRequest beanstalkRequest = new BeanstalkRequest();
    	
    	if (requestObject.has("environment")) {
    		beanstalkRequest.setEnvironment(requestObject.getString("environment"));
    	}
    	if (requestObject.has("mode")) {
    		beanstalkRequest.setMode(requestObject.getString("mode"));
    	}
    	if (requestObject.has("toDate")) {
    		beanstalkRequest.setToDate(requestObject.getString("toDate"));
    	}
    	if (requestObject.has("fromDate")) {
    		beanstalkRequest.setFromDate(requestObject.getString("fromDate"));
    	}
    	if (requestObject.has("debug")) {
    		beanstalkRequest.setDebug(requestObject.getBoolean("debug"));
    	}
    	
        return beanstalkRequest;
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
