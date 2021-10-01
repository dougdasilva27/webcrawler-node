package br.com.lett.crawlernode.crawlers.ranking.keywords.equador;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EquadorFybecaCrawler extends CrawlerRankingKeywords {

   public EquadorFybecaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      String url = "https://www.fybeca.com/FybecaWeb/pages/search-results.jsf?s=" + this.arrayProducts.size() + "&pp=25&q=" + this.keywordEncoded + "&cat=-1&b=-1&ot=0";
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("ul.products-list > li[data-id]");

      if (products != null) {
         for (Element product : products) {
            String internalPid = product.attr("data-id");
            String productUrl = "https://www.fybeca.com" + CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "a.name", "href");
            String name = product.attr("data-name");
            String imageUrl = "https://www.fybeca.com" + CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "img.productImage", "src");
            imageUrl = imageUrl.replace("../..", "");

            String priceElement = product.attr("data-price");
            int price = formatPrice(priceElement);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);
            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private int formatPrice(String priceElement) {
      int result = 0;
      String priceStr = "";
      String regex = ":([0-9]*?\\.[0-9]*?),";

      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(priceElement);
      if (matcher.find()) {
         priceStr = matcher.group(1);
      }

      if (priceStr != null && !priceStr.equals("")) {
         result = MathUtils.parseInt(priceStr);
      }

      return result;
   }

   @Override
   protected boolean hasNextPage(){
      return true;
   }
}
