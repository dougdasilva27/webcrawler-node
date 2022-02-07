package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.LinxImpulseRanking;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BrasilCompreAgoraCrawler extends LinxImpulseRanking {
   public BrasilCompreAgoraCrawler(Session session) {
      super(session);
   }

@Override
   protected int crawlPrice(JSONObject product) {
      JSONArray obj = fetchPrice(product.optString("id"));
      int priceInCents;
;     try{
         priceInCents = obj.optJSONObject(0).optJSONObject("skus").optInt("pricePerUnit");
      }catch (NullPointerException pointer){
         priceInCents = 0;
      }

      return priceInCents;
   }
   protected JSONArray fetchPrice(String id) {
      String url = "https://www.compra-agora.com/api/productLookup/"+id;
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "CPL=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJpbmZyYWNvbW1lcmNlLmNvbS5iciIsInN1YiI6IkluZnJhY29tbWVyY2UiLCJhdWQiOiJjb21wcmEtYWdvcmEuY29tIiwiaWF0IjoxNjQ0MjQ5MTY2LCJkYXRhIjp7InVpZCI6IlRvYndGUmdjS3pOckNzZXhuUnozWnk3a3JwNEJ3T2VUTGxpRmtBNzR1czQ9In19.ob8UfpN1WMPTagamtSsoco1cllnNTSRwNeKrbI2Q9oc2HNjUL56VVEUc8vuz_jUtYEQ3RZAJZJLf5vSV_wXT6V9dkrGP2L7BilB3JamV56muHBqOjA3Xhii6qGFQePgAvNoZaCrw75-pIDSwzfqEzXDYoNqxkBQfVvTG-FCKTKtbVZqcTNXAcNjA80dp9AydpkCi712NxGITCnigZVFaktWl3B3NuBFd44oE-qKI5JCp6IwbM0ptgBHjAT8i43AmLsdS_-JCPcBjbBTFgS_27KNQ26dvuuZjdSwzKHYSxvUO2rI7Vz_4TVFHdaNA-MSfPksfzZT_B_HnX7FkC-imoA; PHPSESSID=7hslf6eioumlusjacjolnn8cf1; ccw=2 3 61 94 147; usrfgpt=367359160001371644248354");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      Response response = dataFetcher.get(session, request);

      return CrawlerUtils.stringToJsonArray(response.getBody());
   }
}
