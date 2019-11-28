package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilPetnanetCrawler extends CrawlerRankingKeywords {

  private static final String HOME_PAGE = "www.petnanet.com.br";
  private String keywordKey;

  public BrasilPetnanetCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;
    this.log("Página " + this.currentPage);

    this.currentDoc = fetchPage();
    Elements products = this.currentDoc.select(".ad-showcase ul > li:not(.helperComplement)");

    if (!products.isEmpty()) {
      if(this.totalProducts == 0) {
        setTotalProducts();
      }
      
      for (Element e : products) {

        String productPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "[data-id]", "data-id");
        String productUrl = CrawlerUtils.scrapUrl(e, ".product-image > a", Arrays.asList("href"), "https", HOME_PAGE);

        saveDataProduct(null, productPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + productPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;

      }
    } else if(this.arrayProducts.isEmpty()) {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  private Document fetchPage() {
    Document doc = new Document("");

    if (this.currentPage == 1) {
      String url = "https://www.petnanet.com.br/" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);
      doc = fetchDocument(url);

      Elements scripts = doc.select("script[type=text/javascript]");
      String token = "/busca?fq=";

      for (Element e : scripts) {
        String html = e.html();

        if (html.contains(token)) {
          this.keywordKey = CrawlerUtils.extractSpecificStringFromScript(html, "fq=", false, "&", false);
          break;

        }
      }
    } else if (this.keywordKey != null) {
      String url = "https://www.petnanet.com.br/buscapagina?fq=" + this.keywordKey
          + "&PS=24&sl=cd07532c-3786-4711-b4b8-2413489a8eee&cc=24&sm=0&PageNumber=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      doc = fetchDocument(url);
    }

    return doc;
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".resultado-busca-numero .value", null, null, true, true, 0);
    this.log("Total da busca: "+this.totalProducts);
  }
}
