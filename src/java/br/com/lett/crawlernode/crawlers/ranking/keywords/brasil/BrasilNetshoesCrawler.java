package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilNetshoesCrawler extends CrawlerRankingKeywords {

   public BrasilNetshoesCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 42;

      this.log("Página " + this.currentPage);

      String url = "https://www.netshoes.com.br/busca?nsCat=Natural&q=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("#item-list .wrapper a");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProductsCarrefour();
         }

         for (Element e : products) {
            String internalPid = crawlInternalPid(e);
            String productUrl = CrawlerUtils.scrapUrl(e, null, "href", "https:", "www.netshoes.com.br");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".item-card__description__product-name > span", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".item-card__images__image-link > img", "data-src");
            int price = crawlPrice(internalPid);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   protected void setTotalProductsCarrefour() {
      Element totalElement = this.currentDoc.select(".items-info .block").first();

      if (totalElement != null) {
         String text = totalElement.ownText();

         if (text.contains("de")) {
            String total = text.split("de")[1].replaceAll("[^0-9]", "").trim();

            if (!total.isEmpty()) {
               this.totalProducts = Integer.parseInt(total);
            }
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalPid(Element e) {
      return e.attr("parent-sku");
   }

   private int crawlPrice(String internalPid) {
      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.netshoes.com.br/refactoring/tpl/frdmprcs/" + internalPid + "/lazy/b")
         .build();
      Document doc = Jsoup.parse(dataFetcher.get(session, request).getBody());

      String productPrice = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".pr", "data-final-price");
      int price = 0;
      if (productPrice != null && !productPrice.isEmpty()) {
         price = Integer.parseInt(productPrice);
      }
      return price;
   }
}
