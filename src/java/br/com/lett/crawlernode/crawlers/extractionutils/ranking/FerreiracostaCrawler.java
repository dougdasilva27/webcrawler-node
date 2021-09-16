package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class FerreiracostaCrawler extends CrawlerRankingKeywords {

   public FerreiracostaCrawler(Session session) {
      super(session);
   }

   public Document fetchDocument(String url){


      String location = session.getOptions().optString("location");

      Map<String,String> headers = new HashMap<>();
      headers.put("cookie", "ASP.NET_SessionId=afdhrtmd1chvych4b1y1zqmv;" +
         " osVisitor=b44b973b-4257-4e0b-a2ca-47e37d439251;" +
         " osVisit=f29d817f-51ed-4082-8dd0-7ae9b309c837;" +
         " eco_ce=; ecoFC=FCB02C35F253FCE56D9696DC7757BC6E;" +
         " _gid=GA1.2.1245936171.1631798355;" +
         " _gat_UA-48783684-2=1;" +
         " _gat_gtag_UA_48783684_2=1;" +
         " G_ENABLED_IDPS=google;" +
         " _fbp=fb.1.1631798354801.1640176812;" +
         " _pin_unauth=dWlkPU9XVTBZMlExTlRRdE5UZ3pNeTAwTWprd0xXSTNNMkV0TVRWaE5XVTJZVE0yTkRZMw;" +
         " _hjid=fdb9c70b-3929-430b-8698-0a11e9020213;" +
         " _hjIncludedInSessionSample=1;" +
         " _hjAbsoluteSessionInProgress=0;" +
         " eco_lo=" + location + ";" +
         " _ga=GA1.2.1447234012.1631798354;" +
         " _ga_DD7Y69210P=GS1.1.1631798009.3.1.1631798369.0;" +
         " cto_bundle=QnJvIl9STm1tMVN2ZVhydXYxSXhnSGJFbEV1ak1kT1VRYXlGRnIyUldQbm4lMkZhM0hFVWNCYXM4JTJCUDRJUFklMkIzJTJGblglMkJaSCUyQkU5QkUlMkYweWVVNUU5bkYxWkV0bXdzYnZEOWxxV2xEdjFZMDIlMkZvSTVWTnRueGVKZDZxT3dRQ05SbnQlMkJ0cWdvYnZac1pBSSUyRkZtenpzVzA0RFRQSiUyQmZBJTNEJTNE;" +
         " AWSALB=AWQren+oPEAFGXDRiJutL5+sy0hpn5zZBAoiHwI5wCsthQh1UcN4sz5hYfT2hEfrlKuY45Vz5J0qEsHDS9JBbMDsPqDb7l12m63zMvEokIrgKyyLHS5mFcV8YyT+;" +
         " AWSALBCORS=AWQren+oPEAFGXDRiJutL5+sy0hpn5zZBAoiHwI5wCsthQh1UcN4sz5hYfT2hEfrlKuY45Vz5J0qEsHDS9JBbMDsPqDb7l12m63zMvEokIrgKyyLHS5mFcV8YyT+;" +
         " RT=s=1631798388140&r=https%3A%2F%2Fwww.ferreiracosta.com%2FProduto%2F408846%2Flavadora-de-roupa-brastemp-12kg-branca-127v-bwk12abana");

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).setCookies(this.cookies).build();
      Response resp = this.dataFetcher.get(session,request);

      return Jsoup.parse(resp.getBody());

   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {

      String url = "https://www.ferreiracosta.com/Pesquisa/" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".busca__lista.column .product-view-card a");

        if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {

            String internalId = scrapInternalIdFromURL(e.attr("href"));
            String internalPid = internalId;
            String productUrl = "https://www.ferreiracosta.com" + e.attr("href");

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected void setTotalProducts() {
      Element totalSearchElement = this.currentDoc.selectFirst("p.toolbar-amount :last-child");

      if(totalSearchElement != null) {
         this.totalProducts = Integer.parseInt(totalSearchElement.text());
      }

      this.log("Total da busca: " + this.totalProducts);
   }


   private String scrapInternalIdFromURL(String rawURL){
     String internalId = "";

     internalId = CommonMethods.getLast(rawURL.split("/Produto/"));
     internalId = internalId.split("/")[0];

     return internalId;
   }
}
