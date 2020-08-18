package br.com.lett.crawlernode.crawlers.ranking.keywords.portugal;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class PortugalAuchanCrawler extends CrawlerRankingKeywords {

   public PortugalAuchanCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   public Document fetch(String url) {
      String doc = "";
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie",
         "ASP.NET_SessionId=axhfttaqkalpwytwes0bw5kb; AuchanSessionCookie=u4xIamdzL6M_R006lJqK8w_Z88O1WWu20eSHUpBHV3yUXV6SxVniYcx3Ce4kUtWPhVeNncTWrYtLsPhOEtAmLV8notQMFHnKZ95Y9BxAx2EPts5JeipBZQXO1QRKMoN70Jq2iyNVsCFpIR545qpMihRprAz7R7Vb1Vbq_xnTheIIVHN4r1adv4IVTStC1Q6KemoHOtEM3gg7ZYEUDohgsyZiTyUDB0MLP5utKPPr_nVNyyJN-__fHWcy18E8KyovRhD_ua9TVffbuvcRxBt8A1yZzWqS6irvSngRNJGYM_kbnflrHr-QsEwCcbYiTfhghH_Tr6hPhBBVcdMJmJawIl4G_8yzl7uL9_wmS3CuYXX8TM-y43yGabA_9dMag_ep_nSbwXZUObqYa-9KhLv-qflp8M-DmPqqYHKjXoCIH3G1-Se_iQRJTenIZHNMFksK; __RequestVerificationToken_L0Zyb250b2ZmaWNl0=sCoJaqCMarHUs6YNNlDrWIhyLTuulzob5CpjdHc765DeeyDQf_C1KiI3LqwdlurrsvBfsm3AWZndp-mPHS-YI2eCCBfawdbFEjISy1Wkyh41; AUCHANCOOKIE=id=18710ecd-8396-4575-a038-45d8eb285d12&an=1&pl=0&nl=False&se=637332914960490421");

      Request request = Request.RequestBuilder.create().setHeaders(headers).setUrl(url).build();
      doc = this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(doc);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 30;
      this.log("Página " + this.currentPage);

      String url = "https://www.auchan.pt/Frontoffice/search/" + this.keywordWithoutAccents;
      System.err.println(url);


      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetch(url);
      Elements products = this.currentDoc.select(".product-item");


      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = e.attr("data-product-id");
            String productUrl = "https://www.auchan.pt" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-header a", "href");
            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, "#page .col-sm-9 h3", false, 0);

      this.log("Total de produtos: " + this.totalProducts);
   }

}
