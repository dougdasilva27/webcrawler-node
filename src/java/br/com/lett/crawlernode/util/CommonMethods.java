package br.com.lett.crawlernode.util;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.SkuStatus;
import br.com.lett.crawlernode.core.session.Session;
import okhttp3.HttpUrl;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * This class contains common methods that can be used in any class within crawler-node project.
 *
 * @author Samir Leao
 */
public class CommonMethods {

   private static String version = "1"; // TODO

   private static final Logger LOGGER = LoggerFactory.getLogger(CommonMethods.class);

   private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
   private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
   private static final Pattern EDGESDHASHES = Pattern.compile("(^-|-$)");

   /**
    * Get last position of array
    *
    * @param array
    * @return
    */
   public static <T> T getLast(T[] array) {
      return array[array.length - 1];
   }

   /**
    * Get last position of array
    *
    * @param <E>
    * @param array
    * @return
    */
   public static <E> E getLast(List<E> array) {
      if (!array.isEmpty()) {
         return array.get(array.size() - 1);
      }

      return null;
   }

   /**
    * Strip string if cross the limit and append '...'
    *
    * @param value  - string
    * @param length - limit
    * @return
    */
   public static String strip(String value, int length) {
      StringBuilder buf = new StringBuilder(value);
      if (buf.length() > length) {
         buf.setLength(length);
         buf.append("...");
      }

      return buf.toString();
   }

   public static String splitStringWithUpperCase(String str) {
      return str.replaceAll("(?!^)([A-Z])", " $1");
   }

