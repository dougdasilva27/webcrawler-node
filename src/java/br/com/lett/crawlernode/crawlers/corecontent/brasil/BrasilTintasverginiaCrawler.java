package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilTintasverginiaCrawler extends Crawler {
  
  private static final String HOST = "www.tintasverginia.com.br";

  public BrasilTintasverginiaCrawler(Session session) {
     super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
     super.extractInformation(doc);
     List<Product> products = new ArrayList<>();

     if (isProductPage(doc)) {
        Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

        String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-view [name=product]", "value");
        String internalPid = internalId;
        String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name > h1", true);
        Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#product-price-" + internalId+ " .price", null, false, ',', session);
        Prices prices = scrapPrices(doc, price);
        CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs ul > li:not(:last-child)", true);
        String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#image > a", Arrays.asList("href"), "https", HOST);
        String secondaryImages = scrapSecondaryImages(doc, ".product-img-box .more-views li > a", Arrays.asList("href"), "https", HOST, primaryImage);
        String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".product-collateral .abas", ".product-collateral .desc"));
        boolean available = doc.selectFirst(".esgotado") == null;
        
        // Site has reviews but it doesn't have any rated product
        RatingsReviews ratingReviews = null;

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
              .setRatingReviews(ratingReviews)
              .build();


         products.add(product);
        
     } else {
        Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
     }

     return products;
  }

  private boolean isProductPage(Document doc) {
     return doc.selectFirst(".catalog-product-view") != null;
  }
  
  /**
   * Copy method from CrawlerUtils that checks the end on primaryImage url to verify if the secondary image capure is not
   * the same image as the primary with different path.
   * 
   * @param doc Jsoup document
   * @param cssSelector selector to query on Jsoup
   * @param attributes list of attributes to search on element
   * @param protocol page URL protocol
   * @param host market page URL host
   * @param primaryImage url of the primary image
   * @return
   */
  public static String scrapSecondaryImages(Document doc, String cssSelector, List<String> attributes, String protocol, String host, String primaryImage) {
   String secondaryImages = null;
   JSONArray secondaryImagesArray = new JSONArray();
   String imageFinalPath = primaryImage != null ? CommonMethods.getLast(primaryImage.split("/")) : null;

   Elements images = doc.select(cssSelector);
   for (Element e : images) {
      String image = CrawlerUtils.sanitizeUrl(e, attributes, protocol, host);

      if ((primaryImage == null || !primaryImage.equals(image)) && image != null && (imageFinalPath == null || !image.contains(imageFinalPath))) {
         secondaryImagesArray.put(image);
      }
   }

   if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
   }

   return secondaryImages;
}


  private Prices scrapPrices(Document doc, Float price) {
     Prices prices = new Prices();

     if (price != null) {
        prices.setBankTicketPrice(CrawlerUtils.scrapFloatPriceFromHtml(doc, ".thebox .preco-a-vista .price", null, false, ',', session));
       
        Map<Integer, Float> installmentPriceMap = new TreeMap<>();
        installmentPriceMap.put(1, price);
        
        Elements installmentElements = doc.select(".thebox .parcelamento ul > li");
        for(Element installmentElement : installmentElements) {
          Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, installmentElement, false, "x", "juros", false, ',');
          
          if (!pair.isAnyValueNull()) {
            installmentPriceMap.put(pair.getFirst(), pair.getSecond());
          }
        }
        
        List<Card> cards = Arrays.asList(Card.VISA, Card.MASTERCARD, Card.AMEX, Card.DINERS, Card.HIPERCARD, Card.AURA, Card.ELO);
        for(Card card : cards) {
          prices.insertCardInstallment(card.toString(), installmentPriceMap);
        }
     }

     return prices;
  }
}
