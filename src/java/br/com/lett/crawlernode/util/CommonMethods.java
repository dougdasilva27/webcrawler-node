package br.com.lett.crawlernode.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

/**
 * This class contains common methods that can be used in any class within
 * crawler-node project.
 * @author Samir Leao
 *
 */
public class CommonMethods {
	
	private static String version = "1"; //TODO
	
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
	 * Remove the attempt parameter from the url. Used in the scheduleRepeatedSeed method.
	 * @param url
	 * @return
	 */
	public static String removeAttemptParam(String url) {
		try {
			List<NameValuePair> paramsOriginal = URLEncodedUtils.parse(new URI(url), "UTF-8");

			if (paramsOriginal.size() == 0) {
				return url;
			}

			else {

				List<NameValuePair> paramsNew = new ArrayList<NameValuePair>();

				for (NameValuePair param : paramsOriginal) {
					if ( !param.getName().contains("attempt") ) {
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
	 * URL modifier to be used in the scheduleRepeatedSeed method, to set the attempt parameter in the url.
	 * @param url
	 * @param attempt
	 * @return
	 */
	public static String setAttemptParameter(String url, int attempt) {
		try {
			List<NameValuePair> paramsOriginal = URLEncodedUtils.parse(new URI(url), "UTF-8");
			List<NameValuePair> paramsNew = new ArrayList<NameValuePair>();

			for (NameValuePair param : paramsOriginal) {
				if ( !param.getName().contains("attempt") ) {
					paramsNew.add(param);
				}
			}

			paramsNew.add(new BasicNameValuePair("attempt", String.valueOf(attempt)));

			URIBuilder builder = new URIBuilder(url.split("\\?")[0]);

			builder.clearParameters();
			builder.setParameters(paramsNew);

			url = builder.build().toString();

			return url;

		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Get the attemp number looking in the attempt parameter in url.
	 * @param url
	 * @return
	 */
	public static Integer getAttempt(String url) {
		try {
			List<NameValuePair> paramsOriginal = URLEncodedUtils.parse(new URI(url), "UTF-8");
			NameValuePair parameterAttempt = null;

			for (NameValuePair param : paramsOriginal) {
				if ( param.getName().contains("attempt") ) {
					parameterAttempt = param;
					break;
				}
			}

			if (parameterAttempt != null) {
				return Integer.valueOf(parameterAttempt.getValue());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return 0;
	}
	
	/**
	 * Modify an URL parameter with a new value
	 * @param url the URL to be modified
	 * @param parameter the name of the paramter to have it's value modified
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
					if ( param.getName().contains(parameter) ) {
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

}
