package br.com.lett.crawlernode.crawlers.extractionutils.ranking;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public abstract class SupernicoliniCrawler extends CrawlerRankingKeywords {

   public SupernicoliniCrawler(Session session){ super(session); }
   protected abstract String getHomepage();

   @Override
   protected void extractProductsFromCurrentPage() {
      //número de produtos por página do market
      this.pageSize = 24;

      this.log("Página "+ this.currentPage);

      //monta a url com a keyword e a página
      String url = getHomepage() + "page/" + this.currentPage + "/?s=" + this.keywordEncoded + "&post_type=product";

      this.log("Link onde são feitos os crawlers: "+url);

      //chama função de pegar a url
      this.currentDoc = fetchDocument(url);
      Elements products =  this.currentDoc.select(".products .product-small.box");

      //se obter 1 ou mais links de produtos e essa página tiver resultado faça:
      if(products.size() >= 1) {
         //se o total de busca não foi setado ainda, chama a função para setar
         if(this.totalProducts == 0) setTotalProducts();
         for(Element e : products) {

            // InternalPid
            String internalPid = null;

            // InternalId
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".button.ajax_add_to_cart.add_to_cart_button", "data-product_id");

            // Url do produto
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".image-fade_in_back a", "href");

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if(this.arrayProducts.size() == productsLimit) break;

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {

      return this.arrayProducts.size() < this.totalProducts;
   }

   @Override
   protected void setTotalProducts()	{

      String allText = CrawlerUtils.scrapStringSimpleInfo(currentDoc,".woocommerce-result-count", false );

      if (allText != null && allText.contains("de")){
          String totalString = allText.split("de")[1];
          this.totalProducts = MathUtils.parseInt(totalString.replaceAll("[^0-9.]", "").trim());

      } else {
         this.totalProducts = MathUtils.parseInt(allText);
      }

   }

}
