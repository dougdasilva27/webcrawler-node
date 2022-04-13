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

import java.io.UnsupportedEncodingException;
import java.util.Collections;

public class CidadeCancaoCrawlerRanking extends CrawlerRankingKeywords {
   public CidadeCancaoCrawlerRanking(Session session) {
      super(session);
   }

   private final String store_id = session.getOptions().optString("storeId", "toledo");

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("storeSelected", "https://" + store_id + ".cidadecancao.com");
      cookie.setDomain(store_id + ".cidadecancao.com");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = "https://"+store_id+".cidadecancao.com/catalogsearch/result/?q="+this.keywordEncoded+"&p=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url, this.cookies);

      Elements products = this.currentDoc.select(".products-grid li");
      if (!products.isEmpty()) {
         for (Element product : products) {
            String internalPid = String.valueOf(CrawlerUtils.scrapIntegerFromHtmlAttr(product, ".containerTag > span", "id", null));
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-image", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".product-name", false);
            String imgUrl = scrapImage(product);
            Integer price = scrapPrice(product);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
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

   private String scrapImage(Element product) {
      String image = CrawlerUtils.scrapSimplePrimaryImage(product, ".product-image img", Collections.singletonList("src"), "https", store_id + ".cidadecancao.com");

      if(image != null && image.contains("/small_image/265x235/")) {
         image = image.replace("/small_image/265x235/", "/image/855x635/");
      }

      return image;
   }

   private Integer scrapPrice(Element product) {
      Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".price-box .special-price .price", null, false, ',', session, null);
      if (price == null) {
         price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".price-box .regular-price .price", null, false, ',', session, null);
      }
      return price;
   }

   @Override
   public boolean hasNextPage() {
      return  true;
   }
}
