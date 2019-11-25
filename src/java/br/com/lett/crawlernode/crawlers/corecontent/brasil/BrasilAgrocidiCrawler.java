package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

public class BrasilAgrocidiCrawler extends Crawler {

   public BrasilAgrocidiCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".principal div[data-produto-id]", "data-produto-id");
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "span[itemprop=sku]", false);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".principal .nome-produto", false);
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".principal .preco-produto .preco-promocional", null, false, ',', session);
         Prices prices = crawlPrices(doc, price);
         boolean available = !doc.select(".principal .disponivel").isEmpty();
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li:not(:first-child) a");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc,
               ".produto-thumbs:not(.thumbs-horizontal) #carouselImagem ul.miniaturas:first-child  a",
               Arrays.asList("data-imagem-grande"), "https:", "cdn.awsli.com.br");

         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc,
               ".produto-thumbs:not(.thumbs-horizontal) #carouselImagem ul.miniaturas:first-child  a",
               Arrays.asList("data-imagem-grande"), "https:", "cdn.awsli.com.br", primaryImage);

         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(
               ".row-fluid:not(#comentarios-container) > div.span12  .abas-custom .tab-content"));

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrice(price)
               .setPrices(prices)
               .setAvailable(available)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setMarketplace(new Marketplace())
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Prices crawlPrices(Document doc, Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installments = new HashMap<>();
         installments.put(1, price);
         prices.setBankTicketPrice(price);
         prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".principal .preco-venda.titulo", null, true, ',', session));

         Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".principal .preco-produto  .preco-parcela", doc, false, "x");
         if (!pair.isAnyValueNull()) {
            installments.put(pair.getFirst(), pair.getSecond());
         }

         prices.insertCardInstallment(Card.VISA.toString(), installments);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
         prices.insertCardInstallment(Card.DINERS.toString(), installments);
         prices.insertCardInstallment(Card.HIPERCARD.toString(), installments);
         prices.insertCardInstallment(Card.AMEX.toString(), installments);
         prices.insertCardInstallment(Card.ELO.toString(), installments);
      }

      return prices;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".secao-principal .produto").isEmpty();
   }

}
