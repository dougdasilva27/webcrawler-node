package br.com.lett.crawlernode.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
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
	 * Parses a Float from an input String. It will parse
	 * only the first match. If there is more than one float in the string,
	 * the others occurrences after the first will be disconsidered.
	 * 
	 * e.g:
	 * R$ 2.779,20 returns the Float 2779.2
	 * 
	 * @param input
	 * @return
	 */
	public static Float parseFloat(String input) {
		return Float.parseFloat( input.replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".") );
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
	 * Parse all numbers from a string and returns a list containing
	 * all the found numbers.
	 * 
	 * @param s
	 * @return
	 */
	public static List<String> parseNumbers(String s) {
		List<String> numbers = new ArrayList<String>();
		Pattern p = Pattern.compile("-?\\d+");
		Matcher m = p.matcher(s);
		while (m.find()) {
		  numbers.add(m.group());
		}
		return numbers;
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
	 * Given a list of ordered disjoint intervals, select the interval where
	 * the number n fits, using a binary search algorithm.
	 * 
	 * @param n
	 * @param intervals
	 * @return
	 */
	public static Interval<Integer> findInterval(List<Interval<Integer>> intervals, Integer n) {
		if (intervals == null) return null;
		
		int beg = 0;
		int end = intervals.size() - 1;
		
		while(beg <= end) {
			int mid = (beg + end)/2;
			if (intervals.get(mid).getStart() <= n && intervals.get(mid).getEnd() >= n) {
				return intervals.get(mid);
			} else if (intervals.get(mid).getStart() <= n) {
				beg = mid + 1;
			} else {
				end = mid - 1;
			}
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
     * Round and normalize Double to have only two decimal places
     * eg: 23.45123 --> 23.45
     * If number is null, the method returns null.
     * 
     * @param number
     * @return A rounded Double with only two decimal places
     */
    public static Float normalizeTwoDecimalPlaces(Float number) {
        if (number == null) return null;
        
        BigDecimal big = new BigDecimal(number);
        String rounded = big.setScale(2, BigDecimal.ROUND_HALF_EVEN).toString();
        
        return Float.parseFloat(rounded);
    }

    /**
     * Replace argument "`" to "%60" from url
     * @param url
     * @return urlFinal
     */
    public static String removeIllegalArguments(String url){
    	String finalUrl = url;
    	
    	// comentei porque não estava funcionando.
    	
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
}
