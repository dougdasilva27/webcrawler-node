package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
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
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 15/12/16
 * 
 * @author gabriel and samirleao
 *
 */
public class BrasilSaraivaCrawler extends Crawler {

  private static final String HOME_PAGE_HTTP = "http://www.saraiva.com.br";
  private static final String HOME_PAGE_HTTPS = "https://www.saraiva.com.br";

  private static final int LARGER_IMAGE_DIMENSION = 550;


  public BrasilSaraivaCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE_HTTP) || href.startsWith(HOME_PAGE_HTTPS));
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject chaordic = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.chaordic_meta =", ";", false);
      JSONObject productJSON = chaordic.has("product") ? chaordic.getJSONObject("product") : chaordic;

      String internalId = crawlInternalId(doc);
      String internalPid = crawlInternalPid(productJSON);

      String apiUrl = "https://api.saraiva.com.br/sc/produto/pdp/" + internalId + "/0/19121647/1/";
      JSONObject apiJson = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, apiUrl, null, cookies);

      String name = crawlName(doc);
      boolean available = crawlAvailability(productJSON);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc, primaryImage);
      Integer stock = null;
      Marketplace marketplace = new Marketplace();
      String description = crawlDescription(doc);

      // price is not displayed when sku is unavailable
      Float price = crawlPrice(apiJson);
      Prices prices = crawlPrices(apiJson, price);

      // Categories
      CategoryCollection categories = crawlCategories(doc);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document document) {
    return !document.select("section.product-allinfo").isEmpty() || !document.select("#pdp-info").isEmpty();
  }

  /**
   * Crawl the code from the displayed number on the main page.
   * 
   * e.g: Cafeteira Espresso Electrolux Chef Crema Silver Em400 - 220 Volts (Cód: 4054233)
   * 
   * internalId = 4054233
   * 
   * @param document
   * @return
   */
  private String crawlInternalId(Document document) {
    String internalId = null;

    Element elementSpan = document.select("section.product-info h1 span").first();
    if (elementSpan != null) {
      String spanText = elementSpan.text();
      List<String> parsedNumbers = MathUtils.parseNumbers(spanText);
      if (!parsedNumbers.isEmpty()) {
        internalId = parsedNumbers.get(0);
      }
    } else {
      elementSpan = document.select("#pdp-info .title_note").first();

      if (elementSpan != null) {
        String spanText = elementSpan.text();
        List<String> parsedNumbers = MathUtils.parseNumbers(spanText);
        if (!parsedNumbers.isEmpty()) {
          internalId = parsedNumbers.get(0);
        }
      }
    }

    return internalId;
  }

  /**
   * InternalPid is the id field inside the chaordicMetadataJSON
   * 
   * @param document
   * @return
   */
  private String crawlInternalPid(JSONObject productJSON) {
    String internalPid = null;

    if (productJSON.has("id")) {
      internalPid = String.valueOf(productJSON.getInt("id"));
    } else if (productJSON.has("pid")) {
      internalPid = productJSON.get("pid").toString();
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;

    Element elementName = document.select("section.product-info h1, #pdp-info h1").first();
    if (elementName != null) {
      name = elementName.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(JSONObject apiJson) {
    Float price = null;

    if (apiJson.has("price_block")) {
      JSONObject priceBlock = apiJson.getJSONObject("price_block");

      if (priceBlock.has("price")) {
        JSONObject priceJson = priceBlock.getJSONObject("price");

        if (priceJson.has("final")) {
          price = MathUtils.parseFloat(priceJson.getString("final"));
        }
      }
    }

    return price;
  }

  private Prices crawlPrices(JSONObject apiJson, Float price) {
    Prices prices = new Prices();

    if (price != null && apiJson.has("price_block")) {
      JSONObject priceBlock = apiJson.getJSONObject("price_block");
      prices.setBankTicketPrice(crawlBilletPrice(priceBlock, price));

      Map<Integer, Float> installments = crawlInstallmentsNormalCard(priceBlock, price);
      Map<Integer, Float> installmentsShopcardMap = crawlInstallmentsShopCard(priceBlock, price);

      if (installments.size() > 0) {
        prices.insertCardInstallment(Card.VISA.toString(), installments);
        prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
        prices.insertCardInstallment(Card.DINERS.toString(), installments);
        prices.insertCardInstallment(Card.AURA.toString(), installments);
        prices.insertCardInstallment(Card.ELO.toString(), installments);
        prices.insertCardInstallment(Card.HIPERCARD.toString(), installments);
        prices.insertCardInstallment(Card.AMEX.toString(), installments);

        if (installmentsShopcardMap.isEmpty()) {
          prices.insertCardInstallment(Card.SHOP_CARD.toString(), installments);
        } else {
          prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentsShopcardMap);
        }
      }
    }

    return prices;
  }

  private Float crawlBilletPrice(JSONObject priceBlock, Float price) {
    Float billet = price;

    if (priceBlock.has("billet")) {
      JSONObject billetJson = priceBlock.getJSONObject("billet");

      if (billetJson.has("has_discount") && billetJson.getInt("has_discount") > 0 && billetJson.has("value_with_discount")) {
        billet = MathUtils.parseFloat(billetJson.getString("value_with_discount"));
      }
    }

    return billet;
  }

  /**
   * Normal cards installments
   * 
   * @param doc
   * @return
   */
  private Map<Integer, Float> crawlInstallmentsNormalCard(JSONObject priceBlock, Float price) {
    Map<Integer, Float> installments = new HashMap<>();

    if (priceBlock.has("price")) {
      JSONObject priceJson = priceBlock.getJSONObject("price");

      if (priceJson.has("qty_installments_without_fee") && priceJson.has("value_installments_without_fee")) {
        Integer installment = priceJson.getInt("qty_installments_without_fee");

        if (installment > 0) {
          installments.put(installment, MathUtils.parseFloat(priceJson.getString("value_installments_without_fee")));
        }
      }

      if (priceJson.has("qty_installments_with_fee") && priceJson.has("value_installments_with_fee")) {
        Integer installment = priceJson.getInt("qty_installments_with_fee");

        if (installment > 0) {
          installments.put(installment, MathUtils.parseFloat(priceJson.getString("value_installments_with_fee")));
        }
      }
    }

    if (priceBlock.has("credit_card")) {
      JSONObject creditCard = priceBlock.getJSONObject("credit_card");

      if (creditCard.has("has_discount") && creditCard.getInt("has_discount") > 0 && creditCard.has("installment_with_discount")
          && creditCard.has("qty_installments_with_discount")) {

        installments.put(creditCard.getInt("qty_installments_with_discount"),
            MathUtils.parseFloat(creditCard.getString("installment_with_discount")));

        if (creditCard.has("value_with_discount")) {
          installments.put(1, MathUtils.parseFloat(creditCard.getString("value_with_discount")));
        }
      }

    }

    if (!installments.containsKey(1)) {
      installments.put(1, price);
    }

    return installments;
  }

  /**
   * Shop card installments
   * 
   * @param doc
   * @return
   */
  private Map<Integer, Float> crawlInstallmentsShopCard(JSONObject priceBlock, Float price) {
    Map<Integer, Float> installmentsShopcardMap = new HashMap<>();

    if (priceBlock.has("saraiva_card")) {
      JSONObject priceJson = priceBlock.getJSONObject("saraiva_card");

      if (priceJson.has("qty_installments_without_fee") && priceJson.has("value_installments_without_fee")) {
        Integer installment = priceJson.getInt("qty_installments_without_fee");

        if (installment > 0) {
          installmentsShopcardMap.put(installment, MathUtils.parseFloat(priceJson.getString("value_installments_without_fee")));
        }
      }

      if (priceJson.has("qty_installments_with_fee") && priceJson.has("value_installments_with_fee")) {
        Integer installment = priceJson.getInt("qty_installments_with_fee");

        if (installment > 0) {
          installmentsShopcardMap.put(installment, MathUtils.parseFloat(priceJson.getString("value_installments_with_fee")));
        }
      }

      if (priceJson.has("has_discount") && priceJson.getInt("has_discount") > 0 && priceJson.has("installment_with_discount")
          && priceJson.has("qty_installments_with_discount")) {

        installmentsShopcardMap.put(priceJson.getInt("qty_installments_with_discount"),
            MathUtils.parseFloat(priceJson.getString("installment_with_discount")));
      }

      if (priceJson.has("discount_percent") && priceJson.has("value_with_discount")) {
        Double discount = MathUtils.parseDouble(priceJson.get("discount_percent").toString());

        if (discount != null && discount > 0) {
          installmentsShopcardMap.put(1, MathUtils.parseFloat(priceJson.getString("value_with_discount")));
        }
      }
    }

    if (!installmentsShopcardMap.containsKey(1)) {
      installmentsShopcardMap.put(1, price);
    }

    return installmentsShopcardMap;
  }

  /**
   * 
   * @param productJSON
   * @return
   */
  private boolean crawlAvailability(JSONObject productJSON) {
    return productJSON.has("status") && "available".equalsIgnoreCase(productJSON.get("status").toString());
  }

  /**
   * Crawl an image with a default dimension of 430. There is a larger image with dimension of 550,
   * but with javascript off this link disappear. So we modify the image URL and set the dimension
   * parameter to the desired larger size.
   * 
   * Parameter to mody: &l
   * 
   * e.g: original:
   * http://images.livrariasaraiva.com.br/imagemnet/imagem.aspx/?pro_id=9220079&qld=90&l=430&a=-1
   * larger:
   * http://images.livrariasaraiva.com.br/imagemnet/imagem.aspx/?pro_id=9220079&qld=90&l=550&a=-1
   * 
   * @param document
   * @return
   */
  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;

    // get original image URL
    Element elementPrimaryImage = document.select("div.product-image-center a img").first();
    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("src");
    }

    if (primaryImage != null) {
      if (primaryImage.contains(".gif")) {
        Elements elementImages = document.select("section.product-image #thumbs-images a img");

        for (int i = 1; i < elementImages.size(); i++) { // skip the first because it's the same as
                                                         // the primary image.gif
          String imageURL = elementImages.get(i).attr("src").trim();

          if (!imageURL.contains(".gif")) {
            primaryImage = CommonMethods.modifyParameter(imageURL, "l", String.valueOf(LARGER_IMAGE_DIMENSION));
            break;
          }
        }
      }

      if (primaryImage.contains(".gif")) {
        primaryImage = null;
      }

      // modify the dimension parameter
      primaryImage = CommonMethods.modifyParameter(primaryImage, "l", String.valueOf(LARGER_IMAGE_DIMENSION));
    } else {
      Element imageSpecial = document.select(".slides li > img").first();

      if (imageSpecial != null) {
        primaryImage = CommonMethods.modifyParameter(imageSpecial.attr("src"), "l", String.valueOf(LARGER_IMAGE_DIMENSION));
      }
    }

    return primaryImage;
  }

  /**
   * Get all the secondary images URL from thumbs container. Analogous treatment to that performed on
   * primary image URL must be applied, so we can get the largest images URL.
   * 
   * @param document
   * @return
   */
  private String crawlSecondaryImages(Document document, String primaryImage) {
    String secondaryImages = null;

    JSONArray secondaryImagesArray = new JSONArray();
    Elements elementImages = document.select("section.product-image #thumbs-images a img, .slides li > img");
    Set<String> setImages = new HashSet<>();

    for (Element e : elementImages) {
      String imageURL = e.attr("src").trim();
      String biggerImageURL = CommonMethods.modifyParameter(imageURL, "l", String.valueOf(LARGER_IMAGE_DIMENSION));

      if (!biggerImageURL.equals(primaryImage) && !biggerImageURL.contains(".gif")) {
        setImages.add(biggerImageURL);
      }
    }

    for (String s : setImages) {
      secondaryImagesArray.put(s);
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumbs ol li");

    for (int i = 1; i < elementCategories.size(); i++) { // start with index 1 because the first
                                                         // item is the home page
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();

    Element skuInformation = document.select("#product-information").first();
    if (skuInformation != null) {
      description.append(skuInformation.html());
    }

    Element skuAdditionalInformation = document.select("#product-additional").first();
    if (skuAdditionalInformation != null) {
      description.append(skuAdditionalInformation.html());
    }

    Element specialDesc = document.select("#product_description").first();
    if (specialDesc != null) {
      description.append(specialDesc.html());
    }

    Element specialInfo = document.select("#product_attributes").first();
    if (specialInfo != null) {
      description.append(specialInfo.html());
    }

    return description.toString();
  }
}
