package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class SaopauloAmericanasCrawler extends CrawlerRankingKeywords {

  public SaopauloAmericanasCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 48;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.americanas.com.br/busca/" + this.keywordEncoded + "?limite=24&offset=" + this.arrayProducts.size();
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url, null);

    Elements products = this.currentDoc.select(".card-product .card-product-url");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        String productUrl = crawlProductUrl(internalPid);

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

  protected void extractProductsFromAPI() {
    this.log("Página " + this.currentPage);

    String keyword = this.keywordEncoded.replaceAll(" ", "+");

    String urlAPi = "https://mystique-v1-americanas.b2w.io/mystique/search?content=" + keyword + "&offset=" + +this.arrayProducts.size()
        + "&sortBy=moreRelevant&source=nanook";

    JSONObject api = fetchJSONObject(urlAPi);
    JSONArray products = new JSONArray();

    if (api.has("products") && api.get("products") instanceof JSONArray) {
      products = api.getJSONArray("products");
    }

    if (products.length() >= 1) {
      if (this.totalProducts == 0) {
        setTotalProducts(api);
      }

      for (int i = 0; i < products.length(); i++) {
        JSONObject product = products.getJSONObject(i);

        String internalPid = crawlInternalPid(product);
        String productUrl = crawlProductUrl(internalPid);

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

  protected void setTotalProducts(JSONObject api) {
    if (api.has("_result")) {
      JSONObject result = api.getJSONObject("_result");

      if (result.has("total")) {
        this.totalProducts = result.getInt("total");

        this.log("Total da busca: " + this.totalProducts);
      }
    }
  }

  @Override
  protected void setTotalProducts() {
    Element e = this.currentDoc.select(".form-group.display-sm-inline-block span").first();

    if (e != null) {
      String total = e.ownText().replaceAll("[^0-9]", "").trim();

      if (!total.isEmpty()) {
        this.totalProducts = Integer.parseInt(total);
        this.log("Total Search: " + this.totalProducts);
      }
    }
  }

  private String crawlInternalPid(JSONObject product) {
    String internalPid = null;

    if (product.has("id")) {
      internalPid = Integer.toString(product.getInt("id"));
    }

    return internalPid;
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;
    String href = e.attr("href");

    if (href.contains("?")) {
      href = href.split("[?]")[0];
    }

    if (!href.isEmpty()) {
      String[] tokens = href.split("/");
      internalPid = tokens[tokens.length - 1];
    }

    return internalPid;
  }

  private String crawlProductUrl(String internalPid) {
    return "https://www.americanas.com.br/produto/" + internalPid;
  }
}
