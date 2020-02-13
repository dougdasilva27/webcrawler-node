package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilMpozenatoCrawler extends Crawler {

   public BrasilMpozenatoCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (isProductPage(doc)) {
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#hdnProdutoId", "value");
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#hdnProdutoVarianteId", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.fbits-produto-nome.prodTitle.title", false);
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".produtoInfo-precos .precoPor", null, false, ',', session);
         Prices prices = scrapPrices(doc, price);
         boolean available = doc.selectFirst(".produto-comprar:not(.hide) .btn-comprar") != null;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".fbits-breadcrumb.bread ol li", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#galeria > li:nth-child(1) > a", Arrays.asList("data-image"), "https",
               "www.mpozenato.com.br");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#galeria > li a", Arrays.asList("data-image"), "https",
               "www.mpozenato.com.br", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".fbits-produto-informacoes-extras"));
         RatingsReviews ratingsReviews = scrapRating(internalId, doc);

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
               .setRatingReviews(ratingsReviews)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private Prices scrapPrices(Document doc, Float price) {
      Prices prices = new Prices();
      if (price != null) {

         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         Integer installmentQuantity = CrawlerUtils.scrapIntegerFromHtml(doc, ".produtoInfo-precos .precoParcela .fbits-quantidadeParcelas", false,
               null);
         Float installmentPrice = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".produtoInfo-precos .precoParcela .fbits-parcela", null, false, ',',
               session);
         Double bankTicketPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".produtoInfo-precos .precoVista .fbits-boleto-preco", null, false, ',',
               session);
         Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".produtoInfo-precos .precoDe", null, false, ',', session);

         prices.setBankTicketPrice(bankTicketPrice);
         prices.setPriceFrom(priceFrom);

         installmentPriceMap.put(1, price);
         installmentPriceMap.put(installmentQuantity, installmentPrice);
         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

      }
      return prices;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".content.produto").isEmpty();
   }

   private RatingsReviews scrapRating(String internalId, Document doc) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "103485", logger);
      return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
   }
}
