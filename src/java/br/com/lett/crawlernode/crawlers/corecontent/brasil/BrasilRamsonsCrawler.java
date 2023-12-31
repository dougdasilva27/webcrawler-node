package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 22/06/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilRamsonsCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.ramsons.com.br/";

  public BrasilRamsonsCrawler(Session session) {
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
      Prices prices = crawlPrices(price, doc);
      boolean available = price != null;
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage =
          CrawlerUtils.scrapSimplePrimaryImage(doc, "a#Zoom1[href], a#Zoom1 img", Arrays.asList("href", "src"), "https", "www.ramsons.com.br");
      String secondaryImages = crawlSecondaryImages(doc, primaryImage);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = crawlMarketplace();

      // Creating the product
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

  private boolean isProductPage(Document doc) {
    return doc.select("#ProdutoCodigo").first() != null;
  }

  private String crawlInternalId(Document document) {
    String internalId = null;

    Element internalIdElement = document.select("#ProdutoCodigo").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    }

    return internalId;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;
    Element pid = doc.select("#liCodigoInterno span").first();

    if (pid != null) {
      internalPid = pid.ownText().trim();
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("h1.name").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;

    String priceText;
    Element salePriceElement = document.selectFirst("#info-product .price.sale strong");

    if (salePriceElement != null) {
      priceText = salePriceElement.ownText();
      price = MathUtils.parseFloatWithComma(priceText);
    } else {
      salePriceElement = document.selectFirst("#lblPreco.regular");

      if (salePriceElement != null) {
        priceText = salePriceElement.ownText();
        price = MathUtils.parseFloatWithComma(priceText);
      }
    }

    return price;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }

  private String crawlSecondaryImages(Document document, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".thumbs li > a > img");
    for (Element e : imagesElement) {
      String image = CrawlerUtils.completeUrl(e.attr("src").trim(), "https", "www.ramsons.com.br").replace("/Detalhes2/", "/Ampliada2/");

      if (!image.isEmpty() && !image.equalsIgnoreCase(primaryImage) && !image.endsWith("gif")) {
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  /**
   * No momento que o crawler foi feito não foi achado produtos com categorias
   * 
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();

    Elements elementCategories = document.select("#breadcrumbs a");
    for (int i = 1; i < elementCategories.size(); i++) {
      String cat = elementCategories.get(i).ownText().trim();
      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element elementDescription = doc.select(".content .main-content #description").first();
    Element elementEspecificacion = doc.select(".content .main-content #panCaracteristica").first();
    Element elementDimensions = doc.select(".content .main-content #divSizeTable").first();

    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    if (elementEspecificacion != null) {
      description.append(elementEspecificacion.html());
    }

    if (elementDimensions != null) {
      description.append(elementDimensions.html());
    }

    return description.toString();
  }

  /**
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();

      Element vista = doc.select("#lblPrecoAVista.preco-avista").first();

      if (vista != null) {
        String priceVistaString = vista.ownText();

        if (!priceVistaString.isEmpty()) {
          Float priceVista = MathUtils.parseFloatWithComma(priceVistaString);

          prices.setBankTicketPrice(priceVista);
          installmentPriceMap.put(1, priceVista);
        } else {
          prices.setBankTicketPrice(price);
          installmentPriceMap.put(1, price);
        }
      } else {
        prices.setBankTicketPrice(price);
        installmentPriceMap.put(1, price);
      }

      Element installments = doc.select("#lblParcelamento").first();

      if (installments != null) {
        Element firstInstallment = installments.select("#lblParcelamento1 strong").first();

        if (firstInstallment != null) {
          Integer installment = Integer.parseInt(firstInstallment.ownText().replaceAll("[^0-9]", ""));

          Element installmentValue = installments.select("#lblParcelamento2 strong").first();

          if (installmentValue != null) {
            Float priceInstallment = MathUtils.parseFloatWithComma(installmentValue.ownText());

            installmentPriceMap.put(installment, priceInstallment);
          }
        }

        Elements secondInstallment = installments.select("#lblOutroParc strong");

        if (secondInstallment.size() >= 2) {
          Integer installment = Integer.parseInt(secondInstallment.get(0).text().replaceAll("[^0-9]", ""));
          Float priceInstallment = MathUtils.parseFloatWithComma(secondInstallment.get(1).text());

          installmentPriceMap.put(installment, priceInstallment);
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }

}
