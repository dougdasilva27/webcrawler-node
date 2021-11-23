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
import models.Offers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilFeiranovaCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.feiranovaemcasa.com.br/";

   public BrasilFeiranovaCrawler(Session session) {
      super(session);
   }

   protected Response fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1lIjoic29saWRjb24iLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9lbWFpbGFkZHJlc3MiOiJzb2xpZGNvbkBzb2xpZGNvbi5jb20uYnIiLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1laWRlbnRpZmllciI6IjM3NTNiYWEzLTVhZGYtNDY0Ni1hNTY5LTIxMmQxMzlhNjdmYyIsImV4cCI6MTYzOTQ5NDAwMCwiaXNzIjoiRG9yc2FsV2ViQVBJIiwiYXVkIjoic29saWRjb24uY29tLmJyIn0.4W4BWzMDXY49nssBpHn5Itdo4z9I6EDWRm9kk-x6n4o");

      String initPayload = "{\"Promocao\":false,\"Comprado\":false,\"Produto\": \""+ this.keywordEncoded +"\",\"Favorito\":false}";

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
            JSONObject product = (JSONObject) e;

            Number internalPidNumber = product.getNumber("cdProduto");
            String internalPid = internalPidNumber.toString();
            String name = JSONUtils.getStringValue(product, "nmProduto");
            String primaryImage = JSONUtils.getStringValue(product, "urlFoto");
            String productUrl = scrapUrl(internalPid);
            Integer price = scrapPrice(product);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(primaryImage)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

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

   private String scrapUrl(String id) {
      String urlFormated = "https://www.feiranovaemcasa.com.br/produtodetalhe/" + id + "/False";

      return urlFormated;
   }

   private Integer scrapPrice(JSONObject product) {
      Double priceKg = JSONUtils.getDoubleValueFromJSON(product, "preco", true);

      if (product.getBoolean("inFracionado") == true) {
         Double priceFraction = JSONUtils.getDoubleValueFromJSON(product,"fracionamento", true);

         priceKg = priceKg * priceFraction;
      }

      Integer price = (int) Math.round((priceKg * 100));

      return price;
   }

   @Override
   protected boolean hasNextPage(){
      return false;
   }
}
