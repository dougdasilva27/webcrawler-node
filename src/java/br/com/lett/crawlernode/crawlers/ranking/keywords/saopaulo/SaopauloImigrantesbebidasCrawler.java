package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SaopauloImigrantesbebidasCrawler extends CrawlerRankingKeywords {

   private static final String BASE_URL = "www.imigrantesbebidas.com.br";

   public SaopauloImigrantesbebidasCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      this.pageSize = 20;
      String url = "https://www.imigrantesbebidas.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url, cookies);

      Elements products = this.currentDoc.select(".categoryListing__list  .productRow__item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0)
            setTotalProducts();

         for (Element product : products) {
            String productUrl = CrawlerUtils.scrapUrl(product, ".product__link", "href", "https", BASE_URL);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".productItem__image > img", List.of("data-src"), "https", BASE_URL);
            String internalId = scrapInternalId(product, imageUrl);
            String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".productItem__name", "title");
            boolean isAvailable = product.selectFirst("article") != null && !product.selectFirst("article").classNames().contains("productItem--out-of-stock");
            Integer price = isAvailable ? CrawlerUtils.scrapPriceInCentsFromHtml(product, ".productItem__price .productItem__price--value", null, false, ',', session, null) : null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(name)
               .setImageUrl(imageUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

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
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".pagination > li:nth-last-child(1) > a", true, 0);
      super.setTotalProducts();
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentPage < totalProducts;
   }

   private String scrapInternalId(Element product, String imageUrl) {
      String id = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".frmCartQuantity input[name=\"products_id\"]", "value");
      if (id == null) {
         String regex = "full\\/([^\\/\\-]+)\\-";
         Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         Matcher matcher = pattern.matcher(imageUrl);
         if (matcher.find()) {
            return matcher.group(1);
         }
      }
      return id;
   }
}
