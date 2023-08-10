package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewImpl;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;

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

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) throws UnsupportedEncodingException {
      String description = JSONUtils.getValueRecursive(productJson, "description", ".", String.class, "");
      if (description.equals("")) {
         String complementName = JSONUtils.getValueRecursive(productJson, "items.0.complementName", ".", String.class, "");
         String nameComplete = JSONUtils.getValueRecursive(productJson, "items.0.nameComplete", ".", String.class, "");

         if (!complementName.isEmpty() && !complementName.equals(nameComplete)) {
            description = description.concat(complementName);
         }
      }

      JSONArray descriptionContenido = productJson.optJSONArray("DescripcionContenido");
      if (descriptionContenido != null && !descriptionContenido.isEmpty()) {
         for (Object descriptionItem : descriptionContenido) {
            description = description.concat((String) descriptionItem);
         }
      }

      return description;
   }
}
