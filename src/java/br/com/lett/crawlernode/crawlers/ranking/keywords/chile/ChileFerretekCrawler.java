package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class ChileFerretekCrawler extends CrawlerRankingKeywords {
   public ChileFerretekCrawler(Session session) {
      super(session);
   }
   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 20;

      this.log("Página : " + this.currentPage);

      String url = "https://herramientas.cl/busquedas?buscar=" + keywordEncoded + "&linea=0&categoria=0&marca=0&desde=0&hasta=0&order=0&page=" + currentPage;

      this.log("URL : " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".grilla-productos > .grilla.grilla-dos");

      if (!products.isEmpty()) {
         for(Element product : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".imgGrilla", "href");
            if (internalId != null) {
               if (internalId.split("/").length > 1) {
                  internalId = internalId.split("/")[1];
               }
            }
            String productUrl = "https://herramientas.cl" + "/" + CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".imgGrilla", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".nombreGrilla.link", true);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".imgGrilla > img", Collections.singletonList("src"), "https", "herramientas.cl");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".valorGrilla.link", null, true, ',', session, 0);
            String valorGrilla = CrawlerUtils.scrapStringSimpleInfo(product, ".valorGrilla.link", true);
            if (valorGrilla != null) {
               if (valorGrilla.equals("")) {
                  price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".valorGrilla.link > .conDescuento", null, true, ',', session, 0);
               }
            }
            boolean isAvailable = checkIfIsAvailable(product);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultados!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   private boolean checkIfIsAvailable(Element product) {
      return product.select(".imgGrilla > .no-stock-grilla").isEmpty();
   }

   @Override
   protected boolean hasNextPage() {
      String hrefNextPageButton = CrawlerUtils.scrapStringSimpleInfoByAttribute(this.currentDoc,".container.paginador > :last-child", "href");
      if (hrefNextPageButton != null) {
         return !hrefNextPageButton.equals("javascript:void(0)");
      }
      return false;
   }
}
