
package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public abstract class VTEXOldScraper extends VTEXScraper {

   public static final String PRODUCT_ID = "productId";

   public VTEXOldScraper(Session session) {
      super(session);
   }

   @Override
   protected String scrapInternalpid(Document doc) {
      JSONObject productJson = crawlSkuJsonVTEX(doc, session);
      String internalPid = null;

      if (productJson.has(PRODUCT_ID)) {
         internalPid = productJson.optString(PRODUCT_ID);
      }

      return internalPid;
   }

   /**
    * Crawl skuJson from html in VTEX Sites
    * 
    * @param document
    * @param session
    * @return
    */
   public static JSONObject crawlSkuJsonVTEX(Document document, Session session) {
      Elements scriptTags = document.getElementsByTag("script");
      String scriptVariableName = "var skuJson_0 = ";
      JSONObject skuJson = new JSONObject();
      String skuJsonString = null;

      for (Element tag : scriptTags) {
         for (DataNode node : tag.dataNodes()) {
            if (tag.html().trim().startsWith(scriptVariableName)) {
               skuJsonString = node.getWholeData().split(Pattern.quote(scriptVariableName))[1]
                     + node.getWholeData().split(Pattern.quote(scriptVariableName))[1].split(Pattern.quote("};"))[0];
               break;
            }
         }
      }

      if (skuJsonString != null) {
         try {
            skuJson = new JSONObject(skuJsonString);

         } catch (JSONException e) {
            Logging.printLogWarn(logger, session, "Error creating JSONObject from var skuJson_0");
            Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));
         }
      }

      return skuJson;
   }
}
