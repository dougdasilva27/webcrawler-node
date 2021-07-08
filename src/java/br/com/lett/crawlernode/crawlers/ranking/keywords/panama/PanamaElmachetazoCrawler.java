package br.com.lett.crawlernode.crawlers.ranking.keywords.panama;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;

public class PanamaElmachetazoCrawler extends CrawlerRankingKeywords {

   public PanamaElmachetazoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {
      this.log("Página " + this.currentPage);

      String url = "https://elmachetazo.com/catalogsearch/result/index/?p="+ this.currentPage+"&q=" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url, cookies);


      Elements products = this.currentDoc.select(".products.list.items.product-items li");
      pageSize = products.size();

      if (products.size() >= 1) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {

            // InternalId
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product.details.product-item-details .price-box","data-product-id");

            // Url do produto
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-info > a","href");

            saveDataProduct(internalId, internalId, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalId + " - Url: " + productUrl);
         }
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }


   @Override
   protected boolean hasNextPage(){
      return this.currentDoc.selectFirst(".item.pages-item-next .action.next") != null;
   }

}

