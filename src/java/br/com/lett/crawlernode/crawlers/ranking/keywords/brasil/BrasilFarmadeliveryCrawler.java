package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.JsonParser;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilFarmadeliveryCrawler extends CrawlerRankingKeywords {

  public static final String PRODUCTS_API_URL = "https://gri9dmsahc-dsn.algolia.net/1/indexes/*/queries?"
      + "x-algolia-agent=Algolia%20for%20vanilla%20JavaScript%20(lite)%203.32.1%3Binstantsearch.js%201.12.1%3BMagento%20integration%20(1.16.0)%3BJS%20"
      + "Helper%202.26.1&x-algolia-application-id=GRI9DMSAHC&x-algolia-api-key=YmJiZTRhNmQ4ZTcxYzQ1N2E5NTYzZGU1ZjIyNjFjYmI0YjRhYzc2ZDZjYzkxMzQ5ZTQzN2YyY"
      + "zkzYjNkYTU5YWZpbHRlcnM9Jm51bWVyaWNGaWx0ZXJzPXZpc2liaWxpdHlfc2VhcmNoJTNEMQ%3D%3D";

  public BrasilFarmadeliveryCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.FETCHER;
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 32;
    this.log("Página " + this.currentPage);

    JSONObject search = fetchProductsFromAPI();
    JSONArray arraySkus = search.has("hits") ? search.getJSONArray("hits") : new JSONArray();

    if (arraySkus.length() > 0) {

      if (this.totalProducts == 0) {
        setTotalProducts(search);
      }

      for (Object product : arraySkus) {
        JSONObject jsonSku = (JSONObject) product;
        String internalId = JSONUtils.getStringValue(jsonSku, "objectID");
        String productUrl = JSONUtils.getStringValue(jsonSku, "url");

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);

        if (this.arrayProducts.size() == productsLimit) {
          break;
        }
      }

    } else {

      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");


  }

  private void setTotalProducts(JSONObject search) {
    this.totalProducts = JSONUtils.getIntegerValueFromJSON(search, "nbHits", 0);
    this.log("Total: " + this.totalProducts);
  }

  private JSONObject fetchProductsFromAPI() {
    JSONObject products = new JSONObject();

    String payload = "{\"requests\":[{\"indexName\":\"farmadelivery_default_products\","
        + "\"params\":\"query=" + this.keywordWithoutAccents.replace(" ", "%20")
        + "&hitsPerPage=32&maxValuesPerFacet=30&page=" + (this.currentPage - 1)
        + "&ruleContexts=%5B%22magento_filters%22%2C%22%22%5D&facets=%5B%22brand%22%2C%22"
        + "composicao_new%22%2C%22manufacturer%22%2C%22activation_information%22%2C%22frete_gratis_dropdown%22%2C%22category_ids%22%2C%22"
        + "price.BRL.default%22%2C%22color%22%2C%22categories.level0%22%5D&tagFilters=\"}]}";

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("Accept-Encoding", "no");

    Request request = RequestBuilder.create().setUrl(PRODUCTS_API_URL).setCookies(cookies).setHeaders(headers).setPayload(payload)
        .mustSendContentEncoding(false).build();
    String page = this.dataFetcher.post(session, request).getBody();

    if (page.startsWith("{") && page.endsWith("}")) {
      try {
        // Using google JsonObject to get a JSONObject because this json can have a duplicate key.
        JSONObject result = new JSONObject(new JsonParser().parse(page).getAsJsonObject().toString());

        if (result.has("results") && result.get("results") instanceof JSONArray) {
          JSONArray results = result.getJSONArray("results");
          if (results.length() > 0 && results.get(0) instanceof JSONObject) {
            products = results.getJSONObject(0);
          }
        }

      } catch (Exception e) {
        Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }
    }

    return products;
  }
}
