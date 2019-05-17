package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilCsdCrawler extends CrawlerRankingKeywords {

  public BrasilCsdCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.FETCHER;
  }

  private static final String HOME_PAGE =
      "https://www.sitemercado.com.br/supermercadoscidadecancao/londrina-loja-londrina-19-rodocentro-avenida-tiradentes/";

  @Override
  public void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);
    JSONObject search = crawlProductInfo();

    // se obter 1 ou mais links de produtos e essa página tiver resultado
    if (search.has("products") && search.getJSONArray("products").length() > 0) {
      JSONArray products = search.getJSONArray("products");

      this.totalProducts = products.length();
      this.log("Total da busca: " + this.totalProducts);

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        // Url do produto
        String productUrl = crawlProductUrl(product);

        // InternalPid
        String internalPid = crawlInternalPid(product);

        // InternalId
        String internalId = crawlInternalId(product);

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
    return false;
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("idLojaProduto")) {
      internalId = product.get("idLojaProduto").toString();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("idProduct")) {
      internalPid = product.get("idProduct").toString();
    }

    return internalPid;
  }

  private String crawlProductUrl(JSONObject product) {
    String productUrl = null;

    if (product.has("url")) {
      productUrl = product.getString("url");

      if (!productUrl.contains("sitemercado")) {
        productUrl = (HOME_PAGE + "/" + productUrl).replace("//produto/", "/produto/");

        if (productUrl.contains("tiradentes//") && !productUrl.contains("produto")) {
          productUrl = productUrl.replace("tiradentes//", "tiradentes/produto/");
        }
      }
    }

    return productUrl;
  }

  private JSONObject crawlProductInfo() {
    String loadUrl = "https://www.sitemercado.com.br/core/api/v1/b2c/page/load";
    String smmc = "2019.03.28-0";

    String payload = "{\"lojaUrl\":\"londrina-loja-londrina-19-rodocentro-avenida-tiradentes\",\"redeUrl\":\"supermercadoscidadecancao\"}";

    Map<String, String> headers = new HashMap<>();
    headers.put("referer", "https://www.sitemercado.com.br/supermercadoscidadecancao/londrina-loja-londrina-19-rodocentro-avenida-tiradentes/");
    headers.put("sm-mmc", smmc);
    headers.put("accept", "application/json, text/plain, */*");
    headers.put("content-type", "application/json");

    Request request = RequestBuilder.create().setUrl(loadUrl).setCookies(cookies).setHeaders(headers).setPayload(payload).build();
    Map<String, String> responseHeaders = new FetcherDataFetcher().post(session, request).getHeaders();

    if (responseHeaders.containsKey("sm-token")) {
      headers.put("sm-token", responseHeaders.get("sm-token"));
    } else {
      smmc = new SimpleDateFormat("yyyy.MM.dd").format(new Date()) + "-0";
      headers.put("sm-mmc", smmc);
      request.setHeaders(headers);

      responseHeaders = new FetcherDataFetcher().post(session, request).getHeaders();

      if (responseHeaders.containsKey("sm-token")) {
        headers.put("sm-token", responseHeaders.get("sm-token"));
      }
    }


    String payloadSearch = "{phrase: \"" + this.keywordWithoutAccents + "\"}";
    Request requestApi = RequestBuilder.create().setUrl("https://www.sitemercado.com.br/core/api/v1/b2c/product/loadSearch").setCookies(cookies)
        .setHeaders(headers).setPayload(payloadSearch).build();
    String res = this.dataFetcher.post(session, requestApi).getBody();

    if (res.isEmpty()) {
      headers.put("sm-mmc", new SimpleDateFormat("yyyy.MM.dd").format(new Date()) + "-0");
      requestApi.setHeaders(headers);
      res = this.dataFetcher.get(session, requestApi).getBody();
    }

    return CrawlerUtils.stringToJson(res);
  }
}
