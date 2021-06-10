package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class LifeappsCrawlerRanking extends CrawlerRankingKeywords {

   private static final String API_URL = "https://superon.lifeapps.com.br/api/v1/app";

   public LifeappsCrawlerRanking(Session session) {
      super(session);
   }

   protected abstract String getCompanyId();

   protected abstract String getApiHash();

   protected abstract String getFormaDePagamento();

   protected abstract String getHomePage();

   protected JSONArray search = new JSONArray();

   @Override
   public void extractProductsFromCurrentPage() {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      search = fetchProductsFromAPI();

      if (search.length() > 0) {

         for (int i = 0; i < search.length(); i++) {
            JSONObject product = search.optJSONObject(i);

            if (product != null && !product.isEmpty()) {
               String internalPid = product.optString("idcadastroextraproduto");
               String internalId = crawlInternalId(product);
               String productUrl = crawlProductUrl(product);

               saveDataProduct(internalId, internalPid, productUrl);

               this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);

               if (this.arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   @Override
   protected boolean hasNextPage() {
      return search.length() >= this.pageSize;
   }

   protected String crawlInternalId(JSONObject json) {
      String internalId = null;
      String idProdutoErp = json.optString("id_produto_erp");

      if (idProdutoErp.contains("|")) {
         String[] idProdutoErpArray = idProdutoErp.split("\\|");
         internalId = idProdutoErpArray[1].concat("-").concat(idProdutoErpArray[0]);
      }

      return internalId;
   }

   protected String crawlProductUrl(JSONObject product) {
      String slug = product.optString("slug", "");

      return getHomePage()
         .concat("commerce")
         .concat("/")
         .concat(getCompanyId())
         .concat("/")
         .concat("produto")
         .concat("/")
         .concat(slug);
   }

   protected JSONArray fetchProductsFromAPI() {
      JSONArray products = new JSONArray();
      String url = API_URL
         .concat("/")
         .concat(getApiHash())
         .concat("/")
         .concat("listaprodutosf")
         .concat("/")
         .concat(getCompanyId())
         .concat("?sk=")
         .concat(this.keywordEncoded)
         .concat("&page=")
         .concat(Integer.toString(this.currentPage - 1))
         .concat("&formapagamento=")
         .concat(getFormaDePagamento())
         .concat("&canalVenda=WEB");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .mustSendContentEncoding(false)
         .build();

      JSONObject apiResponse = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (apiResponse != null && !apiResponse.isEmpty()) {
         products = apiResponse.optJSONArray("dados");
      }

      return products;
   }
}
