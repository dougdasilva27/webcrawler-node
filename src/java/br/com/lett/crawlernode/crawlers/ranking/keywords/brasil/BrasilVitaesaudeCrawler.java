package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BrasilVitaesaudeCrawler extends CrawlerRankingKeywords {

  public BrasilVitaesaudeCrawler(Session session) {
    super(session);
  }

   @Override
  protected void extractProductsFromCurrentPage() throws MalformedProductException {
    this.pageSize = 27;

    this.log("Página " + this.currentPage);

    String url = "https://www.vitaesaude.com.br/search?search_query=" + this.keywordEncoded + "&page=" + this.currentPage + "&ajax=1";
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".ProductItem");

    if (!products.isEmpty()) {
      for (Element e : products) {
        String internalPid = crawlInternalPid(e);
        String internalId = null;
        String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".name a", "href");
        String name = CrawlerUtils.scrapStringSimpleInfo(e, ".name", false);
        String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".image img", Collections.singletonList("data-original"), "https", "vitaesaude.com.br" );
        Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".ValorProduto", null, true, ',', session, 0);
        boolean isAvailable = price != 0;

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setImageUrl(imgUrl)
            .setName(name)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .build();

         saveDataProduct(productRanking);
         if (this.arrayProducts.size() == productsLimit) {
            break;
         }

      }
    } else {
       this.result = false;
       this.log("Keyword sem resultados!");
    }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

  @Override
  protected boolean hasNextPage() {
    return !this.currentDoc.select(".ProductItem").isEmpty();
  }

  private String crawlInternalPid(Element e) {
    String internalPid = null;
    String idElement = e.attr("id");

    if (idElement != null) {
      String id = CommonMethods.getLast(idElement.split("ProductItem_"));

      if (!id.isEmpty()) {
        internalPid = id;
      }
    }

    return internalPid;
  }

}
