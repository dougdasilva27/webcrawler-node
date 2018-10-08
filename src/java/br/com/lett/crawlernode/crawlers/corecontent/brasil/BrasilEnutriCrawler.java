package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.http.impl.cookie.BasicClientCookie;
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
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 20/08/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilEnutriCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.enutri.com.br/";

  public BrasilEnutriCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    BasicClientCookie cookie = new BasicClientCookie("loja", "base");
    cookie.setDomain(".www.enutri.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = crawlInternalPid(doc);
      String name = crawlName(doc);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price, doc);
      boolean available = crawlAvailability(doc);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select("input[name=product]").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select("input[name=product]").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    }

    return internalId;
  }

  private String crawlInternalPid(Document document) {
    String internalPid = null;
    Element codElement = document.select("#display_product_name span").first();

    if (codElement != null) {
      internalPid = CommonMethods.getLast(codElement.ownText().replace("(", "").replace(")", "").split("\\.")).trim();
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("#display_product_name").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document doc) {
    Float price = null;
    Element elementPrice = doc.selectFirst(".product-infos .price-box .regular-price .price");
    if (elementPrice == null) {
      elementPrice = doc.selectFirst(".product-infos .price-box .special-price .price");
    }

    Element elementSpecialPrice = doc.selectFirst(".product-infos .product-discount .price");

    if (elementPrice != null) {
      price = MathUtils.parseFloatWithComma(elementPrice.text());
    } else if (elementSpecialPrice != null) {
      price = MathUtils.parseFloatWithComma(elementSpecialPrice.ownText());
    }

    return price;
  }

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element elementPrimaryImage = doc.selectFirst(".product-img-box li > a");

    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("href");
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select(".product-img-box li" + CrawlerUtils.CSS_SELECTOR_IGNORE_FIRST_CHILD + " > a");
    for (Element e : images) {
      secondaryImagesArray.put(e.attr("href"));
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumbs li:not(.home):not(.product)");

    for (Element e : elementCategories) {
      String cat = e.ownText().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element elementShortdescription = doc.selectFirst(".short-description");

    if (elementShortdescription != null) {
      description.append(elementShortdescription.html());
    }

    Element elementDescription = doc.selectFirst(".product-view > .product-collateral");

    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    return description.toString();
  }

  private boolean crawlAvailability(Document doc) {
    return !doc.select(".availability.in-stock").isEmpty();
  }

  /**
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Element bank = doc.selectFirst(".product-infos .product-discount .price");

      if (bank != null) {
        Float discount = MathUtils.parseFloatWithComma(bank.ownText());

        if (discount != null) {
          prices.setBankTicketPrice(discount);
        } else {
          prices.setBankTicketPrice(price);
        }
      } else {
        prices.setBankTicketPrice(price);
      }

      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      Elements installmentsElements = doc.select("#productPlots ul li");

      for (Element e : installmentsElements) {
        Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, false);
        installmentPriceMap.put(pair.getFirst(), pair.getSecond());
      }

      if (!installmentPriceMap.isEmpty()) {
        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      }
    }

    return prices;
  }

}
