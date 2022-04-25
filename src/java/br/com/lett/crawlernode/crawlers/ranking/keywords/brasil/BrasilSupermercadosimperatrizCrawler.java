package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;

public class BrasilSupermercadosimperatrizCrawler extends CrawlerRankingKeywords {
   public BrasilSupermercadosimperatrizCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("vtex_segment", session.getOptions().optString("vtex_segment"));
      cookie.setDomain("www.supermercadosimperatriz.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);
      String url = "https://www.supermercadosimperatriz.com.br/" + this.keywordEncoded + "?page=" + currentPage;
      this.log("Link onde são feitos os crawlers:  " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".main-shelf.n20colunas > ul > li:not(.helperComplement)");
      pageSize = products.size();

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".shelf__product-item", "data-product-id");;
            String productUrl = CrawlerUtils.scrapUrl(e, ".product-item__info > h3 > a", "href", "https:", "www.supermercadosimperatriz.com.br");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".shelf__product-item > .product-item__info > .product-item__title > a", false);
            String image = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-item__img >div > noscript > img", Collections.singletonList("src"), "https", "www.supermercadosimperatriz.com.br");
            Integer priceInCents = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".product-item__info > .product-item__price > .product-item__best-price", null, false, ',', session, null);

            if(priceInCents == 0) priceInCents = null;
            boolean available = priceInCents != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(available)
               .build();

            saveDataProduct(productRanking);
         }
      }


   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }

}
