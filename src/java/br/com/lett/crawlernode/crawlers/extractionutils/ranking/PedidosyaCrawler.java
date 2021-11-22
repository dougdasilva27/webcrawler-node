package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class PedidosyaCrawler extends CrawlerRankingKeywords {

   public PedidosyaCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   String storeId = session.getOptions().optString("store_id");
   String type = session.getOptions().optString("type");
   String city = session.getOptions().optString("city");
   String market = session.getOptions().optString("market");

   private JSONObject getInfoFromAPI(){
      String url = "https://www.pedidosya.com.ar/mobile/v3/catalogues/298755/search?max=50&offset=0&partnerId="+storeId+"&query=" +this.keywordWithoutAccents+  "&sort=default";
      Map<String,String> headers = new HashMap<>();
      headers.put("cookie", "_gcl_au=1.1.1011751451.1637583178; _pxvid=866bbcf0-4b8d-11ec-b905-6f634349694f; _fbp=fb.2.1637583178210.170594016; _ga=GA1.3.774476031.1637583179; _gid=GA1.3.996295816.1637583179; _hjSessionUser_350054=eyJpZCI6Ijk2N2M2ODcyLWZmYmMtNWIxNC1iZjI3LTk0MTUxMWYzNWE5NiIsImNyZWF0ZWQiOjE2Mzc1ODMxNzc5MTMsImV4aXN0aW5nIjp0cnVlfQ==; _pxhd=Q/jSWxeJcG5bPGZvno91ZeOE5/qmvOZdERbzu/S84kSWQdGREa4OFYjsWsJ8uoHO/N-NUt0KPDxd7WAjfdg70A==:3zyyELqg4n3ARRY3ndaPBNJzsyOKtFv5QRopUngk6zaJFpbAkewzAg6A0jqnMrJhW1S-vgJUz/OoTDvPoaOcXHvmxqsyqryWTQCAIdP9H5pjhecPw-FXdJtIxtXqRXvFADSjdUWMV-KxcrU4T010DQ==; pxcts=90549120-4b9e-11ec-8817-4f0b938e9d55; dhhPerseusGuestId=1637601706139.173991779938371460.9g0bmi95tsp; dhhPerseusSessionId=1637601706139.254603884442502900.dw4yjheabkf; __Secure-peya.sid=s%3A1c3f5a7a-c34a-452b-af94-02edc619e933.1V6q0xhX1BZXR1c1sWIuHp5vfBchYf01rNWdWdMERQo; __Secure-peyas.sid=s%3A8a1c5ae7-ed41-414c-9156-109449554894.ZWPiSl%2BxEPjKHDeWC5HM1WkPNcom0bHJxC0m0zbfg28; _hjSession_350054=eyJpZCI6ImExMWYxYTZhLTFmOTctNGEzZi05ZmI1LTg0ODA1YjRiYTAwMCIsImNyZWF0ZWQiOjE2Mzc2MDE3MDc3NTZ9; _hjAbsoluteSessionInProgress=0; AMP_TOKEN=%24NOT_FOUND; __cf_bm=nIsASfBnojbz8Iuw8qYhAET5eaNeu7OF3TdhsKIWnwM-1637603068-0-AXm+V6m/2z8gV5Wn4xYVJ4R/XDDbCpJypClbN69spl077kwvffKwwpFPP4wV2gh8YlXhZhyrLi7C7Of202dwL4o=; _gat_WD2_Tracker_PeYa_Prod=1; _pxff_rf=1; _pxff_fp=1; _hjIncludedInPageviewSample=1; _hjIncludedInSessionSample=0; _px3=9bee2c641434a409f1bcab654abf5269187d49021a945a8452a482be61c9ef1f:7t1evwZ3jgl2iJnT7RztxfwgT6I47+yQkX5Fkoo6CFuQ8jQUJzx+2L1pKIepvXPL3vjSG3ugyqesn9YX08Teig==:1000:dh1s1R11YhlDz4cSmH82s9Gl9bCiZsnXwrhfGAKOPawT8f5PCBf6uwCXbMrcA0sWwtRmuj0LWU/5xQuI60TMYlPrN3rp9Wv73wFQ+o2DqBgB6VWiYe8rVijNdUnNsheCaJT4qK4VoSoDBCF3/JiFnqBBBwUPt70pJN8B4wRs9K34oClHTV2i6aZicV0s7r782CJYgpRjVsO11PWqGiifOQ==; dhhPerseusHitId=1637603280808.835820777691865900.y0s7zlyc3; _tq_id.TV-81819090-1.20dc=875790c825561a67.1637583179.0.1637603281..");
      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build();
      String resp = this.dataFetcher.get(session,request).getBody();
      return CrawlerUtils.stringToJson(resp);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 30;
      this.log("Página " + this.currentPage);

      JSONObject json = getInfoFromAPI();
      JSONArray data = json.optJSONArray("data");

      if (!data.isEmpty()) {

         for (Object o : data) {
            JSONObject productInfo = (JSONObject) o;

            String internalId = productInfo.optString("id");
            String internalPid = internalId;
            String productUrl = " https://www.pedidosya.com.ar/" + type + "/" + city + "/" + market +"-menu?p=" + internalId;
            String name = productInfo.optString("name");
            int price = productInfo.optInt("price");
            boolean isAvailable = price != 0;

            //New way to send products to save data product
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
            log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (arrayProducts.size() == productsLimit) {
               break;
            }

         }
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }
}
