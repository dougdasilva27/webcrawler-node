package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilKoerichCrawler extends CrawlerRankingKeywords {

  public BrasilKoerichCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;
    this.log("Página " + this.currentPage);

    JSONObject resultsList = fetchJsonApi();
    JSONArray products = resultsList.has("records") ? resultsList.getJSONArray("records") : new JSONArray();

    if (products.length() > 0) {
      if (this.totalProducts == 0) {
        setTotalBusca(resultsList);
      }

      for (Object o : products) {
        JSONObject productInfo = (JSONObject) o;

        if (productInfo.has("records")) {
          JSONArray records = productInfo.getJSONArray("records");
          this.position++;

          for (Object obj : records) {
            JSONObject jsonSku = (JSONObject) obj;
            String internalId = crawlInternalId(jsonSku);
            String productUrl = crawlProductUrl(jsonSku);

            saveDataProduct(internalId, null, productUrl, this.position);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) {
              break;
            }
          }
        }
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultados!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

  }


  protected void setTotalBusca(JSONObject apiSearch) {
    if (apiSearch.has("totalNumRecs")) {
      String text = apiSearch.get("totalNumRecs").toString().replaceAll("[^0-9]", "");

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }


  private String crawlInternalId(JSONObject sku) {
    String internalId = null;

    if (sku.has("attributes")) {
      JSONObject attributes = sku.getJSONObject("attributes");

      if (attributes.has("sku.repositoryId")) {
        String id = attributes.get("sku.repositoryId").toString().replace("[", "").replace("]", "").replace("\"", "").trim();

        if (!id.isEmpty()) {
          internalId = id;
        }
      }
    }

    return internalId;
  }

  private String crawlProductUrl(JSONObject sku) {
    String productUrl = null;

    if (sku.has("attributes")) {
      JSONObject attributes = sku.getJSONObject("attributes");
      if (attributes.has("product.route")) {
        String route = attributes.get("product.route").toString().replace("[", "").replace("]", "").replace("\"", "").trim();

        if (!route.isEmpty()) {
          productUrl = CrawlerUtils.completeUrl(route, "https:", "www.koerich.com.br");
        }
      }
    }

    return productUrl;
  }

  private JSONObject fetchJsonApi() {
    JSONObject resultsList = new JSONObject();

    String url = "https://www.koerich.com.br/ccstoreui/v1/search?Ntt=" + this.keywordWithoutAccents.replace(" ", "%20") + "&No="
        + this.arrayProducts.size() + "&Nrpp=50";

    JSONObject response = CrawlerUtils.stringToJson(fetchGETString(url, cookies));

    if (response.has("resultsList")) {
      resultsList = response.getJSONObject("resultsList");
    }

    return resultsList;
  }

}
