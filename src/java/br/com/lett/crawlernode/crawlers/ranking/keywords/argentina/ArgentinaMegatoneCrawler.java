package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ArgentinaMegatoneCrawler extends CrawlerRankingKeywords {


  public ArgentinaMegatoneCrawler(Session session) {
    super(session);
  }

  private String keywordKey;

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 24;

    this.log("Página " + this.currentPage);

    JSONObject apiSearch = fetchJsonApi();

    if (apiSearch.has("_HTMLProductos") && apiSearch.get("_HTMLProductos") instanceof String) {
      this.currentDoc = Jsoup.parse(apiSearch.getString("_HTMLProductos"));
    } else {
      this.currentDoc = new Document("");
    }

    Elements products = this.currentDoc.select(".itemMegatoneComun > div:first-child > a[onclick][href]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalBusca(apiSearch);
      }

      for (Element e : products) {
        String internalId = crawlInternalId(e);
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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
    if (apiSearch.has("_ProductosFiltrados") && apiSearch.get("_ProductosFiltrados") instanceof Integer) {
      this.totalProducts = apiSearch.getInt("_ProductosFiltrados");
    }

    this.log("Total da busca: " + this.totalProducts);
  }


  private String crawlInternalId(Element e) {
    String internalId = null;

    String text = e.attr("onclick");
    if (text.contains("('")) {
      int x = text.indexOf("('") + 2;
      int y = text.indexOf("')", x);

      internalId = text.substring(x, y).trim();
    }

    return internalId;
  }

  private String crawlProductUrl(Element e) {
    return CrawlerUtils.sanitizeUrl(e, "href", "https://", "www.megatone.net").split("\\?")[0];
  }

  private JSONObject fetchJsonApi() {
    JSONObject apiSearch = new JSONObject();

    if (this.currentPage == 1) {
      Document doc = fetchDocument("https://www.megatone.net/search/" + this.keywordWithoutAccents.replace(" ", "-") + "/");

      Element key = doc.selectFirst("#MainContent_lblPalabraBuscada");
      if (key != null) {
        this.keywordKey = key.ownText().trim();
      }
    }

    if (this.keywordKey != null) {
      String url = "https://www.megatone.net/Listado.aspx/CargarMas";
      String payload =
          "{\"idMenu\":\"\",\"paginaActual\":\"" + this.currentPage + "\",\"familiasBuscadas\":\"\",\"filtroCategorias\":\"\",\"filtroGeneros\":\"\","
              + "\"filtroPlataformas\":\"\",\"filtroMarcas\":\"\",\"filtroPrestadoras\":\"\",\"filtroPrecios\":\"\",\"filtroOfertas\":\"0\","
              + "\"palabraBuscada\":\"" + this.keywordKey
              + "\",\"menorPrecioMultiploDe10\":\"\",\"intervaloPrecios\":\"\",\"tipoListado\":\"Grilla\",\"orden\":\"0\","
              + "\"productosBuscados\":\"\",\"filtroCuotas\":\"\",\"NroBoca\":0}";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json; charset=UTF-8");

      JSONObject response = CrawlerUtils.stringToJson(fetchStringPOST(url, payload, headers, null));

      if (response.has("d")) {
        apiSearch = response.getJSONObject("d");
      }
    }

    return apiSearch;
  }
}
