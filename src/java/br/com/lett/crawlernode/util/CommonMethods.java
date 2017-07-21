package br.com.lett.crawlernode.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.session.Session;


/**
 * This class contains common methods that can be used in any class within
 * crawler-node project.
 * 
 * @author Samir Leao
 *
 */
public class CommonMethods {
	
	private static String version = "1"; //TODO
	
	private static final Logger logger = LoggerFactory.getLogger(CommonMethods.class);

	/**
	 * 
	 * @return
	 * @throws NullPointerException
	 * @throws IOException
	 */
	public static String computeMD5(File file) throws IOException {
		String md5 = null;
		if (file != null) {
			FileInputStream fis = new FileInputStream(file);
			md5 = DigestUtils.md5Hex(fis);
			fis.close();
		}
		return md5;
	}
	
	/**
	 * Check wether the string contentType contains identifiers of binary content.
	 * This method is mainly used by the Parser, when fetching web pages content.
	 * @param contentType
	 * @return
	 */
	public static boolean hasBinaryContent(String contentType) {
		String typeStr = (contentType != null) ? contentType.toLowerCase() : "";

		return typeStr.contains("image") || typeStr.contains("audio") || typeStr.contains("video") ||
				typeStr.contains("application");
	}
	
	/**
	 * Check wether the string contentType contains identifiers of text content.
	 * This method is mainly used by the Parser, when fetching web pages content.
	 * Can be used to parse only the text of a web page, for example.
	 * @param contentType
	 * @return
	 */
	public static boolean hasPlainTextContent(String contentType) {
		String typeStr = (contentType != null) ? contentType.toLowerCase() : "";

		return typeStr.contains("text") && !typeStr.contains("html");
	}
	
	/**
	 * Modify an URL parameter with a new value.
	 * 
	 * @param url the URL to be modified
	 * @param parameter the name of the parameter to have it's value modified
	 * @param value the new value of the parameter
	 * @return an URL with the new value in the specified parameter
	 */
	public static String modifyParameter(String url, String parameter, String value) {
		try {
			List<NameValuePair> paramsOriginal = URLEncodedUtils.parse(new URI(url), "UTF-8");

			if (paramsOriginal.size() == 0) {
				return url;
			}

			else {

				List<NameValuePair> paramsNew = new ArrayList<NameValuePair>();

				for (NameValuePair param : paramsOriginal) {
					if ( param.getName().equals(parameter) ) {
						NameValuePair newParameter = new BasicNameValuePair(parameter, value);
						paramsNew.add(newParameter);
					}
					else {
						paramsNew.add(param);
					}
				}

				URIBuilder builder = new URIBuilder(url.split("\\?")[0]);

				builder.clearParameters();
				builder.setParameters(paramsNew);

				url = builder.build().toString();
				
				if (paramsNew.size() == 0) {
					url = url.replace("?", "").trim();
				}

				return url;
			}

		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	/**
	 * Print the stack trace of an exception on a String
	 * @param e the exception we want the stack trace from
	 * @return the string containing the stack trace
	 */
	public static String getStackTraceString(Exception e) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		e.printStackTrace(printWriter);
		
		return stringWriter.toString(); 
	}
	
	public static String getStackTrace(Throwable t) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		t.printStackTrace(printWriter);
		
		return stringWriter.toString();
	}
	
