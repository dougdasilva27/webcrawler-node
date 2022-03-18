package br.com.lett.crawlernode.crawlers.ranking.keywords.curitiba;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
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
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

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

            String internalPid = crawlInternalPid(e);
            String internalId = crawlInternalId(e);
            String productUrl = CrawlerUtils.scrapUrl(e, ".fbits-spot-conteudo a", "href", "https", HOST);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".spotTitle", true);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".fbits-valor", null, true, ',', session,null);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".jsImgSpot.imagem-primaria", Arrays.asList("src"), "https", "casafiesta.fbitsstatic.net");
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setImageUrl(imageUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

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

   }

   private String crawlInternalPid(Element e){

      String dataSeparated = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".imagem-spot img:first-of-type", "data-original").split(".jpg")[0];

      String internalPid = null;
      if(!dataSeparated.isEmpty()){
         internalPid = CommonMethods.getLast(dataSeparated.split("/"));

      }

      return internalPid;
   }



}
