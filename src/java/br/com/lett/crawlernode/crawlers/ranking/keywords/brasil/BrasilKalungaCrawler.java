package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilKalungaCrawler extends CrawlerRankingKeywords {


  public BrasilKalungaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 40;

    this.log("Página " + this.currentPage);

    JSONObject apiSearch = fetchJsonApi();

    if (apiSearch.has("html") && apiSearch.get("html") instanceof String) {
      this.currentDoc = Jsoup.parse(apiSearch.getString("html"));
    } else {
      this.currentDoc = new Document("");
    }

    Elements products = this.currentDoc.select(".blocoproduto a:not(.small):first-child");

    if (!products.isEmpty()) {
      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0) {
        setTotalBusca(apiSearch);
      }

      for (Element e : products) {
        // InternalPid
        String internalPid = crawlInternalPid(e);

        // InternalId
        String internalId = internalPid;

        // monta a url
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultados!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

  }


  protected void setTotalBusca(JSONObject apiSearch) {
    if (apiSearch.has("quantidade")) {
      try {
        this.totalProducts = Integer.parseInt(apiSearch.getString("quantidade"));
      } catch (Exception e) {
        this.logError(CommonMethods.getStackTraceString(e));
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }


  private String crawlInternalPid(Element e) {
    String internalPid;

    String[] tokens = e.attr("href").split("/");
    internalPid = tokens[tokens.length - 1].split("\\?")[0].replaceAll("[^0-9]", "");

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String productUrl;
    productUrl = e.attr("href");

    if (!productUrl.contains("kalunga")) {
      productUrl = ("https://www.kalunga.com.br/" + productUrl).replace("br//", "br/");
    }

    if (productUrl.contains("?")) {
      productUrl = productUrl.split("\\?")[0];
    }

    return productUrl;
  }

  private JSONObject fetchJsonApi() {
    String url = "https://www.kalunga.com.br/webapi/Busca/BindSearch";
    String payload = "{\"pageIndex\":\"" + this.currentPage + "\",\"idClassificacao\":\"0\",\"idGrupo\":\"0\","
        + "\"tipoOrdenacao\":\"1\",\"termoBuscado\":\"" + this.location + "\",\"itensFiltro\":\"\"," + "\"visao\":\"T\"}";

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json; charset=UTF-8");
    headers.put("X-Requested-With", "XMLHttpRequest");

    String jsonString = fetchStringPOST(url, payload, headers, null);
    JSONObject apiSearch = new JSONObject();

    if (jsonString.startsWith("{")) {
      apiSearch = new JSONObject(jsonString);
    }

    return apiSearch;
  }
}
