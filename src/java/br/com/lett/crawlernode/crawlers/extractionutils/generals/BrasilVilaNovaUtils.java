package br.com.lett.crawlernode.crawlers.extractionutils.generals;

import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class BrasilVilaNovaUtils {
   private final Session session;

   public BrasilVilaNovaUtils(Session session) {
      this.session = session;
   }

   public String getToken() {
      return session.getOptions().optString("token");
   }

   public String getMarket() {
      return session.getOptions().optString("market");
   }

   public void login(DataFetcher dataFetcher, List<Cookie> cookies) {
      Response loginResponse = new Response();
      try {
         Request request = Request.RequestBuilder.create()
            .setUrl("https://www.vilanova.com.br/loginlett/access/account/token/" + getToken())
            .setProxy(
               getFixedIp()
            )
            .build();
         loginResponse = dataFetcher.get(session, request);
      } catch (IOException e) {
         e.printStackTrace();
      }

      List<Cookie> cookiesResponse = loginResponse.getCookies();

      for (Cookie cookieResponse : cookiesResponse) {
         BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
         cookie.setDomain("www.vilanova.com.br");
         cookie.setPath("/");
         cookies.add(cookie);
      }
   }

   public LettProxy getFixedIp() throws IOException {
      LettProxy lettProxy = new LettProxy();
      lettProxy.setSource("fixed_ip");
      lettProxy.setPort(3144);
      lettProxy.setAddress("haproxy.lett.global");
      lettProxy.setLocation("brazil");
      return lettProxy;
   }

   public JSONObject getJsonConfig(Element element, String path, String id) {
      String jsonString = CrawlerUtils.scrapScriptFromHtml(element, path);
      JSONArray jsonArray = JSONUtils.stringToJsonArray(jsonString);
      JSONObject json = JSONUtils.getValueRecursive(jsonArray, "0.[data-role=swatch-option-" + id + "].Magento_Swatches/js/swatch-renderer", JSONObject.class, new JSONObject());
      if (!json.isEmpty()) {
         return json.optJSONObject("jsonConfig");
      }
      return json;
   }

   public JSONArray getAttributes(JSONObject json, String attribute) {
      JSONObject attributes = json.optJSONObject("attributes");
      Iterator<String> keys = attributes.keys();
      if (!attributes.isEmpty()) {
         while (keys.hasNext()) {
            String key = keys.next();
            String code = JSONUtils.getValueRecursive(attributes, key + ".code", String.class, "");
            if (attribute.equals(code)) {
               return JSONUtils.getValueRecursive(attributes, key + ".options", JSONArray.class, new JSONArray());
            }
         }
      }
      return new JSONArray();
   }

   public JSONArray getObjectImages(String id, JSONObject json) {
      JSONObject jsonImage = json.optJSONObject("images");
      return id != null && jsonImage.has(id) ? jsonImage.optJSONArray(id) : new JSONArray();
   }

   public String getPrimaryImage(JSONArray images) {
      for (Object o : images) {
         JSONObject objImage = (JSONObject) o;
         if (objImage.optBoolean("isMain")) {
            return objImage.optString("img");
         }
      }
      return null;
   }

   public String findId(JSONObject variation, JSONObject objectMarket) {
      List<String> idsProducts = JSONUtils.jsonArrayToStringList(variation.optJSONArray("products"));
      List<String> idsMarket = JSONUtils.jsonArrayToStringList(objectMarket.optJSONArray("products"));
      for (String idProduct : idsProducts) {
         for (String idMarket : idsMarket) {
            if (idProduct.equals(idMarket)) {
               return idProduct;
            }
         }
      }
      return null;
   }

   public JSONObject getObjectMarket(JSONObject jsonObject) {
      JSONArray allMarkets = getAttributes(jsonObject, "variant_seller");
      String idMarket = getMarket();
      for (Object o : allMarkets) {
         JSONObject market = (JSONObject) o;
         String candidateIdMarket = market.optString("id", "");
         if (!candidateIdMarket.isEmpty() && idMarket.equals(candidateIdMarket)) {
            return market;
         }
      }
      return new JSONObject();
   }

   public String getSanitizedUrl(String url) {
      int indexPointer = url.indexOf('?');
      return indexPointer != -1 ? url.substring(0, indexPointer) : url;
   }

   public String getInternalId(String internalPid, String label) {
      return internalPid + label.replace("-", "");
   }
}
