package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

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

import java.util.HashMap;
import java.util.Map;

public class BrasilVilanova extends CrawlerRankingKeywords {

   public BrasilVilanova(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      this.pageSize = 24;
      String url = "https://www.vilanova.com.br/Busca/Resultado/?p="
         + this.currentPage
         + "&loja=&q="
         + this.keywordEncoded
         + "&ordenacao=6&limit=24&avancado=true";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".card-product");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         int alternativePosition = 1;

         for (Element product : products) {
            String internalPid = String.valueOf(CrawlerUtils.scrapIntegerFromHtmlAttr(product, null, "id", null));
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "p.product-name > a", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".product-name", false);

            Elements variations = product.select(".item.picking");

            if (!variations.isEmpty()) {
               for (Element variation : variations) {
                  String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(variation, ".picking", "data-sku-id");
                  Integer price = CrawlerUtils.scrapIntegerFromHtmlAttr(variation, ".picking", "data-preco-por", null);
                  String imageUrl = scrapImageUrl(variation);
                  String variationName = assembleName(name, variation);
                  boolean available = scrapAvaiability(variation);

                  RankingProduct productRanking = RankingProductBuilder.create()
                     .setUrl(productUrl)
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setImageUrl(imageUrl)
                     .setName(variationName)
                     .setPriceInCents(price)
                     .setAvailability(available)
                     .setPosition(alternativePosition)
                     .build();

                  saveDataProduct(productRanking);
               }
            }

            alternativePosition++;
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

   private boolean scrapAvaiability(Element variation) {
      Element availability = variation.selectFirst(".sem-estoque");
      return availability == null;
   }

   private String assembleName(String name, Element variation) {
      String variationName = CrawlerUtils.scrapStringSimpleInfo(variation, ".caixa-com", false);
      if (variationName != null && !variationName.isEmpty()) {
         name += " " + variationName;
      }
      return name.trim();
   }

   private String scrapImageUrl(Element variation) {
      String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(variation, ".picking", "data-foto");

      if (imageUrl != null && !imageUrl.isEmpty()) {
         imageUrl = imageUrl.replace("/200x200/", "/1000x1000/");
      }

      return imageUrl;
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".qtd-produtos", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   public String getCnpj() {
      return session.getOptions().optString("cnpj");
   }

   public String getPassword() {
      return session.getOptions().optString("password");
   }

   public String getCookieLogin() {
      return session.getOptions().optString("cookie_login");
   }

   @Override
   protected Document fetchDocument(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", getCookieLogin());
      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build();
      Response response = this.dataFetcher.get(session, request);
      Document doc = Jsoup.parse(response.getBody());

      return doc;
   }

}
