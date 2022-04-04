package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class BrasilRiachueloCrawler extends CrawlerRankingKeywords {


   private static final String X_API_KEY = "KhMO3jH1hsjvSRzQXNfForv5FrnfSpX6StdqMmjncjGivPBj3MS4kFzRWn2j7MPn";
   private static final String TOKEN_KEY = "SjqJQstBFWjIqzYzP73umkNHT7RTeWcHanVu1K7mGYHrqIskym+BvChLueA0qnAstBZzgVcwOt/UNlU1wXbhJ7ta6/8esxROylJS6kTk3VEw1l3QBHijzGk/CF8afz1HmOHFFQ4u/+N7+GqJ1Pax8BmrOt3KitkBF47zyxMagTAUruSogIx0A/ib7JtSUvDHLi53MRlODpjG/Pezkm/EhhczAjYk2+3bRWMu0/nk3KknXXoO+SDf826ukLDpkfjwg8OUYOTWdvt5X7WiuspIB2E5ZklYYK8C8hxda3Sy5QaGngElEgzZfZkcC0slJuVMSS3+7F6ysxgKLIX0K1LZPZALGe7BtEsCKMDv9L2LarGUzZkOJT9X6kFa3wsQj3YggZtIGASIznkWWUg0hhrX+FzWsvjwhxvCaX4LYpXQ2byA9lmlliZ1wtf0ZvNmrjc01tzvZHfm67PdqO3VHqK+tEhlVdTZuQlWb4ekExpkyoKpZnkqSVdpQ/LkemnKgzVmah00EvCWOJhFgEzqxxTCRobzBoUKNmj/ZSg51H/3e95+Xxdpf0Y5+TIpuWyq79tY3ZxQcUceF0dQUQlptRIlOjzt9jGHyYrO5El3PwAH1FOvyQialAomF2mjo2ffa73l9d6IN+8H+6s5dVUYsT9FCqeO1RKveZcWQ5TEVe+Y5lw=";

   public BrasilRiachueloCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }


   protected Document fetch() {
      return Jsoup.parse(fetchPage(session.getOriginalURL(), session));
   }

   public String fetchPage(String url, Session session) {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("connection", "keep-alive");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setIgnoreStatusCode(false)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .setForbiddenCssSelector("#px-captcha")
               .build()
         ).build();

      Response response = this.dataFetcher.get(session, request);

      return response.getBody();
   }

   private String getToken() {
      String token = "";
      String url = "https://9hyxh9dsj1.execute-api.us-east-1.amazonaws.com/v1/bf60cb91-a86d-4a68-86eb-46855b4738c8/get-token";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "*/*");
      headers.put("connection", "keep-alive");
      headers.put("x-api-key", X_API_KEY);

      JSONObject payload = new JSONObject();
      payload.put("value", TOKEN_KEY);

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setIgnoreStatusCode(false)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setPayload(payload.toString())
         .build();

      JSONObject json = JSONUtils.stringToJson(this.dataFetcher.post(session, request).getBody());

      if (json != null && !json.isEmpty()) {
         token = json.optString("IdToken");
      }

      return token;
   }


   private JSONObject fetchProducts() {
      String url = "https://api-dc-rchlo-prd.riachuelo.com.br/ecommerce-web-catalog/v2/products";

      String payload = "{\"includeFilters\":true,\"order\":\"asc\",\"q\":\""+ this.keywordEncoded +"\",\"sort\":\"relevance\",\"attributes\":{},\"price\":[],\"page\":"+ this.currentPage +",\"soldOut\":false}";

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "*/*");
      headers.put("accept-encoding", "no");
      headers.put("connection", "keep-alive");
      headers.put("content-type", "application/json");
      headers.put("x-api-key", X_API_KEY);
      headers.put("x-app-token", getToken());

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setPayload(payload)
         .setHeaders(headers)

         .build();
      String jsonStr = this.dataFetcher.post(session, request).getBody();

      return JSONUtils.stringToJson(jsonStr);
   }


   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      JSONObject json = fetchProducts();

      if (json != null && !json.isEmpty()) {

      }
   }
}

