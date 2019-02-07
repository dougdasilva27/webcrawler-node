package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (02/08/2016):
 * 
 * 1) For this crawler, we have one url per mutiple skus.
 * 
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for url.
 * 
 * 5) Is used a script in html to get name, price, availability and internalIDS.
 * 
 * 6) Even if a product is unavailable, its price is not displayed. Only the price for payment via
 * 'boleto' is displayed.
 * 
 * 7) When price is not displayed, in script the price is 9999876.0, then is not crawled.
 * 
 * 8) There is internalPid for skus in this ecommerce. The internalPid is a number that is the same
 * for all the variations of a given sku.
 * 
 * 9) The primary image is the first image in the secondary images selector.
 * 
 * 10) Is not crawled price "a vista" because on the purchase does not have this option.
 * 
 * Examples: ex1 (available): http://www.catral.com.br/bebedouro-de-pressso-inox-ibbl-bag40/p ex2
 * (unavailable):
 * http://www.catral.com.br/bobina-termica-para-ecf-80mmx30m-1-via-cor-palha-caixa-30-unid-maxprint/p
 * ex3 (unavailable/available):
 * http://www.catral.com.br/batedeira-planetaria-g-paniz-12-litros-bp12rp-monofasica-/p
 *
 * Optimizations notes: No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilCatralCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.catral.com.br/";

  public BrasilCatralCrawler(Session session) {
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
    List<Product> products = new ArrayList<Product>();

    if (isProductPage(this.session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      /*
       * *********************************** crawling data of only one product *
       *************************************/

      // Pid
      String internalPid = crawlInternalPid(doc);

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

      // Skus
      JSONArray arraySkus = this.crawlSkuJsonArray(doc);

      // Eans
      JSONArray arrayEans = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {

        JSONObject jsonSku = arraySkus.getJSONObject(i);

        // InternalId
        String internalId = crawlInternalId(jsonSku);

        // Name
        String name = crawlName(jsonSku);

        // Availability
        boolean available = crawlAvailability(jsonSku);

        // Price
        Float price = crawlPrice(jsonSku, available);

        // Prices
        Prices prices = crawlPrices(doc, jsonSku);

        // Ean
        String ean = i < arrayEans.length() ? arrayEans.getString(i) : null;

        List<String> eans = new ArrayList<>();
        eans.add(ean);

        Product product = ProductBuilder.create().setUrl(this.session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid)
            .setName(name).setPrice(price).setPrices(prices).setAvailable(available).setCategory1(category1).setCategory2(category2)
            .setCategory3(category3).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description).setStock(stock)
            .setMarketplace(marketplace).setEans(eans).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }



  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url) {
    if (url.endsWith("/p") || url.contains("/p?attempt="))
      return true;
    return false;
  }


  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject jsonSku) {
    String internalId = null;
    if (jsonSku.has("sku")) {
      internalId = Integer.toString(jsonSku.getInt("sku"));
    }
    return internalId;
  }

  private String crawlInternalPid(Document document) {
    String internalPid = null;
    Element inElement = document.select("#___rc-p-id").first();

    if (inElement != null)
      internalPid = inElement.attr("value");

    return internalPid;
  }

  private String crawlName(JSONObject jsonSku) {
    String name = null;
    if (jsonSku.has("skuname")) {
      name = jsonSku.getString("skuname");
    }
    return name;
  }

  private Float crawlPrice(JSONObject jsonSku, boolean available) {
    Float price = null;
    if (jsonSku.has("bestPriceFormated") && available) {
      price = Float.parseFloat(jsonSku.getString("bestPriceFormated").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
    }

    return price;
  }

  /**
   * 
   * @param jsonSku
   * @return
   */
  private Prices crawlPrices(Document document, JSONObject jsonSku) {
    Prices prices = new Prices();

    // bank slip
    Float bankSlipPrice = crawlBankSlipPrice(document, jsonSku);
    if (bankSlipPrice != null) {
      prices.setBankTicketPrice(bankSlipPrice);
    }

    // installments
    if (jsonSku.has("sku")) {

      // fetch the page with payment options
      String skuId = Integer.toString(jsonSku.getInt("sku"));
      String paymentOptionsURL = "http://www.catral.com.br/productotherpaymentsystems/" + skuId;
      Document paymentOptionsDocument = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, paymentOptionsURL, null, null);

      // get all cards brands
      List<String> cardBrands = new ArrayList<String>();
      Elements cardsBrandsElements = paymentOptionsDocument.select(".div-card-flag #ddlCartao option");
      for (Element cardBrandElement : cardsBrandsElements) {
        String cardBrandText = cardBrandElement.text().toLowerCase();
        if (cardBrandText.contains("american express") || cardBrandText.contains(Card.AMEX.toString()))
          cardBrands.add(Card.AMEX.toString());
        else if (cardBrandText.contains(Card.VISA.toString()))
          cardBrands.add(Card.VISA.toString());
        else if (cardBrandText.contains(Card.DINERS.toString()))
          cardBrands.add(Card.DINERS.toString());
        else if (cardBrandText.contains(Card.MASTERCARD.toString()))
          cardBrands.add(Card.MASTERCARD.toString());
        else if (cardBrandText.contains(Card.HIPERCARD.toString()))
          cardBrands.add(Card.HIPERCARD.toString());
      }

      // get each table payment option in the same sequence as we got the cards brands (the html
      // logic was this way)
      Elements paymentElements = paymentOptionsDocument.select("#divCredito .tbl-payment-system tbody");

      for (int i = 0; i < cardBrands.size(); i++) {
        if (paymentElements.size() > i) {
          Element paymentElement = paymentElements.get(i);
          Map<Integer, Float> installments = crawlInstallmentsFromTableElement(paymentElement);
          if (installments.size() > 0)
            prices.insertCardInstallment(cardBrands.get(i), installments);
        }
      }
    }

    return prices;
  }

  /**
   * Extract all installments from a table html element.
   * 
   * e.g: Nº de Parcelas Valor de cada parcela American Express à vista R$ 1.799,00 American Express 2
   * vezes sem juros R$ 899,50 American Express 3 vezes sem juros R$ 599,66 American Express 4 vezes
   * sem juros R$ 449,75 American Express 5 vezes sem juros R$ 359,80 American Express 6 vezes sem
   * juros R$ 299,83 American Express 7 vezes sem juros R$ 257,00 American Express 8 vezes sem juros
   * R$ 224,87 American Express 9 vezes sem juros R$ 199,88 American Express 10 vezes sem juros R$
   * 179,90 American Express 11 vezes com juros R$ 173,41 American Express 12 vezes com juros R$
   * 159,73
   *
   * @param tableElement
   * @return
   */
  private Map<Integer, Float> crawlInstallmentsFromTableElement(Element tableElement) {
    Map<Integer, Float> installments = new TreeMap<Integer, Float>();

    Elements tableLinesElements = tableElement.select("tr");
    for (int j = 1; j < tableLinesElements.size(); j++) { // the first one is just the table header
      Element tableLineElement = tableLinesElements.get(j);
      Element installmentNumberElement = tableLineElement.select("td.parcelas").first();
      Element installmentPriceElement = tableLineElement.select("td").last();

      if (installmentNumberElement != null && installmentPriceElement != null) {
        String installmentNumberText = installmentNumberElement.text().toLowerCase();
        String installPriceText = installmentPriceElement.text();

        List<String> parsedNumbers = MathUtils.parseNumbers(installmentNumberText);
        if (parsedNumbers.size() == 0) { // à vista
          installments.put(1, MathUtils.parseFloatWithComma(installPriceText));
        } else {
          installments.put(Integer.parseInt(parsedNumbers.get(0)), MathUtils.parseFloatWithComma(installPriceText));
        }
      }
    }

    return installments;
  }

  /**
   * Computes the bank slip price by applying a discount on the base price. The base price is the same
   * that is crawled on crawlPrice method.
   * 
   * @param document
   * @param jsonSku
   * @return
   */
  private Float crawlBankSlipPrice(Document document, JSONObject jsonSku) {
    Float bankSlipPrice = null;

    // check availability
    boolean available = false;
    if (jsonSku.has("available")) {
      available = jsonSku.getBoolean("available");
    }

    if (available) {
      if (jsonSku.has("bestPriceFormated") && available) {
        Float basePrice = MathUtils.parseFloatWithComma(jsonSku.getString("bestPriceFormated"));
        Float discountPercentage = crawlDiscountPercentage(document);

        // apply the discount on base price
        if (discountPercentage != null) {
          bankSlipPrice = MathUtils.normalizeTwoDecimalPlaces(basePrice - (discountPercentage * basePrice));
        }
      }
    }

    return bankSlipPrice;
  }

  /**
   * Look for the discount html element and parses the discount percentage from the element name. In
   * this ecommerce we have elements in this form
   * <p class="flag boleto-10--off">
   * Boleto 10% Off
   * </p>
   * where the 10 in the name of the class indicates the percentual value we must apply on the base
   * value.
   * 
   * @return
   */
  private Float crawlDiscountPercentage(Document document) {
    Float discountPercentage = null;
    Element discountElement = document.select(".product-discount-hight-light p[class^=flag boleto]").first();
    if (discountElement != null) {
      List<String> parsedNumbers = MathUtils.parsePositiveNumbers(discountElement.attr("class"));
      if (parsedNumbers.size() > 0) {
        try {
          Integer discount = Integer.parseInt(parsedNumbers.get(0));
          Float discountFloat = new Float(discount);
          discountPercentage = MathUtils.normalizeTwoDecimalPlaces(discountFloat / 100);
        } catch (NumberFormatException e) {
          Logging.printLogError(logger, session, "Error parsing integer from String in CrawlDiscountPercentage method.");
        }
      }
    }
    return discountPercentage;
  }

  private boolean crawlAvailability(JSONObject jsonSku) {
    if (jsonSku.has("available"))
      return jsonSku.getBoolean("available");
    return true;
  }

  private Map<String, Float> crawlMarketplace(Document document) {
    return new HashMap<String, Float>();
  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
    return new Marketplace();
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select("#image a").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("href").trim();
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select("#botaoZoom");

    for (int i = 1; i < imagesElement.size(); i++) { // starts with index 1 because the first item
                                                     // is the primary image
      Element e = imagesElement.get(i);
      if (e.hasAttr("zoom") && !e.attr("zoom").isEmpty()) {
        secondaryImagesArray.put(e.attr("zoom").trim());
      } else {
        secondaryImagesArray.put(e.attr("rel").trim());
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private ArrayList<String> crawlCategories(Document document) {
    ArrayList<String> categories = new ArrayList<String>();
    Elements elementCategories = document.select(".bread-crumb ul li");

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
    Element descriptionElement = document.select(".productDescription").first();

    if (descriptionElement != null)
      description = description + descriptionElement.html();

    return description;
  }

  /**
   * Get the script having a json with the availability information
   * 
   * @return
   */
  private JSONArray crawlSkuJsonArray(Document document) {
    Elements scriptTags = document.getElementsByTag("script");
    JSONObject skuJson = null;
    JSONArray skuJsonArray = new JSONArray();

    for (Element tag : scriptTags) {
      for (DataNode node : tag.dataNodes()) {
        if (tag.html().trim().startsWith("var skuJson_0 = ")) {
          skuJson = new JSONObject(node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1]
              + node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]);

        }
      }
    }

    if (skuJson != null && skuJson.has("skus")) {
      skuJsonArray = skuJson.getJSONArray("skus");
    }

    return skuJsonArray;
  }

}
