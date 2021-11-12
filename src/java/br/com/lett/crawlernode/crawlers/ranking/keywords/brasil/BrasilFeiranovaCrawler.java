package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BrasilFeiranovaCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.feiranovaemcasa.com.br/";

   public BrasilFeiranovaCrawler(Session session) {
      super(session);
   }

   private Response validateToken() {
      String initPayload = "{\n" +
         "    \"Token\": null,\n" +
         "    \"CdCliente\": null,\n" +
         "    \"CdEmpresa\": 113,\n" +
         "    \"Nome\": null,\n" +
         "    \"Senha\": \"7C4A8D09CA3762AF61E59520943DC26494F8941B\",\n" +
         "    \"Email\": \"ttatianeisabelfigueiredo@atiara.com.br\",\n" +
         "    \"Cpf\": null,\n" +
         "    \"inCNPJ\": false,\n" +
         "    \"LiberaDescontos\": false,\n" +
         "    \"LiberaPrDesconto\": null,\n" +
         "    \"LiberaValidadeDias\": null\n" +
         "}";

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("authorization", "Bearer");

      Request request = Request.RequestBuilder.create().setUrl("https://ecom.solidcon.com.br/api/crm/login/")
         .setPayload(initPayload)
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .build();

      Response response = this.dataFetcher.post(session, request);

      return response;
   }

   protected Response fetch() {
      Map<String, String> headers = new HashMap<>();
      JSONObject tokenJson = JSONUtils.stringToJson(validateToken().getBody());
      headers.put("content-type", "application/json");
      headers.put("authorization", "Bearer " + tokenJson.getString("token"));

      String initPayload = "{\"Promocao\":false,\"Comprado\":false,\"Produto\":\"café\",\"Favorito\":false}";

      Request request = Request.RequestBuilder.create().setUrl("https://ecom.solidcon.com.br/api/v2/shop/produto/empresa/113/filial/329/GetProdutos")
         .setPayload(initPayload)
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .build();

      Response response = this.dataFetcher.post(session, request);

      return response;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 1;
      this.log("Página " + this.currentPage);

      JSONArray products = JSONUtils.stringToJsonArray(fetch().getBody());

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = products.length();
         }
         for (Object e : products) {

//            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.info-produto > input", "value");
//            String productUrl = HOME_PAGE + CrawlerUtils.scrapStringSimpleInfoByAttribute(e,"a.content-produto", "href");
//            String name = CrawlerUtils.scrapStringSimpleInfo(e,"span.nome",true);
//            String imgUrl = scrapUrl(e);
//            int price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "div.valor-principal", null, false, ',', session, 0);
//            boolean isAvailable = price != 0;

            //New way to send products to save data product
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl("productUrl")
               .setInternalPid("internalPid")
               .setName("name")
               .setImageUrl("imgUrl")
               .setPriceInCents(1)
               .setAvailability(true)
               .build();

            saveDataProduct(productRanking);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + null +
                  " - InternalPid: " + "internalPid" +
                  " - Url: " + "productUrl");

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

   private String scrapUrl(Element doc) {
      String url = HOME_PAGE;
      String slug = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "img[onerror]", "urlreal");

      if (slug != null) {
         slug = slug.replace("../", "");
         url += slug;
      }

      return url;
   }

   @Override
   protected boolean hasNextPage(){
      return false;
   }
}
