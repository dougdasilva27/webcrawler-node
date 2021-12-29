package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Collections;

public class BrasilAnhangueraFerramentas extends CrawlerRankingKeywords {
   public BrasilAnhangueraFerramentas(Session session) {
      super(session);
   }

   private final String HOME_PAGE = "https://www.anhangueraferramentas.com.br";

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 24;

      this.log("Página : " + this.currentPage);

      String url = "https://www.anhangueraferramentas.com.br/busca?busca=" + keywordEncoded + "&pagina=" + this.currentPage;
      this.log("URL : " + url);
      this.currentDoc = fetchDocument(url);

      JSONArray products = getProductsFromPage(currentDoc);

      if(!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for(Object product : products) {
            if(product instanceof JSONObject) {
               JSONObject productJSON = (JSONObject) product;
               String internalPid = productJSON.optString("ProdutoId");
               String productUrl = HOME_PAGE + productJSON.optString("Link");
               String name = productJSON.optString("Nome");
               String imgUrl = getProductImageFromPid(currentDoc, internalPid);
               Integer price = (int) (productJSON.optDouble("PrecoPor") * 100);
               boolean isAvailable = productJSON.optInt("Disponivel") == 1;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalPid)
                  .setInternalPid(internalPid)
                  .setImageUrl(imgUrl)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(productRanking);
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultados!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   private String getProductImageFromPid(Document doc, String internalPid) {
      return CrawlerUtils.scrapSimplePrimaryImage(doc, "#produto-spot-imagem-" + internalPid + "-1", Collections.singletonList("data-original"), "https", "anhangueraferramentas.fbitsstatic.net");
   }

   private JSONArray getProductsFromPage(Document doc) {
      String productsScript = doc.selectFirst("script:containsData([{\"ProdutoId\":)").data();
      String productsJSON = CrawlerUtils.extractSpecificStringFromScript(productsScript, "[{\"ProdutoId\"", false, ";", true);
      return JSONUtils.stringToJsonArray("[{\"ProdutoId\"" + productsJSON);
   }

   @Override
   protected void setTotalProducts(){
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".fbits-qtd-produtos-pagina", true, 0);
   }
}
