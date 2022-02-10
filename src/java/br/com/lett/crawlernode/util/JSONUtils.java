package br.com.lett.crawlernode.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class JSONUtils {

   private static final Logger LOGGER = LoggerFactory.getLogger(JSONUtils.class);

   public static Object getValue(JSONObject json, String key) {
      Object value = null;

      if (json.has(key) && !json.isNull(key)) {
         value = json.get(key);
      }

      return value;
   }

   public static JSONObject getJSONValue(JSONObject json, String key) {
      JSONObject value = new JSONObject();

      if (json != null && json.has(key) && json.get(key) instanceof JSONObject) {
         value = json.getJSONObject(key);
      }

      return value;
   }

   /**
    * access internal field in json
    * ex1: {"key1":{"key2": "value"}} -> String value = getValueRecursive(json, ".", "key1.key2", String.class, "")
    * ex2: {"key1":[{"key2":2}]}      -> Integer value = getValueRecursive(json, ".", "key1.0.key2", Integer.class, 0)
    *
    * @param json         -> JSONObject ou JSONArray
    * @param path         -> field path in JSON;
    * @param separator    -> string regex for split path;
    * @param clazz        ->
    * @param defaultValue -> if null, return defaultValue
    * @return T
    */
   public static <T> T getValueRecursive(Object json, String path, String separator, Class<T> clazz, T defaultValue) {

      try {
         String[] keys = path.split("["+separator+"]");

         Object currentObject = json;

         for (String key : keys) {
            if (currentObject instanceof JSONObject) {
               currentObject = ((JSONObject) currentObject).opt(key);
            } else if (currentObject instanceof JSONArray) {
               int keyInt = Integer.parseInt(key);
               if (keyInt >= 0 && keyInt < ((JSONArray) currentObject).length()) {
                  currentObject = ((JSONArray) currentObject).opt(keyInt);
               }
            }

            if (currentObject == null) {
               return defaultValue;
            }
         }


         return clazz.cast(currentObject);
      } catch (Exception e) {
         return defaultValue;
      }
   }

   public static <T> T getValueRecursive(Object json, String path, Class<T> clazz) {

      return getValueRecursive(json, path, clazz, null);
   }

   public static <T> T getValueRecursive(Object json, String path, Class<T> clazz, T defaultValue) {

      return getValueRecursive(json, path, ".", clazz, defaultValue);
   }

   public static JSONArray getJSONArrayValue(JSONObject json, String key) {
      JSONArray value = new JSONArray();

      if (json != null && json.has(key) && json.get(key) instanceof JSONArray) {
         value = json.getJSONArray(key);
      }

      return value;
   }

   public static String getStringValue(JSONObject json, String key) {
      String value = null;

      if (json!=null && json.has(key) && json.get(key) instanceof String) {
         value = json.getString(key);
      }

      return value;
   }

   public static JSONObject stringToJson(String str) {
      JSONObject json = new JSONObject();

      if (str != null && str.trim().startsWith("{") && str.trim().endsWith("}")) {
         try {
            // We use gson to parse because this library treats "\n" and duplicate keys
            JsonObject gson = ((JsonObject) new JsonParser().parse(str.trim()));
            json = new JSONObject(gson.toString());
         } catch (Exception e1) {
            Logging.printLogWarn(LOGGER, CommonMethods.getStackTrace(e1));
         }
      }

      return json;
   }

   public static JSONArray stringToJsonArray(String str) {
      JSONArray json = new JSONArray();

      if (str != null && str.trim().startsWith("[") && str.trim().endsWith("]")) {
         try {
            json = new JSONArray(str.trim());
         } catch (Exception e1) {
            Logging.printLogWarn(LOGGER, CommonMethods.getStackTrace(e1));
         }
      }

      return json;
   }

   /**
    * 
    * @param json
    * @param key
    * @param stringWithFloatLayout -> if price string is a float in a string format like "23.99", if
    *        false e.g: R$ 2.779,20 returns the Float 2779.2
    * 
    * @return
    */
   public static Float getFloatValueFromJSON(JSONObject json, String key, boolean stringWithFloatLayout) {
      Float price = null;

      if (json.has(key)) {
         Object priceObj = json.get(key);

         if (priceObj instanceof Integer) {
            price = MathUtils.normalizeTwoDecimalPlaces(((Integer) priceObj).floatValue());
         } else if (priceObj instanceof Double) {
            price = MathUtils.normalizeTwoDecimalPlaces(((Double) priceObj).floatValue());
         } else {
            if (stringWithFloatLayout) {
               price = MathUtils.parseFloatWithDots(priceObj.toString());
            } else {
               price = MathUtils.parseFloatWithComma(priceObj.toString());
            }
         }
      }

      return price;
   }

   /**
    * 
    * @param json
    * @param key
    * @param stringWithDoubleLayout -> if price string is a double in a string format like "23.99", if
    *        false e.g: R$ 2.779,20 returns the Double 2779.2
    * @return
    */
   public static Double getDoubleValueFromJSON(JSONObject json, String key, boolean stringWithDoubleLayout) {
      Double doubleValue = null;

      if (json.has(key)) {
         Object obj = json.get(key);

         if (obj instanceof Integer) {
            doubleValue = ((Integer) obj).doubleValue();
         } else if (obj instanceof Double) {
            doubleValue = (Double) obj;
         } else {
            if (stringWithDoubleLayout) {
               doubleValue = MathUtils.parseDoubleWithDot(obj.toString());
            } else {
               doubleValue = MathUtils.parseDoubleWithComma(obj.toString());
            }
         }
      }

      return doubleValue;
   }

   /**
    * 
    * @param json
    * @param key
    * @param defaultValue - return this value if key not exists
    * @return
    */
   public static Integer getIntegerValueFromJSON(JSONObject json, String key, Integer defaultValue) {
      Integer value = defaultValue;

      if (json.has(key)) {
         Object valueObj = json.get(key);

         if (valueObj instanceof Integer) {
            value = (Integer) valueObj;
         } else {
            String text = valueObj.toString().replaceAll("[^0-9]", "");

            if (!text.isEmpty()) {
               value = Integer.parseInt(text);
            }
         }
      }

      return value;
   }



   /**
    * This method simply adds all the string values inside a json array to a string list.
    * @param jsonArray Json Array.
    * @return String list with all the values inside the array
    */
   public static List<String> jsonArrayToStringList(JSONArray jsonArray){
      return jsonArrayToStringList(jsonArray, null, null);
   }

   /**
    * This method simply adds all the string values inside a json array to a string list.
    * The path is similar with the getValueRecursive method.
    * Example: 'produts.images.url'
    * @param jsonArray Json Array.
    * @param path If its a {@link JSONObject} array, it's necessary to specify the path containing the key that must be added to the list.
    *              If its array of strings, just pass a null path.
    * @return String list with all the values inside the array
    */
   public static List<String> jsonArrayToStringList(JSONArray jsonArray, String path){
      return jsonArrayToStringList(jsonArray, path, null);
   }

   /**
    * This method simply adds all the string values inside a json array to a string list.
    * The path is similar with the getValueRecursive method.
    * Example: 'produts.images.url'
    * @param jsonArray Json Array.
    * @param path If its a {@link JSONObject} array, it's necessary to specify the path containing the key that must be added to the list.
    *              If its array of strings, just pass a null path.
    * @param pathSeparator Separator to the json keys in the path, default '.'
    * @return String list with all the values inside the array
    */
   public static List<String> jsonArrayToStringList(JSONArray jsonArray, String path, String pathSeparator) {
      List<String> list = new ArrayList<>();

      if(jsonArray != null && !jsonArray.isEmpty()){
         if(path != null && !path.equals("")){
            if(pathSeparator == null){
               pathSeparator = ".";
            }

            for (Object o : jsonArray) {
               JSONObject jsonObject = (JSONObject) o;

               String str = getValueRecursive(jsonObject, path, pathSeparator, String.class, null);

               list.add(str);
            }
         }else{
            for (Object o : jsonArray) {
               list.add(o.toString());
            }
         }
      }

      return list;
   }

   /**
    * This extracts a double or integer value and convert it to cents.
    * @param product JSONObject where we can find the desired value .
    * @param key String with the key that we want to extract.
    * @return integer with the value in cents.
    */
   public static int getPriceInCents(JSONObject product, String key) {
      int priceInCents = 0;
      Object price = product.opt(key);
      if (price instanceof Double) {
         priceInCents = (int) Math.round((Double) price * 100);
      } else if (price instanceof Integer) {
         priceInCents = (int) price * 100;
      }
      return priceInCents;
   }
}
