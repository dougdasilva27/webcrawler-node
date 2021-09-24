package br.com.lett.crawlernode.crawlers.ranking.keywords.manaus;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.models.RankingProducts;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ManausSupernovaeraCrawler extends CrawlerRankingKeywords {

   public ManausSupernovaeraCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   protected final String storeId = getStoreId();

   protected String getStoreId() {
      return session.getOptions().optString("store_id");
   }


   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      // número de produtos por página do market
      this.pageSize = 50;

      this.log("Página " + this.currentPage);

      String url = "https://www.supernovaera.com.br/buscapagina?&ft=" + this.keywordEncoded + "&PS=50&sl=433f5785-6ccd-4cb2-977f-148ecbd02b68&cc=100&sm=0&PageNumber="
         + this.currentPage;

      Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      this.currentDoc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
      this.log("Link onde são feitos os crawlers: " + url);

      Elements products = this.currentDoc.select("ul .mz-product-summary");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".mz-stamps a", "href"), "https", "www.supernovaera.com.br") + "?sc=" + storeId;
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".insert-sku-checkbox", "name");
            String internalPid = null;
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".mz-product-summary__name a", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".embed-responsive.qd_sil_img_wrapper img", "src");
            int price = CrawlerUtils.scrapIntegerFromHtml(e, ".mz-product-summary__best-price", true, 0);
            boolean isAvailable = price != 0;

            //New way to send products to save data product
            RankingProducts productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }

}
