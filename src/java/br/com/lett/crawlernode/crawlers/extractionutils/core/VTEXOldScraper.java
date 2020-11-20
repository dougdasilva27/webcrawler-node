
package br.com.lett.crawlernode.crawlers.extractionutils.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public abstract class VTEXOldScraper extends VTEXScraper {

   public static final String PRODUCT_ID = "productId";
   public static final String IMAGES = "Images";
   public static final String IS_PRINCIPAL_IMAGE = "IsMain";
   public static final String IMAGE_PATH = "Path";

   public VTEXOldScraper(Session session) {
      super(session);
   }

   protected List<String> scrapImagesOldWay(String internalId) {
      List<String> images = new ArrayList<>();
      JSONObject skuApi = scrapSkuApi(internalId);

      if (skuApi.has(IMAGES)) {
         JSONArray jsonArrayImages = skuApi.getJSONArray(IMAGES);

         for (int i = 0; i < jsonArrayImages.length(); i++) {
            JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
            JSONObject jsonImage = arrayImage.getJSONObject(0);

            if (jsonImage.has(IMAGE_PATH)) {
               if (jsonImage.has(IS_PRINCIPAL_IMAGE) && jsonImage.getBoolean(IS_PRINCIPAL_IMAGE)) {
                  images.add(0, changeImageSizeOnURL(jsonImage.getString(IMAGE_PATH)));
               } else {
                  images.add(changeImageSizeOnURL(jsonImage.getString(IMAGE_PATH)));
               }
            }
         }
      }

      return images;
   }

   @Override
   protected String scrapInternalpid(Document doc) {
      JSONObject productJson = crawlSkuJsonVTEX(doc, session);
      String internalPid = null;

      if (productJson.has(PRODUCT_ID)) {
         internalPid = productJson.optString(PRODUCT_ID);
      }

      return internalPid;
   }

   protected String scrapPrimaryImage(JSONObject json) {
      String primaryImage = null;

      if (json.has(IMAGES)) {
         JSONArray jsonArrayImages = json.getJSONArray(IMAGES);

         for (int i = 0; i < jsonArrayImages.length(); i++) {
            JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
            JSONObject jsonImage = arrayImage.getJSONObject(0);

            if (jsonImage.has(IS_PRINCIPAL_IMAGE) && jsonImage.getBoolean(IS_PRINCIPAL_IMAGE) && jsonImage.has(IMAGE_PATH)) {
               primaryImage = changeImageSizeOnURL(jsonImage.getString(IMAGE_PATH));
               break;
            }
         }
      }

      return primaryImage;
   }

   protected String scrapSecondaryImages(JSONObject apiInfo) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      if (apiInfo.has(IMAGES)) {
         JSONArray jsonArrayImages = apiInfo.getJSONArray(IMAGES);

         for (int i = 0; i < jsonArrayImages.length(); i++) {
            JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
            JSONObject jsonImage = arrayImage.getJSONObject(0);

            // jump primary image
            if (jsonImage.has(IS_PRINCIPAL_IMAGE) && jsonImage.getBoolean(IS_PRINCIPAL_IMAGE)) {
               continue;
            }

            if (jsonImage.has(IMAGE_PATH)) {
               String urlImage = changeImageSizeOnURL(jsonImage.getString(IMAGE_PATH));
               secondaryImagesArray.put(urlImage);
            }

         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   /**
    * Get the image url and change it size
    * 
    * @param url
    * @return
    */
   protected String changeImageSizeOnURL(String url) {
      String[] tokens = url.trim().split("/");
      String dimensionImage = tokens[tokens.length - 2]; // to get dimension image and the image id

      String[] tokens2 = dimensionImage.split("-"); // to get the image-id
      String dimensionImageFinal = tokens2[0] + "-1000-1000";

      return url.replace(dimensionImage, dimensionImageFinal); // The image size is changed
   }


   public JSONObject scrapSkuApi(String internalId) {
      String url = homePage + "produto/sku/" + internalId;

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      JSONArray jsonArray = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      if (jsonArray.length() > 0) {
         return jsonArray.getJSONObject(0);
      }

      return new JSONObject();
   }

   /**
    * Crawl skuJson from html in VTEX Sites
    * 
    * @param document
    * @param session
    * @return
    */
   public static JSONObject crawlSkuJsonVTEX(Document document, Session session) {
      Elements scriptTags = document.getElementsByTag("script");
      String scriptVariableName = "var skuJson_0 = ";
      JSONObject skuJson = new JSONObject();
      String skuJsonString = null;

      for (Element tag : scriptTags) {
         for (DataNode node : tag.dataNodes()) {
            if (tag.html().trim().startsWith(scriptVariableName)) {
               skuJsonString = node.getWholeData().split(Pattern.quote(scriptVariableName))[1]
                     + node.getWholeData().split(Pattern.quote(scriptVariableName))[1].split(Pattern.quote("};"))[0];
               break;
            }
         }
      }

      if (skuJsonString != null) {
         try {
            skuJson = new JSONObject(skuJsonString);

         } catch (JSONException e) {
            Logging.printLogWarn(logger, session, "Error creating JSONObject from var skuJson_0");
            Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));
         }
      }

      return skuJson;
   }
}
