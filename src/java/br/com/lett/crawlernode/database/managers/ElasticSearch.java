package br.com.lett.crawlernode.database.managers;

import credentials.models.ElasticCredentials;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;

public class ElasticSearch {

   RestHighLevelClient client;


   public void connection(ElasticCredentials credentials) {

      CredentialsProvider cp = new BasicCredentialsProvider();

      cp.setCredentials(
         AuthScope.ANY,
         new UsernamePasswordCredentials(credentials.getUsername(),
            credentials.getPassword()));

      client = new RestHighLevelClient(
         RestClient.builder(
               new HttpHost(
                  credentials.getHost(),
                  Integer.parseInt(credentials.getPort()), "https"))
            .setHttpClientConfigCallback(
               httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)));


   }

   public void closeConnection() {
      try {
         client.close();
      } catch (Exception e) {
      }
   }

   public boolean pingConnection() {
      try {
         client.ping(RequestOptions.DEFAULT);
         return true;
      } catch (Exception e) {
      }
      return false;
   }

   public SearchResponse searchResponse(String[] sources, BoolQueryBuilder query) throws IOException {

      String[] empty = new String[0];

      SearchSourceBuilder builder = new SearchSourceBuilder();
      builder.query(query);
      builder.fetchSource(sources, empty);
      builder.size(1);
      builder.from(0);
      builder.sort("created");

      SearchRequest searchRequest = new SearchRequest();
      searchRequest.searchType(SearchType.DEFAULT);
      searchRequest.source(builder);

      return client.search(searchRequest, RequestOptions.DEFAULT);

   }

}
