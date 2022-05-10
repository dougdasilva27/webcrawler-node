package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

public class ColombiaExitoCrawler extends VTEXNewScraper {
   public ColombiaExitoCrawler(@NotNull Session session) {
      super(session);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      String vtex_segment = (String) session.getOptions().optQuery("/cookies/vtex_segment");
      BasicClientCookie cookie = new BasicClientCookie("vtex_segment", vtex_segment);
      cookie.setDomain(getHomePage().replace("https://", "").replace("/", ""));
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected List<String> getMainSellersNames() {
      List<String> sellers = new ArrayList<>();
      session.getOptions().optJSONArray("sellers").toList().forEach(s -> sellers.add((String) s));
      return sellers;
   }

   @Override
   protected String scrapName(Document doc, JSONObject productJson, JSONObject jsonSku) {
      StringBuilder name = new StringBuilder();
      String productName = productJson.optString("productName");
      String brand = productJson.optString("brand");

      if (!brand.isEmpty()) {
         name.append(brand).append(" ");
      }

      name.append(productName);

      return name.toString();
   }
}
