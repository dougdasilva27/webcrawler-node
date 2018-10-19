package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ChileParisCrawler extends CrawlerRankingKeywords {

  public ChileParisCrawler(Session session) {
    super(session);
  }

  @Override
  public void extractProductsFromCurrentPage() {
    this.pageSize = 30;

    this.log("Página " + this.currentPage);
    JSONObject search = crawlSearchApi();

    if (search.has("hits") && search.getJSONArray("hits").length() > 0) {
      JSONArray products = search.getJSONArray("hits");

      if (this.totalProducts == 0) {
        setTotalProducts(search);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        if (product.has("_source")) {
          product = product.getJSONObject("_source");
        }

        String productUrl = crawlProductUrl(product);
        String internalPid = crawlInternalPid(product);

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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
    return true;
  }

  protected void setTotalProducts(JSONObject search) {
    if (search.has("total") && search.get("total") instanceof Integer) {
      this.totalProducts = search.getInt("total");
      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("partNumber")) {
      internalPid = product.get("partNumber").toString().replace("-PPP-", "");
    }

    return internalPid;
  }

  private String crawlProductUrl(JSONObject product) {
    String urlProduct = null;

    if (product.has("product_can")) {
      urlProduct = product.getString("product_can");
    }

    return urlProduct;
  }

  private JSONObject crawlSearchApi() {
    JSONObject searchApi = new JSONObject();
    String url = "https://www.paris.cl/store-api/pyload/_search";
    this.log("Link onde são feitos os crawlers: " + url);

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("authority", "www.paris.cl");
    headers.put("Referer", "https://www.paris.cl/store/search");

    String payload;

    if (this.currentPage == 1) {
      payload = "{\"query\":{\"function_score\":{\"query\":{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"function_score\":{\"query\":"
          + "{\"multi_match\":{\"query\":\"" + this.keywordWithoutAccents
          + "\",\"fields\":[\"name^1000\",\"brand\",\"cat_3.stop\"],\"type\":\"best_fields\","
          + "\"operator\":\"and\"}},\"field_value_factor\":{\"field\":\"boost\",\"factor\":6}}},{\"multi_match\":{\"query\":\""
          + this.keywordWithoutAccents + "\","
          + "\"fields\":[\"name^8\",\"cat_3.stop\"],\"type\":\"best_fields\",\"operator\":\"or\"}},{\"span_first\":{\"match\":{\"span_term\":"
          + "{\"name.dym\":\"" + this.keywordWithoutAccents
          + "\"}},\"end\":1,\"boost\":2000}}],\"minimum_should_match\":\"1\"}}]}},\"boost_mode\":\"sum\","
          + "\"field_value_factor\":{\"field\":\"boost\",\"factor\":0}}},\"size\":" + this.productsLimit + ",\"from\":" + this.arrayProducts.size()
          + ",\"sort\":[{\"_score\":{\"order\":\"desc\"}}],"
          + "\"aggs\":{\"specs\":{\"nested\":{\"path\":\"specs\"},\"aggs\":{\"key\":{\"terms\":{\"field\":\"specs.key\"},\"aggs\":{\"value\":"
          + "{\"terms\":{\"field\":\"specs.value\",\"size\":100,\"order\":{\"_key\":\"asc\"}}}}}}},\"brands\":{\"terms\":"
          + "{\"field\":\"brand.keyword\",\"size\":1000,\"order\":{\"_key\":\"asc\"}}},\"price\":{\"range\":{\"field\":\"price\","
          + "\"ranges\":[{\"to\":5000},{\"from\":5001,\"to\":10000},{\"from\":10001,\"to\":20000},{\"from\":20001,\"to\":50000},"
          + "{\"from\":50001,\"to\":100000},{\"from\":100001,\"to\":500000},{\"from\":500001,\"to\":1000000},{\"from\":1000001}]}},\"despacho\":"
          + "{\"terms\":{\"field\":\"delivery.keyword\",\"size\":1000,\"order\":{\"_key\":\"asc\"}}},\"cat_1.raw\":{\"terms\":{\"field\":"
          + "\"cat_1.raw\",\"size\":250,\"order\":{\"_key\":\"asc\"}}}}}";
    } else {
      headers.put("Referer", "https://www.paris.cl/store/search?query=smartphone+blanco&sort=_score-desc&cur_pos=28");
      payload =
          "{\"query\":{\"function_score\":{\"query\":{\"bool\":{\"must\":[{\"bool\":{\"should\":[{\"function_score\":{\"query\":{\"multi_match\":{\"query\":\"smartphone blanco\",\"fields\":[\"name^1000\",\"brand\",\"cat_3.stop\"],\"type\":\"best_fields\",\"operator\":\"and\"}},\"field_value_factor\":{\"field\":\"boost\",\"factor\":6}}},{\"multi_match\":{\"query\":\"smartphone blanco\",\"fields\":[\"name^8\",\"cat_3.stop\"],\"type\":\"best_fields\",\"operator\":\"or\"}},{\"span_first\":{\"match\":{\"span_term\":{\"name.dym\":\"smartphone blanco\"}},\"end\":1,\"boost\":2000}}],\"minimum_should_match\":\"1\"}}]}},\"boost_mode\":\"sum\",\"field_value_factor\":{\"field\":\"boost\",\"factor\":0}}},\"size\":30,\"from\":30,\"sort\":[{\"_score\":{\"order\":\"desc\"}}],\"aggs\":{\"specs\":{\"nested\":{\"path\":\"specs\"},\"aggs\":{\"key\":{\"terms\":{\"field\":\"specs.key\"},\"aggs\":{\"value\":{\"terms\":{\"field\":\"specs.value\",\"size\":100,\"order\":{\"_key\":\"asc\"}}}}}}},\"brands\":{\"terms\":{\"field\":\"brand.keyword\",\"size\":1000,\"order\":{\"_key\":\"asc\"}}},\"price\":{\"range\":{\"field\":\"price\",\"ranges\":[{\"to\":5000},{\"from\":5001,\"to\":10000},{\"from\":10001,\"to\":20000},{\"from\":20001,\"to\":50000},{\"from\":50001,\"to\":100000},{\"from\":100001,\"to\":500000},{\"from\":500001,\"to\":1000000},{\"from\":1000001}]}},\"despacho\":{\"terms\":{\"field\":\"delivery.keyword\",\"size\":1000,\"order\":{\"_key\":\"asc\"}}}}";
    }

    String content = POSTFetcher.fetchPagePOSTWithHeaders(url, session, payload, cookies, 1, headers,
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36", null);
    JSONObject json = CrawlerUtils.stringToJson(content);

    if (json.has("hits")) {
      searchApi = json.getJSONObject("hits");
    }

    return searchApi;
  }
}
