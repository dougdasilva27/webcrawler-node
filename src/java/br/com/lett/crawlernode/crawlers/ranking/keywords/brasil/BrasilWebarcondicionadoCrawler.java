package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Date;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilWebarcondicionadoCrawler extends CrawlerRankingKeywords {

  public BrasilWebarcondicionadoCrawler(Session session) {
    super(session);
  }

  private String nextUrl;
  private StringBuilder products = new StringBuilder();

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 16;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url;

    if (this.currentPage == 1) {
      url = "http://www.webarcondicionado.com.br/pesquisa/?s=" + this.keywordEncoded + "&filtro=produtos";
    } else {
      url = nextUrl;
    }

    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar o html
    this.currentDoc = fetchDocument(url);
    this.nextUrl = crawlNextUrl();

    CommonMethods.saveDataToAFile(currentDoc, "/home/gabriel/htmls/WA-" + this.currentPage + ".html");

    Elements products = this.currentDoc.select(".product-div > a");

    // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
    if (!products.isEmpty()) {
      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0) {
        setTotalProductsCarrefour();
      }

      for (Element e : products) {

        // Url do produto
        String productUrl = crawlProductUrl(e);

        saveDataProduct(null, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + null + " - Url: " + productUrl);
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

  private String crawlNextUrl() {
    StringBuilder str = new StringBuilder();

    // this function was found in http://s3.webarcondicionado.com.br/m/production/js/system.1923.js
    // function token_rand(){return"/?token="+(new Date).getTime()+Math.floor(101*Math.random())}
    long token = (new Date().getTime() + ((Double) Math.floor(Math.random() * 101)).intValue());

    str.append("http://www.webarcondicionado.com.br/pesquisa//?token=" + token);
    str.append("&filtro=produtos&scroll=true&s=" + this.keywordEncoded);

    Elements productIds = this.currentDoc.select("input[name=\"product[]\"]");
    for (Element e : productIds) {
      this.products.append("&procuct%5B%5D=" + e.val());
    }

    str.append(this.products.toString());

    return str.toString();
  }



  @Override
  protected boolean hasNextPage() {
    return !this.currentDoc.select(".read-more").isEmpty();
  }

  protected void setTotalProductsCarrefour() {
    Element totalElement = this.currentDoc.select(".result-count strong span").first();

    if (totalElement != null) {
      try {
        this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
      } catch (Exception e) {
        this.logError(CommonMethods.getStackTrace(e));
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlProductUrl(Element e) {
    String productUrl;

    productUrl = e.attr("href");

    if (!productUrl.contains("webarcondicionado")) {
      productUrl = ("https://www.webarcondicionado.com.br/" + e.attr("href")).replace("br//", "br/");
    }


    return productUrl;
  }
}
