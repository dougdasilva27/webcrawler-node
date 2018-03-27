package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.base.CharMatcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (27/10/2016):
 * 
 * 1) For this crawler, we have one url per each sku.
 * 
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is marketplace information in this ecommerce.
 * 
 * 4) InternalId, Pid and Price is crawl in json like this:
 * 
 * { "page": "product", "sku": "C00497LRE00", "price": 19.90, "pid": "21397" }
 * 
 * 
 * 5) The sku page identification is done simply looking the html element.
 * 
 * 6) Even if a product is unavailable, its price is not displayed, then price is null.
 * 
 * Examples: ex1 (available):
 * https://www.netfarma.com.br/mascara-para-cilios-maybelline-the-colossal-volum-express-preto-a-prova-dagua-1un.-21397
 * ex2 (unavailable): https://www.netfarma.com.br/formula-infantil-nan-supreme-2-lata-400g
 * 
 * Optimizations notes: No optimizations.
 *
 ************************************************************************************************************************************************************************************/


public class SaopauloNetfarmaCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.netfarma.com.br/";

  public SaopauloNetfarmaCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String name = crawlName(doc);
      String internalId = crawlInternalId(doc, name);
      String internalPid = internalId;
      boolean available = doc.select(".product-details__unavailable").first() == null;
      Float price = crawlPrice(doc);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = new Marketplace();
      Prices prices = crawlPrices(doc, price);

      // The url of the products has changed, if one day to stop redirecting already we have the new one
      String newUrl = crawlNewUrl(internalId);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(newUrl).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document doc) {
    return (doc.select(".product-details__code").first() != null);
  }

  private String crawlNewUrl(String internalId) {
    String url = session.getOriginalURL();

    if (internalId != null) {
      String redirectUrl = session.getRedirectedToURL(url);

      url = redirectUrl != null ? redirectUrl : url;
    }

    return url;
  }

  private CategoryCollection crawlCategories(Document doc) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = doc.select(".breadcrumb__link span");

    for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String crawlInternalId(Document doc, String name) {
    String internalId = null;

    JSONObject object = new JSONObject();
    String token = "vargoogle_tag_params=";
    Elements scripts = doc.select("script");

    for (Element e : scripts) {
      String script = e.outerHtml();

      // replace name because some names has ' that break json
      script = script.replace(name, "").replace(" ", "");

      if (script.contains(token)) {
        int x = script.indexOf(token) + token.length();
        int y = script.indexOf("};", x) + 1;

        String json = script.substring(x, y).trim();

        if (json.startsWith("{") && json.endsWith("}")) {
          try {
            object = new JSONObject(json);
          } catch (Exception e1) {
            Logging.printLogError(logger, session, CommonMethods.getStackTrace(e1));
          }
        }

        break;
      }
    }

    if (object.has("ecomm_prodid")) {
      internalId = object.getString("ecomm_prodid");
    }

    return internalId;
  }

  private String crawlName(Document document) {
    String name = null;

    // get base name
    Element elementName = document.select(".product-details__title").first();
    if (elementName != null) {
      name = elementName.text().trim();
    }

    if (name != null) {
      // get 'gramatura' attribute
      Element gramaturaElement = document.select(".product-details__measurement").first();
      if (gramaturaElement != null) {
        name = name + " " + gramaturaElement.text().trim();
      }
    }

    return name;
  }

  private Float crawlPrice(Document doc) {
    Float price = null;
    Element priceElement = doc.select(".product-details__price span[itemprop=price]").first();

    if (priceElement != null) {
      price = MathUtils.parseFloat(priceElement.ownText());
    }

    return price;
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element elementPrimaryImage = document.select("#product-gallery a").first();
    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("data-zoom-image").trim();
    }
    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements elementSecundaria = document.select("#product-gallery a");
    if (elementSecundaria.size() > 1) {
      for (int i = 1; i < elementSecundaria.size(); i++) {
        Element e = elementSecundaria.get(i);
        Element img = e.select("> img").first();

        String image = e.attr("data-zoom-image");

        if (!image.isEmpty() && !image.contains("youtube")) {
          secondaryImagesArray.put(image);
        } else if (img != null && !image.contains("youtube")) {
          secondaryImagesArray.put(img.attr("src"));
        }
      }

    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String crawlDescription(Document document) {
    String description = "";

    Element elementWarning = document.select("#detalhes.product-description").first();
    if (elementWarning != null) {
      description = description + elementWarning.outerHtml();
    }

    Element elementProductDetails = document.select("#product-tips.product-description .nano-content").last();
    if (elementProductDetails != null) {
      description = description + elementProductDetails.outerHtml();
    }

    // A character is considered to be an ISO control character if its code is in
    // the range '\u0000' through '\u001F' or in the range '\u007F' through '\u009F'
    CharMatcher desired = CharMatcher.JAVA_ISO_CONTROL.negate();

    // This happen to remove illegal characters
    return desired.retainFrom(description);
  }

  /**
   * In product page has this: Ex: 2x de R$32,45 Ex: 8x de R$ 16,12
   * 
   * Cards 3X Mastercard Diners Visa Elo 7x Amex
   * 
   * So for installments > 3, only amex have this installment But all card has 1x
   *
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      Map<Integer, Float> installmentPriceMapAmex = new HashMap<>();

      Element parcels = doc.select(".parcels b").first();

      if (parcels != null) {
        String text = parcels.text().toLowerCase().trim();

        if (text.contains("x")) {
          int x = text.indexOf("x") + 1;

          Integer installment = Integer.parseInt(text.substring(0, x).replaceAll("[^0-9]", ""));
          Float value = MathUtils.parseFloat(text.substring(x));

          if (installment > 3) {
            installmentPriceMapAmex.put(installment, value);
          } else {
            installmentPriceMap.put(installment, value);
            installmentPriceMapAmex.put(installment, value);
          }
        }
      }

      installmentPriceMapAmex.put(1, price);
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMapAmex);
    }


    return prices;
  }
}
