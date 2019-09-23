package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ColombiaMerqueoCrawler extends CrawlerRankingKeywords {

  public ColombiaMerqueoCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    String url = "https://merqueo.com/api/2.0/stores/63/search?q=" + this.keywordEncoded + "&per_page=15&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    JSONObject apiJson = fetchApiProducts(url);
    JSONArray productsArray = new JSONArray();

    if (apiJson.has("data") && !apiJson.isNull("data")) {
      productsArray = apiJson.getJSONArray("data");
    }

    if (productsArray.length() > 0) {
      if (this.totalProducts == 0) {
        this.totalProducts = productsArray.length();
      }

      for (Object object : productsArray) {
        JSONObject data = (JSONObject) object;

        String internalId = data.has("id") ? data.get("id").toString() : null;
        String productUrl = assembleProductUrl(data);
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

  /*
   * Url exemple:
   * https://merqueo.com/bogota/despensa/condimentos-especias-y-adobos/canela-su-despensa-20-gr
   */
  private String assembleProductUrl(JSONObject data) {
    String productUrl = "";

    if (data.has("slugs") && !data.isNull("slugs")) {
      JSONObject slugs = data.getJSONObject("slugs");

      if (slugs.has("data") && !slugs.isNull("data")) {
        JSONObject dataSlugs = slugs.getJSONObject("data");

        boolean hasFields = dataSlugs.has("city")
            && dataSlugs.has("department")
            && dataSlugs.has("shelf")
            && dataSlugs.has("product");

        boolean isFieldsNull =
            !dataSlugs.isNull("city")
                && !dataSlugs.isNull("department")
                && !dataSlugs.isNull("shelf")
                && !dataSlugs.isNull("product");

        if (hasFields && isFieldsNull) {
          productUrl = productUrl
              .concat("https://merqueo.com/")
              .concat(dataSlugs.getString("city"))
              .concat("/")
              .concat(dataSlugs.getString("department"))
              .concat("/")
              .concat(dataSlugs.getString("shelf"))
              .concat("/")
              .concat(dataSlugs.getString("product"));
        }
      }
    }

    return productUrl;
  }

  private JSONObject fetchApiProducts(String url) {

    Request request = RequestBuilder
        .create()
        .setUrl(url)
        .mustSendContentEncoding(false)
        .build();

    return CrawlerUtils.stringToJson(new FetcherDataFetcher().get(session, request).getBody());
  }

}
