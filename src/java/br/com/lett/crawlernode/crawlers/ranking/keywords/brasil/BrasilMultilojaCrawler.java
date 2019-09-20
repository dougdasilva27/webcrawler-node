package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

/**
 * 
 * @author gabriel
 *
 */
public class BrasilMultilojaCrawler extends CrawlerRankingKeywords {

  public BrasilMultilojaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    if (this.currentPage == 1) {
      String url = "https://www.multiloja.com.br/" + this.keywordWithoutAccents.replace(" ", "-") + ".mht";
      scrapProducts(url);
    } else {
      List<String> categoriesUrls = scrapCategoriesUrls();
      for (String url : categoriesUrls) {
        scrapProducts(url);

        if (this.arrayProducts.size() == productsLimit) {
          break;
        }

        this.currentPage++;
      }
    }
  }

  private void scrapProducts(String url) {
    this.log("Página " + this.currentPage);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".listaprod li[itemprop=\"offers\"]");

    if (!products.isEmpty()) {

      for (Element e : products) {
        String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "meta[itemprop=productID]", "content");
        String productUrl = CrawlerUtils.scrapUrl(e, "a[itemprop=url]", "href", "https", "www.multiloja.com.br");

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;

      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size()
        + " produtos crawleados");
  }

  @Override
  protected boolean hasNextPage() {
    return !this.currentDoc.select(".conteudo .BarraPaginas a:last-child input").isEmpty() ||
        !this.currentDoc.select("#conteudoProdutosSB > div > .mProdutosEspacoAntPonto a").isEmpty();
  }

  private List<String> scrapCategoriesUrls() {
    List<String> urls = new ArrayList<>();

    Elements categories = this.currentDoc.select("#conteudoProdutosSB > div > .mProdutosEspacoAntPonto");
    for (Element e : categories) {
      if (!e.select("a").isEmpty()) {
        String in = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "input[name=in]", "value");

        if (in != null) {
          try {
            urls.add("https://www.multiloja.com.br/comprar/encontrar.php?des=s&in=" + URLEncoder.encode(in, "UTF-8"));
          } catch (UnsupportedEncodingException ex) {
            Logging.printLogError(logger, session, CommonMethods.getStackTrace(ex));
          }
        }
      }
    }

    return urls;
  }
}
