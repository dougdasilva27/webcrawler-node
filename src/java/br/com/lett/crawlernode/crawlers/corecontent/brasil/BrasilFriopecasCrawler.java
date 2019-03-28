package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;


/*****************************************************************************************************************************
 * Crawling notes (12/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku 2) There is no stock information for skus in
 * this ecommerce by the time this crawler was made 3) There is no marketplace in this ecommerce by
 * the time this crawler was made 4) The sku page identification is done simply looking for an
 * specific html element 5) if the sku is unavailable, it's price is not displayed. 6) There is no
 * internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku 7) For the availability we crawl a script in the html. A script
 * that has a variable named skuJson_0. It's been a common script, that contains a jsonObject with
 * certain informations about the sku. It's used only when the information needed is too complicated
 * to be crawled by normal means, or inexistent in other place. Although this json has other
 * informations about the sku, only the availability is crawled this way in this website. 8) We have
 * one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples: ex1 (available):
 * http://www.friopecas.com.br/ar-condicionado-split-piso-teto-electrolux-60000-btus-frio-220v-trifasico-r410/p
 * ex2 (unavailable):
 * http://www.friopecas.com.br/ar-condicionado-multi-split-inverter-cassete-lg-3x9000-btus-quente-frio/p
 *
 ******************************************************************************************************************************/

