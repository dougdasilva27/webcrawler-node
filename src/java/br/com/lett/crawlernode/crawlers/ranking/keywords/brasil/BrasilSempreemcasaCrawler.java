package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilSempreemcasaCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "sempreemcasa.com.br";

  public BrasilSempreemcasaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 30;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://sempreemcasa.com.br/search?page=" + this.currentPage + "&q=" + this.keywordEncoded;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".product-item");

    if (!products.isEmpty()) {
      for (Element e : products) {
        String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item__variant-data[data-id]", "data-id");
        String productUrl = scrapUrl(e);

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
  
  private String scrapUrl(Element e) {
    String url = null;
    String fullUrl = CrawlerUtils.scrapUrl(e, "a.product-link", Arrays.asList("href"), "https", HOME_PAGE);
    
    if(fullUrl != null) {
      url = fullUrl.split("\\?")[0];
    }
    
    return url;
  }
  
  @Override
  protected boolean hasNextPage() {
    return this.currentDoc.selectFirst(".pages-navigation__btn-next > a[href=\"\"]") == null;
  }
}
