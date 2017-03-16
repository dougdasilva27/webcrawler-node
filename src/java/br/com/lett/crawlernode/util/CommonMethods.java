package br.com.lett.crawlernode.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;


/**
 * This class contains common methods that can be used in any class within
 * crawler-node project.
 * @author Samir Leao
 *
 */
public class CommonMethods {
	
	private static String version = "1"; //TODO
	
	
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
	

	/**
	 * String
	 * @param object 
	 * @return Boolean - se é string ou não
	 */
	public static boolean isString(Object object) {
		if (object instanceof String) {
			return true;
		}

		return false;
	}

	/**
	 * Integer
	 * @param object 
	 * @return Boolean - se é Integer ou não
	 */
	public static boolean isInteger(Object object) {
		if (object instanceof Integer) {
			return true;
		}

		return false;
	}

	/**
	 * Long
	 * @param object 
	 * @return Boolean - se é Long ou não
	 */
	public static boolean isLong(Object object) {
		if (object instanceof Long) {
			return true;
		}

		return false;
	}

	/**
	 * Double
	 * @param object 
	 * @return Boolean - se é Double ou não
	 */
	public static boolean isDouble(Object object) {
		if (object instanceof Double) {
			return true;
		}

		return false;
	}

	/**
	 * Boolean
	 * @param object 
	 * @return Boolean - se é Boolean ou não
	 */
	public static boolean isBoolean(Object object) {
		if (object instanceof Boolean) {
			return true;
		}

		return false;
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
     * Replace argument "`" to "%60" from url
     * @param url
     * @return urlFinal
     */
    public static String removeIllegalArguments(String url){
    	String finalUrl = url;
    	
    	// comentei porque não estava funcionando.
    	
    	if(url.contains(" ")) {
    		finalUrl = url.replaceAll(" ", "%20");
    	}
    	
//    	// In cases with argument (`), it is repalce to %60
//    	if(url.contains("`")){
//    		finalUrl = url.replaceAll("`","%60");
//    	}
//    	
//    	// In cases with argument (´), it is repalce to %C2%B4
//    	if(url.contains("´")){
//    		finalUrl = url.replaceAll("´","%C2%B4");
//    	}
    	
    	
    	return finalUrl;
    }
    
	/**
	 * Take a screenshot from page in webdriver
	 * @param driver
	 * @param nameShot
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
}