public class BrasilFriopecasCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.friopecas.com.br/";

  private final String INTERNALID_SELECTOR = "#___rc-p-id";
  private final String INTERNALID_SELECTOR_ATTRIBUTE = "value";

  private final String NAME_SELECTOR = ".fn.productName";
  private final String PRICE_SELECTOR = ".plugin-preco .preco-a-vista .skuPrice";

  private final String CATEGORIES_SELECTOR = ".bread-crumb ul li a";

  private final String DESCRIPTION_SELECTOR = ".product-description";
  private final String SPECS_SELECTOR = ".product-specification";

  public BrasilFriopecasCrawler(Session session) {
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

    if (isProductPage(this.session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      /*
       * *********************************** crawling data of only one product *
       *************************************/

      // crawl the skuJson
      JSONObject skuJson = crawlSkuJson(doc);

      // InternalId
      String internalId = crawlInternalId(doc);

      // Pid
      String internalPid = crawlInternalPid(doc);

      // Name
      String name = crawlName(doc);

      // Price
      Float price = crawlMainPagePrice(doc);

      // Availability
      boolean available = crawlAvailability(skuJson);

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

      // Marketplace
      Marketplace marketplace = crawlMarketplace(doc);

      // Prices
      Prices prices = crawlPrices(internalId, price, doc);

      // Ean
      JSONArray arr = CrawlerUtils.scrapEanFromVTEX(doc);
      String ean = 0 < arr.length() ? arr.getString(0) : null;

      List<String> eans = new ArrayList<>();
      eans.add(ean);

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
      product.setEans(eans);

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }



  /*******************************
   * Product page identification *
   *******************************/
  private boolean isProductPage(String url) {
    return url.endsWith("/p");
  }


  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(Document document) {
    String internalId = null;
    Element internalIdElement = document.select(INTERNALID_SELECTOR).first();

    if (internalIdElement != null) {
      internalId = internalIdElement.attr(INTERNALID_SELECTOR_ATTRIBUTE).trim();
    }

    return internalId;
  }

  private String crawlInternalPid(Document document) {
    return null;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(NAME_SELECTOR).first();

    if (nameElement != null) {
      name = sanitizeName(nameElement.text());
    }

    return name;
  }

  private Float crawlMainPagePrice(Document document) {
    Float price = null;
    Element mainPagePriceElement = document.select(PRICE_SELECTOR).first();

    if (mainPagePriceElement != null) {
      price = Float.parseFloat(mainPagePriceElement.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject skuJson) {
    if (skuJson != null && skuJson.has("skus")) {
      JSONArray skus = skuJson.getJSONArray("skus");
      if (skus.length() > 0) {
        JSONObject sku = skus.getJSONObject(0);
        if (sku.has("available")) {
          return sku.getBoolean("available");
        }
      }
    }
    return false;
  }

  private Marketplace crawlMarketplace(Document document) {
    return new Marketplace();
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select("#image a").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("href");
    } else {
      primaryImageElement = document.select("#image img").first();

      if (primaryImageElement != null) {
        primaryImage = primaryImageElement.attr("src").trim();
      }
    }

    return CommonMethods.sanitizeUrl(primaryImage);
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".thumbs li a");

    for (int i = 1; i < imagesElement.size(); i++) { // starting from index 1, because the first is
                                                     // the primary image
      String imageUrl = imagesElement.get(i).attr("zoom");
      if (imageUrl.isEmpty())
        imageUrl = imagesElement.get(i).attr("rel");
      if (!imageUrl.isEmpty())
        secondaryImagesArray.put(CommonMethods.sanitizeUrl(imageUrl));
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private ArrayList<String> crawlCategories(Document document) {
    ArrayList<String> categories = new ArrayList<>();
    Elements elementCategories = document.select(CATEGORIES_SELECTOR);

    for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first
                                                         // is the market name
      categories.add(elementCategories.get(i).text().trim());
    }

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
    Element descriptionElement = document.select(DESCRIPTION_SELECTOR).first();
    Element specsElement = document.select(SPECS_SELECTOR).first();

    if (descriptionElement != null) {
      description = description + descriptionElement.html();
    }

    if (specsElement != null) {
      description = description + specsElement.html();
    }

    return description;
  }


  private Prices crawlPrices(String internalId, Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      String url = "http://www.friopecas.com.br/productotherpaymentsystems/" + internalId;

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      Document docPrices = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

      // O preço no boleto não aparece com javascript desligado, mas aparece a porcentagem de
      // desconto
      // Assim é calculado o preço no boleto de acordo com o preço principal.
      Element bankDiscount = doc.select(".discount-highlights > p").first();
      if (bankDiscount != null) {
        Integer discount = 0;
        String text = bankDiscount.text().replaceAll("[^0-9]", "").trim();

        if (!text.isEmpty()) {
          discount = Integer.parseInt(text);
        }

        Float result = (float) (price - (price * (discount.floatValue() / 100.0)));

        Float bankTicketPrice = MathUtils.normalizeTwoDecimalPlaces(result);
        prices.setBankTicketPrice(bankTicketPrice);
      } else {
        prices.setBankTicketPrice(price);
      }

      Element cardDiscountElement = doc.select(".discount-highlights > p").last();
      Integer cardDiscount = 0;

      if (cardDiscountElement != null) {
        String text = cardDiscountElement.text().replaceAll("[^0-9]", "").trim();

        if (!text.isEmpty()) {
          cardDiscount = Integer.parseInt(text);
        }
      }

      Float cardFirstInstallment = MathUtils.normalizeTwoDecimalPlaces((float) (price - (price * (cardDiscount.floatValue() / 100.0))));

      Elements cardsElements = docPrices.select("#ddlCartao option");

      for (Element e : cardsElements) {
        String text = e.text().toLowerCase();

        if (text.contains("visa")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
          installmentPriceMap.put(1, cardFirstInstallment);

          prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

        } else if (text.contains("mastercard")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
          installmentPriceMap.put(1, cardFirstInstallment);

          prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

        } else if (text.contains("diners")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
          installmentPriceMap.put(1, cardFirstInstallment);

          prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

        } else if (text.contains("american") || text.contains("amex")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
          installmentPriceMap.put(1, cardFirstInstallment);

          prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

        } else if (text.contains("hipercard")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
          installmentPriceMap.put(1, cardFirstInstallment);

          prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);

        } else if (text.contains("credicard")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
          installmentPriceMap.put(1, cardFirstInstallment);

          prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);

        } else if (text.contains("elo")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
          installmentPriceMap.put(1, cardFirstInstallment);

          prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

        } else if (text.contains("aura")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
          installmentPriceMap.put(1, cardFirstInstallment);

          prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);

        } else if (text.contains("discover")) {
          Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(docPrices, e.attr("value"));
          installmentPriceMap.put(1, cardFirstInstallment);

          prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);

        }
      }


    }

    return prices;
  }

  private Map<Integer, Float> getInstallmentsForCard(Document doc, String idCard) {
    Map<Integer, Float> mapInstallments = new HashMap<>();

    Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
    for (Element i : installmentsCard) {
      Element installmentElement = i.select("td.parcelas").first();

      if (installmentElement != null) {
        String textInstallment = removeAccents(installmentElement.text().toLowerCase());
        Integer installment = null;

        if (textInstallment.contains("vista")) {
          continue;
        } else {
          installment = Integer.parseInt(textInstallment.replaceAll("[^0-9]", "").trim());
        }

        Element valueElement = i.select("td:not(.parcelas)").first();

        if (valueElement != null) {
          Float value = Float.parseFloat(valueElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

          mapInstallments.put(installment, value);
        }
      }
    }

    return mapInstallments;
  }

  private String removeAccents(String str) {
    str = Normalizer.normalize(str, Normalizer.Form.NFD);
    str = str.replaceAll("[^\\p{ASCII}]", "");
    return str;
  }



  /**************************
   * Specific manipulations *
   **************************/

  private String sanitizeName(String name) {
    return name.replace("'", "").replace("’", "").trim();
  }

  /**
   * Get the script having a json with the availability information
   * 
   * @return
   */
  private JSONObject crawlSkuJson(Document document) {
    Elements scriptTags = document.getElementsByTag("script");
    JSONObject skuJson = null;

    for (Element tag : scriptTags) {
      for (DataNode node : tag.dataNodes()) {
        if (tag.html().trim().startsWith("var skuJson_0 = ")) {

          skuJson = new JSONObject(node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1]
              + node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]);

        }
      }
    }

    return skuJson;
  }

}
