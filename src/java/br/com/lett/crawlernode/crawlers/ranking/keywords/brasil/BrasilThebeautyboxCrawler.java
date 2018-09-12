package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilThebeautyboxCrawler extends CrawlerRankingKeywords {

  public BrasilThebeautyboxCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 42;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "http://busca.thebeautybox.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    // chama função de pegar o html
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("#neemu-products-container li[layout]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProductsCarrefour();
      }

      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        String productUrl = crawlProductUrl(e);
        String internalId = crawlInternalId(productUrl);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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

  protected void setTotalProductsCarrefour() {
    Element totalElement = this.currentDoc.select("#neemu-total-products-container").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalId(String url) {
    String internalId = null;

    if (url != null && url.toLowerCase().contains("idsku=")) {
      String idCandidate = CommonMethods.getLast(url.toLowerCase().split("idsku=")).trim();

      // if url contains more parameters
      if (idCandidate.contains("&")) {
        internalId = idCandidate.split("\\&")[0];
      } else {
        internalId = idCandidate;
      }
    }

    return internalId;
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;

    Element pid = e.selectFirst(".yv-review-quickreview");
    if (pid != null) {
      internalPid = pid.val();
    }

    return internalPid;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.select(".subtitle a").first();

    if (url != null) {
      productUrl = url.attr("href");

      if (!productUrl.startsWith("http")) {
        productUrl = "https:" + productUrl;
      }
    }

    return productUrl;
  }
}
