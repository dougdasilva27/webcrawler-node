package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilGazinCrawler extends CrawlerRankingKeywords {

  public BrasilGazinCrawler(Session session) {
    super(session);
  }


  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 21;
    this.log("Página " + this.currentPage);

    this.currentDoc = fetchCurrentPage();
    Elements products = this.currentDoc.select(".listaprod li");

    if (!products.isEmpty()) {

      for (Element e : products) {
        String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "meta[itemprop=productID]", "content");
        String productUrl = CrawlerUtils.scrapUrl(e, "a[itemprop=url]", "href", "https", "www.gazin.com.br");

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
  protected boolean hasNextPage() {
    return !this.currentDoc.select(".conteudo .BarraPaginas a:last-child input").isEmpty();
  }

  /**
   * For this site you need to click on a button to follow search pages, even on the first page has a
   * button to list products, if you click the page reload and appears the search page
   * 
   * For categories this site only redirect you to categories page
   * 
   * @return
   */
  private Document fetchCurrentPage() {
    Document doc = new Document("");

    if (this.currentPage == 1) {
      String url = "https://www.gazin.com.br/" + this.keywordWithoutAccents.replace(" ", "-") + ".mht";
      doc = fetchDocument(url);
      String in = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=in]", "value");

      // this happen because when returns more than one category, we just crawl the first page
      if (in != null && doc.select("#conteudoProdutosSB > div > .mProdutosEspacoAntPonto").size() < 2) {
        try {
          url = "https://www.gazin.com.br/comprar/encontrar.php?des=s&in=" + URLEncoder.encode(in, "UTF-8");
          doc = fetchDocument(url);
        } catch (UnsupportedEncodingException e) {
          Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
        }
      }
    } else {
      StringBuilder url = new StringBuilder();
      url.append("https://www.gazin.com.br/comprar/encontrar.php?");

      try {
        Elements inputs = this.currentDoc.select(".conteudo .BarraPaginas a:last-child input");
        for (Element e : inputs) {
          url.append("&").append(e.attr("name")).append("=").append(URLEncoder.encode(e.val(), "UTF-8"));
        }

        doc = fetchDocument(url.toString());
      } catch (UnsupportedEncodingException e) {
        Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }
    }

    return doc;
  }

}
