package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilFnacCrawler extends CrawlerRankingKeywords {

  public BrasilFnacCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);
    this.pageSize = 40;

    String url = "https://www.fnac.com.br/#!Popup_opzSearch=termos--"
        + this.location.trim().replace(" ", "--e--");

    takeAScreenshot(url);

    String apiUrl = "http://search.oppuz.com/opz/api/search?page=" + this.currentPage + "&limit="
        + this.productsLimit + "&sort=score.desc&sortingOrMoreItemsEvent=true&store=fnac"
        + "&text=&typedText=&hashBangQuery=termos--" + this.keywordEncoded
        + "&fallbackSubstantives&callback=Opz.SearchPage.callback";

    this.log("Link onde são feitos os crawlers: " + apiUrl);

    JSONObject search = fetchAPI(apiUrl);
    JSONArray products = crawlProducts(search);

    if (products.length() > 0) {
      if (this.totalProducts == 0) {
        setTotalProducts(search);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String internalPid = null;
        String internalId = crawlInternalId(product);
        String productUrl = crawlProductUrl(product);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
            + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected boolean hasNextPage() {
    return arrayProducts.size() < this.totalProducts;
  }

  protected void setTotalProducts(JSONObject search) {
    if (search.has("total")) {
      Object total = search.get("total");

      if (total instanceof Integer) {
        this.totalProducts = (Integer) total;

        this.log("Total: " + this.totalProducts);
      }
    }
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("sku")) {
      internalId = product.get("sku").toString();
    }

    return internalId;
  }

  private String crawlProductUrl(JSONObject product) {
    String productUrl = null;

    if (product.has("url")) {
      productUrl = CommonMethods.sanitizeUrl(product.getString("url"));
    }

    return productUrl;
  }


  private JSONArray crawlProducts(JSONObject json) {
    JSONArray products = new JSONArray();

    if (json.has("results")) {
      products = json.getJSONArray("results");
    }

    return products;
  }

  private JSONObject fetchAPI(String url) {
    JSONObject api = new JSONObject();

    String body = fetchGETString(url, null);

    if (body.contains("({")) {
      int x = body.indexOf("({") + 1;
      int y = body.indexOf("})", x) + 1;

      String json = body.substring(x, y);

      try {
        api = new JSONObject(json);
      } catch (Exception e) {
        this.logError("Erro ao parsear json.", e);
      }
    }

    return api;
  }
}
