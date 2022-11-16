package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class LifeappsCrawlerRanking extends CrawlerRankingKeywords {

   private static final String API_URL = "https://superon.lifeapps.com.br/api/v2/app";

   public LifeappsCrawlerRanking(Session session) {
      super(session);
   }

   protected abstract String getCompanyId();

   protected abstract String getApiHash();

   protected abstract String getFormaDePagamento();

   protected abstract String getHomePage();

   protected JSONArray search = new JSONArray();

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
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
               String name = JSONUtils.getValueRecursive(product, "nome", String.class);
               String image = crawlImage(product);
               Double priceDouble = JSONUtils.getValueRecursive(product, "preco_original", Double.class, null);
               Integer priceInCents = null;
               if (priceDouble != null) {
                  priceInCents = CommonMethods.doublePriceToIntegerPrice(priceDouble, null);
               }
               boolean available = priceInCents != null;

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

   private String crawlImage(JSONObject product) {
      String imageId = JSONUtils.getValueRecursive(product, "idcadastroextraproduto", String.class);
      if (imageId != null) {
         return "https://s3-sa-east-1.amazonaws.com/prod-superon-public-media/shared/product-image/" + imageId + ".jpg";
      }
      return null;
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
         .concat("/listaprodutos/74c326ab-a4f0-4431-9682-e38678118ae3")
         .concat("?fornec=")
         .concat(getCompanyId())
         .concat("&sk=")
         .concat(this.keywordEncoded)
         .concat("&page=")
         .concat(Integer.toString(this.currentPage - 1))
         .concat("&formapagamento=")
         .concat(getFormaDePagamento())
         .concat("&ordenacao=Relev%C3%A2ncia+na+pesquisa&canalVenda=WEB&tipoentrega=DELIVERY");

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
