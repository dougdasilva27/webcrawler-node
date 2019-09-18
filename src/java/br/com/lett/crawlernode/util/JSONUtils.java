package br.com.lett.crawlernode.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JSONUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(JSONUtils.class);

  public static Object getValueWithPath(JSONObject json, String path) {
    Object value = null;

    JSONObject finalJson = json;
    String[] keys = path.split("=>");
    String finalKey = CommonMethods.getLast(keys);

    for (String key : keys) {
      if (finalJson.has(key) && finalJson.get(key) instanceof JSONObject) {
        finalJson = finalJson.getJSONObject(key);
      } else {
        break;
      }
    }

    if (finalJson.has(finalKey) && !finalJson.isNull(finalKey)) {
      value = json.get(finalKey);
    }

    return value;
  }

  public static Object getValue(JSONObject json, String key) {
    Object value = null;

    if (json.has(key) && !json.isNull(key)) {
      value = json.get(key);
    }

    return value;
  }

  public static JSONObject getJSONValue(JSONObject json, String key) {
    JSONObject value = new JSONObject();

    if (json.has(key) && json.get(key) instanceof JSONObject) {
      value = json.getJSONObject(key);
    }

    return value;
  }

  public static JSONArray getJSONArrayValue(JSONObject json, String key) {
    JSONArray value = new JSONArray();

    if (json.has(key) && json.get(key) instanceof JSONArray) {
      value = json.getJSONArray(key);
    }

    return value;
  }

  public static String getStringValue(JSONObject json, String key) {
    String value = null;

    if (json.has(key) && json.get(key) instanceof String) {
      value = json.getString(key);
    }

    return value;
  }

  public static JSONObject stringToJson(String str) {
    JSONObject json = new JSONObject();

    if (str.trim().startsWith("{") && str.trim().endsWith("}")) {
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

    if (str.trim().startsWith("[") && str.trim().endsWith("]")) {
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
}
