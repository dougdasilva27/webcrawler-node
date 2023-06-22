package br.com.lett.crawlernode.crawlers.extractionutils.ranking;


import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FerreiracostaCrawler extends CrawlerRankingKeywords {

   public FerreiracostaCrawler(Session session) {
      super(session);
   }

   protected JSONObject fetchDocument() {
      String location = session.getOptions().optString("location");
      String region = session.getOptions().optString("region");
      String url = "https://fcxlabs-ecommerce-api.ferreiracosta.com/catalog/v1/products?SearchByTerm=" + this.keywordEncoded + "&SortBy=%20&Pagesize=1000";

      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .header("cookie", "ASP.NET_SessionId=afdhrtmd1chvych4b1y1zqmv;" +
               " osVisitor=b44b973b-4257-4e0b-a2ca-47e37d439251;" +
               " osVisit=f29d817f-51ed-4082-8dd0-7ae9b309c837;" +
               " eco_ce=; ecoFC=FCB02C35F253FCE56D9696DC7757BC6E;" +
               " _gid=GA1.2.1245936171.1631798355;" +
               " _gat_UA-48783684-2=1;" +
               " _gat_gtag_UA_48783684_2=1;" +
               " G_ENABLED_IDPS=google;" +
               " _fbp=fb.1.1631798354801.1640176812;" +
               " _pin_unauth=dWlkPU9XVTBZMlExTlRRdE5UZ3pNeTAwTWprd0xXSTNNMkV0TVRWaE5XVTJZVE0yTkRZMw;" +
               " _hjid=fdb9c70b-3929-430b-8698-0a11e9020213;" +
               " _hjIncludedInSessionSample=1;" +
               " _hjAbsoluteSessionInProgress=0;" +
               "@FC:Ecom:Dropdown:BranchLocation=" + location +
               ";@FC:Ecom:Dropdown:Region=" + region +
               " _ga=GA1.2.1447234012.1631798354;" +
               " _ga_DD7Y69210P=GS1.1.1631798009.3.1.1631798369.0;" +
               " cto_bundle=QnJvIl9STm1tMVN2ZVhydXYxSXhnSGJFbEV1ak1kT1VRYXlGRnIyUldQbm4lMkZhM0hFVWNCYXM4JTJCUDRJUFklMkIzJTJGblglMkJaSCUyQkU5QkUlMkYweWVVNUU5bkYxWkV0bXdzYnZEOWxxV2xEdjFZMDIlMkZvSTVWTnRueGVKZDZxT3dRQ05SbnQlMkJ0cWdvYnZac1pBSSUyRkZtenpzVzA0RFRQSiUyQmZBJTNEJTNE;" +
               " AWSALB=AWQren+oPEAFGXDRiJutL5+sy0hpn5zZBAoiHwI5wCsthQh1UcN4sz5hYfT2hEfrlKuY45Vz5J0qEsHDS9JBbMDsPqDb7l12m63zMvEokIrgKyyLHS5mFcV8YyT+;" +
               " AWSALBCORS=AWQren+oPEAFGXDRiJutL5+sy0hpn5zZBAoiHwI5wCsthQh1UcN4sz5hYfT2hEfrlKuY45Vz5J0qEsHDS9JBbMDsPqDb7l12m63zMvEokIrgKyyLHS5mFcV8YyT+;" +
               " RT=s=1631798388140&r=https%3A%2F%2Fwww.ferreiracosta.com%2FProduto%2F408846%2Flavadora-de-roupa-brastemp-12kg-branca-127v-bwk12abana")
            .build();

         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         Response resp = new Response.ResponseBuilder()
            .setBody(response.body())
            .setLastStatusCode(response.statusCode())
            .build();
         return JSONUtils.stringToJson(resp.getBody());
      } catch (IOException | InterruptedException e) {
         throw new RuntimeException("Failed to load document: " + url, e);
      }
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      JSONObject json = fetchDocument();

      if (json != null && !json.isEmpty()) {
         JSONArray productsArray = json.getJSONArray("products");
         this.totalProducts = productsArray.length();
         for (Object product : productsArray) {
            JSONObject productJson = (JSONObject) product;

            if (productJson != null) {
               JSONObject jsonProduct = (JSONObject) product;
               String internalId = jsonProduct.optString("id");
               String internalPid = internalId;
               String name = jsonProduct.optString("description");
               String productUrl = "https://www.ferreiracosta.com/produto/" + internalId;
               String imgUrl = getImage(jsonProduct);
               JSONObject pricesInfo = JSONUtils.getValueRecursive(jsonProduct, "prices.0", JSONObject.class, new JSONObject());
               Integer price = getPrice(pricesInfo);
               boolean isAvailable = !pricesInfo.isNull("priceList");

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setImageUrl(imgUrl)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(productRanking);
               if (this.arrayProducts.size() == productsLimit)
                  break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private Integer getPrice(JSONObject pricesInfo) {
      Boolean hasDiscount = !pricesInfo.isNull("spotPrice");
      Integer price = hasDiscount ? pricesInfo.optInt("spotPrice") : pricesInfo.optInt("priceList");
      if (!hasDiscount && pricesInfo.optDouble("priceList")!=pricesInfo.optDouble("salePrice")){
         price = pricesInfo.optInt("salePrice");
      }
      return price;
   }

   private String getImage(JSONObject jsonProduct) {
      JSONArray mediaLinks = jsonProduct.optJSONArray("mediaLinks");
      for (int i = 0; i < mediaLinks.length(); i++) {
         JSONObject mediaLink = mediaLinks.optJSONObject(i);
         if (mediaLink.optString("linkType").equals("IMAGEM")) {
            return mediaLink.optString("imageUrl");
         }
      }
      return "";
   }

}
