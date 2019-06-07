package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class MexicoSamsclubCrawler extends CrawlerRankingKeywords {

  public MexicoSamsclubCrawler(Session session) {
    super(session);
    // TODO Auto-generated constructor stub
  }

  @Override
  public void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 20;

    this.log("Página " + this.currentPage);
    String url = "https://www.sams.com.mx/sams/search?Ntt=" + this.keywordEncoded + "&_=1559844888311";

    this.log("Link onde são feitos os crawlers: " + url);

    JSONArray search = fetchJSONApi(url);

    if (search.length() > 0) {

      if (this.totalProducts == 0) {
        setTotalProducts(search);
      }

      for (int i = 0; i < search.length(); i++) {
        JSONObject product = search.getJSONObject(i);

        if (product.has("attributes")) {
          JSONObject attributes = product.getJSONObject("attributes");
          String productUrl = crawlProductUrl(attributes);
          String internalId = crawlInternalId(attributes);

          saveDataProduct(internalId, null, productUrl);

          this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
        }

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

  protected void setTotalProducts(JSONArray search) {
    this.totalProducts = search.length();
    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("sku.repositoryId")) {
      JSONArray ids = product.getJSONArray("sku.repositoryId");

      if (ids.length() > 0) {
        internalId = ids.get(0).toString();
      }
    }

    return internalId;
  }

  private String crawlProductUrl(JSONObject product) {
    String productUrl = null;
    if (product.has("product.seoURL")) {
      JSONArray urls = product.getJSONArray("product.seoURL");

      if (urls.length() > 0) {
        productUrl = "https://www.sams.com.mx" + urls.get(0).toString().replace("[", "").replace("]", "");
      }
    }

    return productUrl;
  }

  private JSONArray fetchJSONApi(String url) {
    JSONArray records = new JSONArray();

    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "application/json, text/javascript, */*; q=0.01");
    headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).mustSendContentEncoding(false).build();
    JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

    if (response.has("mainArea")) {
      JSONArray mainArea = response.getJSONArray("mainArea");

      for (Object object : mainArea) {
        JSONObject contents = (JSONObject) object;

        if (contents.has("contents")) {
          JSONArray content = contents.getJSONArray("contents");

          for (Object object2 : content) {
            JSONObject objContent = (JSONObject) object2;

            if (objContent.has("records")) {
              records = objContent.getJSONArray("records");
              break;
            }
          }
        }
      }
    }

    return records;
  }

}
