package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.Collections;

import static br.com.lett.crawlernode.util.CrawlerUtils.getRedirectedUrl;

public class ArgentinaCotoCrawler extends CrawlerRankingKeywords {
   private String idSucursal = this.session.getOptions().optString("idSucursal", "200");


   public ArgentinaCotoCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   @Override
   protected Document fetchDocument(String url) {
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .build();
      Response response = dataFetcher.get(session, request);
      Document doc = Jsoup.parse(response.getBody());
      String redirectedUrl = getRedirectedUrl(url, session);

      if (doc.select("#products > li").size() == 0 && redirectedUrl != null) {
         int productsShow = this.currentPage == 1 ? 0 : this.pageSize * (this.currentPage - 1);
         request = Request.RequestBuilder.create()
            .setUrl(redirectedUrl + "?Nf=product.startDate%7CLTEQ+1.6723584E12%7C%7Cproduct.endDate%7CGTEQ+1.6723584E12&No=" + productsShow + "&Nr=AND%28product.sDisp_200%3A1004%2Cproduct.language%3Aespa%C3%B1ol%2COR%28product.siteId%3ACotoDigital%29%29&Nrpp=72")
            .build();
         response = dataFetcher.get(session, request);
         doc = Jsoup.parse(response.getBody());
      }

      return doc;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
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
      } else {
         this.log("Não foram encontrados produtos para a página " + this.currentPage);
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String getPageUrl() {
      int numberPage = currentPage - 1;
      int pagination = this.pageSize * numberPage;
      String url = "https://www.cotodigital3.com.ar" + getUrlKeyword();
      url += "?No=" + pagination;
      if (!url.contains(this.keywordEncoded)) {
         url += "&Ntt=" + this.keywordEncoded + "&atg_store_searchInput=" + this.keywordEncoded;
      }
      url += "&Nr=AND%28product.language%3Aespa%C3%B1ol%2Cproduct.sDisp_" + this.idSucursal + "%3A1004%2COR%28product.siteId%3ACotoDigital%29%29";
      url += "&Nrpp=" + this.pageSize;
      return url;
   }

   private String getUrlKeyword() {
      Document doc = currentDoc();
      String content = doc.select("meta[name=DC.identifier][scheme=DCTERMS.URI]").first().attr("content");
      return content;
   }

   private Document currentDoc() {
      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.cotodigital3.com.ar/sitios/cdigi/browse?Ntt=" + this.keywordEncoded)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .setFollowRedirects(true)
         .build();
      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), true);
      return Jsoup.parse(response.getBody());
   }

   @Override
   protected boolean hasNextPage() {
      if (this.totalProducts == 0 && this.currentPage == 1) return true;

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
