package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import com.google.common.collect.Maps;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;
import java.util.Map;

import static br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;

public class ArgentinaCotoCrawler extends CrawlerRankingKeywords {
   public ArgentinaCotoCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 72;
      this.log("Página " + this.currentPage);
      String url = getPageUrl();
      this.log("URL : " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("#products > li");
      pageSize = products.size();

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String internalPid = crawlInternalPid(e);
            String internalId = crawlInternalId(e);
            String productUrl = "https://www.cotodigital3.com.ar" + e.select("a").attr("href").replaceAll("(\\r|\\n)", "");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".descrip_full", true);
            String image = CrawlerUtils.scrapSimplePrimaryImage(e, ".atg_store_productImage img", Collections.singletonList("src"), "https", "www.cotodigital3.com.ar");
            Integer priceInCents = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".atg_store_productPrice", null, false, ',', session, 0);
            boolean available = priceInCents != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(available)
               .build();

            saveDataProduct(productRanking);
         }
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String getPageUrl() {
      int productsShow = this.currentPage == 1 ? 0 : this.pageSize * (this.currentPage - 1);
      return "https://www.cotodigital3.com.ar/sitios/cdigi/browse?Dy=1&Nf=product.startDate%7CLTEQ+1.640304E12%7C%7Cproduct.endDate%7CGTEQ+1.640304E12&No=" + productsShow + "&Nr=AND%28product.language%3Aespa%C3%B1ol%2Cproduct.sDisp_200%3A1004%2Cproduct.siteId%3ACotoDigital%2COR%28product.siteId%3ACotoDigital%29%29&Nrpp=72&Ntt="+ this.keywordEncoded +"&Nty=1&_D%3AidSucursal=+&_D%3AsiteScope=+&atg_store_searchInput=" + this.keywordEncoded + "&idSucursal=200&siteScope=ok";
   }

   @Override
   protected boolean hasNextPage() {
      if(this.totalProducts == 0 && this.currentPage == 1 ) return true;

      return this.totalProducts > 0 && this.currentPage * this.pageSize < this.totalProducts;
   }

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.select("#resultsCount").first();

      if (totalElement != null) {
         try {
            this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
         } catch (Exception e) {
            this.logError(CommonMethods.getStackTrace(e));
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalId(Element e) {
      String internalId = null;

      String url = e.attr("href");
      if (url.contains("/p/")) {
         String[] tokens = url.split("/");
         internalId = tokens[tokens.length - 1].replaceAll("[^0-9]", "");
      }

      return internalId;
   }

   private String crawlInternalPid(Element e) {
      return e.attr("id").replaceAll("[^0-9]", "").trim();
   }
}
