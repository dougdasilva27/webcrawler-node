package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.apache.commons.lang.WordUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilDivvinoCrawler extends CrawlerRankingKeywords {

   public BrasilDivvinoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      this.pageSize = 12;

      this.log("Página " + this.currentPage);

      int pageNumber = 0;

      if (currentPage != 1) {
         pageNumber = pageSize * (this.currentPage - 1);
      }

      String url = "https://www.divvino.com.br/busca?No="+ pageNumber +"&Nrpp=" + this.pageSize + "&Ntt=" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".container_products > div");

      Integer totalProducts = crawlTotalProducts(this.currentDoc);

      if (products.size() >= 1) {
         if (this.totalProducts == 0) {
            this.totalProducts = totalProducts;
            if (this.totalProducts > 0) {
               this.log("Total: " + this.totalProducts);
            }
         }

         for (Element e : products) {

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,"[data-product]","data-product");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,"a","href");
            String name = WordUtils.capitalizeFully(CrawlerUtils.scrapStringSimpleInfo(e, ".productName", false));
            String image = CrawlerUtils.scrapSimplePrimaryImage(e, ".img_responsive.img_center.prod_box_img", List.of("src"), "https", "statics.divvino.com.br");
            String inStockInfo = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".lazy_discount_stamp", "data-instock");
            boolean isAvailable = inStockInfo != null && inStockInfo.equals("true");
            int priceInCents = crawlPrice(e, internalId);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) break;

         }
      } else {
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private Integer crawlTotalProducts(Document doc) {
      Elements scripts = doc.select(".col.s80.conteudo_princ_content > script");
      Integer totalProducts = 0;

      for (Element script : scripts) {
         if (script.dataNodes().size() > 0) {
            String scriptContent =  script.dataNodes().get(0).toString();
            if (scriptContent.contains("verifyTotalRecords")) {
               String regex = "verifyTotalRecords\\('([0-9]+)+'\\)";
               Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
               Matcher matcher = pattern.matcher(scriptContent);

               if (matcher.find()) {
                  String totalProductsStr = matcher.group(1);
                  totalProducts = MathUtils.parseInt(totalProductsStr);
                  totalProducts = totalProducts != null ? totalProducts : 0;
                  break;
               }
            }
         }
      }
      return totalProducts;
   }

   private int crawlPrice(Element e, String internalId) {
      String url = "https://www.divvino.com.br/liquor/ajax/priceProductBoxAjax.jsp?productId=" + internalId + "&skuId=" + internalId + "&displayListPrice=true";

      Document priceDoc = fetchDocument(url);
      String centsStr = CrawlerUtils.scrapStringSimpleInfo(priceDoc, ".prod_price_value > .prod_price_value > .prod_price_cen.prod_price_cen_sale_price.price_text", false);
      centsStr = centsStr != null ? centsStr.replace(",", "") : null;
      String priceIntStr = CrawlerUtils.scrapStringSimpleInfo(priceDoc, ".prod_price_value > .prod_price_value > .prod_price_int.prod_price_int_sale_price.price_text", false);
      Integer priceInt = priceIntStr != null ? MathUtils.parseInt(priceIntStr) : null;
      Integer cents = centsStr != null ? MathUtils.parseInt(centsStr) : null;
      int priceInCents = priceInt != null ? priceInt * 100 : 0;
      priceInCents += cents != null ? cents : 0;

      return priceInCents;
   }
}
