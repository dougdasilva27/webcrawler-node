package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import java.util.Arrays;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.ChileJumboCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class ChileJumbovinaCrawler extends CrawlerRankingKeywords {

  public ChileJumbovinaCrawler(Session session) {
    super(session);
  }

  @Override
	protected void processBeforeFetch() {
		super.processBeforeFetch();
		
		Logging.printLogDebug(logger, session, "Adding cookie...");

	    BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=" + ChileJumboCrawler.JUMBO_VINA_ID);
	    cookie.setDomain("." + ChileJumboCrawler.HOST);
	    cookie.setPath("/");
	    this.cookies.add(cookie);
	}
  
  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    this.pageSize = 18;

    String url = "https://" + ChileJumboCrawler.HOST + "/busca/?ft=" + this.keywordWithoutAccents.replace(" ", "%20") + "&PageNumber=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url, cookies);

    Elements products = this.currentDoc.select(ChileJumboCrawler.RANKING_SELECTOR);

    if (!products.isEmpty()) {
      if (this.totalProducts == 0)
        setTotalProducts();

      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        String productUrl = crawlProductUrl(e);

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.selectFirst(ChileJumboCrawler.RANKING_SELECTOR_TOTAL);

    if (totalElement != null) {
      String text = totalElement.text().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalPid(Element e) {
    return e.attr(ChileJumboCrawler.RANKING_ATTRIBUTE_ID);
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element urlElement = e.selectFirst(ChileJumboCrawler.RANKING_SELECTOR_URL);

    if (urlElement != null) {
      productUrl = CrawlerUtils.sanitizeUrl(urlElement, Arrays.asList("href"), "https:", ChileJumboCrawler.HOST);
    }

    return productUrl;
  }
}
