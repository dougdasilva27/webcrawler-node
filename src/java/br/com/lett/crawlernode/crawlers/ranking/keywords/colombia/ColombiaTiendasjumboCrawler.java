package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import java.util.Arrays;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ColombiaTiendasjumboCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://busqueda.tiendasjumbo.co/";
   private String vtex_segment = getVtex_segment();

   public ColombiaTiendasjumboCrawler(Session session) {
      super(session);
   }

   private String getVtex_segment(){
      return session.getOptions().optString("vtex_segment");
   }

   @Override
   public void processBeforeFetch(){
      if(vtex_segment != null){
         BasicClientCookie cookie = new BasicClientCookie("vtex_segment", vtex_segment);
         cookie.setDomain("www.tiendasjumbo.co");
         cookie.setPath("/");
         this.cookies.add(cookie);
      }
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = HOME_PAGE + "busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("ul.neemu-products-container.nm-view-type-grid > li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String internalPid = e.attr("data-sku");
            String productUrl = CrawlerUtils.scrapUrl(e, ".nm-product-name a", Arrays.asList("href"), "https", HOME_PAGE);
            String imageUrl = "https:" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "img[itemprop=image]", "src");
            int price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "span.nm-price-value", null, true, ',', session, 0);
            String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a.nm-product-img-link", "title");

            RankingProduct product = new RankingProductBuilder()
               .setInternalPid(internalPid)
               .setImageUrl(imageUrl)
               .setName(name)
               .setAvailability(price != 0)
               .setPriceInCents(price)
               .setUrl(productUrl)
               .build();

            saveDataProduct(product);
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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".neemu-mb-total-products-container", true, 0);
      this.log("Total: " + this.totalProducts);
   }
}
