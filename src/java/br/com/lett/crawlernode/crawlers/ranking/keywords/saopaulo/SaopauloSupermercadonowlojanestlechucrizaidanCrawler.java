package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class SaopauloSupermercadonowlojanestlechucrizaidanCrawler extends CrawlerRankingKeywords {

  public SaopauloSupermercadonowlojanestlechucrizaidanCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://supermercadonow.com/produtos/loja-nestle-chucri-zaidan/";
  private Map<String, String> headers = new HashMap<>();

  @Override
  protected void processBeforeFetch() {
    super.processBeforeFetch();
    Request request = RequestBuilder.create().setUrl(HOME_PAGE).setCookies(cookies).setHeaders(headers).build();
    Response response = this.dataFetcher.get(session, request);
    Document doc = Jsoup.parse(response.getBody());

    headers.put("Accept", "application/json, text/plain, */*");
    headers.put("X-SNW-TOKEN", scrapToken(doc));

    this.cookies = response.getCookies();
  }

  private String scrapToken(Document doc) {
    String token = null;
    String identifier = "AuthenticateUser.setToken('";

    Elements scripts = doc.select("script");

    for (Element e : scripts) {
      String script = e.html().replace(" ", "");
      if (script.contains(identifier)) {
        token = CrawlerUtils.extractSpecificStringFromScript(script, "AuthenticateUser.setToken('", "')", false);
        break;
      }
    }

    return token;
  }

  @Override
  public void extractProductsFromCurrentPage() {
    this.pageSize = 0;

    this.log("Página " + this.currentPage);
    JSONObject search = crawlSearchApi();

    if (search.has("items") && search.getJSONArray("items").length() > 0) {
      JSONArray products = search.getJSONArray("items");

      if (this.totalProducts == 0) {
        this.totalProducts = products.length();
        this.log("Total da busca: " + this.totalProducts);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String productUrl = crawlProductUrl(product);
        String internalPid = crawlInternalPid(product);
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

  protected void setTotalProducts(JSONObject search) {
    if (search.has("result_meta")) {
      JSONObject resultMeta = search.getJSONObject("result_meta");

      if (resultMeta.has("total")) {
        this.totalProducts = resultMeta.getInt("total");
        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("product_store_id")) {
      internalId = json.get("product_store_id").toString();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("slug")) {
      internalPid = json.get("slug").toString().split("-")[0];
    }

    return internalPid;
  }

  private String crawlProductUrl(JSONObject product) {
    String urlProduct = null;

    if (product.has("slug")) {
      urlProduct = "https://supermercadonow.com/produtos/loja-nestle-chucri-zaidan/produto/" + product.getString("slug");
    }

    return urlProduct;
  }

  private JSONObject crawlSearchApi() {
    JSONObject searchApi = new JSONObject();

    String url = "https://supermercadonow.com/api/v2/stores/loja-nestle-chucri-zaidan/search/" + this.keywordWithoutAccents.replace(" ", "%20");
    this.log("Link onde são feitos os crawlers: " + url);
    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();

    JSONArray array = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

    if (array.length() > 0) {
      searchApi = array.getJSONObject(0);
    }

    return searchApi;
  }
}
