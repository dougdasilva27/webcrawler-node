package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class MexicoLacomerCrawler extends CrawlerRankingKeywords {

   public MexicoLacomerCrawler(Session session) {
      super(session);
   }

   private final String succId = session.getOptions().optString("succId");

   @Override
   protected void extractProductsFromCurrentPage() {
      // número de produtos por página do market
      this.pageSize = 30;

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      // primeira página começa em 0 e assim vai.
      String url = "https://lacomer.buscador.amarello.com.mx/searchArtPrior?col=lacomer_2&npagel=20&p=" + this.currentPage + "&pasilloId=false&s=" + this.keywordEncoded + "&succId=" + succId;

      this.log("Link onde são feitos os crawlers: " + url);

      // chama função de pegar a url
      JSONObject search = fetchJSONObject(url);

      // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
      if (search.has("res") && search.getJSONArray("res").length() > 0) {
         JSONArray products = search.getJSONArray("res");

         // se o total de busca não foi setado ainda, chama a função para setar
         if (this.totalProducts == 0) {
            setTotalBusca(search);
         }

         for (int i = 0; i < products.length(); i++) {

            JSONObject product = products.getJSONObject(i);

            // InternalPid
            String internalPid = crawlInternalPid();

            // InternalId
            String internalId = crawlInternalId(product);

            // Url do produto
            String productUrl = crawlProductUrl(product);

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
                  + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
            + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return this.arrayProducts.size() < this.totalProducts;
   }

   protected void setTotalBusca(JSONObject search) {
      if (search.has("total")) {

         this.totalProducts = search.getInt("total");


         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalId(JSONObject product) {
      String internalId = null;

      if (product.has("artCod")) {
         internalId = String.valueOf(product.getInt("artCod"));
      }

      return internalId;
   }

   private String crawlInternalPid() {
      return null;
   }

   private String crawlProductUrl(JSONObject product) {
      String ean = product.optString("artEan");
      String gruid = product.optString("agruId");
      return "https://www.lacomer.com.mx/lacomer/#!/detarticulo/"+ean+"/0/"+gruid+"/1///"+gruid+"?succId="+succId+"&succFmt=100";
   }
}
