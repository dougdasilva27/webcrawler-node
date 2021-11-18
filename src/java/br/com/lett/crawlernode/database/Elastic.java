package br.com.lett.crawlernode.database;

import br.com.lett.crawlernode.util.Logging;
import com.google.gson.Gson;
import credentials.models.ElasticCredentials;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Elastic {

   private static final Logger logger = LoggerFactory.getLogger(Elastic.class);
   RestHighLevelClient client;

   public void connection(ElasticCredentials elasticCredentials) {

      CredentialsProvider cp = new BasicCredentialsProvider();
      cp.setCredentials(
         AuthScope.ANY,
         new UsernamePasswordCredentials(elasticCredentials.getUsername(),
            elasticCredentials.getPassword()));

      client = new RestHighLevelClient(
         RestClient.builder(
               new HttpHost(
                  elasticCredentials.getHost(),
                  Integer.parseInt(elasticCredentials.getPort()), "https"))
            .setHttpClientConfigCallback(
               httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)));

      if (pingConnection()) {
         Logging.printLogInfo(logger, "Connecting to elastic was a success!");

      } else {
         Logging.printLogInfo(logger, "Failed to try to connect with elastic\n");
      }
   }

   public void closeConnection() {
      try {
         client.close();
      } catch (Exception e) {
         Logging.printLogWarn(logger, "There is no connection to be closed");
      }
   }


   public boolean pingConnection() {
      try {
         client.ping(RequestOptions.DEFAULT);
         return true;
      } catch (Exception e) {
         Logging.printLogWarn(logger, "Ping on connection with elastic failed. Application not connected to elastic");
      }
      return false;
   }

   public List<String> fetch(SearchSourceBuilder search) throws IOException {

//      String[] sources = new String[3];
//      sources[0] = "internal_id";
//      sources[1] = "url";
//      sources[2] = "market_id";
//
//      String[] empty = new String[0];
//
//      BoolQueryBuilder query = QueryBuilders.boolQuery();
//
//      if (!trackedInformation.getTrackedLettIds().isEmpty()) {
//         query.must(QueryBuilders.termsQuery("lett_id", trackedInformation.getTrackedLettIds()));
//
//      }
//      if (trackedInformation.getMarketId() != null) {
//         query.must(QueryBuilders.termQuery("market_id", trackedInformation.getMarketId()));
//
//      } else {
//         query.must(QueryBuilders.termsQuery("market_id", trackedInformation.getTrackedMarkets()));
//      }
//
//      SearchSourceBuilder builder = new SearchSourceBuilder();
//      builder.query(query);
//      builder.fetchSource(sources, empty);
//      builder.size(100);
//      builder.from(0);
//      builder.sort("created");

      final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
      SearchRequest searchRequest = new SearchRequest();
      searchRequest.searchType(SearchType.QUERY_THEN_FETCH);
      searchRequest.source(search);
      searchRequest.scroll(scroll);

      SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

      String scrollId = searchResponse.getScrollId();
      SearchHit[] searchHits = searchResponse.getHits().getHits();

      List<String> results = new ArrayList<>();

      while (searchHits != null && searchHits.length > 0) {
         SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
         scrollRequest.scroll(scroll);
         searchResponse = client.searchScroll(scrollRequest, RequestOptions.DEFAULT);
         scrollId = searchResponse.getScrollId();
         searchHits = searchResponse.getHits().getHits();

         Arrays.stream(searchHits).forEach(hit -> results.add(hit.getSourceAsString()));
      }

      clearScroll(scrollId);
      closeConnection();

      return results;

   }

   public void clearScroll(String scrollId) {
      ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
      clearScrollRequest.addScrollId(scrollId);
      try {
         client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
      } catch (IOException e) {
         e.printStackTrace();
      }

      Logging.printLogDebug(logger, "Scroll was cleanup");
   }
}
