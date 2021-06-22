package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.brasil.BrasilCarrefourCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 18/04/2020
 * 
 * @author Fabrício
 *
 */
public class SaopauloCarrefourbutantaCrawler extends BrasilCarrefourCrawler {

   public SaopauloCarrefourbutantaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";
   public static final String LOCATION = "05512-300";
   public static final String REGION_ID = "U1cjY2FycmVmb3VyYnI5NDU=";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLocation() {
      return LOCATION;
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      String homePage = getHomePage();
      String url = buildUrl(homePage);

      String body = fetchPage(url);
      JSONObject json = CrawlerUtils.stringToJson(body);

      JSONArray products = JSONUtils.getValueRecursive(json, "data.vtex.productSearch.products", JSONArray.class);

      if (products.length() > 0) {
         for (Object object : products) {

            JSONObject product = (JSONObject) object;
            String internalPid = product.optString("id");
            String productUrl = homePage + product.optString("linkText") + "/p?skuId=" + internalPid;

            saveDataProduct(null, internalPid, productUrl);
            this.log("Position: " + this.position + " - InternalPid: " + internalPid + " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }
   }

   protected String buildUrl(String homepage) {
      String url = homepage + "graphql/?operationName=SearchQuery&extensions=%7B%22persistedQuery%22%3A%7B%22sha256Hash%22%3A%229cf99aaf600194530c192667b4126b8b023405be3fa94cb667092595aef77d6f%22%7D%7D&variables=";

      String jsonVariables = "{\"fullText\":\"" + this.keywordWithoutAccents + "\",\"selectedFacets\":[{\"key\":\"region-id\",\"value\":\"" + REGION_ID + "\"}],\"orderBy\":\"\",\"from\":0,\"to\":9}";

      String encodedJson = "\"" + Base64.getEncoder().encodeToString(jsonVariables.getBytes(StandardCharsets.UTF_8)) + "\"";

      try{
         url += URLEncoder.encode(encodedJson, "UTF-8");
      }catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }

      return url;
   }
}
