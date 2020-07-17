package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public abstract class BrasilIfood extends CrawlerRankingKeywords {

  public BrasilIfood(Session session) {
    super(session);
  }

  protected String region = getRegion();
  protected String store_name = getStore_name();

  protected abstract String getRegion();

  protected abstract String getStore_name();

  @Override
  protected void extractProductsFromCurrentPage() {

    this.log("Página " + this.currentPage);

    String url = "https://www.ifood.com.br/delivery/" + region + "/" + store_name;

    this.currentDoc = fetchDocument(url, cookies);

    if (this.currentDoc.selectFirst("#__NEXT_DATA__") != null) {

      JSONObject json = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "#__NEXT_DATA__", null, null, false, false);
      JSONObject props = JSONUtils.getJSONValue(json, "props");
      JSONObject initialState = JSONUtils.getJSONValue(props, "initialState");
      JSONObject restaurant = JSONUtils.getJSONValue(initialState, "restaurant");
      JSONArray menu = JSONUtils.getJSONArrayValue(restaurant, "menu");

      if (menu != null && !menu.isEmpty()) {

        for (Object menuArr : menu) {

          JSONObject menuObject = (JSONObject) menuArr;
          JSONArray itens = JSONUtils.getJSONArrayValue(menuObject, "itens");

          for (Object itensArr : itens) {

            JSONObject itensObject = (JSONObject) itensArr;

            if (!itensObject.isEmpty()) {

              String internalId = itensObject.optString("code");
              String internalPid = internalId;

              String urlId = itensObject.optString("id");
              String productUrl = url + "?prato=" + internalId;


              //https://www.ifood.com.br/delivery/sao-paulo-sp/big-pacaembu---express-bom-retiro?prato=179415637
              // https://www.ifood.com.br/delivery/sao-paulo-sp/big-pacaembu---express-bom-retiro?prato=102721778
              saveDataProduct(internalId, internalPid, productUrl);

              this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
              if (this.arrayProducts.size() == productsLimit)
                break;

            } else {
              this.result = false;
              this.log("Keyword sem resultado!");
            }

            this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

          }
        }
      }

    }

    Elements products = this.currentDoc.select(".product-list-item");

  }


}
