package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public abstract class RomaniaBringoCrawler extends CrawlerRankingKeywords {

   private final String searchPage = "https://www.bringo.ro/ro/search/"+ formatSeller(getMainSeller()) + "?criteria%5Bsearch%5D%5Bvalue%5D=";
   int totalPages;
   public RomaniaBringoCrawler(Session session) {
      super(session);
   }

   protected abstract String getMainSeller();

   private String formatSeller(String seller){
      return seller.toLowerCase().replace(" ", "_");
   }

   private Document fetch(){

      String url = searchPage + this.keywordEncoded + "&page=" + this.currentPage;

      Request request = Request.RequestBuilder.create().setUrl(url).build();

      String response = this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(response);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.log("Página " + this.currentPage);
      this.currentDoc = fetch();

      Elements products = this.currentDoc.select(".col-product-listing-box");

      if(products.size() >= 1){

         if(this.totalPages == 0){
            setTotalPages();
         }

         for (Element product : products) {

            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-listing-quantity button.decrement-quantity", "data-product_id");

            String internalId = scrapInternalId(product);

            String nonFormattedUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".top-product-listing-box a", "href");

            String host = "www.bringo.ro";
            String urlProduct = CrawlerUtils.completeUrl(nonFormattedUrl, "https", host);

            saveDataProduct(internalId, internalPid, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
            if (this.arrayProducts.size() == productsLimit) break;

         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

   }

   @Override
   protected boolean hasNextPage(){
      return this.currentPage < this.totalPages;
   }

   private void setTotalPages(){

      Elements li = this.currentDoc.select("li.page-item:not(.next)");
      if(!li.isEmpty()){
         this.totalPages = CrawlerUtils.scrapIntegerFromHtml(li.last(), "a", true, 1);
      }
   }

   private String scrapInternalId(Element product){

      String infoProduct = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".addToCartForm", "action");

      String internalId = null;

      if(infoProduct != null){

         String[] arrayInfo = infoProduct.split("/");

         internalId = CommonMethods.getLast(arrayInfo);
      }

      return internalId;
   }
}
