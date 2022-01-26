package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.List;

public class BrasilBifarmaCrawler extends CrawlerRankingKeywords {


   public BrasilBifarmaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      String url = "https://www.bifarma.com.br/busca_Loja.html?q="
         + this.keywordWithoutAccents.replace(" ", "+");

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("#gridProdutos .product");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalPid = crawlInternalPid(e);
            String productUrl = crawlProductUrl(e);
            String name = CrawlerUtils.scrapStringSimpleInfo(e,".product_body .name", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product_image a img", Arrays.asList("src"), "https", "");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".product_price > div > strong", null, false, ',', session, 0);

            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(imgUrl)
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
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return false;
   }

   private String crawlInternalPid(Element e) {
      String internalPid = null;

      Element id = e.select("#produto_id").first();

      if (id != null) {
         internalPid = id.val();
      }

      return internalPid;
   }

   private String crawlProductUrl(Element e) {
      String productUrl = null;

      Element url = e.select("meta[itemprop=url]").first();

      if (url != null) {
         productUrl = url.attr("content");

         if (!productUrl.contains("bifarma")) {
            productUrl = "https://www.bifarma.com.br" + productUrl;
         } else if (!productUrl.contains("http")) {
            productUrl = "https://" + productUrl;
         }
      }

      return productUrl;
   }
}
