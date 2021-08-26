package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

import java.util.List;

public class ChileKitchencenterCrawler extends CrawlerRankingKeywords {

  public ChileKitchencenterCrawler(Session session) {
    super(session);
  }

  @Override
   protected Document fetchDocument(String url) {
      this.currentDoc = new Document(url);

      Request request = Request.RequestBuilder.create().setUrl(url).build();
      Response response = dataFetcher.get(session, request);

      String content = response.getBody().replace("\\","");

      return Jsoup.parse(content);
   }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;
    this.log("Página " + this.currentPage);

    String url = "https://kitchencenter.hawksearch.com/sites/kitchencenter//?searchInput=" + this.keywordWithoutAccents.replace(" ", "%20") + "&pg=" + this.currentPage + "&ajax=1&json=1&hawkcustom=undefined";
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".card-title");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
         String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");

         String internalPid = CommonMethods.getLast(productUrl.split("="));

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

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
