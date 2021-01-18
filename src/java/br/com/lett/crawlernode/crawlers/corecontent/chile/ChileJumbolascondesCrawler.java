package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ChileJumboCrawler;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

public class ChileJumbolascondesCrawler extends ChileJumboCrawler {
   public static final String CODE_LOCATE = "11";

   public ChileJumbolascondesCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCodeLocate() {
      return CODE_LOCATE;
   }

   @Override
   protected String scrapName(Document doc, JSONObject productJson, JSONObject jsonSku) {
      String name = super.scrapName(doc, productJson, jsonSku);
      String brand = productJson.has("brand") ? productJson.get("brand").toString(): null;
      String nameWithBrand = null;
     if (name != null && brand != null){
         nameWithBrand = name + " - " + brand;
     }
      return nameWithBrand;
   }
}
