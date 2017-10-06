package br.com.lett.crawlernode.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
	 * Get last position of array
	 * @param array
	 * @return
	 */
	public static <T> T getLast(T[] array) {
	    return array[array.length - 1];
	}
	
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
	 *	e.g:
	 *	vtxctx = {
	 *		skus:"825484",
	 *		searchTerm:"",
	 *		categoryId:"38",
	 *		categoryName:"Leite infantil",
	 *		departmentyId:"4",
	 *		departmentName:"Infantil",
	 *		url:"www.araujo.com.br"
	 *	};
	 *
	 *	token = "vtxctx="
	 *	finalIndex = ";"
	 * 
	 * @param doc
	 * @param cssElement selector used to get the desired json element
	 * @param token whithout spaces
	 * @param finalIndex
	 * @return JSONObject
	 * 
	 * @throws JSONException
	 * @throws ArrayIndexOutOfBoundsException if finalIndex doesn't exists or there is a duplicate 
	 * @throws IllegalArgumentException if doc is null
	 */
	public static JSONObject selectJsonFromHtml(Document doc, String cssElement, String token, String finalIndex) 
			throws JSONException, ArrayIndexOutOfBoundsException, IllegalArgumentException{
		
		if(doc == null) throw new IllegalArgumentException("Argument doc cannot be null");
		
		JSONObject object = new JSONObject();
		
		Elements scripts = doc.select(cssElement);
		
		for(Element e : scripts) {
			String script = e.outerHtml().replace(" ", "");
			
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
	
	/**
	 *
	 * @param body
	 * @param path
	 */
	public static void saveDataToAFile(Object body, String path) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			
			out.write(body.toString());
			out.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	
}
