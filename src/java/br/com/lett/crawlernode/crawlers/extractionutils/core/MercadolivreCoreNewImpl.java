package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.session.Session;
import org.json.JSONArray;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MercadolivreCoreNewImpl extends MercadolivreCrawler{


   public MercadolivreCoreNewImpl(Session session) {
      super(session);
      super.setHomePage(session.getOptions().optString("HomePage"));
      super.setMainSellerNameLower(session.getOptions().optString("Seller"));
      super.setSellerVariations(getSellersWithVariations());
   }

//This method was implemented to get the product with 1P availability even if there is any variation in the seller's name
   public List<String> getSellersWithVariations() {
      JSONArray sellers = session.getOptions().optJSONArray("SellerVariations");
      if(sellers!= null){
       return sellers.toList().stream().map(Object::toString).collect(Collectors.toList());
      } else {
         return null;
      }
   }
}
