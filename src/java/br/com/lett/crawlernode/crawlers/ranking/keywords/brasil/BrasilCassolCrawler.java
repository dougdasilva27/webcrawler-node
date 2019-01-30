package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilCassolCrawler extends CrawlerRankingKeywords {

  public BrasilCassolCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 20;

    this.log("Página " + this.currentPage);

    String url =
        "https://www.cassol.com.br/resultadopesquisa?pag=" + this.currentPage + "&departamento=&buscarpor=" + this.keywordEncoded + "&smart=0";
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url, cookies);
    Elements products = this.currentDoc.select(".main-clear .collection > li");
    if (!products.isEmpty()) {
      for (Element e : products) {
        JSONObject info = scrapJsonInfo(e);
        String internalPid = info.has("pid") ? info.get("pid").toString() : null;
        String productUrl = info.has("url") ? info.get("url").toString() : null;

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
    return !this.currentDoc.select(".collection-pagination-pages .fa-chevron-right").isEmpty();
  }

  private JSONObject scrapJsonInfo(Element e) {
    JSONObject json = new JSONObject();
    Element ancorElement = e.selectFirst(".boxselosvitrine a");
    String jsScript = ancorElement.attr("href").replace(" ", "");

    if (jsScript.contains("push(")) {
      JSONObject product = new JSONObject(CrawlerUtils.extractSpecificStringFromScript(jsScript, "push(", ";", false));
      if (product.has("ecommerce")) {
        JSONObject ecommerce = product.getJSONObject("ecommerce");

        if (ecommerce.has("click")) {
          JSONObject click = ecommerce.getJSONObject("click");

          if (click.has("products")) {
            JSONArray products = click.getJSONArray("products");

            if (products.length() > 0) {
              JSONObject productJson = products.getJSONObject(0);

              if (productJson.has("id")) {
                json.put("pid", productJson.get("id"));
              }
            }
          }
        }
      }

      json.put("url", CrawlerUtils.extractSpecificStringFromScript(jsScript, "window.location=", ";", false).replace("'", ""));
    }

    return json;
  }
}
