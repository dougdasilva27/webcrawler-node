package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.models.RankingProducts;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public abstract class VtexRankingPidPosition extends CrawlerRankingKeywords {

   public VtexRankingPidPosition(Session session) {
      super(session);
   }

   protected final String storeId = getStoreId();
   protected final String homePage = getHomePage();
   protected final String urlParams = getUrlParams();

   protected abstract String getStoreId();

   protected abstract String getHomePage();

   protected abstract String getUrlParams();


   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {

      this.log("Página " + this.currentPage);

      String url = homePage + "buscapagina?" +
         "ft=" + this.keywordEncoded +
         urlParams + "&PageNumber="+ this.currentPage;

      this.currentDoc = fetchDocument(url);
      this.log("Link onde são feitos os crawlers: " + url);

      Elements products = this.currentDoc.select("li[layout]");
      Elements productsIdList = this.currentDoc.select("li[id].helperComplement");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (int index = 0; index < products.size(); index++) {
            Element product = products.get(index);

            String internalPid = crawlInternalPid(productsIdList.get(index));
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".mz-stamps a", "href"), "https", "www.supernovaera.com.br") + "?sc=" + storeId;
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".mz-product-summary__name a", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".embed-responsive.qd_sil_img_wrapper img", "src");
            int price = CrawlerUtils.scrapIntegerFromHtml(product, ".mz-product-summary__best-price", true, 0);
            boolean isAvailable = price != 0;

            //New way to send products to save data product
            RankingProducts productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   private String crawlInternalPid(Element productId) {
      String id = CrawlerUtils.scrapStringSimpleInfoByAttribute(productId, null, "id");
      String[] split = id.split("_");
      return split[1];
   }

   @Override
   protected void setTotalProducts() {
      Document html = fetchDocument(homePage + keywordEncoded);
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(html, ".resultado-busca-numero span.value", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
