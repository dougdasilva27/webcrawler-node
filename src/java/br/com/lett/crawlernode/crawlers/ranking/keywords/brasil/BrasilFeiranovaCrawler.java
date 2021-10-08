package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BrasilFeiranovaCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.feiranovaemcasa.com.br/";

   public BrasilFeiranovaCrawler(Session session) {
      super(session);
      dataFetcher = new JsoupDataFetcher();
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 48;
      this.log("Página " + this.currentPage);

      //Website has no pagination
      String url = HOME_PAGE + "busca?q=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("div.produto");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = products.size();
         }
         for (Element e : products) {

            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.info-produto > input", "value");
            String productUrl = HOME_PAGE + CrawlerUtils.scrapStringSimpleInfoByAttribute(e,"a.content-produto", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e,"span.nome",true);
            String imgUrl = scrapUrl(e);
            int price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "div.valor-principal", null, false, ',', session, 0);
            boolean isAvailable = price != 0;

            //New way to send products to save data product
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + null +
                  " - InternalPid: " + internalPid +
                  " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

   }

   private String scrapUrl(Element doc) {
      String url = HOME_PAGE;
      String slug = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "img[onerror]", "urlreal");

      if (slug != null) {
         slug = slug.replace("../", "");
         url += slug;
      }

      return url;
   }

   @Override
   protected boolean hasNextPage(){
      return false;
   }
}
