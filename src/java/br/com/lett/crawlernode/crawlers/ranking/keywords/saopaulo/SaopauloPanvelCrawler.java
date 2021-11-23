package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

import java.time.LocalDate;
import java.util.Collections;

public class SaopauloPanvelCrawler extends CrawlerRankingKeywords {

   public SaopauloPanvelCrawler(Session session) {
      super(session);
      fetchMode = FetchMode.APACHE;
      cookies.add(new BasicClientCookie("stc112189", String.valueOf(LocalDate.now().toEpochDay())));
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 16;
      this.log("Página " + this.currentPage);

      String url = "https://www.panvel.com/panvel/buscarProduto.do?termoPesquisa=" + keywordEncoded + "&paginaAtual=" + currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      Request request = Request.RequestBuilder.create().setCookies(cookies).setUrl(url)
         .mustSendContentEncoding(false)
         .setSendUserAgent(false)
         .setProxyservice(Collections.singletonList(ProxyCollection.BUY))
         .build();
      Response response = dataFetcher.get(session, request);

      currentDoc = Jsoup.parse(response.getBody());
      Elements products = this.currentDoc.select("li.search-item");

      for (Element e : products) {
         String urlProduct = "https://www.panvel.com" + e.selectFirst(".details").attr("href");
         String internalId = CommonMethods.getLast(urlProduct.split("-"));
         String name = CrawlerUtils.scrapStringSimpleInfo(e, "p.name", true);
         String image = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.image img.ng-star-inserted", "src");
         int price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "div.price div.deal-price.ng-star-inserted", null, true, ',', session, 0);
         boolean isAvailable = price != 0;

         //New way to send products to save data product
         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(urlProduct.replace("'", "&apos;"))
            .setInternalId(internalId)
            .setName(name)
            .setImageUrl(image)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .build();

         saveDataProduct(productRanking);
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return (arrayProducts.size() % pageSize - currentPage) < 0;
   }
}
