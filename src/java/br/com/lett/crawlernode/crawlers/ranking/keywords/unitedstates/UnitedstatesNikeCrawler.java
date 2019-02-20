package br.com.lett.crawlernode.crawlers.ranking.keywords.unitedstates;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class UnitedstatesNikeCrawler extends CrawlerRankingKeywords {

  public UnitedstatesNikeCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 60;

    this.log("Página " + this.currentPage);

    String key = this.keywordWithoutAccents.replaceAll(" ", "%20");
    String url = "https://store.nike.com/html-services/gridwallData?gridwallPath=n%2F1j7&country=US&lang_locale=en_US&pn=2&sl=" + key + "&anchor="
        + (this.currentPage - 1) * this.pageSize + "&vst=" + key;
    this.log("Url: " + url);

    JSONObject json = new JSONObject();

    try {
      String docString = Jsoup.connect(url).ignoreContentType(true).execute().body();
      json = new JSONObject(docString);
    } catch (IOException e) {
      e.printStackTrace();
    }

    setTotalProducts(json);

    if (json.has("foundResults") && json.getBoolean("foundResults") && json.has("sections")) {
      JSONArray array = json.getJSONArray("sections");

      if (array.length() > 0) {
        json = array.getJSONObject(0);

        if (json.has("items")) {
          array = json.getJSONArray("items");

          for (Object o : array) {
            JSONObject product = (JSONObject) o;

            String internalPid = null;
            String internalId = null;
            String productUrl = product.has("pdpUrl") ? product.getString("pdpUrl") : null;

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit) {
              break;
            }
          }
        }
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  protected void setTotalProducts(JSONObject json) {
    if (json.has("trackingData")) {
      String subJson = json.getString("trackingData");

      json = new JSONObject(subJson);
      if (json.has("response")) {
        json = json.getJSONObject("response");

        if (json.has("totalResults")) {
          this.totalProducts = json.getInt("totalResults");

          this.log("Total da busca: " + this.totalProducts);
          System.err.println(this.totalProducts);
        }
      }
    }
  }
}
