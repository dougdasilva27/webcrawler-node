package br.com.lett.crawlernode.util;

import br.com.lett.crawlernode.database.DatabaseDataFetcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static br.com.lett.crawlernode.util.CrawlerUtils.stringToJson;

public class ScraperInformation {


   private JSONObject optionsScraper;
   private JSONObject optionsScraperClass;
   private String className;
   private String name;
   private String proxiesMarket;
   private boolean useBrowser;

   private boolean isMiranha;
   private JSONArray proxies;
   private JSONObject options;

   public ScraperInformation(String optionsScraper, String optionsScraperClass, String className, String name, boolean useBrowser, String proxiesMarket) {

      this.optionsScraper = stringToJson(optionsScraper);
      this.optionsScraperClass = stringToJson(optionsScraperClass);
      this.className = className;
      this.name = name;
      this.useBrowser = useBrowser;
      this.proxiesMarket = proxiesMarket;

      setOptions(this.optionsScraper, this.optionsScraperClass, proxiesMarket);
      setMiranha(this.optionsScraper, this.optionsScraperClass);

   }

   public String getProxiesMarket() {
      return proxiesMarket;
   }

   public JSONObject getOptionsScraper() {
      return optionsScraper;
   }

   public JSONObject getOptionsScraperClass() {
      return optionsScraperClass;
   }


   public String getName() {
      return name;
   }

   public String getClassName() {
      return className;
   }

   public boolean isUseBrowser() {
      return useBrowser;
   }

   public boolean isMiranha() {
      return isMiranha;
   }

   public void setMiranha(JSONObject optionsScraper, JSONObject optionsScraperClass) {
      if ((optionsScraper != null && optionsScraper.optBoolean("is_miranha")) || (optionsScraperClass != null && optionsScraperClass.optBoolean("is_miranha"))) {
         this.isMiranha = true;
         setProxies();
      } else {
         this.isMiranha = false;
      }
   }

   public JSONArray getProxies() {
      return proxies;
   }

   public void setProxies() {
      JSONArray proxies = this.options.optJSONArray("proxies");
      this.proxies =  DatabaseDataFetcher.fetchProxiesFromMongoFetcher(proxies);
   }

   public JSONObject getOptions() {
      return options;
   }

   public void setOptions(JSONObject result, JSONObject optionsScraperSuperClassJson, String proxiesMarket) throws JSONException {
      String keyProxies = "proxies";

      if (!result.has(keyProxies)) {
         JSONArray superClassProxies = optionsScraperSuperClassJson.optJSONArray(keyProxies);
         if (superClassProxies != null && superClassProxies.length() > 0) {
            result.put(keyProxies, optionsScraperSuperClassJson.get(keyProxies));
         } else {
            result.put(keyProxies, new JSONArray(proxiesMarket));
         }
      }

      if (optionsScraperSuperClassJson.has(keyProxies)) {
         optionsScraperSuperClassJson.remove(keyProxies);
      }
      if (optionsScraperSuperClassJson.has("recipe")) {
         JSONObject recipe = getRecipe(result, optionsScraperSuperClassJson);
         optionsScraperSuperClassJson.remove("recipe");
         result.put("recipe", recipe);
      }

      String[] keys = JSONObject.getNames(optionsScraperSuperClassJson);
      if (keys != null && keys.length > 0) {

         for (String key : JSONObject.getNames(optionsScraperSuperClassJson)) {
            if (!result.has(key)) {
               result.put(key, optionsScraperSuperClassJson.get(key));
            }
         }

         result.put("scraperClass", optionsScraperSuperClassJson);
      }

      this.options = result;
   }


   public JSONObject getRecipe(JSONObject optionsScraperJson, JSONObject optionsScraperSuperClassJson) {
      JSONObject recipe = new JSONObject();
      if (optionsScraperSuperClassJson.has("recipe")) {
         JSONObject recipeTemplate = optionsScraperSuperClassJson.optJSONObject("recipe");
         String recipeString = recipeTemplate != null ? recipeTemplate.toString() : "";
         JSONArray variables = optionsScraperJson.optJSONArray("variables");
         if (variables != null) {
            for (Object v : variables) {
               if (v instanceof JSONObject) {
                  JSONObject json = (JSONObject) v;
                  String key = json.keys().next();
                  String value = json.optString(json.keys().next());
                  recipeString = recipeString.replace(key, value);
               }
            }
         }

         recipe = stringToJson(recipeString);
      }

      return recipe;
   }


}
