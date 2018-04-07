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

public class SaopauloDrogasilCrawler extends Crawler {

  public SaopauloDrogasilCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches();
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // ID interno
      String internalId = crawlInternalId(doc);

      // Pid
      String internalPid = crawlInternalPid(doc);

      // Disponibilidade
      boolean available = true;
      Element elementNotAvailable = doc.select(".product-shop .alert-stock.link-stock-alert a").first();
      if (elementNotAvailable != null) {
        if (elementNotAvailable.attr("title").equals("Avise-me")) {
          available = false;
        }
      }

      // Nome
      String name = crawlName(doc);

      // Preço
      Float price = null;
      Element elementSpecialPrice = doc.select(".product-shop .price-info .price-box .special-price").first();
      Element elementPrice = doc.select(".product-shop .price-info .price-box .price").first();
      if (elementSpecialPrice != null) { // está em promoção
        price = Float.parseFloat(elementSpecialPrice.select(".price").text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
      } else if (elementPrice != null) { // preço normal sem promoção
        price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
      }

      CategoryCollection categories = crawlCategories(doc);

      // Imagem primária
      Elements elementImages = doc.select(".product-img-box .product-image.product-image-zoom .product-image-gallery img");
      String primaryImage = null;
      Element elementPrimaryImage = elementImages.first();
      if (elementPrimaryImage != null) {
        primaryImage = elementPrimaryImage.attr("data-zoom-image");
      }

      // Imagens Secundárias
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      if (elementImages.size() > 2) {
        for (int i = 0; i < elementImages.size(); i++) {
          Element elementSecondaryImage = elementImages.get(i);
          if (elementSecondaryImage != null) {
            String secondaryImage = elementSecondaryImage.attr("data-zoom-image");

            if (!isPrimaryImage(primaryImage, secondaryImage)) {
              secondaryImagesArray.put(secondaryImage);
            }
          }
        }
      }

      if (secondaryImagesArray.length() > 0) {
        secondaryImages = secondaryImagesArray.toString();
      }

      // Descrição
      String description = crawlDescription(doc);

      // Estoque
      Integer stock = null;

      // Marketplace
      Marketplace marketplace = new Marketplace();

      // Prices
      Prices prices = crawlPrices(doc, price);

      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
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

  private boolean isProductPage(Document document) {
    return (!document.select(".product-shop").isEmpty() || !document.select(".shipping-quote").isEmpty());
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumbs ul li a");

    for (int i = 1; i < elementCategories.size(); i++) { // first item is the home page
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }


  private boolean isPrimaryImage(String primaryImage, String image) {
    if (primaryImage == null || image == null) {
      return false;
    }

    String[] tokens = primaryImage.split("/");
    String[] tokens2 = image.split("/");

    return tokens[tokens.length - 1].split("\\?")[0].equals(tokens2[tokens2.length - 1].split("\\?")[0]);
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;
    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "dataLayer.push(", ");");

    if (json.has("ecommerce")) {
      JSONObject ecommerce = json.getJSONObject("ecommerce");

      if (ecommerce.has("detail")) {
        JSONObject detail = ecommerce.getJSONObject("detail");

        if (detail.has("products")) {
          JSONArray products = detail.getJSONArray("products");

          if (products.length() > 0) {
            JSONObject product = products.getJSONObject(0);

            if (product.has("id")) {
              internalId = product.getString("id");
            }
          }
        }
      }
    }

    return internalId;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;
    Element pid = doc.select("input[name=product][value]").first();

    if (pid != null) {
      internalPid = pid.attr("value");
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element elementName = document.select(".product-view .limit.columns .col-1 .product-info .product-name h1").first();
    if (elementName != null) {
      name = elementName.text().trim();

      // brand
      Element elementBrand = document.select(".product-view .limit.columns .col-1 .product-info .product-attributes ul .marca").first();
      if (elementBrand != null) {
        name = name + " " + elementBrand.text().trim();
      }

      // quantity
      Element productAttributes = document.select(".product-attributes").last();
      if (productAttributes != null) {
        Element quantity = productAttributes.select("ul li.quantidade").first();
        if (quantity != null) {
          name = name + " - " + quantity.text().trim();
        }
      }
    }

    return name;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element shortDescription = doc.select(".product-short-description").first();
    if (shortDescription != null) {
      description.append(shortDescription.html());
    }

    Element elementDescription = doc.select("div#details.product-details").first();
    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    return description.toString();
  }

  /**
   * In this market, installments not appear in product page
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      Element priceFrom = doc.select(".old-price span[id]").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDouble(priceFrom.text()));
      }

      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }

    return prices;
  }

}
