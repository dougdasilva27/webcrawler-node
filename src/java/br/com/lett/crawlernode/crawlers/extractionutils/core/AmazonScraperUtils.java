package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmazonScraperUtils {

   private final Logger logger;
   private final Session session;

   public AmazonScraperUtils(Logger logger, Session session) {
      this.logger = logger;
      this.session = session;
   }

   public List<Cookie> handleCookiesBeforeFetch(String url, List<Cookie> cookies, DataFetcher dataFetcher) {
      Request request = getRequestCookies(url, cookies, dataFetcher);
      return CrawlerUtils.fetchCookiesFromAPage(request, "www.amazon.com.br", "/", null, session, dataFetcher);
   }

   public Request getRequestCookies(String url, List<Cookie> cookies, DataFetcher dataFetcher) {

      Map<String, String> headers = new HashMap<>();
      headers.put("Accept-Encoding", "no");
      headers.put("authority", "www.amazon.com.br");
      headers.put("upgrade-insecure-requests", "1");
      headers.put("service-worker-navigation-preload", "true");
      headers.put("rrt", "200");
      headers.put("cache-control", "max-age=0");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36");

      RequestBuilder request = RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers);

      if (dataFetcher instanceof FetcherDataFetcher) {

         request = request
            .setProxyservice(
               Arrays.asList(
                  ProxyCollection.INFATICA_RESIDENTIAL_BR,
                  ProxyCollection.NETNUT_RESIDENTIAL_BR))
            .mustSendContentEncoding(false)
            .setFetcheroptions(FetcherOptionsBuilder.create()
               .mustRetrieveStatistics(true)
               .setForbiddenCssSelector("#captchacharacters")
               .build());
      } else {

         request.setProxyservice(
            Arrays.asList(
               ProxyCollection.BUY,
               ProxyCollection.INFATICA_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_BR)).setFetcheroptions(FetcherOptionsBuilder.create()
            .mustRetrieveStatistics(true)
            .setForbiddenCssSelector("#captchacharacters").build());
      }

      return request.build();
   }

   public Response fetchProductPageResponse(List<Cookie> cookies, DataFetcher dataFetcher) {
      return fetchResponse(session.getOriginalURL(), new HashMap<>(), cookies, dataFetcher);
   }

   /**
    * Fetch html from amazon
    */
   public String fetchPage(String url, Map<String, String> headers, List<Cookie> cookies, DataFetcher dataFetcher) {
      return fetchResponse(url, headers, cookies, dataFetcher).getBody();
   }

   private Response fetchResponse(String url, Map<String, String> headers, List<Cookie> cookies, DataFetcher dataFetcher) {

      Request requestApache = RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.BUY,
               ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .setFetcheroptions(FetcherOptionsBuilder.create().setForbiddenCssSelector("#captchacharacters").build())
         .build();

      Map<String, String> headersClone = new HashMap<>(headers);
      headersClone.put("Accept-Encoding", "no");

      Request requestFetcher = RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.BUY,
               ProxyCollection.BONANZA_BELGIUM_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .setFetcheroptions(FetcherOptionsBuilder.create()
            .mustRetrieveStatistics(true)
            .setForbiddenCssSelector("#captchacharacters").build())
         .build();

      Request request = dataFetcher instanceof FetcherDataFetcher ? requestFetcher : requestApache;

      Response response = dataFetcher.get(session, request);

      int statusCode = response.getLastStatusCode();

      if ((Integer.toString(statusCode).charAt(0) != '2' &&
         Integer.toString(statusCode).charAt(0) != '3'
         && statusCode != 404)) {

         if (dataFetcher instanceof FetcherDataFetcher) {
            response = new ApacheDataFetcher().get(session, requestApache);
         } else {
            headers.put("Accept-Encoding", "no");
            response = new FetcherDataFetcher().get(session, requestFetcher);
         }
      }

      return response;
   }

   /**
    * @param images   - array present on html
    * @param doc      - html
    * @param host     - host of image url ex: www.amazon.com or www.amazon.com.br or www.amazon.com.mx
    * @param protocol - http or https
    * @return
    */
   public String scrapPrimaryImage(JSONArray images, Document doc, String host, String protocol) {
      String primaryImage = null;

      if (images.length() > 0) {
         JSONObject image = images.getJSONObject(0);

         if (image.has("mainUrl") && !image.isNull("mainUrl")) {
            primaryImage = image.get("mainUrl").toString().trim();
         } else if (image.has("thumbUrl") && !image.isNull("thumbUrl")) {
            primaryImage = image.get("thumbUrl").toString().trim();
         } else if (image.has("hiRes") && !image.isNull("hiRes")) {
            primaryImage = image.get("hiRes").toString().trim();
         } else if (image.has("large") && !image.isNull("large")) {
            primaryImage = image.get("large").toString().trim();
         } else if (image.has("thumb") && !image.isNull("thumb")) {
            primaryImage = image.get("thumb").toString().trim();
         }

      } else {
         Element img = doc.select("#ebooksImageBlockContainer img").first();

         if (img != null) {
            primaryImage = img.attr("src").trim();
         }
      }

      return primaryImage;
   }


   /**
    * @param images   - array present on html
    * @param host     - host of image url ex: www.amazon.com or www.amazon.com.br or www.amazon.com.mx
    * @param protocol - http or https
    * @return
    */
   public String scrapSecondaryImages(JSONArray images, String host, String protocol) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();


      for (int i = 1; i < images.length(); i++) { // first index is the primary Image
         JSONObject imageJson = images.getJSONObject(i);

         String image = null;

         if (imageJson.has("mainUrl") && !imageJson.isNull("mainUrl")) {
            image = imageJson.get("mainUrl").toString().trim();
         } else if (imageJson.has("thumbUrl") && !imageJson.isNull("thumbUrl")) {
            image = imageJson.get("thumbUrl").toString().trim();
         } else if (imageJson.has("hiRes") && !imageJson.isNull("hiRes")) {
            image = imageJson.get("hiRes").toString().trim();
         } else if (imageJson.has("large") && !imageJson.isNull("large")) {
            image = imageJson.get("large").toString().trim();
         } else if (imageJson.has("thumb") && !imageJson.isNull("thumb")) {
            image = imageJson.get("thumb").toString().trim();
         }

         if (image != null) {
            secondaryImagesArray.put(image);
         }

      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   /**
    * Get json of images inside html
    *
    * @param doc
    * @return
    */
   public JSONArray scrapImagesJSONArray(Document doc) {
      JSONArray images = new JSONArray();

      JSONObject data = scrapImagesJson(doc);

      if (data.has("imageGalleryData")) {
         images = data.getJSONArray("imageGalleryData");
      } else if (data.has("colorImages")) {
         JSONObject colorImages = data.getJSONObject("colorImages");

         if (colorImages.has("initial")) {
            images = colorImages.getJSONArray("initial");
         }
      } else if (data.has("initial")) {
         images = data.getJSONArray("initial");
      }

      return images;
   }

   private JSONObject scrapImagesJson(Document doc) {
      JSONObject data = new JSONObject();

      String firstIndex = "vardata=";
      String lastIndex = "};";

      // this keys are to identify images JSON
      String idNormalImages = "imageGalleryData";
      String idColorImages = "colorImages";

      Elements scripts = doc.select("script[type=\"text/javascript\"]");
      for (Element e : scripts) {
         // This json can be broken, we need to remove additional ','
         String script = e.html()
            .replace(" ", "")
            .replaceAll("\n", "")
            .replace(",}", "}")
            .replace(",]", "]");

         if (script.contains(firstIndex) && script.contains(lastIndex) && (script.contains(idColorImages) || script.contains(idNormalImages))) {
            String json = CrawlerUtils.extractSpecificStringFromScript(script, firstIndex, false, lastIndex, false);
            if (json != null && json.trim().startsWith("{") && json.trim().endsWith("}")) {

               try {
                  data = new JSONObject(json.trim());
               } catch (JSONException e1) {
                  Logging.printLogWarn(logger, session, e1.getMessage());

                  // This case we try to scrap initialJsonArray, because the complete json is not valid
                  String initialJson = CrawlerUtils.extractSpecificStringFromScript(json, "initial':", false, "},'", false);
                  if (initialJson != null && initialJson.trim().startsWith("[") && initialJson.trim().endsWith("]")) {
                     try {
                        data = new JSONObject().put("initial", new JSONArray(initialJson.trim()));
                     } catch (JSONException e2) {
                        Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e2));
                     }
                  }

               }
            }

            break;
         }
      }

      return data;
   }
}
