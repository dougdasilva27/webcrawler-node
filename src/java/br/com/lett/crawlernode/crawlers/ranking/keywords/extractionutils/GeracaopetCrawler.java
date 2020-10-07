package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

public class GeracaopetCrawler extends CrawlerRankingKeywords {
  protected String cep;

  public GeracaopetCrawler(Session session, String cep) {
    super(session);
    this.cep = cep;
  }

  @Override
  public void processBeforeFetch() {
    BasicClientCookie cookie = new BasicClientCookie("zipcode", cep);
    cookie.setDomain(".www.geracaopet.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  private JSONArray acessAPI(){

    String url = "https://api.geracaopet.com.br/api/V2/catalogs/products/search?text=" + this.keywordEncoded + "&page=0&perPage=32";
    this.log("Link onde são feitos os crawlers: " + url);
    Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session,request).getBody());
    JSONObject data = !response.isEmpty()? response.optJSONObject("data"): new JSONObject();

    return data.optJSONArray("products");

  }


  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    this.pageSize = 16;

    JSONArray products = acessAPI();


    if (!products.isEmpty()) {

      for (Object e : products) {

        JSONObject product = (JSONObject) e;

        String internalId = product.optString("sku");
        String productUrl = product.optString("url");

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

}
