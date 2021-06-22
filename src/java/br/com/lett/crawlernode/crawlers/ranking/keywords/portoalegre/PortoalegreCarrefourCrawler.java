package br.com.lett.crawlernode.crawlers.ranking.keywords.portoalegre;

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
public class PortoalegreCarrefourCrawler extends BrasilCarrefourCrawler {

   public PortoalegreCarrefourCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = br.com.lett.crawlernode.crawlers.corecontent.portoalegre.PortoalegreCarrefourCrawler.HOME_PAGE;
   public static final String CEP = br.com.lett.crawlernode.crawlers.corecontent.portoalegre.PortoalegreCarrefourCrawler.CEP;
   public static final String LOCATION_TOKEN = br.com.lett.crawlernode.crawlers.corecontent.portoalegre.PortoalegreCarrefourCrawler.LOCATION_TOKEN;
   public static final String REGION_ID = "U1cjY2FycmVmb3VyYnI5NzY=";

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getLocation() {
      return LOCATION_TOKEN;
   }

   @Override
   protected String getCep() {
      return CEP;
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
