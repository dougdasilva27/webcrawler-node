package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ArgentinaPigmentoCrawler extends CrawlerRankingKeywords {

   public ArgentinaPigmentoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      String url = "https://www.perfumeriaspigmento.com.ar/" + this.keywordEncoded + "?_q=" + this.keywordEncoded + "&map=ft&page=" + this.currentPage;
      currentDoc = fetchDocument(url);

      if (this.totalProducts == 0) {
         setTotalProducts();
      }

      JSONObject jsonProducts = scrapJson();

      Elements products = currentDoc.select("div.vtex-search-result-3-x-galleryItem.vtex-search-result-3-x-galleryItem--normal.vtex-search-result-3-x-galleryItem--grid.pa4");

      for (Element product : products) {
         String slug = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "a.vtex-product-summary-2-x-clearLink.h-100.flex.flex-column", "href");
         String link = "https://www.perfumeriaspigmento.com.ar" + slug;
         String jsonKey = "Product:" + slug.substring(1, slug.indexOf("/p"));
         String internalPid = jsonProducts.has(jsonKey) ? jsonProducts.optJSONObject(jsonKey).optString("productId") : null;

         String name = CrawlerUtils.scrapStringSimpleInfo(product, "span.vtex-product-summary-2-x-productBrand.vtex-product-summary-2-x-brandName.t-body", true);
         String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "img.vtex-product-summary-2-x-imageNormal.vtex-product-summary-2-x-image", "src");
         Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, "span.vtex-product-price-1-x-currencyInteger.vtex-product-price-1-x-currencyInteger--summary", null, true, ',', session, 0);
         boolean isAvailable = price != 0;

         //New way to send products to save data product
         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(link)
            .setInternalId(internalPid)
            .setName(name)
            .setImageUrl(imageUrl)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .build();

         saveDataProduct(productRanking);
      }
   }

   private JSONObject scrapJson() {
      String result = "";
      Elements els = currentDoc.select("script");

      for (Element el : els) {
         String stringEl = el.toString();

         if (stringEl.contains("__STATE__")) {
            String partialJson = stringEl.substring(stringEl.indexOf("__STATE__"));
            partialJson = partialJson
               .replace("__STATE__ = ", "")
               .replace("</script>", "");
            result = partialJson.trim();
            break;
         }
      }

      return CrawlerUtils.stringToJson(result);
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
