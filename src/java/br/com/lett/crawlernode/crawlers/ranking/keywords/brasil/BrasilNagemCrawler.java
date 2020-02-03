package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;

public class BrasilNagemCrawler extends CrawlerRankingKeywords {

  public BrasilNagemCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 10;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url =
        "http://www.nagem.com.br/navegacao?busca=" + this.keywordEncoded + "&p=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar a url
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("#divlistaprodutos > div > a");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (products.size() >= 1) {
      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        // monta a url
        String productUrl = crawlProductUrl(e);

        // InternalPid
        String internalPid = crawlInternalPid(productUrl);

        // InternalId
        String internalId = internalPid;

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
            + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }

      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");

  }

  @Override
  protected boolean hasNextPage() {
    return this.arrayProducts.size() < this.totalProducts;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select("#spanpaginacao").first();

    if (totalElement != null) {
      String text = totalElement.text().toLowerCase();

      if (text.contains("de")) {
        int x = text.indexOf("de") + 2;
        String value = text.substring(x).replaceAll("[^0-9]", "").trim();

        if (!value.isEmpty()) {
          this.totalProducts = Integer.parseInt(value);
        }
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }


  private String crawlInternalPid(String url) {
    String internalPid = null;

    if (url != null) {
      try {
        URL productUrl = new URL(url);

        String path = productUrl.getPath();

        if (path != null) {
          internalPid = path.split("/")[3];
        }
      } catch (MalformedURLException e) {
        this.logError(CommonMethods.getStackTrace(e));
      }
    }

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String urlProduct = e.attr("href");

    if (!urlProduct.contains("nagem")) {
      urlProduct = "http://www.nagem.com.br" + urlProduct;
    }

    return urlProduct;
  }
}
