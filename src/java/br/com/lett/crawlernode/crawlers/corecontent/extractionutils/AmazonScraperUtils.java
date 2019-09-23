package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class AmazonScraperUtils {

  private Logger logger;
  private Session session;

  public AmazonScraperUtils(Logger logger, Session session) {
    this.logger = logger;
    this.session = session;
  }

  public List<Cookie> handleCookiesBeforeFetch(String url, List<Cookie> cookies, DataFetcher dataFetcher) {
    Request request;

    if (dataFetcher instanceof FetcherDataFetcher) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept-Encoding", "no");

      request = RequestBuilder.create().setUrl(url)
          .setCookies(cookies)
          .setHeaders(headers)
          .setProxyservice(
              Arrays.asList(
                  ProxyCollection.STORM_RESIDENTIAL_EU,
                  ProxyCollection.STORM_RESIDENTIAL_US))
          .mustSendContentEncoding(false)
          .setFetcheroptions(FetcherOptionsBuilder.create().setForbiddenCssSelector("#captchacharacters").build())
          .build();
    } else {
      request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    }

    return CrawlerUtils.fetchCookiesFromAPage(request, "www.amazon.com.br", "/", null, session, dataFetcher);
  }

  public Document fetchProductPage(List<Cookie> cookies, DataFetcher dataFetcher) {
    Document doc = Jsoup.parse(fetchPage(session.getOriginalURL(), new HashMap<>(), cookies, dataFetcher));

    if (doc.selectFirst("#captchacharacters") != null) {
      Logging.printLogWarn(logger, session, "Trying to fetch page again, because this site returned a captcha.");
      doc = Jsoup.parse(fetchPage(session.getOriginalURL(), new HashMap<>(), cookies, dataFetcher));
    }

    return doc;
  }

  /**
   * Fetch html from amazon
   * 
   * @param url
   * @param headers
   * @param cookies
   * @param session
   * @param dataFetcher
   * @return
   */
  public String fetchPage(String url, Map<String, String> headers, List<Cookie> cookies, DataFetcher dataFetcher) {
    String content;

    if (dataFetcher instanceof FetcherDataFetcher) {
      headers.put("Accept-Encoding", "no");

      Request request = RequestBuilder.create()
          .setUrl(url)
          .setCookies(cookies)
          .setHeaders(headers)
          .setProxyservice(
              Arrays.asList(
                  ProxyCollection.STORM_RESIDENTIAL_EU,
                  ProxyCollection.STORM_RESIDENTIAL_US))
          .mustSendContentEncoding(false)
          // We send this selector fetcher try again when returns captcha
          .setFetcheroptions(FetcherOptionsBuilder.create().setForbiddenCssSelector("#captchacharacters").build())
          .build();
      content = dataFetcher.get(session, request).getBody();

      if (content == null || content.isEmpty()) {
        Request requestApache = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
        content = new ApacheDataFetcher().get(session, requestApache).getBody();
      }
    } else {
      Request requestApache = RequestBuilder.create().setUrl(url).setHeaders(headers).setCookies(cookies).build();
      content = dataFetcher.get(session, requestApache).getBody();
    }

    return content;
  }

  /**
   * 
   * @param images - array present on html
   * @param doc - html
   * @param host - host of image url ex: www.amazon.com or www.amazon.com.br or www.amazon.com.mx
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
   * 
   * @param images - array present on html
   * @param host - host of image url ex: www.amazon.com or www.amazon.com.br or www.amazon.com.mx
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
