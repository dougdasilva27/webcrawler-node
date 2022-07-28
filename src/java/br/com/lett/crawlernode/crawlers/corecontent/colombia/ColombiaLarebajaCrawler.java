package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewImpl;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

public class ColombiaLarebajaCrawler extends VTEXNewImpl {

   public ColombiaLarebajaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String scrapName(Document doc, JSONObject productJson, JSONObject jsonSku) {
      String name = null;

      if (productJson.has("productName")) {
         name = productJson.optString("productName");
      }

      if (name != null && !name.isEmpty() && productJson.has("brand")) {
         String brand = productJson.optString("brand");
         if (brand != null && !brand.isEmpty() && !checkIfNameHasBrand(brand, name)) {
            name = name + " " + brand;
         }
      }

      String unit = JSONUtils.getValueRecursive(productJson, "Presentacionunidadmedida.0", String.class);
      if (name != null && unit != null) {
         name = name + " - " + unit;
      }

      return name;
   }
}
