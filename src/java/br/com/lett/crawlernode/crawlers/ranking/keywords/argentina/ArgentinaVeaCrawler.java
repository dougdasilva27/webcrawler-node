package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;


import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VTEXRankingKeywords;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Base64;

public class ArgentinaVeaCrawler extends VTEXRankingKeywords {

   public ArgentinaVeaCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected String getHomePage() {
      return "https://www.vea.com.ar/";
   }

   @Override
   protected String getLocation() {
      return "";
   }

   @Override
   protected String getVtexSegment() {
      return "eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIzNCIsInByaWNlVGFibGVzIjpudWxsLCJyZWdpb25JZCI6bnVsbCwidXRtX2NhbXBhaWduIjpudWxsLCJ1dG1fc291cmNlIjpudWxsLCJ1dG1pX2NhbXBhaWduIjpudWxsLCJjdXJyZW5jeUNvZGUiOiJBUlMiLCJjdXJyZW5jeVN5bWJvbCI6IiQiLCJjb3VudHJ5Q29kZSI6IkFSRyIsImN1bHR1cmVJbmZvIjoiZXMtQVIiLCJjaGFubmVsUHJpdmFjeSI6InB1YmxpYyJ9";
   }

   private static final String HOME_PAGE = "https://www.veadigital.com.ar";


   protected Document fetchDocument() {

      StringBuilder searchPage = new StringBuilder();

      searchPage.append(getHomePage())
         .append(this.keywordEncoded)
         .append("?_q=")
         .append(this.keywordEncoded)
         .append("map=ft");

     // https://www.vea.com.ar/coca?_q=coca&map=ft


      String apiUrl = searchPage.toString().replace("+", "%20");


      return Jsoup.parse(fetchPage("https://www.vea.com.ar/coca?_q=coca&map=ft"));
   }


   @Override
   protected String getUrlLocate() {
      return "es-AR";
   }


}
