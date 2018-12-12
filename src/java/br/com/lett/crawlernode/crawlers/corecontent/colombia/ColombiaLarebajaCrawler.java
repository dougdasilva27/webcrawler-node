package br.com.lett.crawlernode.crawlers.corecontent.colombia;

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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class ColombiaLarebajaCrawler extends Crawler{

  public ColombiaLarebajaCrawler(Session session) {
    super(session);
    // TODO Auto-generated constructor stub
  }
  

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".descripciones h1", true);
      
      Float price = CrawlerUtils.scrapSimplePriceFloat(doc, ".pricened", false);
      Prices prices = crawlPrices(price, doc);
      boolean available = crawlAvailability(doc);
      
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".caption-img > img", Arrays.asList("src"), "http:", "s7d2.scene7.com");
      String secondaryImages =
          CrawlerUtils.scrapSimpleSecondaryImages(doc, ".caption-img > img", Arrays.asList("src"), "http:", "s7d2.scene7.com", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".wrap-text-descriptions"));

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {    
    return !doc.select(".product_detail").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element id = doc.selectFirst("input.btn-add-cart");
    if (id != null) {
      internalId = id.val();
    }

    return internalId;
  }
  
  private boolean crawlAvailability(Document doc) {    
    return doc.select("btn btn-primary btn-block") != null;
  }

  public static CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumb-nav a");

    for (Element e : elementCategories) {
      categories.add(e.text().replace(">", "").trim());
    }

    Element lastCategory = document.selectFirst(".breadcrumb-nav h3");
    if (lastCategory != null) {
      categories.add(lastCategory.ownText().replace("/", "").trim());
    }

    return categories;
  }

  /**
   * In the time when this crawler was made, this market hasn't installments informations
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);

      Map<Integer, Float> installmentPriceMapShop = new HashMap<>();
      installmentPriceMapShop.put(1, price);

      prices.setPriceFrom(CrawlerUtils.scrapSimplePriceDouble(doc, ".price-selector .nule-price", false));

      Element discounts = doc.selectFirst(".active-offer .red");
      if (discounts != null) {
        if (prices.getPriceFrom() == null) {
          prices.setPriceFrom(MathUtils.normalizeNoDecimalPlaces(price.doubleValue()));
        }

        String text = discounts.ownText();

        String[] tokens = text.split(",");
        Float normalCardDiscount = 0f;
        Float shopCardDiscount = 0f;

        if (tokens.length > 1) {
          String shopDiscount = tokens[0].replaceAll("[^0-9]", "");
          String cardDiscount = tokens[1].replaceAll("[^0-9]", "");

          if (!shopDiscount.isEmpty()) {
            shopCardDiscount = Integer.parseInt(shopDiscount) / 100f;
          }

          if (!cardDiscount.isEmpty()) {
            normalCardDiscount = Integer.parseInt(cardDiscount) / 100f;
          }
        } else {
          String cardsDiscount = text.replaceAll("[^0-9]", "");

          if (!cardsDiscount.isEmpty()) {
            normalCardDiscount = Integer.parseInt(cardsDiscount) / 100f;
            shopCardDiscount = normalCardDiscount;
          }
        }

        installmentPriceMap.put(1, MathUtils.normalizeNoDecimalPlaces(price - (price * normalCardDiscount)));
        installmentPriceMapShop.put(1, MathUtils.normalizeNoDecimalPlaces(price - (price * shopCardDiscount)));
      }

      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMapShop);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }

  
}
