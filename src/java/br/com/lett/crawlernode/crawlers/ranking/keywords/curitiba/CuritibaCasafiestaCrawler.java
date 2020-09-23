package br.com.lett.crawlernode.crawlers.ranking.keywords.curitiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.List;

public class CuritibaCasafiestaCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.casafiesta.com.br/";
   private static final String HOST = "www.casafiesta.com.br/";

   public CuritibaCasafiestaCrawler(Session session){
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      String url = HOME_PAGE + "busca?busca=" + this.keywordEncoded + "&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products =  this.currentDoc.select(".spots-interna .spot");

      if(products.size() >= 1) {
         //se o total de busca não foi setado ainda, chama a função para setar
         if(this.totalProducts == 0) setTotalProducts();
         for(Element e : products) {

            // InternalPid
            String internalPid = crawlInternalPid(e);

            // InternalId
            String internalId = crawlInternalId(e);

            // Url do produto
            String productUrl = CrawlerUtils.scrapUrl(e, ".fbits-spot-conteudo a", "href", "https", HOST);

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

   protected boolean hasNextPage() {
      //se  elemeno page obtiver algum resultado
      //tem próxima página
      return this.arrayProducts.size() < this.totalProducts;
   }

   @Override
   protected void setTotalProducts(){
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".mainBar .bgResultadosCat.fbits-info-top .mostrando span", true, 0);
   }

   private String crawlInternalId(Element e){

      return CommonMethods.getLast(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "id").split("-"));
//      String idProperty = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "id");
//
//      List<String> idSeparated = Arrays.asList(idProperty.split("-"));
//
//      return idSeparated.size() > 0 ? idSeparated.get(idSeparated.size() - 1): null;
   }

   private String crawlInternalPid(Element e){

      String dataSeparated = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".imagem-spot img:first-of-type", "data-original").split(".jpg")[0];
//      String dataProperty = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".imagem-spot img:first-of-type", "data-original");
//
//      List<String> dataSeparated = Arrays.asList(dataProperty.split(".jpg"));
      String internalPid = null;
      if(!dataSeparated.isEmpty()){
         internalPid = CommonMethods.getLast(dataSeparated.split("/"));
//         List<String> firstPart = Arrays.asList(dataSeparated.get(0).split("/"));
//
//         internalPid = firstPart.size() > 0 ? firstPart.get(firstPart.size() - 1).replaceAll("[^0-9]", "") : null;
      }

      return internalPid;
   }

//   private String crawlProductUrl(Element e){
//
//      String nonFormattedUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".fbits-spot-conteudo a", "href");
//      return HOME_PAGE + nonFormattedUrl;
//   }


}
