package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (20/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 * 
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking the html element.
 * 
 * 5) Even if a product is unavailable, its price is displayed.
 * 
 * 6) There is no internalPid for skus in this ecommerce. The internalPid must be a number that is
 * the same for all the variations of a given sku.
 * 
 * 7) There is no categories in product page in this market.
 * 
 * Examples: ex1 (available):
 * http://www.unicaarcondicionado.com.br/ar-condicionado-split-hi-wall-carrier-x-power-inverter-9000-btus-frio-220v.html
 * ex2 (unavailable):
 * http://www.unicaarcondicionado.com.br/ar-condicionado-split-hi-wall-elgin-compact-9000-btus-frio-110v.html
 *
 * Optimizations notes: No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilUnicaarcondicionadoCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.unicaarcondicionado.com.br/";

  public BrasilUnicaarcondicionadoCrawler(Session session) {
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

      // InternalId
      String internalId = crawlInternalId(doc);

      // Pid
      String internalPid = crawlInternalPid(doc);

      // Name
      String name = crawlName(doc);

      // Prices
      Prices prices = crawlPrices(doc);

      // Price
      Float price = crawlMainPagePrice(doc, prices);

      // Availability
      boolean available = crawlAvailability(doc);

      // Categories
      ArrayList<String> categories = crawlCategories(doc);
      String category1 = getCategory(categories, 0);
      String category2 = getCategory(categories, 1);
      String category3 = getCategory(categories, 2);

      // Primary image
      String primaryImage = crawlPrimaryImage(doc);

      // Secondary images
      String secondaryImages = crawlSecondaryImages(doc);

      // Description
      String description = crawlDescription(doc);

      // Stock
      Integer stock = null;

      // Marketplace map
      Map<String, Float> marketplaceMap = crawlMarketplace(doc);

      // Marketplace
      Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);

      // Creating the product
      Product product = new Product();

      product.setUrl(this.session.getOriginalURL());
      product.setInternalId(internalId);
      product.setInternalPid(internalPid);
      product.setName(name);
      product.setPrice(price);
      product.setPrices(prices);
      product.setAvailable(available);
      product.setCategory1(category1);
      product.setCategory2(category2);
      product.setCategory3(category3);
      product.setPrimaryImage(primaryImage);
      product.setSecondaryImages(secondaryImages);
      product.setDescription(description);
      product.setStock(stock);
      product.setMarketplace(marketplace);

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }



  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document doc) {
    Element productPage = doc.select(".product-view").first();

    if (productPage != null)
      return true;
    return false;
  }


  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(Document document) {
    String internalId = null;
    Element internalIdElement = document.select("input[name=product]").first();

    if (internalIdElement != null) {
      internalId = internalIdElement.attr("value").toString().trim();
    }

    return internalId;
  }

  private String crawlInternalPid(Document document) {
    String internalPid = null;

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(".product-name h1").first();

    if (nameElement != null) {
      name = nameElement.text().toString().trim();
    }

    return name;
  }

  private Float crawlMainPagePrice(Document document, Prices prices) {
    Float price = null;
    Element mainPagePriceElement = document.select(".box-buy .regular-price span").first();

    if (mainPagePriceElement != null) {
      price = Float.parseFloat(mainPagePriceElement.text().toString().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
    }

    if (price == null && prices.getCardInstallmentValue(Card.VISA.toString(), 1) != null) {
      price = prices.getCardInstallmentValue(Card.VISA.toString(), 1).floatValue();
    }

    return price;
  }

  private Prices crawlPrices(Document document) {
    Prices prices = new Prices();
    Map<Integer, Float> installments = new TreeMap<>();

    // bank ticket
    Element bankTicketPriceElement = document.select(".box-buy .price-info .price-box .avista .price").first();
    if (bankTicketPriceElement != null) {
      Float bankTicketPrice = Float.parseFloat(bankTicketPriceElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
      prices.setBankTicketPrice(bankTicketPrice);
    } else {
      Element firstInstallment = document.selectFirst(".well-parcelamento div.row div.col-xs-6 div");

      if (firstInstallment != null) {
        String line = firstInstallment.text().trim(); // 1x de R$2.399,00 sem juros
        int indexOfX = line.indexOf('x') + 1;
        prices.setBankTicketPrice(MathUtils.parseFloatWithComma(line.substring(indexOfX, line.length())));
      }
    }

    // card payment options
    Elements installmentElements = document.select(".well-parcelamento div.row div.col-xs-6 div");
    for (Element installmentElement : installmentElements) {
      String line = installmentElement.text().trim(); // 1x de R$2.399,00 sem juros
      Integer installmentNumber = null;
      Float installmentPrice = null;

      // parsing the installment number
      int indexOfX = line.indexOf('x') + 1;
      String installmentNumberString = line.substring(0, indexOfX); // "1x"
      installmentNumber = Integer.parseInt(MathUtils.parseNumbers(installmentNumberString).get(0));

      // parsing the installment price
      String installmentPriceString = line.substring(indexOfX, line.length()); // " de R$2.399,00 sem juros"
      installmentPrice = Float.parseFloat(installmentPriceString.replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

      // the payment options are the same for all cards brands
      installments.put(installmentNumber, installmentPrice);
    }

    // insert the map of installments and installments values for each card brand on the Prices object
    // the payment options are the same for all the card brands
    prices.insertCardInstallment(Card.VISA.toString(), installments);
    prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
    prices.insertCardInstallment(Card.DINERS.toString(), installments);
    prices.insertCardInstallment(Card.ELO.toString(), installments);
    prices.insertCardInstallment(Card.HIPERCARD.toString(), installments);
    prices.insertCardInstallment(Card.AMEX.toString(), installments);

    return prices;
  }

  private boolean crawlAvailability(Document document) {
    Element notifyMeElement = document.select(".availability.in-stock").first();

    if (notifyMeElement != null) {
      return true;
    }

    return false;
  }

  private Map<String, Float> crawlMarketplace(Document document) {
    return new HashMap<String, Float>();
  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
    return new Marketplace();
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".product-image-gallery img#image-main").first();

    if (primaryImageElement != null) {
      if (primaryImageElement.hasAttr("data-zoom-image")) {
        primaryImage = primaryImageElement.attr("data-zoom-image").trim();

        if (!primaryImage.contains("unica"))
          primaryImage = primaryImageElement.attr("src").trim();

      } else {
        primaryImage = primaryImageElement.attr("src").trim();
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".product-image-gallery img");

    for (int i = 1; i < imagesElement.size() - 1; i++) { // starting from index 1, because the first is the primary image and finish before the last
                                                         // image because it's the primary image too
      if (imagesElement.get(i).hasAttr("data-zoom-image")) {
        if (imagesElement.get(i).attr("data-zoom-image").contains("unica")) {
          secondaryImagesArray.put(imagesElement.get(i).attr("data-zoom-image").trim());
        } else {
          secondaryImagesArray.put(imagesElement.get(i).attr("src").trim());
        }
      } else {
        secondaryImagesArray.put(imagesElement.get(i).attr("src").trim());
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private ArrayList<String> crawlCategories(Document document) {
    ArrayList<String> categories = new ArrayList<String>();

    return categories;
  }

  private String getCategory(ArrayList<String> categories, int n) {
    if (n < categories.size()) {
      return categories.get(n);
    }

    return "";
  }

  private String crawlDescription(Document document) {
    String description = "";
    Element descriptionElement = document.select(".box-collateral.box-description").first();
    Element specElement = document.select(".box-collateral.box-additional").first();

    if (descriptionElement != null)
      description = description + descriptionElement.html();
    if (specElement != null)
      description = description + specElement.html();

    return description;
  }

}
