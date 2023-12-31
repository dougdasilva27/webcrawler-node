package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BrasilExtrabomCrawler extends CrawlerRankingKeywords {

   private final String sellerId = this.session.getOptions().optString("seller_id");

   public BrasilExtrabomCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {

      Map<String, String> headers = new HashMap<>();
      headers.put("user-agent", "LettDigital/1.0");

      Request request = Request.RequestBuilder.create().setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
         ))
         .build();

      return Jsoup.parse(dataFetcher.get(session, request).getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      String url = "https://www.extrabom.com.br/busca/?q=" + this.keywordEncoded + "&Id_seller" + sellerId + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".carousel__detalhe-item ");

      if (!products.isEmpty()) {

         for (Element e : products) {
            String data = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");
            String urlProduct = "";
            String internalId = "";
            if (data != null) {
               urlProduct = "https://www.extrabom.com.br/" + data;
               String[] internalIdSplit = data.split("/");
               if (internalIdSplit.length > 2) {
                  internalId = internalIdSplit[2];
               }
            }

            String name = crawlName(e);
            String image = CrawlerUtils.scrapSimplePrimaryImage(e, ".carousel__box-img > table > tbody > tr > td > a > img", Collections.singletonList("src"), "https", "www.extrabom.com.br");
            Integer priceInCents = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".item-por > strong ", null, false, ',', session, null);
            boolean available = priceInCents != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(urlProduct)
               .setInternalId(internalId)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(available)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      Element pagination = this.currentDoc.selectFirst(".pagination-box a:nth-last-child(2) > span");
      String selector = pagination != null ? pagination.text() : null;
      return selector != null && selector.contains("»");
   }

   private String crawlName(Element e) {
      String scrapName = CrawlerUtils.scrapStringSimpleInfo(e, ".carousel__inner > a > table > tbody > tr > td > div.name-produto", false);
      if (scrapName == null) {
         scrapName = CrawlerUtils.scrapStringSimpleInfo(e, ".carousel__box__dados > a > table > tbody > tr > td", false);
      }
      return scrapName;

   }

}
