package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date: 28/11/2016
 * 
 * 1) Only one sku per page.
 * 
 * Price crawling notes: 1) We couldn't find any sku with status available when writing this
 * crawler. 2) There is no bank slip (boleto bancario) payment option. 3) There is no installments
 * for card payment. So we only have 1x payment, and for this value we use the cash price crawled
 * from the sku page. (nao existe divisao no cartao de credito). 4) In this market has two others
 * possibles markets, City Market = 305 and Fresko = 14 5) In page of product, has all physicals
 * stores when it is available.
 * 
 * Url example:
 * http://www.lacomer.com.mx/lacomer/doHome.action?succId=14&pasId=63&artEan=7501055901401&ver=detallearticulo&opcion=detarticulo
 * 
 * pasId -> Lacomer succId -> Tienda Lomas Anahuac (Mondelez choose)
 * 
 * @author Gabriel Dornelas
 *
 */
public class MexicoLacomerCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.lacomer.com.mx/";

  public MexicoLacomerCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
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
      Prices prices = crawlPrices(price);
      boolean available = crawlAvailability(price);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = crawlMarketplace(doc);
      List<String> eans = scrapEans(doc);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private List<String> scrapEans(Document doc) {
    Element upc = doc.selectFirst("#artean");
    List<String> eans = new ArrayList<>();
    if (upc != null) {
      eans.add(upc.attr("value"));
    }

    return eans;
  }

  private boolean isProductPage(Document doc) {
    return doc.select("div.product-detail-content").first() != null;
  }

  private String crawlInternalId(Document document) {
    Element internalIdElement = document.select("input[name=artEan]").first();
    if (internalIdElement != null) {
      return internalIdElement.attr("value").trim();
    }
    return null;
  }

  /**
   * There is no internalPid.
   * 
   * @param document
   * @return
   */
  private String crawlInternalPid(Document document) {
    String internalPid = null;

    return internalPid;
  }

  private String crawlName(Document document) {
    Element nameElement = document.select(".txt-product-name[itemprop=name]").first();
    if (nameElement != null) {
      return nameElement.text().trim();
    }
    return null;
  }

  private Float crawlPrice(Document document) {
    Float price = null;
    Element skuPriceOffersElement = document.select("div.product-detail-content div[itemprop=offers]").first();
    if (skuPriceOffersElement != null) {
      Element priceElement = skuPriceOffersElement.select("span").last();

      if (priceElement != null) {
        String text = priceElement.text();
        if (!text.isEmpty()) {
          Pattern regex = Pattern.compile("(\\$)(\\s*\\d+[\\.]\\d+)", Pattern.CASE_INSENSITIVE);
          Matcher matcher = regex.matcher(text);
          if (matcher.find()) {
            try {
              price = Float.parseFloat(matcher.group(2));
            } catch (NullPointerException | NumberFormatException ex) {
              price = null;
            }
          }
        }
      }
    }

    if (price != null && price.compareTo(0.0f) == 0) {
      price = null;
    }

    return price;
  }

  private boolean crawlAvailability(Float price) {
    return price != null;
  }

  private Marketplace crawlMarketplace(Document document) {
    return new Marketplace();
  }

  private String crawlPrimaryImage(Document document) {
    Element primaryImageElement = document.select(".img-product-detail.centerImg").first();
    if (primaryImageElement != null) {
      String dataZoomImageUrl = primaryImageElement.attr("data-zoom-image").trim();
      if (!dataZoomImageUrl.isEmpty()) {
        if (!dataZoomImageUrl.contains("empty")) {
          return dataZoomImageUrl;
        }
      } else {
        String srcUrl = primaryImageElement.attr("src").trim();
        if (!srcUrl.isEmpty()) {
          return srcUrl;
        }
      }
    }

    return null;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select("ul.imageslist .td-product-image a .img-product-detail");

    for (int i = 1; i < images.size(); i++) { // first image is the primary Image
      Element e = images.get(i);
      String image = e.attr("data-zoom-image").trim();

      if (image.isEmpty() || image.equals("#")) {
        image = e.attr("src").trim();
      }

      secondaryImagesArray.put(image);
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();

    Elements elementCategories = document.select(".breadcrumb li a");
    for (int i = 1; i < elementCategories.size(); i++) { // first item is the home page
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();

    Element descriptionContentElement = document.select("[itemprop=description]").first().parent();
    if (descriptionContentElement != null) {
      description.append(descriptionContentElement.html());
    }

    return description.toString();
  }

  /**
   * There is no bankSlip price.
   * 
   * There is no card payment options, other than cash price. So for installments, we will have only
   * one installment for each card brand, and it will be equals to the price crawled on the sku main
   * page.
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }

}
