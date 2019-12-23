package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilBelatintasCrawler extends Crawler {
   public BrasilBelatintasCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalPid = scrapInternalPid(doc);
         String internalId = scrapInternalId(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".produtoInfo-title h1", false);
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#fbits-forma-pagamento #divFormaPagamento .precoPor", null, false, ',', session);
         Prices prices = crawlPrices(doc, price);
         boolean available = !doc.select("meta[property*=\"product:availability\"]").isEmpty();
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#fbits-breadcrumb li", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".fbits-componente-imagem #zoomImagemProduto", Arrays.asList("src"), "https",
               "www.supermercadodospets.com.br");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".jcarousel #galeria li:not(:first-child) a img", Arrays.asList("src"), "https", "www.belatintas.com.br", primaryImage);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".infoProd .paddingbox"));
         RatingsReviews ratingsReviews = scrapRating(internalId, doc);
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
               .setRatingReviews(ratingsReviews)
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
         prices.setBankTicketPrice(CrawlerUtils.scrapFloatPriceFromHtml(doc, "#fbits-forma-pagamento .precoVista .fbits-boleto-preco", null, false, ',', session));
         prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, "#fbits-forma-pagamento #divFormaPagamento .precoPor", null, true, ',', session));

         Elements intallmentsElements = doc.select(".details-content p");
         for (Element e : intallmentsElements) {
            Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, false);

            if (!pair.isAnyValueNull()) {
               installments.put(pair.getFirst(), pair.getSecond());
            }
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

   private RatingsReviews scrapRating(String internalId, Document doc) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "78142", logger);
      return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".content.produto").isEmpty();
   }

   private String scrapInternalPid(Document doc) {
      String internalPid = null;

      String pid = CrawlerUtils.scrapStringSimpleInfo(doc, ".produtoInfo-title .fbits-sku", false);
      if (pid != null) {
         internalPid = CommonMethods.getLast(pid.split(" "));
      }

      return internalPid;
   }

   private String scrapInternalId(Document doc) {
      String internalId = null;

      String idAtt = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div #hdnProdutoId", "value");
      if (idAtt != null) {
         internalId = idAtt;
      }

      return internalId;
   }


}
