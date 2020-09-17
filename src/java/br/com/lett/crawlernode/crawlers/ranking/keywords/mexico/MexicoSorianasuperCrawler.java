package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MexicoSorianasuperCrawler extends CrawlerRankingKeywords {

  public MexicoSorianasuperCrawler(Session session) {
     super(session);
     super.fetchMode = FetchMode.FETCHER;
  }

  private static final String PROTOCOL = "https://";
  private static final String DOMAIN = "superentucasa.soriana.com/Default.aspx";



   @Override protected Document fetchDocument(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", "ASP.NET_SessionId=jlg2o22z13aawvuupormhlwz; NumTipServProduccion=NumTipServ=1; NumTdaProduccion=NumTda=51; NombreTiendaProduccion=NombreTienda=Las Palmas; ak_bmsc=4B1A9D3922FA768C410FCBE2D8058F7FBBBD0C66093E00000653635F90894447~plU2z70pKcWwGdm+L04fL9QLVPYbTkwVrJYNhOn+2To0yxUGAuN5GON6+m47KR37IZy45g6eQUvkjoikpSAhuleL9pOadprKJpxrbYQfQe6kzdPXExNfERAZtnd/cztaJjqb/VRZ70CSkPcXA6qBeMfWMjTmffyHYGEe6ScBdCtPjc1fQdHyzaD1M6NBOEq3ElbYgf/rStsF7oDvClOpZyVXUUa+jMuX2gLb6E4xlna2o=; _ga=GA1.3.1591322.1600344843; _gid=GA1.3.1641728810.1600344843; _fbp=fb.1.1600344844919.1605184225; _hjTLDTest=1; _hjid=6e343037-4ee8-48c7-9e49-a5abf1093868; _hjIncludedInPageviewSample=1; _hjAbsoluteSessionInProgress=1; isPushNotificationClient=false; LinkToStoreUrl=?p=13365&Txt_Bsq_Descripcion=Galleta&Marca=&Linea=&Paginacion=2&nuor=0; TS01f618f3=015c364ca06ba3c49675ac1ae5f693f54c11b9e3cffa88d6e321851a8f12994fd973c75f711d05ecad3ab355e73912fbb906aff8152652fe2bfb25bd71fae25e5fba39cda80f03017c707b408ff9445264028b9822f23385c218fb12a7107f6111f40710d5dd2dc06176eed889451df0190b575c30180d83c62daca52b025493a56f212d9e; bm_sv=DE56AD8BF013B90939026FFFF79D8F1B~fZWDV2ik8K6ftTGGQwGLjavj1i8DfYV+uX9MasTzkqpZEHDnoGimmKuNKTFWdOde2CDznCgJBcCrK6BKoVLV/S4/W2uZ0EWQsdCHEdOUKJ9pJVHrijQdzfZJBGuOb8yT0ySSpSO0/T0lo9lLD1aeWVn5US4j7Rql0zggry5J5zw=");

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_MX
            )
         )
         .setCookies(cookies)
         .build();

      Document response = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

      return response;
   }


   @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    String url = PROTOCOL +DOMAIN+"?p=13365&Txt_Bsq_Descripcion=Galleta&Paginacion=" +this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".product-item");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {

        String internalId = crawlInternalId(e);

        String productUrl = PROTOCOL +DOMAIN + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a[href]:first-child", "href" );
         System.err.println(productUrl);
         saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  @Override
  protected boolean hasNextPage() {
    return false;
  }

  private String crawlInternalId(Element e) {
    String internalId = null;
    Element id = e.selectFirst("input[type=hidden][name=s]");

    if (id != null) {
      internalId = id.val();
    }

    return internalId;
  }
}
