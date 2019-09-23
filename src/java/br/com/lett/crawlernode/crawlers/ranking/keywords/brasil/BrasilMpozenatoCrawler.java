package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

public class BrasilMpozenatoCrawler extends CrawlerRankingKeywords {

  public BrasilMpozenatoCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 96;

    this.log("Página " + this.currentPage);

    String url = "https://www.mpozenato.com.br/busca?busca=" + this.keywordEncoded + "&pagina=" + this.currentPage;

    this.log("Url: " + url);

    this.currentDoc = fetchDocument(url);

    JSONArray products = getJsonArrayProducts(this.currentDoc);

    if (products.length() > 0) {

      for (Object object : products) {
        JSONObject product = (JSONObject) object;
        String internalPid = JSONUtils.getValue(product, "ProdutoId").toString();
        String productUrl = scrapUrl(product);

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
    return !this.currentDoc.select(".pg.sel").isEmpty();
  }

  private String scrapUrl(JSONObject product) {
    String url = JSONUtils.getStringValue(product, "Link");

    if (url != null) {
      url = CrawlerUtils.completeUrl(url, "https", "www.mpozenato.com.br");
    }

    return url;
  }

  private JSONArray getJsonArrayProducts(Document doc) {
    return CrawlerUtils.selectJsonArrayFromHtml(doc, "body > div.content.busca > div > script",
        "Fbits.ListaProdutos.Produtos = ", ";", false, true, true);
  }

}
