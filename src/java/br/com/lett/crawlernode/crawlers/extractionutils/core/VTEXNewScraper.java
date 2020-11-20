package br.com.lett.crawlernode.crawlers.extractionutils.core;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

public abstract class VTEXNewScraper extends VTEXScraper {

   public VTEXNewScraper(Session session) {
      super(session);
   }

   protected String scrapInternalPidOldWay(JSONObject runtimeJSON) {
      JSONObject initialJson = scrapProductJson(runtimeJSON);
      return initialJson.optString("productId", null);
   }

   protected JSONObject scrapRuntimeJson(Document doc) {
      JSONObject runtimeJson = new JSONObject();
      String token = "__RUNTIME__ =";

      Elements scripts = doc.select("script");
      for (Element e : scripts) {
         String script = e.html();

         if (script.contains(token)) {
            runtimeJson = CrawlerUtils.stringToJSONObject(CrawlerUtils.extractSpecificStringFromScript(script,
                  token, false, "", true));
            break;
         }
      }
      return runtimeJson;
   }

   private JSONObject scrapProductJson(JSONObject stateJson) {
      JSONObject product = new JSONObject();
      Object queryData = JSONUtils.getValue(stateJson, "queryData");
      JSONObject queryDataJson = new JSONObject();

      if (queryData instanceof JSONObject) {
         queryDataJson = (JSONObject) queryData;
      } else if (queryData instanceof JSONArray) {
         JSONArray queryDataArray = (JSONArray) queryData;
         if (queryDataArray.length() > 0 && queryDataArray.get(0) instanceof JSONObject) {
            queryDataJson = queryDataArray.getJSONObject(0);
         }
      }

      if (queryDataJson.has("data") && queryDataJson.get("data") instanceof JSONObject) {
         product = queryDataJson.getJSONObject("data");
      } else if (queryDataJson.has("data") && queryDataJson.get("data") instanceof String) {
         product = CrawlerUtils.stringToJson(queryDataJson.getString("data"));
      }
      return JSONUtils.getJSONValue(product, "product");
   }

   @Override
   protected String scrapInternalpid(Document doc) {
      String internalPid = null;
      Element elem = doc.selectFirst(".vtex-product-context-provider script");
      if (elem != null) {
         JSONObject json = JSONUtils.stringToJson(elem.data());
         internalPid = json.optString("mpn", null);
      }

      if (internalPid == null) {
         JSONObject runtimeJSON = scrapRuntimeJson(doc);
         internalPid = scrapInternalPidOldWay(runtimeJSON);

         if (internalPid == null) {
            internalPid = scrapInternalpidThirdWay(runtimeJSON);
         }
      }

      return internalPid;
   }

   protected String scrapInternalpidThirdWay(JSONObject runtimeJSON) {
      JSONObject route = runtimeJSON.optJSONObject("route");
      JSONObject params = route != null ? route.optJSONObject("params") : new JSONObject();

      return params.optString("id", null);
   }
}