   /**
    * @param delay
    */
   public static void delay(int delay) {
      int count = delay * 1000;

      try {
         Thread.sleep(count);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   /**
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
    * Check wether the string contentType contains identifiers of binary content. This method is mainly
    * used by the Parser, when fetching web pages content.
    *
    * @param contentType
    * @return
    */
   public static boolean hasBinaryContent(String contentType) {
      String typeStr = (contentType != null) ? contentType.toLowerCase() : "";

      return typeStr.contains("image") || typeStr.contains("audio") || typeStr.contains("video") || typeStr.contains("application");
   }

   /**
    * Check wether the string contentType contains identifiers of text content. This method is mainly
    * used by the Parser, when fetching web pages content. Can be used to parse only the text of a web
    * page, for example.
    *
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
    * @param url       the URL to be modified
    * @param parameter the name of the parameter to have it's value modified
    * @param value     the new value of the parameter
    * @return an URL with the new value in the specified parameter
    */
   public static String modifyParameter(String url, String parameter, String value) {
      try {
         List<NameValuePair> paramsOriginal = URLEncodedUtils.parse(new URI(url), "UTF-8");

         if (paramsOriginal.size() == 0) {
            return url;
         } else {

            List<NameValuePair> paramsNew = new ArrayList<NameValuePair>();

            for (NameValuePair param : paramsOriginal) {
               if (param.getName().equals(parameter)) {
                  NameValuePair newParameter = new BasicNameValuePair(parameter, value);
                  paramsNew.add(newParameter);
               } else {
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
    *
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
    *
    * @return the string containing the version
    */
   public static String getVersion() {

      if (version == null) {

         try {
            Properties properties = new Properties();
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("project.properties"));
            version = properties.getProperty("version");
         } catch (Exception e) {
         }

      }

      return version;
   }

   /**
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
    *
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
      return rand.nextInt((max - min) + 1) + min;
   }


   /**
    * Rebuild a string as an URI and remove all illegal characters.
    *
    * @param url the url string
    * @return <br>
    * the rebuilded URL as a string <br>
    * the original string if any problem occurred during rebuild
    */
   public static String sanitizeUrl(String url) {
      if (url != null) {
         try {
            URL urlObject = new URL(url.replaceAll("\\u00e2\\u0080\\u0093", "–"));

            URIBuilder uriBuilder = new URIBuilder().setHost(urlObject.getHost()).setPath(urlObject.getPath()).setScheme(urlObject.getProtocol())
               .setPort(urlObject.getPort());

            List<NameValuePair> params = getQueryMap(urlObject);

            if (!params.isEmpty()) {
               uriBuilder.setParameters(params);
            }

            // replace porque tem casos que a url tem um – e o apache não interpreta esse caracter
            return replaceSpecialCharacterDash(uriBuilder.build().toString());
         } catch (MalformedURLException | URISyntaxException e) {
            Logging.printLogWarn(LOGGER, getStackTraceString(e));
         }
      }

      return url;
   }

   /**
    * In some cases, url has this character – So then when we encode this url, this character become
    * like this %C3%A2%C2%80%C2%93 or this %25E2%2580%2593, for resolve this problem, we replace this
    * encode for %E2%80%93
    *
    * @param str
    * @return
    */
   private static String replaceSpecialCharacterDash(String str) {
      String finalStr = str;

      if (finalStr.contains("–")) {
         finalStr = finalStr.replaceAll("–", "%E2%80%93");
      }

      if (finalStr.contains("%C3%A2%C2%80%C2%93")) {
         finalStr = finalStr.replaceAll("%C3%A2%C2%80%C2%93", "%E2%80%93");
      }

      if (finalStr.contains("%25E2%2580%2593")) {
         finalStr = finalStr.replaceAll("%25E2%2580%2593", "%E2%80%93");
      }

      return finalStr;
   }

   /**
    * Parse all the parameters inside the url.
    *
    * @param url a java.net.URL instance
    * @return <br>
    * an array list with NameValuePairs <br>
    * an empty array list if the url doesn't have any parameter
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

            if (chunks.length > 1) {
               value = chunks[1];
            }
            queryMap.add(new BasicNameValuePair(name, value));
         }
      }

      return queryMap;
   }

   //get query parameter from url
   public static String getQueryParamFromUrl(String url, String queryParam) {
      HttpUrl httpUrl = HttpUrl.parse(url);

      if (httpUrl != null) {
         return httpUrl.queryParameter(queryParam);
      }

      return null;
   }

   /**
    * Check if the url contains a valid start with a valid protocol. A valid start could be http:// or
    * https:// If the url contains anything like http:/// for example, it won't be a valid url string.
    *
    * @param urlString
    * @return true if it's a valid url string <br>
    * false otherwise
    */
   public static boolean checkUrlStart(String urlString) {
      String protocolRegex = "(^https?://[^/])"; // -> the '?' tells we want s to be optional
      // -> the [^//] tells that the next character after the two slashes '//' cannot be another slash
      Pattern pattern = Pattern.compile(protocolRegex);
      Matcher matcher = pattern.matcher(urlString);

      return matcher.find();
   }

   /**
    * Remove illegal characters that do not belong to an xml or html
    *
    * @param s - string that contains part of an xml or html
    * @return
    */
   public static String stripNonValidXMLOrHTMLCharacters(String s) {
      StringBuilder validXML = new StringBuilder();

      char current; // Used to reference the current character.
      char[] charArray = s.toCharArray();

      if (s.isEmpty()) {
         return s;
      }

      for (int i = 0; i < charArray.length; i++) {

         current = charArray[i]; // NOTE: No IndexOutOfBoundsException caught here; it should not happen.

         if ((current == 0x9) || (current == 0xA) || (current == 0xD) || ((current >= 0x20) && (current <= 0xD7FF))
            || ((current >= 0xE000) && (current <= 0xFFFD)) || ((current >= 0x10000) && (current <= 0x10FFFF)))

            validXML.append(current);
      }

      return validXML.toString();
   }

   /**
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

   /**
    * @param path
    * @return
    */
   public static String readFile(String path) {
      StringBuilder str = new StringBuilder();

      try {
         FileReader fr = new FileReader(path);
         BufferedReader br = new BufferedReader(fr);

         String sCurrentLine;

         while ((sCurrentLine = br.readLine()) != null) {
            str.append(sCurrentLine);
         }

         br.close();
         fr.close();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return str.toString();
   }

   /**
    * Str must not be null and empty, if thath happens this function return ""
    *
    * @param str
    * @return
    */
   public static String upperCaseFirstCharacter(String str) {
      StringBuilder strBuilder = new StringBuilder();

      if (str != null && !str.isEmpty()) {
         strBuilder.append(str.substring(0, 1).toUpperCase());
         strBuilder.append(str.substring(1));
      }

      return strBuilder.toString();
   }

   /**
    * Encode url for ISO-8859-1
    *
    * @param str
    * @param logger
    * @param session
    * @return
    */
   public static String encondeStringURLToISO8859(String str, Logger logger, Session session) {
      String strEncoded = null;

      try {
         strEncoded = URLEncoder.encode(str, "ISO-8859-1");
      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }

      return strEncoded;
   }

   /**
    * Transform simple string to slug string
    *
    * @param input
    * @return
    */
   public static String toSlug(String input) {
      String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
      String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
      String slug = NONLATIN.matcher(normalized).replaceAll("");
      slug = EDGESDHASHES.matcher(slug).replaceAll("");
      return slug.toLowerCase(Locale.ENGLISH);
   }

   public static JSONArray listToJSONArray(List<?> list) {
      JSONArray array = new JSONArray();

      if (list != null) {
         for (Object o : list) {
            array.put(o);
         }
      }

      return array;
   }

   public static String cookiesToString(List<Cookie> cookies) {
      return cookies.stream()
         .map(c -> c.getName() + "=" + c.getValue() + ";")
         .collect(Collectors.joining(" "));
   }

   public static String substring(String str, String start, String end, Boolean between) {
      str = str == null ? "" : str;
      start = start == null ? "" : start;
      end = end == null ? "" : end;

      String result = "";
      int s = between ? str.indexOf(start) + start.length() : str.indexOf(start);
      int e = between ? str.indexOf(end, s) : str.indexOf(end, s) + end.length();

      if (s >= 0 && s < e && e <= str.length()) {
         result = str.substring(s, e);
      }
      return result;
   }

   public static ScraperInformation getScraperInformation(ResultSet resultSet) throws SQLException {
      ScraperInformation scraperInformation = new ScraperInformation(resultSet.getString("options_scraper"), resultSet.getString("options"), resultSet.getString("class"), resultSet.getString("name"), resultSet.getBoolean("use_browser"), resultSet.getString("proxies"));


      return scraperInformation;
   }

   public static Integer doublePriceToIntegerPrice(Double price, Integer defaultValue) {
      return price != null ? (int) (price * 100) : defaultValue;
   }

   public static Integer stringPriceToIntegerPrice(String priceStr, char decimalSeparator, Integer defaultValue) {
      Double price;

      if (priceStr != null && !priceStr.isEmpty()) {
         if (decimalSeparator == '.') {
            price = MathUtils.parseDoubleWithDot(priceStr);
         } else {
            price = MathUtils.parseDoubleWithComma(priceStr);
         }
         return doublePriceToIntegerPrice(price, defaultValue);
      }

      return defaultValue;
   }

   //convert camelcase to normal text
   public static String camelcaseToText(String str) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < str.length(); i++) {
         char c = str.charAt(i);
         if ((c >= 'A' && c <= 'Z' )|| (c >= 'À' && c <= 'Ü') ) {
            sb.append(" ");
         }
         sb.append(c);
      }
      return sb.toString();
   }

   /**
    * Transform a object to Double if possible
    *
    * @param object
    * @return Double || null
    */
   public static Double objectToDouble(Object object) {
      Double result = null;
      if (object instanceof Integer) {
         result = (double) (int) object;
      } else if (object instanceof Double) {
         result = (double) object;
      }

      return result;
   }

   public static SkuStatus getSkuStatus(Product product) {
      SkuStatus skuStatus;
      if (product.isVoid()) {
         skuStatus = SkuStatus.VOID;
      } else if (product.getAvailable()) {
         skuStatus = SkuStatus.AVAILABLE;
      } else if (product.getMarketplace() != null && product.getMarketplace().size() > 0) {
         skuStatus = SkuStatus.MARKETPLACE_ONLY;
      } else {
         skuStatus = SkuStatus.UNAVAILABLE;
      }

      return skuStatus;
   }

}
