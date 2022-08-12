package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;

public class GpsfarmaCrawler extends CrawlerRankingKeywords {

   public GpsfarmaCrawler(Session session) {
      super(session);
   }

   private String CUSTOMER_LOCATION = session.getOptions().optString("geo_customer_location");
   private String INVENTORY_SOURCE = session.getOptions().optString("geo_inventory_source");

   private String categoryUrl;

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie location = new BasicClientCookie("geo_customer_location", CUSTOMER_LOCATION);
      location.setDomain(".gpsfarma.com");
      location.setPath("/");
      this.cookies.add(location);

      BasicClientCookie source = new BasicClientCookie("geo_inventory_source", INVENTORY_SOURCE);
      source.setDomain(".gpsfarma.com");
      source.setPath("/");
      this.cookies.add(source);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      String url = "https://gpsfarma.com/index.php/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;

      if (this.currentPage > 1 && this.categoryUrl != null) {
         url = this.categoryUrl + "?p=" + this.currentPage;
      }

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("ol.products.list.items > li");

      if (this.currentPage == 1) {
         String redirectUrl = CrawlerUtils.getRedirectedUrl(url, session);

         if (!url.equals(redirectUrl)) {
            this.categoryUrl = redirectUrl;
         }
      }

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element product : products) {

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "div.price-box.price-final_price", "data-product-id");
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "form[data-role=tocart-form]", "data-product-sku");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "a.product-item-link", "href");
            String name = scrapName(product);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".product-image-photo", Collections.singletonList("src"), "https", "covabra.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, "span[data-price-type=finalPrice]", null, false, ',', session, null);
            boolean isAvailable = product.selectFirst(".stock.unavailable") == null;

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

            if (this.arrayProducts.size() == productsLimit)
               break;

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String scrapName(Element product) {
      String name = CrawlerUtils.scrapStringSimpleInfo(product, ".product-item-name", false);
      String brand = CrawlerUtils.scrapStringSimpleInfo(product, ".product-item-brand", false);
      if (brand != null) {
         return name + " - " + brand;
      }
      return name;
   }

   @Override
   protected void setTotalProducts() {
      Element totalSearchElement = this.currentDoc.selectFirst("p.toolbar-amount :last-child");

      if (totalSearchElement != null) {
         this.totalProducts = Integer.parseInt(totalSearchElement.text());
      }

      this.log("Total da busca: " + this.totalProducts);
   }
}
