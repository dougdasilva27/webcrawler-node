package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilFastshopCrawler extends CrawlerRankingKeywords {

  public BrasilFastshopCrawler(Session session) {
    super(session);
  }

  private String urlCategory;
  private boolean isCategory = false;
  private boolean specialSearch = false;

  @Override
  protected void extractProductsFromCurrentPage() {

    this.log("Página " + this.currentPage);

    // monta url temporária de parãmetro para verificação do isCategory
    String urlTemp =
        "https://www.fastshop.com.br/webapp/wcs/stores/servlet/SearchDisplay?searchTerm="
            + this.keywordEncoded + "&pageSize=50&" + "beginIndex=" + this.arrayProducts.size()
            + "&storeId=10151&catalogId=11052&langId=-6&sType=SimpleSearch"
            + "&resultCatEntryType=2&showResultsPage=true&searchSource=Q&hotsite=fastshop";

    String apiUrl =
        "https://fastshop-v6.neemu.com/searchapi/v3/search?apiKey=fastshop-v6&secretKey=7V0dpc8ZFxwCRyCROLZ8xA%253D%253D&terms="
            + this.keywordWithoutAccents.replace(" ", "%20") + "&resultsPerPage=9&page="
            + this.currentPage;

    String url = urlTemp;
    // monta a url de acordo com o tipo: categoria ou busca.
    if (isCategory) {
      url = this.urlCategory + "&beginIndex=" + this.arrayProducts.size();
    } else if (specialSearch) {
      url = apiUrl;
    }

    this.currentDoc = specialSearch ? new Document("") : fetchDocument(url);

    Elements products = this.currentDoc.select("div.row > div");

    if (this.currentPage == 1) {
      this.specialSearch = isSpecialSearch(products);

      if (!this.specialSearch) {
        isCategory(url);
      }
    }

    this.log("Link onde são feitos os crawlers: " + url);

    if (specialSearch) {
      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "https://www.fastshop.com.br");

      String json = fetchGetFetcher(apiUrl, null, headers, null);

      JSONObject api = new JSONObject(json);
      extractProductFromJSON(api);
    } else {
      extractProductsFromHTML(products);
    }
  }

  private void extractProductFromJSON(JSONObject api) {
    if (api.has("products")) {
      if (this.totalProducts == 0) {
        setTotalProductsFromJSON(api);
      }

      JSONArray products = api.getJSONArray("products");
      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String productUrl = crawlProductUrlFromJson(product);
        JSONArray internalIds = crawlInternalId(product);

        this.position++;

        for (int j = 0; j < internalIds.length(); j++) {
          String internalId = internalIds.getString(j);
          saveDataProduct(internalId, null, productUrl, this.position);

          this.log("Position: " + this.position + " - InternalId: " + internalId
              + " - InternalPid: " + null + " - Url: " + productUrl);
        }

        if (this.arrayProducts.size() == productsLimit) {
          break;
        }
      }
    }
  }

  private void extractProductsFromHTML(Elements products) {
    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProductsForNormalPage();
      }

      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        String productUrl = crawlProductUrl(internalPid);

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: "
            + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");

    // número de produtos por página do market
    if (this.currentPage == 1) {
      if (isCategory)
        this.pageSize = 12;
      else
        this.pageSize = 9;
    }
  }

  private boolean isSpecialSearch(Elements products) {
    if (!products.isEmpty()) {
      return crawlInternalPid(products.get(0)) == null;
    }

    return false;
  }

  @Override
  protected boolean hasNextPage() {
    return this.arrayProducts.size() < this.totalProducts;
  }

  private void isCategory(String url) {
    if (session.getRedirectedToURL(url) != null && !url.equals(session.getRedirectedToURL(url))) {
      Element codigoCatElement = this.currentDoc.select("div.compare_controls.disabled a").first();

      if (codigoCatElement != null) {
        String[] tokens = codigoCatElement.attr("href").split(",");
        int x = tokens[tokens.length - 1].indexOf("'");
        int y = tokens[tokens.length - 1].indexOf("'", x + 1);

        String codigoCat = tokens[tokens.length - 1].substring(x + 1, y);

        this.isCategory = true;
        // monta a url com a keyword e a página
        this.urlCategory =
            "http://www.fastshop.com.br/webapp/wcs/stores/servlet/CategoryNavigationResultsView?pageSize=12"
                + "&categoryId=" + codigoCat
                + "&storeId=10151&catalogId=11052&langId=-6&sType=SimpleSearch&resultCatEntryType=2"
                + "&showResultsPage=true&searchSource=Q&hotsite=fastshop";
      }
    }
  }

  protected void setTotalProductsForNormalPage() {
    Element totalElement = this.currentDoc.select("div#catalog_search_result_information").first();

    if (totalElement != null) {
      try {
        int x = totalElement.text().indexOf("totalResultCount:");
        int y = totalElement.text().indexOf(",", x + ("totalResultCount:".length()));

        String token =
            (totalElement.text().substring(x + ("totalResultCount:".length()), y)).trim();

        this.totalProducts = Integer.parseInt(token);
      } catch (Exception e) {
        this.logError(e.getMessage());
      }

      this.log("Total da busca: " + this.totalProducts);
    }

  }

  private void setTotalProductsFromJSON(JSONObject api) {
    if (api.has("size") && api.get("size") instanceof Integer) {
      this.totalProducts = api.getInt("size");
      this.log("Total de produtos: " + this.totalProducts);
    }
  }

  private JSONArray crawlInternalId(JSONObject product) {
    JSONArray internalIds = new JSONArray();

    if (product.has("details")) {
      JSONObject details = product.getJSONObject("details");

      if (details.has("catalogEntryId")) {
        internalIds = details.getJSONArray("catalogEntryId");
      }
    }

    return internalIds;
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;
    Element ids = e.select("div.catEntryIDPriceList").first();

    if (ids != null) {
      String[] tokens = ids.attr("id").split("_");
      internalPid = tokens[tokens.length - 1];
    }

    return internalPid;
  }

  private String crawlProductUrlFromJson(JSONObject product) {
    String productUrl = product.has("url") ? product.get("url").toString() : null;

    if (productUrl != null && !productUrl.contains("http")) {
      productUrl = ("https://" + productUrl).replace("////", "//").replace("//:", "//");
    }

    return productUrl;
  }

  private String crawlProductUrl(String pid) {
    return "http://www.fastshop.com.br/webapp/wcs/stores/servlet/ProductDisplay?productId=" + pid
        + "&storeId=10151";
  }

}
