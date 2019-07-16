package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;

public class BrasilEletrumCrawler extends CrawlerRankingKeywords {

  public BrasilEletrumCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;

    this.log("Página " + this.currentPage);

    String pageUrl = "https://www.eletrum.com.br/" + this.keywordEncoded + "?map=ft";
    this.currentDoc = fetchDocument(pageUrl);

    Integer index = this.arrayProducts.size();
    String urlApi = "https://www.eletrum.com.br/api/catalog_system/pub/products/search/" + this.keywordEncoded + "?map=ft&_from=" + index + "&_to="
        + (index + 49) + "&O=OrderByTopSaleDESC";

    JSONArray apiJson = fetchApiJson(urlApi);

    if (apiJson.length() > 0) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Object object : apiJson) {
        JSONObject product = (JSONObject) object;

        String internalPid = crawlInternalPid(product);
        String productUrl = crawlProductUrl(product);

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


    this.log("Link onde são feitos os crawlers: " + urlApi);
  }

  private String crawlProductUrl(JSONObject product) {
    String url = null;

    if (product.has("link")) {
      url = product.getString("link");
    }

    return url;
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("productId")) {
      internalPid = product.getString("productId");
    }

    return internalPid;
  }

  private JSONArray fetchApiJson(String urlApi) {
    Request request = RequestBuilder.create().setCookies(cookies).setUrl(urlApi).build();
    String response = dataFetcher.get(session, request).getBody();
    return CrawlerUtils.stringToJsonArray(response);
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.selectFirst(".resultado-busca-numero .value");

    if (totalElement != null) {
      this.totalProducts = MathUtils.parseInt(totalElement.text());
    }

  }

}
