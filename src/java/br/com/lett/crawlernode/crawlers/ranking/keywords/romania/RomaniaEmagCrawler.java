package br.com.lett.crawlernode.crawlers.ranking.keywords.romania;

import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;

public class RomaniaEmagCrawler extends CrawlerRankingKeywords {

   public RomaniaEmagCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {

      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie",
            "EMAGVISITOR=a%3A1%3A%7Bs%3A7%3A%22user_id%22%3Bi%3A2024580195347975914%3B%7D; site_version_11=not_mobile; EMAG_VIEW=not_mobile; ltuid=1599074195.138-b24ce42a01649d8df35e123caf9ffe88362ef632; EMAGUUID=1598879521-291954679-31430.446; _pdr_internal=GA1.2.5464345008.1599074195; eab290=c; profile_token=pftk_7165403916746472080; loginTooltipShown=1; _gcl_au=1.1.1094787864.1599074447; G_ENABLED_IDPS=google; _scid=c538a4af-60d8-41d8-bf9a-0ee452eeed17; _pin_unauth=dWlkPU0yUTNZV1l3WXpZdE1UZ3pNeTAwTURVMExUZzBPVGd0WkRRNE9HSmtOamt5T0RFeiZycD1abUZzYzJV; __gads=ID=ae9aa331d1556e73:T=1599074460:S=ALNI_MZPen0PNzhKsK9sSchpR8Wtq6bxOw; _sctr=1|1599015600000; EMAGROSESSID=d1c99e13d3eee65cf9365ce0c0f80d2d; eab275=a; eab279=a; eab282=a; eab283=a; sr=1920x1080; vp=1920x1008; _rsv=2; _rscd=1; _rsdc=2; listingDisplayId=2; supermarket_delivery_address=%7B%22name%22%3A%22Bucure%5Cu015fti%22%2C%22id%22%3A4954%2C%22delivery_type%22%3A2%2C%22storage_type%22%3A%7B%221%22%3A%221%22%2C%222%22%3A%221%22%2C%223%22%3A%221%22%7D%2C%22delivery_categories%22%3A%7B%22Fructe+si+Legume%22%3A1%2C%22Lactate%2C+Oua+si+Paine%22%3A1%2C%22Carne%2C+Mezeluri+si+Pes+...%22%3A1%2C%22Produse+congelate%22%3A1%2C%22Alimente+de+baza%2C+cons+...%22%3A1%2C%22Cafea%2C+cereale%2C+dulciu+...%22%3A1%2C%22Bauturi+si+tutun%22%3A1%2C%22Ingrijire+copii%22%3A1%2C%22Intretinerea+casei+si++...%22%3A1%2C%22Ingrijire+personala+%22%3A1%2C%22Vinoteca%22%3A1%2C%22Produse+naturale+si+sa+...%22%3A1%7D%7D; supermarket_delivery_zone=%7B%22id%22%3A4954%2C%22name%22%3A%22Bucure%5Cu015fti%22%7D; campaign_notifications={\"4535\":1}; delivery_locality_id=4958; _uetsid=055bd66b1122354e5eef99173501229f; _uetvid=43ccf5f349725a648f90f16e7ac9221d; _pdr_view_id=1599144227-14804.696-563806517; _dc_gtm_UA-220157-3=1");

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).setCookies(this.cookies).build();
      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      this.pageSize = 60;

      String url = "https://www.emag.ro/supermarket/search/" + this.keywordEncoded + "/p" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url, cookies);

      Elements products = this.currentDoc.select(".js-products-container .card-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0)
            setTotalProducts();

         for (Element e : products) {
            String internalId = e.attr("data-offer-id");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".card-section-wrapper .card-heading a", "href");

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
      this.totalProducts =
            MathUtils.parseInt(
                  currentDoc.selectFirst(".title-phrasing.title-phrasing-sm").text());
      this.log("Total da busca: " + this.totalProducts);
   }

}
