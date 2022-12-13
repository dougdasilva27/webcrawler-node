package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.ranking.RankingSession;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilWebcontinentalCrawler extends CrawlerRankingKeywords {

   private final String HOME_PAGE = "https://www.webcontinental.com.br";

   public BrasilWebcontinentalCrawler(Session session) {
      super(session);
   }

   private String categoryId;
   private String categoryUrl;

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      // número de produtos por página do market
      this.pageSize = 21;

      this.log("Página " + this.currentPage);

      String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");

      // monta a url com a keyword e a página
      String url = "https://www.webcontinental.com.br/ccstoreui/v1/search?Ntt=" + keyword + "&No=" + this.arrayProducts.size() + "&Nrpp=21";
      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject resultsAPI = fetchApi(url);

      if (resultsAPI.has("resultsList")) {
         crawlShareWithSearch(resultsAPI);
      } else if (resultsAPI.has("items") && resultsAPI.getJSONArray("items").length() > 0) {
         crawlShareWithCategory(resultsAPI);
      } else {
         this.result = false;
         this.log("Keyword sem resultados!");
      }

      if (session instanceof RankingSession && ((RankingSession) session).mustTakeAScreenshot() && this.currentPage <= 2) {
         String printUrl;

         if (this.categoryUrl == null) {
            printUrl = "https://www.webcontinental.com.br/searchresults?Ntt=ar" + keyword
               + "&Nty=1&No=0&Nrpp=12&Rdm=856&searchType=simple&type=search&page=" + this.currentPage;
         } else {
            printUrl = this.categoryUrl;
         }

      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return this.arrayProducts.size() < this.totalProducts;
   }

   protected void setTotalBusca(JSONObject results) {
      if (results.has("totalNumRecs")) {
         this.totalProducts = results.getInt("totalNumRecs");
      } else if (results.has("totalResults")) {
         this.totalProducts = results.getInt("totalResults");
      }

      this.log("Total da busca: " + this.totalProducts);
   }

   /**
    * Fetch functions
    */

   private JSONObject fetchApi(String url) {
      if (this.currentPage == 1 || this.categoryId == null) {
         JSONObject resultsAPI = fetchJSONObject(url);

         if (resultsAPI.has("resultsList")) {
            return resultsAPI;
         } else if (resultsAPI.has("endeca:redirect")) {
            JSONObject redirect = resultsAPI.getJSONObject("endeca:redirect");

            if (redirect.has("link")) {
               JSONObject link = redirect.getJSONObject("link");

               if (link.has("url")) {
                  String[] tokens = link.getString("url").split("/");
                  String categoryID = tokens[tokens.length - 1].trim();

                  return fetchCategoryApi(categoryID);
               }
            }
         }
      } else if (this.categoryId == null) {
         return fetchJSONObject(url);
      } else {
         return fetchCategoryApi(this.categoryId);
      }

      return new JSONObject();
   }

   private JSONObject fetchCategoryApi(String categoryId) {
      this.categoryUrl = "https://www.webcontinental.com.br/ccstoreui/v1/products?totalResults=true&totalExpandedResults=true"
         + "&catalogId=cloudCatalog&offset=" + this.arrayProducts.size() + "&limit=60&categoryId=" + categoryId + "&includeChildren=true&"
         + "fields=items.displayName%2Citems.brand%2Citems.route%2Citems.childSKUs.listPrice%2Citems.childSKUs.salePrice"
         + "%2Citems.childSKUs.repositoryId%2Citems.repositoryId%2Citems.id%2Citems.primaryFullImageURL%2Citems.primaryImageAltText"
         + "%2Citems.primaryImageTitle%2Citems.primaryLargeImageURL%2Citems.primaryMediumImageURL%2Citems.primarySmallImageURL"
         + "%2Citems.primarySourceImageURL%2Citems.primaryThumbImageURL%2Citems.salePrice%2Citems.listPrice%2Citems.type"
         + "%2Citems.description%2CtotalExpandedResults%2CtotalResults%2Coffset&storePriceListGroupId=realaVista";

      return fetchJSONObject(this.categoryUrl);
   }

   /**
    * Crawl functions
    */

   private void crawlShareWithSearch(JSONObject apiSearch) throws MalformedProductException {
      JSONObject results = apiSearch.getJSONObject("resultsList");

      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0) {
         setTotalBusca(results);
      }

      JSONArray products = new JSONArray();

      if (results.has("records")) {
         products = results.getJSONArray("records");
      }

      for (int i = 0; i < products.length(); i++) {
         JSONObject product = products.getJSONObject(i);

         String internalPid = crawlInternalPid(product);
         String productUrl = crawlProductUrl(product);
         String productName = getProductName(product);
         String imageUrl = getImageUrl(product);
         Integer price = getPrice(product);
         boolean isAvailable = getAvailable(product);
         if(!isAvailable){price = null;}

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalPid(internalPid)
            .setName(productName)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .setImageUrl(imageUrl)
            .build();

         saveDataProduct(productRanking);
         if (this.arrayProducts.size() == productsLimit)
            break;
      }
   }

   private void crawlShareWithCategory(JSONObject apiCategory) {
      this.log("Essa keyword redireciona para uma categoria.");

      // se o total de busca não foi setado ainda, chama a função para setar
      if (this.totalProducts == 0) {
         setTotalBusca(apiCategory);
      }

      JSONArray products = apiCategory.getJSONArray("items");

      for (int i = 0; i < products.length(); i++) {
         JSONObject product = products.getJSONObject(i);

         String internalPid = crawlInternalPidCategory(product);
         String productUrl = crawlProductUrl(product);

         saveDataProduct(null, internalPid, productUrl);

         if (this.arrayProducts.size() == productsLimit) {
            break;
         }
      }
   }

   private boolean getAvailable(JSONObject object) {
      String available;
      JSONArray records = JSONUtils.getValueRecursive(object, "records", JSONArray.class);
      if (records != null && !records.isEmpty()) {
         JSONObject attributes = JSONUtils.getValueRecursive(records, "0.attributes", JSONObject.class);
         if (attributes != null && !attributes.isEmpty()) {
            JSONArray nameArray = JSONUtils.getJSONArrayValue(attributes, "sku.availabilityStatus");
            available = nameArray.getString(0);
            if (available != null && !available.isEmpty() && available.contains("INSTOCK")) {
               return true;
            }
         }
      }
      return false;
   }

   private String getProductName(JSONObject object) {
      String productName = null;
      JSONArray records = JSONUtils.getValueRecursive(object, "records", JSONArray.class);
      if (records != null && !records.isEmpty()) {
         JSONObject attributes = JSONUtils.getValueRecursive(records, "0.attributes", JSONObject.class);
         if (attributes != null && !attributes.isEmpty()) {
            JSONArray nameArray = JSONUtils.getJSONArrayValue(attributes, "product.primaryImageAltText");
            productName = nameArray.getString(0);
         }
      }
      return productName;
   }

   private Integer getPrice(JSONObject object) {
      String priceString;
      Integer price = null;
      JSONArray records = JSONUtils.getValueRecursive(object, "records", JSONArray.class);
      if (records != null && !records.isEmpty()) {
         JSONObject attributes = JSONUtils.getValueRecursive(records, "0.attributes", JSONObject.class);
         if (attributes != null && !attributes.isEmpty()) {
            JSONArray nameArray = JSONUtils.getJSONArrayValue(attributes, "product.salePrice");
            priceString = nameArray.getString(0);
            price = CommonMethods.stringPriceToIntegerPrice(priceString, '.', null);
         }
      }
      return price;
   }

   private String getImageUrl(JSONObject object) {
      String imageUrl = null;
      JSONArray records = JSONUtils.getValueRecursive(object, "records", JSONArray.class);
      if (records != null && !records.isEmpty()) {
         JSONObject attributes = JSONUtils.getValueRecursive(records, "0.attributes", JSONObject.class);
         if (attributes != null && !attributes.isEmpty()) {
            JSONArray nameArray = JSONUtils.getJSONArrayValue(attributes, "product.primaryFullImageURL");
            imageUrl = nameArray.getString(0);
         }
      }
      return HOME_PAGE + imageUrl;
   }

   private String crawlInternalPid(JSONObject product) {
      String internalPid = null;

      if (product.has("attributes")) {
         JSONObject attributes = product.getJSONObject("attributes");

         if (attributes.has("product.repositoryId")) {
            JSONArray id = attributes.getJSONArray("product.repositoryId");

            if (id.length() > 0) {
               internalPid = id.getString(0);
            }
         }
      }

      return internalPid;
   }

   private String crawlInternalPidCategory(JSONObject product) {
      String internalPid = null;

      if (product.has("id")) {
         internalPid = product.get("id").toString();
      }

      return internalPid;
   }

   private String crawlProductUrl(JSONObject product) {
      String productUrl = null;

      if (product.has("records")) {
         JSONArray records = product.getJSONArray("records");

         if (records.length() > 0) {
            JSONObject productInfo = records.getJSONObject(0);

            if (productInfo.has("attributes")) {
               JSONObject attributes = productInfo.getJSONObject("attributes");

               if (attributes.has("product.route")) {
                  JSONArray urls = attributes.getJSONArray("product.route");

                  if (urls.length() > 0) {
                     productUrl = urls.getString(0);

                     if (!productUrl.contains("webcontinental")) {
                        productUrl = ("https://www.webcontinental.com.br/" + productUrl).replace(".br//", ".br/");
                     }
                  }
               }
            }
         }
      }

      return productUrl;
   }
}
