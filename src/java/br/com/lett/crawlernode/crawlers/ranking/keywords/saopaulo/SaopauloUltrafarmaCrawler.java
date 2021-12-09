package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class SaopauloUltrafarmaCrawler extends CrawlerRankingKeywords {

  public SaopauloUltrafarmaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() throws MalformedProductException {
    this.pageSize = 30;

    this.log("Página " + this.currentPage);
    String url = "https://www.ultrafarma.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".prd-list-item");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String productUrl = CrawlerUtils.scrapUrl(e, "a.product-item-link", "href", "https", "www.ultrafarma.com.br");
        String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-wrapper-container", "data-product-id");
        String name = CrawlerUtils.scrapStringSimpleInfo(e, "h3.product-name", false);
        String imgUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "img.lazy", "data-original");
        Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e,".product-price-sell", "data-preco", false, ',', session, 0);
        String soldOutMessage = CrawlerUtils.scrapStringSimpleInfo(e, ".product-price-unavailable", false);
        Boolean isAvailable = soldOutMessage != null;

        RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setName(name)
            .setImageUrl(imgUrl)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .build();

         saveDataProduct(productRanking);

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
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".prd-list-count", "de", null, false, true, 0);
    this.log("Total de produtos: " + this.totalProducts);
  }
}
