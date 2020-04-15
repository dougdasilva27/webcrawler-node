package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilSalonlineCrawler extends CrawlerRankingKeywords {

  public BrasilSalonlineCrawler(Session session) {
    super(session);
    pageSize = 32;
  }

  private final static String homePage = "https://www.lojadasalonline.com.br";

  @Override
  protected void extractProductsFromCurrentPage() {
    Document doc = fetchDocument(
        homePage + "/pesquisa/?pg=" + currentPage + "&t=" + keywordEncoded);
    Elements elements = doc.select("li .wd-product-line");
    for (Element elem : elements) {
      String internalPid = elem.attr("data-product-id");
      String urlProduct = homePage + elem.selectFirst("div > div > a").attr("href");
      saveDataProduct(null, internalPid, urlProduct);
      this.log("Position: " + this.position + " - InternalPid: " + internalPid + " - Url: "
          + urlProduct);
    }
  }

  @Override
  protected boolean hasNextPage() {
    return this.position % pageSize < 1;
  }
}
