package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
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
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

public class BrasilFarmadeliveryCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.farmadelivery.com.br/";

  public BrasilFarmadeliveryCrawler(Session session) {
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
      String internalId = null;
      Element elementID = doc.select("input[name=product]").first();
      if (elementID != null) {
        internalId = Integer.toString(Integer.parseInt(elementID.val()));
      }

      // Pid
      String internalPid = null;
      Elements elementInternalPid = doc.select("#product-attribute-specs-table tr td.data");
      if (elementInternalPid != null && elementInternalPid.size() > 1) {
        internalPid = elementInternalPid.get(0).text().trim();
      }

      // Nome
      Element elementName = doc.select(".product-name h1").first();
      String name = elementName.text().replace("'", "").replace("’", "").trim();

      Float price = crawlPrice(doc);

      // Disponibilidade
      boolean available = true;
      Element buttonUnavailable = doc.select("a.btn-esgotado").first();
      if (buttonUnavailable != null) {
        available = false;
      }

      // Categorias
      String category1 = "";
      String category2 = "";
      String category3 = "";
      ArrayList<String> categories = new ArrayList<>();
      Elements elementCategories = doc.select(".breadcrumbs ul li");

      for (Element e : elementCategories) {
        if (!e.attr("class").equals("home") && !e.attr("class").equals("product")) {
          categories.add(e.select("a").text());
        }
      }

      for (String category : categories) {
        if (category1.isEmpty()) {
          category1 = category;
        } else if (category2.isEmpty()) {
          category2 = category;
        } else if (category3.isEmpty()) {
          category3 = category;
        }
      }

      // Imagens
      String primaryImage = null;
      Element elementPrimaryImage = doc.select(".product-img-box img#image").first();
      if (elementPrimaryImage != null) {
        primaryImage = elementPrimaryImage.attr("src").trim();
        primaryImage = primaryImage.replace("275x275/", "");
      }

      if (primaryImage != null) {
        if (primaryImage.contains("banner_produto_sem_imagem")) {
          primaryImage = "";
        }
      }

      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();
      Elements elementSecondaryImages = doc.select(".product-img-box .more-views li a");

      if (elementSecondaryImages.size() > 1) {
        for (int i = 1; i < elementSecondaryImages.size(); i++) {
          Element e = elementSecondaryImages.get(i);
          secondaryImagesArray.put(e.attr("href"));
        }

      }
      if (secondaryImagesArray.length() > 0) {
        secondaryImages = secondaryImagesArray.toString();
      }

      // Descrição
      Element elementDescription = doc.select("div.product-collateral .box-description").first();
      Element elementAdditional = doc.select("div.product-collateral .box-additional").first();
      String description = "";

      if (elementDescription != null) {
        description += elementDescription.html();
      }

      if (elementAdditional != null) {
        description += elementAdditional.html();
      }

      // Estoque
      Integer stock = null;

      // Prices
      Prices prices = crawlPrices(doc, price);

      // Marketplace
      Marketplace marketplace = new Marketplace();

      Product product = new Product();
      product.setUrl(this.session.getOriginalURL());
      product.setInternalId(internalId);
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
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }


  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url, Document doc) {
    Element elementProduct = doc.select("div.product-view").first();
    return elementProduct != null && !url.contains("/review/");
  }

  private Float crawlPrice(Document doc) {
    Float price = null;
    Element elementPrice = doc.select(".price-box .regular-price .price").first();
    if (elementPrice == null) {
      elementPrice = doc.select(".price-box .special-price .price").first();
    }

    Element elementSpecialPrice = doc.select(".pagamento .boleto span").first();

    if (elementPrice != null) {
      price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
    } else if (elementSpecialPrice != null) {
      price = MathCommonsMethods.parseFloat(elementSpecialPrice.ownText());
    }

    return price;
  }

  /**
   * In product page only has bank slip price and showcase price
   * 
   * @param document
   * @return
   */
  private Prices crawlPrices(Document document, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Element bankSlip = document.select(".pagamento .boleto > span").first();

      if (bankSlip != null) {
        Float bankSlipPrice = MathCommonsMethods.parseFloat(bankSlip.text());
        prices.setBankTicketPrice(bankSlipPrice);
      }

      Map<Integer, Float> installmentPriceMap = new TreeMap<>();

      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }

    return prices;
  }
}
