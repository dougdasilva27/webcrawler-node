package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BrasilMaxAtacadistaCrawler extends CrawlerRankingKeywords {
   public BrasilMaxAtacadistaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://delivery.maxatacadista.com.br/buscapagina?ft="+this.keywordEncoded+"&PS=48&sl=d85149b5-097b-4910-90fd-fa2ce00fe7c9&cc=48&sm=0&PageNumber="+this.currentPage;
      this.currentDoc = fetch(url);
      Elements products = this.currentDoc.select("ul li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = products.size();
         }

         for (Element e : products) {
            Elements valid = e.select(".row");
            if(!valid.isEmpty()){
               String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".col-xs-5.col-sm-12.prd-list-item-img a", "href");
               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".col-sm-6.col-lg-4.col-xl-3.prd-list-item","data-product-id");;
               String name = CrawlerUtils.scrapStringSimpleInfo(e,".prd-list-item-name", false);
               String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".prd-list-item-link img", Arrays.asList("src"), "https", "");
               Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".prd-list-item-price .prd-list-item-price-sell", null, false, ',', session, null);

               boolean isAvailable = price != null;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setName(name)
                  .setImageUrl(imgUrl)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(productRanking);
               this.log(
                  "Position: " + this.position +
                     " - InternalId: " + internalId +
                     " - Url: " + productUrl);

            }

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }

         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }


   }
   protected Document fetch(String url) {
      Map<String, String> headers = new HashMap<>();

      headers.put("Accept","*/*");
      headers.put("Accept-Encoding","gzip, deflate, br");
      headers.put("Connection","keep-alive");
      headers.put("cookie","ISSMB=ScreenMedia=0&UserAcceptMobile=False; VtexRCMacIdv7=62e56c83-ec89-4f69-afaf-265fc49238f2; checkout.vtex.com=__ofid=a12e85cf618c4ed1aa55c0f117b4fe89; smd_has-channel_name=S.%20J.%20Rio%20Preto%20-%20Regi%C3%A3o%20Norte%20-%20SP; smd_has-channel=true; VTEXSC=sc=25; ISS=InternalCampaign=7312022185517183; ISICI=InternalCampaign=7312022185517183; janus_sid=83993ad3-872d-4027-a68b-acb4e2f036ac; .ASPXAUTH=B28E2B1B602945A9D53C5D8D25457C6340F2840B73CBDEC0CFE24C45900AEF770CFC76577B4532091697906D0D74C5F402303A0EA658AC0CBEF6FA8767DCA7F3D9B0AC55977D7D5036E1F63520CBCB6063F353381CA0B1F17E5A8FC8B62373137C3C76FD37877F249BFEFDDB3521F96F27DD1CDD9D75D59C3FE8F3AE3B80671146005EF617CBB307E618D503A17C520D51A8D15F21CA63DAE8DB048EA1E2E6419856EF3E; vtex_session=eyJhbGciOiJFUzI1NiIsImtpZCI6IjRBQjVERTgwQjVGMkIzRUVFNzE1ODEyRTBFMTZCRkNFREI5QjVEM0EiLCJ0eXAiOiJqd3QifQ.eyJhY2NvdW50LmlkIjoiM2IxMGQzY2QtYjhjMC00YWM2LWE4MDEtNGU3ZTc5Zjk1ZTE0IiwiaWQiOiI1ODM4Yjg1Mi01OTc0LTQ1MTgtYWEwOS05MzVlNjVkOWNhMDEiLCJ2ZXJzaW9uIjozLCJzdWIiOiJzZXNzaW9uIiwiYWNjb3VudCI6InNlc3Npb24iLCJleHAiOjE2NTAwNTk3MjEsImlhdCI6MTY0OTM2ODUyMSwiaXNzIjoidG9rZW4tZW1pdHRlciIsImp0aSI6IjQ4OThiZjQ1LTA5NDItNGVmMS05MDllLWQzYTc3ZGUyMDE1ZSJ9.L2dfDxRhAA_aSlx7KKs6zsH7jm0q_97V8w0KfAsVy9hLj4FCQitnNERm4nJxM24FHT8G2sDK9HTubss5CZQXvQ; vtex_segment=eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIyNSIsInByaWNlVGFibGVzIjpudWxsLCJyZWdpb25JZCI6bnVsbCwidXRtX2NhbXBhaWduIjpudWxsLCJ1dG1fc291cmNlIjpudWxsLCJ1dG1pX2NhbXBhaWduIjoiNzMxMjAyMjE4NTUxNzE4MyIsImN1cnJlbmN5Q29kZSI6IkJSTCIsImN1cnJlbmN5U3ltYm9sIjoiUiQiLCJjb3VudHJ5Q29kZSI6IkJSQSIsImN1bHR1cmVJbmZvIjoicHQtQlIiLCJhZG1pbl9jdWx0dXJlSW5mbyI6InB0LUJSIiwiY2hhbm5lbFByaXZhY3kiOiJwdWJsaWMifQ; urlLastSearch=http://delivery.maxatacadista.com.br/cerveja; VtexRCSessionIdv7=b7018fb9-b24f-4370-bf21-41da8ee84136; SGTS=EFBD68D37B5A455FE7F1B082C436F181");


      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Collections.singletonList(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .setSendUserAgent(false)
         .build();

      Response a = this.dataFetcher.get(session, request);

      String content = a.getBody();

      return Jsoup.parse(content);
   }
   @Override
   protected boolean hasNextPage() {
      return arrayProducts.size() < this.totalProducts;
   }
}