	public static void printStringToFile(String data, String path) {
		try {
			extracted(path).println(data);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static PrintWriter extracted(String path) throws FileNotFoundException {
		return new PrintWriter(path);
	}
	
	/**
	 * Fetch the current package version
	 * @return the string containing the version
	 */
	public static String getVersion() {
		
		if(version == null) {

			try {
				Properties properties = new Properties();
				properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("project.properties"));
				version = properties.getProperty("version");
			} catch (Exception e) { }
			
		} 
		
		return version;
	}
	
	public static boolean isString(Object object) {
		return object instanceof String;
	}

	public static boolean isInteger(Object object) {
		return object instanceof Integer;
	}

	/**
	 * 
	 * @param str
	 * @return
	 */
	public static String removeAccents(String str) {
		str = Normalizer.normalize(str, Normalizer.Form.NFD);
		str = str.replaceAll("[^\\p{ASCII}]", "");
		return str;
	}

	/**
	 * 
	 * @param str
	 * @return
	 */
	public static String removeParentheses(String str){

		if(str.contains("(")) {
			int x = str.indexOf("(");
			str = str.substring(0, x).replaceAll("'", "''").replaceAll("<", "").replaceAll(">", "");
		} else {
			return str.replaceAll("'", "''").replaceAll("<", "").replaceAll(">", "");
		}

		return str;
	}

	/**
	 * Delay in thread
	 */
	public static void delay() {
		int count = randInt(4, 9) * 1000;

		try {
			Thread.sleep(count);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Delay in thread with specific time
	 * @param delay
	 */
	public static void delay(int delay) {
		int count =  delay * 1000;

		try {
			Thread.sleep(count);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Generates a random integer in the interval between min and max
	 * @param min
	 * @param max
	 * @return
	 */
	public static int randInt(int min, int max) {

		// NOTE: Usually this should be a field rather than a method
		// variable so that it is not re-seeded every call.
		Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}
	
    
    /**
     * Rebuild a string as an URI and remove all
     * illegal characters.
     * 
     * @param url the url string
     * @return	<br>the rebuilded URL as a string
     * 			<br>the original string if any problem occurred during rebuild
     */
    public static String sanitizeUrl(String url) {
		try {
			URL urlObject = new URL(url.replaceAll("\\u00e2\\u0080\\u0093", "–"));
						
			URIBuilder uriBuilder = new URIBuilder()
					.setHost(urlObject.getHost())
					.setPath(urlObject.getPath())
					.setScheme(urlObject.getProtocol())
					.setPort(urlObject.getPort());					
			
			List<NameValuePair> params = getQueryMap(urlObject);
						
			if (params != null && !params.isEmpty()) {
				uriBuilder.setParameters(params);
			}
			
			// replace porque tem casos que a url tem um – e o apache não interpreta esse caracter
			return replaceSpecialCharacterDash(uriBuilder.build().toString());
		} catch (MalformedURLException | URISyntaxException e) {
			Logging.printLogError(logger, getStackTraceString(e));
			return url;
		}
    }
    
    /**
     * In some cases, url has this character –
     * So then when we encode this url, this character become like this %C3%A2%C2%80%C2%93 or this %25E2%2580%2593,
     * for resolve this problem, we replace this encode for %E2%80%93
     * @param str
     * @return
     */
    private static String replaceSpecialCharacterDash(String str) {
    	String finalStr = str;
    	
    	if(finalStr.contains("–")) {
    		finalStr = finalStr.replaceAll("–", "%E2%80%93");
    	}
    	
    	if(finalStr.contains("%C3%A2%C2%80%C2%93")) {
    		finalStr = finalStr.replaceAll("%C3%A2%C2%80%C2%93", "%E2%80%93");
    	}
    	
    	if(finalStr.contains("%25E2%2580%2593")) {
    		finalStr = finalStr.replaceAll("%25E2%2580%2593", "%E2%80%93");
    	}
    	
    	return finalStr;
    }
    
    /**
     * Parse all the parameters inside the url.
     * 
     * @param url a java.net.URL instance
     * @return  <br>an array list with NameValuePairs
     * 			<br>an empty array list if the url doesn't have any parameter
     */
    public static List<NameValuePair> getQueryMap(URL url) {  
		List<NameValuePair> queryMap = new ArrayList<>();	    
		String query = url.getQuery();

		if (query != null) {
			String[] params = query.split(Pattern.quote("&"));  
			for (String param : params) {
				String[] chunks = param.split(Pattern.quote("="));
				
				String name = chunks[0]; 
				String value = null;  
				
				if(chunks.length > 1) {
					value = chunks[1];
				}
				queryMap.add(new BasicNameValuePair(name, value));
			}
		}
		
		return queryMap;
	}
    
    /**
     * Check if the url contains a valid start with a valid protocol.
     * A valid start could be http:// or https://
     * If the url contains anything like http:/// for example, it won't
     * be a valid url string.
     * 
     * @param urlString
     * @return 	true if it's a valid url string
     * 			<br>false otherwise
     */
    public static boolean checkUrlStart(String urlString) {
		String protocolRegex = "(^https?://[^/])";  // -> the '?' tells we want s to be optional
													// -> the [^//] tells that the next character after the two slashes '//' cannot be another slash
		Pattern pattern = Pattern.compile(protocolRegex);
		Matcher matcher = pattern.matcher(urlString);
		
		return matcher.find();
	}

	/**
	 *
	 * @param driver
	 * @param path
	 * @param logger
	 */
	public static void takeAScreenShot(WebDriver driver, String path, Logger logger){
		CommonMethods.delay();
		
		File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
		// Now you can do whatever you need to do with it, for example copy somewhere
		try {
			FileUtils.copyFile(scrFile, new File(path + ".png"));
		} catch (IOException e) {
			Logging.printLogError(logger, getStackTrace(e));
		}
	}
	
	/**
	 * Convert jsonarray ro ArrayList<String>
	 * @param json
	 * @return
	 */
	public static ArrayList<String> convertJSONArrayToArrayListString(JSONArray json, Logger logger) {
		ArrayList<String> list = new ArrayList<>();
		
		for(int i = 0; i < json.length(); i++) {
			try {
				list.add(json.getString(i));
			} catch (JSONException e) {
				Logging.printLogError(logger, getStackTrace(e));
			}
		}
		
		return list;
	}
	
	/**
	 * Convert jsonarray to List<String>
	 * @param json
	 * @return
	 */
	public static List<String> convertJSONArrayToListString(JSONArray json, Logger logger) {
		List<String> list = new ArrayList<>();
		
		for(int i = 0; i < json.length(); i++) {
			try {
				list.add(json.getString(i));
			} catch (JSONException e) {
				Logging.printLogError(logger, getStackTrace(e));
			}
		}
		
		return list;
	}
	
	/**
	 * Get Intersection of two arrays
	 * @param a1
	 * @param a2
	 * @return
	 */
	public static List<?> getIntersectionOfTwoArrays(List<?> a1, List<?> a2) {
		List<Object> list = new ArrayList<>();
		
		for(int i = 0; i < a1.size(); i++) {
			Object obj = a1.get(i);
			
			if(a2.contains(obj) && !list.contains(obj)) {
				list.add(obj);
			}
		}
		
		return list;
	}
	
	/**
	 * Crawl skuJson from html in VTEX Sites
	 * @param document
	 * @param session
	 * @return
	 */
	public static JSONObject crawlSkuJsonVTEX(Document document, Session session) {
		Elements scriptTags = document.getElementsByTag("script");
		String scriptVariableName = "var skuJson_0 = ";
		JSONObject skuJson;
		String skuJsonString = null;
		
		for (Element tag : scriptTags){                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith(scriptVariableName)) {
					skuJsonString =
							node.getWholeData().split(Pattern.quote(scriptVariableName))[1] +
							node.getWholeData().split(Pattern.quote(scriptVariableName))[1].split(Pattern.quote("};"))[0];
					break;
				}
			}        
		}
		
		try {
			skuJson = new JSONObject(skuJsonString);
			
		} catch (JSONException e) {
			Logging.printLogError(logger, session, "Error creating JSONObject from var skuJson_0");
			Logging.printLogError(logger, session, getStackTraceString(e));
			
			skuJson = new JSONObject();
		}
		
		return skuJson;
	}
	
	/**
	 * Crawl json inside element html
	 * 
	 * @param doc
	 * @param cssElement
	 * @param token
	 * @param finalIndex
	 * @return
	 */
	public static JSONObject getSpecificJsonFromHtml(Document doc, String cssElement, String token, String finalIndex) {
		JSONObject object = new JSONObject();
		
		Elements scripts = doc.select(cssElement);
		
		for(Element e : scripts) {
			String script = e.outerHtml().replaceAll(" ", "");
			
			if(script.contains(token)) {
				int x = script.indexOf(token) + token.length();
				int y = script.indexOf(finalIndex, x);
				
				String json = script.substring(x, y);
				
				if(json.startsWith("{") && json.endsWith("}")) {
					object = new JSONObject(json);
				}
				
				break;
			}
		}
		
		
		return object;
	}
}
