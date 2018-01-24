package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilKitchenaidCrawler extends CrawlerRankingKeywords {

  public BrasilKitchenaidCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 24;

    this.log("Página " + this.currentPage);

    // se a key contiver o +, substitui por %20, pois nesse market a pesquisa na url é assim
    String url = "http://busca.kitchenaid.com.br/busca?q=" + this.keywordWithoutAccents.replace(" ", "%20") + "&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".nm-search-results-container ul > li#nm-product-");
    JSONArray productsIds = crawlIdsFromScript();

    if (!products.isEmpty() && productsIds.length() > 0) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (int i = 0; i < products.size(); i++) {
        String internalPid = crawlInternalPid(productsIds.getJSONObject(i));
        String productUrl = crawlProductUrl(products.get(i));

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

  @Override
  protected boolean hasNextPage() {
    return this.arrayProducts.size() < this.totalProducts;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select(".neemu-total-products-container").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }

  private JSONArray crawlIdsFromScript() {
    JSONArray productsIds = new JSONArray();

    Element e = this.currentDoc.select(".nm-search-results-container script").first();

    if (e != null) {
      String html = e.html().replace(" ", "");

      if (html.contains("product_list=")) {
        int x = html.indexOf('=') + 1;

        productsIds = new JSONArray(html.substring(x));
      }
    }

    return productsIds;
  }

  private String crawlInternalPid(JSONObject obj) {
    return obj.has("id") ? obj.getString("id") : null;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element urlElement = e.select(".nm-product-info a").first();

    if (urlElement != null) {
      productUrl = urlElement.attr("href");

      if (!productUrl.startsWith("http")) {
        productUrl = "http:" + productUrl;
      }
    }

    return productUrl;
  }
}
