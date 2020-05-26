package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public abstract class VTEXNewScraper extends VTEXScraper {

  public VTEXNewScraper(Session session) {
    super(session);
  }

  protected String scrapInternalPidOldWay(Document doc) {
    JSONObject runTimeJSON = scrapRuntimeJson(doc);
    JSONObject initialJson = scrapProductJson(runTimeJSON);
    return initialJson.optString("productId", null);
  }

  private JSONObject scrapRuntimeJson(Document doc) {
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
    Element elem = doc.selectFirst(".vtex-product-context-provider script");
    JSONObject json = JSONUtils.stringToJson(elem.data());
    String internalPid = json.optString("mpn", null);
    if (internalPid == null) {
      internalPid = scrapInternalPidOldWay(doc);
    }
    return internalPid;
  }
}
