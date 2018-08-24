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
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class SaopauloDrogaraiaCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.drogaraia.com.br/";

  public SaopauloDrogaraiaCrawler(Session session) {
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

    if (isProductPage(this.session.getOriginalURL(), doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // ID interno
      String internalID = crawlInternalId(doc);

      // Pid
      String internalPid = null;
      Element elementInternalPid = doc.select("input[name=product]").first();
      if (elementInternalPid != null) {
        internalPid = elementInternalPid.attr("value").trim();
      }

      // Disponibilidade
      boolean available = true;
      Element buyButton = doc.select(".add-to-cart button").first();

      if (buyButton == null) {
        available = false;
      }

      // Nome
      String name = crawlName(doc);

      // Preço
      Float price = null;
      Element elementPrice = doc.select(".product-shop .regular-price").first();
      if (elementPrice == null) {
        elementPrice = doc.select(".product-shop .price-box .special-price .price").first();
      }
      if (elementPrice != null) {
        price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
      }

      // Categorias
      Elements elementsCategories = doc.select(".breadcrumbs ul li:not(.home):not(.product) a");
      String category1 = "";
      String category2 = "";
      String category3 = "";
      for (Element category : elementsCategories) {
        if (category1.isEmpty()) {
          category1 = category.text();
        } else if (category2.isEmpty()) {
          category2 = category.text();
        } else if (category3.isEmpty()) {
          category3 = category.text();
        }
      }

      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc, primaryImage);

      // Descrição
      String description = "";
      Element shortDescription = doc.select(".product-short-description").first();
      Element elementDescription = doc.select("#details").first();

      if (shortDescription != null) {
        description += shortDescription.html().trim();
      }

      if (elementDescription != null) {
        description += elementDescription.html().trim();
      }

      // Estoque
      Integer stock = null;

      // Marketplace
      Marketplace marketplace = new Marketplace();

      // Prices
      Prices prices = crawlPrices(doc, price);

      Product product = new Product();

      product.setUrl(session.getOriginalURL());
      product.setInternalId(internalID);
      product.setInternalPid(internalPid);
      product.setName(name);
      product.setPrice(price);
      product.setPrices(prices);
      product.setCategory1(category1);
      product.setCategory2(category2);
      product.setCategory3(category3);
      product.setPrimaryImage(primaryImage);
      product.setSecondaryImages(secondaryImages);
      product.setDescription(description);
      product.setStock(stock);
      product.setMarketplace(marketplace);
      product.setAvailable(available);

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }


  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url, Document document) {
    Element elementInternalID = document.select("#details .col-2 .data-table tr .data").first();
    return elementInternalID != null;
  }

  private String crawlName(Document doc) {
    StringBuilder name = new StringBuilder();

    Element firstName = doc.selectFirst(".product-name h1");
    if (firstName != null) {
      name.append(firstName.text());

      Elements attributes = doc.select(".product-attributes .show-hover");
      for (Element e : attributes) {
        name.append(" ").append(e.ownText().trim());
      }
    }

    return name.toString().replace("  ", " ").trim();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;
    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "dataLayer.push(", ");", true, false);

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

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;

    Element elementPrimaryImage = doc.select(".product-image-gallery img#image-main").first();
    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("data-zoom-image");
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc, String primaryImage) {
    String secondaryImages = null;
    JSONArray secundaryImagesArray = new JSONArray();
    Elements elementImages = doc.select(".product-image-gallery img.gallery-image");

    for (Element e : elementImages) {
      String image = e.attr("data-zoom-image").trim();

      if (!isPrimaryImage(image, primaryImage)) {
        secundaryImagesArray.put(image);
      }
    }

    if (secundaryImagesArray.length() > 0) {
      secondaryImages = secundaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private boolean isPrimaryImage(String image, String primaryImage) {
    String x = CommonMethods.getLast(image.split("/"));
    String y = CommonMethods.getLast(primaryImage.split("/"));

    return x.equals(y);
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

      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      Element priceFrom = doc.select(".old-price span[id]").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDouble(priceFrom.text()));
      }

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
