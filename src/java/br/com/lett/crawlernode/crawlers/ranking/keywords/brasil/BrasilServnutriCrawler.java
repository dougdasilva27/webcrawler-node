package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilServnutriCrawler extends CrawlerRankingKeywords {
  public BrasilServnutriCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 16;

    this.log("Página " + this.currentPage);

    String key = this.keywordWithoutAccents.replaceAll(" ", "+");
    String url = "http://www.servnutri.com.br/search?q=" + key;

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".item-box");

     for (Element e : products) {
        String internalId =CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div[data-productid]", "data-productid");
        String captureUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");
        String productUrl = CrawlerUtils.completeUrl(captureUrl,"http:","www.servnutri.com.br");
        saveDataProduct(internalId, internalId, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - Url: " + productUrl);

        if (this.arrayProducts.size() == productsLimit) {
           break;
        }
     }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected boolean hasNextPage() {
    return !this.currentDoc.select(".next.page-numbers").isEmpty();
  }
}
