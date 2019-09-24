package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;

public class RiodejaneiroAnossadrogariaCrawler extends CrawlerRankingKeywords {

  public RiodejaneiroAnossadrogariaCrawler(Session session) {
    super(session);
  }


  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;

    this.log("Página " + this.currentPage);

    String url = "https://www.anossadrogaria.com.br/" + this.keywordWithoutAccents.replace(" ", "%20") + "?PageNumber=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);
    CommonMethods.saveDataToAFile(this.currentDoc, Test.pathWrite + "exemple.html");
    Elements products = this.currentDoc.select(".prateleira ul li[layout] .prateleira__item");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element element : products) {
        String internalPid = element.attr("data-id");
        String productUrl = crawlProductUrl(element);
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

  private String crawlProductUrl(Element element) {
    Element elementUrl = element.selectFirst(".prateleira__image-link");
    String productUrl = null;

    if (elementUrl != null) {
      productUrl = elementUrl.attr("href");
    }

    return productUrl;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select(".searchResultsTime .value").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }
    }

    this.log("Total da busca: " + this.totalProducts);
  }


}
