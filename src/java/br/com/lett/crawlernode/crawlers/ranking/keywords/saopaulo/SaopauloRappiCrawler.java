package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.ranking.RankingKeywordsSession;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class SaopauloRappiCrawler extends CrawlerRankingKeywords {

  public SaopauloRappiCrawler(Session session) {
    super(session);
  }

  private static final String STORES_API_URL = "https://services.rappi.com.br/api/base-crack/principal?lat=-23.584&lng=-46.671&device=2";
  public static final String PRODUCTS_API_URL = "https://services.rappi.com.br/chewbacca/search/v2/products?page=1";

  private List<String> stores = new ArrayList<>();

  @Override
  protected void processBeforeFetch() {
    this.stores = crawlStores();
  }

  @Override
  public void extractProductsFromCurrentPage() {
    this.pageSize = 20;
    this.log("Página " + this.currentPage);

    JSONObject search;

    if (session instanceof RankingKeywordsSession) {
      // a pesquisa no site sempre redireciona para esses dois markets (extra e pao de acucar)
      search = fetchProductsFromAPI("market", Arrays.asList("700001704", "700001341"));
    } else {
      // para descobrir precisamos passar em todas as lojas
      search = fetchProductsFromAPI("market", this.stores);
    }

    // se obter 1 ou mais links de produtos e essa página tiver resultado
    if (search.has("hits") && search.getJSONArray("hits").length() > 0) {
      JSONArray products = search.getJSONArray("hits");

      // se o total de busca não foi setado ainda, chama a função para
      if (this.totalProducts == 0) {
        setTotalProducts(search);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String internalPid = crawlInternalPid(product);
        String internalId = crawlInternalId(product);
        String productUrl = crawlProductUrl(product, internalPid);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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

  @Override
  protected boolean hasNextPage() {
    return this.arrayProducts.size() < this.totalProducts;
  }

  protected void setTotalProducts(JSONObject search) {
    if (search.has("total_results") && search.get("total_results") instanceof Integer) {
      this.totalProducts = search.getInt("total_results");
      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("id")) {
      internalId = product.getString("id");
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("product_id")) {
      internalPid = product.getString("product_id");
    }

    return internalPid;
  }

  private String crawlProductUrl(JSONObject product, String internalPid) {
    String productUrl = null;

    if (product.has("store_type")) {
      productUrl = "https://www.rappi.com.br/search?store_type=" + product.get("store_type") + "&query=" + internalPid;
    }

    return productUrl;
  }

  private JSONObject fetchProductsFromAPI(String storeType, List<String> storeIds) {
    String payload =
        "{\"query\":\"" + this.location + "\",\"stores\":" + storeIds.toString() + "," + "\"helpers\":{\"type\":\"by_products\",\"storeType\":\""
            + storeType + "\"},\"page\":" + this.currentPage + ",\"store_type\":\"" + storeType + "\",\"options\":{}}";

    String url = "https://services.rappi.com.br/chewbacca/search/v2/products?page=" + this.currentPage;

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).setPayload(payload).build();
    return CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());
  }

  private List<String> crawlStores() {
    Request request = RequestBuilder.create().setUrl(STORES_API_URL).setCookies(cookies).build();
    JSONArray options = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

    for (Object o : options) {
      JSONObject option = (JSONObject) o;

      // foi definido que nao iremos capturar informacoes da secao restaurante
      if (option.has("store_type") && option.get("store_type").toString().equals("restaurant")) {
        continue;
      }

      if (option.has("suboptions")) {
        JSONArray suboptions = option.getJSONArray("suboptions");

        for (Object ob : suboptions) {
          JSONObject suboption = (JSONObject) ob;
          if (suboption.has("stores")) {
            setStores(suboption.getJSONArray("stores"), stores);
          }
        }
      } else if (option.has("stores")) {
        setStores(option.getJSONArray("stores"), stores);
      }
    }

    return stores;
  }

  private void setStores(JSONArray storesArray, List<String> stores) {
    for (Object o : storesArray) {
      JSONObject store = (JSONObject) o;

      if (store.has("store_id")) {
        stores.add("\"" + store.getString("store_id") + "\"");
      }
    }
  }
}
