package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaCarrefoursuper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class ArgentinaCarrefoursupervicentelopezCrawler extends ArgentinaCarrefoursuper {

   public ArgentinaCarrefoursupervicentelopezCrawler(Session session) {
      super(session);
   }

   public static final String TOKEN = "eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIxIiwicHJpY2VUYWJsZXMiOm51bGwsInJlZ2lvbklkIjoiVTFjalkyRnljbVZtYjNWeVlYSXdNREF5TzJOaGNuSmxabTkxY21GeU1EZzVPUT09IiwidXRtX2NhbXBhaWduIjpudWxsLCJ1dG1fc291cmNlIjpudWxsLCJ1dG1pX2NhbXBhaWduIjpudWxsLCJjdXJyZW5jeUNvZGUiOiJBUlMiLCJjdXJyZW5jeVN5bWJvbCI6IiQiLCJjb3VudHJ5Q29kZSI6IkFSRyIsImN1bHR1cmVJbmZvIjoiZXMtQVIiLCJjaGFubmVsUHJpdmFjeSI6InB1YmxpYyJ9";

   /**
    * Address: Av. del Libertador 215, B1638 Vicente López, Provincia de Buenos Aires, Argentina
    *
    * @return token for specified address.
    */
   @Override
   protected String getLocationToken() {
      return TOKEN;
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) throws UnsupportedEncodingException {
      StringBuilder description = new StringBuilder();

      List<String> specs = getSpecs(productJson);

      for (String spec : specs) {
         description.append("<div>");
         description.append("<h4>").append(spec).append("</h4>");
         description.append(sanitizeDescription(productJson.get(spec)));
         description.append("</div>");
      }

      return description.toString();
   }

   protected List<String> getSpecs(JSONObject json){
      List<String> specs = new ArrayList<>();

      JSONArray keys = json.optJSONArray("allSpecificationsGroups");
      for (Object o : keys) {
         if (!o.toString().equalsIgnoreCase("Especificaciones Genesix")) {
            if(o.toString().equalsIgnoreCase("Especificaciones Default")){
               JSONArray defaultSpecs = json.optJSONArray(o.toString());
               defaultSpecs.forEach(x -> specs.add(x.toString()));
            }else{
               specs.add(o.toString());
            }
         }
      }

      return specs;
   }

}
