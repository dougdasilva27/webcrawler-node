package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BelohorizonteSantahelenaCrawler extends CrawlerRankingKeywords {

   public BelohorizonteSantahelenaCrawler(Session session) {
      super(session);
   }

   private static final String urlAPI = "https://santahelenacenter.com.br/wp-admin/admin-ajax.php";

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 16;

      this.log("Página " + this.currentPage);

      if (this.currentPage == 1) {
         String url = "https://santahelenacenter.com.br/?s=" + this.keywordEncoded.replace(" ", "+") + "&post_type=product&dgwt_wcas=1";
         this.log("Link onde são feitos os crawlers: " + url);
         this.currentDoc = fetchDocument(url);
      } else {
         this.log("Link onde são feitos os crawlers: " + urlAPI);
         this.currentDoc = nextPageHTML();
      }

      Elements products = this.currentDoc.select("div.product");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h2", true);
            String productUrl = CrawlerUtils.scrapUrl(e, ".product-body a", "href", "https", "santahelenacenter.com.br");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-footer a", "data-product_id");
            Integer price = scrapPrice(e);
            String image = CrawlerUtils.scrapSimplePrimaryImage(e, ".img-fluid", Arrays.asList("src"), "https", "santahelenacenter.com.br");
            boolean available = e.selectFirst("form") != null;

            RankingProduct rankingProduct = RankingProductBuilder.create()
               .setName(name)
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setImageUrl(image)
               .setAvailability(available)
               .setPriceInCents(price)
               .build();

            saveDataProduct(rankingProduct);

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

   private Integer scrapPrice(Element e) {
      Integer spotlightPrice = CrawlerUtils.scrapPriceInCentsFromHtml(e, "ins > .woocommerce-Price-amount.amount > bdi", null, false, ',', session, null);
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".woocommerce-Price-amount.amount > bdi", null, false, ',', session, null);
      }
      return spotlightPrice;
   }

   protected Document nextPageHTML() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("Connection", "keep-alive");
      headers.put("X-Requested-With", "XMLHttpRequest");

      String payloadString = "cache=false&action=load_more&beforeSend=&current_page=" + (this.currentPage - 1) + "&per_page=" + this.pageSize +
         "&cat_id=&filter_cat=&on_sale=&orderby=&shop_view=&min_price=&max_price=&is_search=yes&s=" + this.keywordEncoded.replace(" ", "+");

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(urlAPI)
         .setPayload(payloadString)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_ES,
            ProxyCollection.NETNUT_RESIDENTIAL_BR
         ))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session, "post");

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
