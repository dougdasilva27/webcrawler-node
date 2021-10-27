package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

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
   }
   String storeId = session.getOptions().optString("store_id");
   String type = session.getOptions().optString("type");
   String city = session.getOptions().optString("city");
   String market = session.getOptions().optString("market");

   private JSONObject getInfoFromAPI(){


      String url = "https://www.pedidosya.com.ar/mobile/v3/catalogues/298755/search?max=50&offset=0&partnerId="+storeId+"&query=coca&sort=default";
      Map<String,String> headers = new HashMap<>();
      headers.put("cookie", "_fbp=fb.2.1635281248370.496271014; _pxhd=6U5JYRKMSk6RzsQWMwANFlWmB1/kTV5DET3V6-AVNsCGjQiXiLmupcVbJfyNf2/R7k0iwDpKalZz68ibNJNDsg==:RCvqJeZHjASLONsiYhtUc1qYsQD9apAa-6RjaN/XPRt-4pj5RqcKvVfNEHm9AyxY-xnzUdFODUi4Mw-XE0tD1noVcoQNiwVpnfptUxZHTwI5q2bhWsK2W/EfSgPpdBcs; pxcts=f543cf30-369d-11ec-88df-8389a1d74c91; _pxvid=ef399119-369d-11ec-a6e1-49646e467a67; _hjid=05198fcf-eadc-4017-9886-d554d56eade8; _gcl_au=1.1.924052929.1635281260; _ga=GA1.3.293164481.1635281260; _gid=GA1.3.1074224560.1635281260; __Secure-peya.sid=s%3Afae4d9e4-7bd2-43f3-ad4f-c5c6b3282281.MioIplqrvnNAEu94JYbWreMN0ZcF6oZgR%2FIApTN8%2FX4; __Secure-peyas.sid=s%3A2d9ba471-0c9e-463d-977d-0060169c2aa5.Fbq6NPL9gxNQIpYiUviKQcdMwl3xcPBOVO2xbQy3uCY; __cf_bm=i6N6CXna3aXcYx0fpiJRXK_hZ9M06ZgSwlysEw9RTNE-1635361545-0-AY9LhaJXZm6LctAV2pLAX6fr88kVdd8CCU3Wt7jdNIc2xH0B/OFoAhxPPAI5Gk0GyGoY2zHQYhtDAkP1VxROiLA=; _hjIncludedInSessionSample=0; _hjAbsoluteSessionInProgress=1; AMP_TOKEN=%24NOT_FOUND; _px3=13cfe4458c93f119941e21ca559e3894dd2a0d6b621560a9979278d5cfc54161:k7FsqkIIWpvuBTLkbe+J6dHVQdHWhG21P3PYbLW4zTGKV4QgzhBMsXo3Gk55sbS9fpgc1QqA/XTW5KyeEKDXBw==:1000:wzIU4jhXy9177R4oB3RptpzUyZ8ODKser89dfyGKkbKMWQrQRd8pk1Af6dg6cfexs/PzErJ14W1BFLBkO8Udg4pew9how7tBWam5k3rK4MqGXiIB4QdyM+SZLZ0E5f8wUyPuV/wY+LMI1hFrtVAncQE38ch+FwUeQHNUpm9beVidkJrTP1XEq45DHU/eX8g1BTPM5ABDBR5IODmd9Y1zwg==; _gat_WD2_Tracker_PeYa_Prod=1; _tq_id.TV-81819090-1.20dc=6c224119bc0470f7.1635281248.0.1635362468..");

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
