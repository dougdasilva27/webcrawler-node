package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
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

/**
 * Date: 09/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilCentraltecCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.centraltec.com.br";

  public BrasilCentraltecCrawler(Session session) {
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
      Float price = crawlPrice(doc, internalId);
      Prices prices = crawlPrices(price, doc);
      boolean available = crawlAvailability(doc, price);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
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
    return doc.select(".product-name h1").first() != null;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select("input[name=product]").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    }

    return internalId;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;
    Element pdi = doc.select(".sku").first();

    if (pdi != null) {
      internalPid = pdi.ownText().replaceAll("[^0-9]", "").trim();
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(".product-name h1").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document, String internalId) {
    Float price = 0f;
    Element normalPrice = document.select("#product-price-" + internalId).first();

    if (normalPrice != null) {
      price = MathUtils.parseFloatWithComma(normalPrice.text());
    }

    if (price == null || price <= 0) {
      Elements optionsList = document.select(".options-list li .price-notice");

      for (Element e : optionsList) {
        String operator = e.ownText().trim();
        String priceText = e.text().replaceAll("[^0-9,]", "").replace(",", ".").trim();

        if (!operator.isEmpty() && !priceText.isEmpty()) {
          Float priceFloat = Float.parseFloat(priceText);

          if ("+".equals(operator)) {
            price += priceFloat;
          } else if ("-".equals(operator)) {
            price -= priceFloat;
          } else if ("/".equals(operator)) {
            price /= priceFloat;
          } else if ("*".equals(operator)) {
            price *= priceFloat;
          }
        }
      }
    }

    if (price <= 0) {
      price = null;
    }

    return price;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element elementPrimaryImage = doc.select(".product-image > a").first();

    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("href").trim();
    }

    return primaryImage;
  }

  /**
   * 
   * @param doc
   * @param primaryImage
   * @return
   */
  private String crawlSecondaryImages(Document doc, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select("#galeria > a");

    for (Element e : images) {
      String image = e.attr("href").trim();

      if (!image.equals(primaryImage) && !image.contains("iframe")) {
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  /**
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumbs li[class^=category] span");

    for (int i = 0; i < elementCategories.size(); i++) {
      String cat = elementCategories.get(i).ownText().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element elementDescription = doc.select(".box-description").first();

    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    return description.toString();
  }

  private boolean crawlAvailability(Document doc, Float price) {
    return doc.select(".alert-indisponivel").first() == null && price != null;
  }

  /**
   * Installments in some cases, don't appear the values in html for this we calculate based in price
   * table, in html appear
   * 
   * data-juros = 2.5 data-maximo_parcelas_sem_juros = 10 data-maximo_parcelas = 12
   * 
   * We calculated when the installment value appear like this R$ 0,00
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      Element bank = doc.select(".box-central .price-box .boletoBox").first();

      if (bank != null) {
        Float bankTicketPrice = MathUtils.parseFloatWithComma(bank.text());

        if (bankTicketPrice == null || bankTicketPrice <= 0) {
          bankTicketPrice = MathUtils.normalizeTwoDecimalPlaces(price - (Float.parseFloat(bank.attr("data-desconto")) * price));
        }

        prices.setBankTicketPrice(bankTicketPrice);
      }

      Element installmentsElement = doc.select(".box-central .price-box .parcelaBloco").first();

      if (installmentsElement != null) {
        Element semJuros = installmentsElement.select(".parcela-semjuros").first();
        Element comJuros = installmentsElement.select(".parcela-comjuros").first();

        if (semJuros != null) {
          Element parcela = semJuros.select(".parcela").first();
          Element priceElement = semJuros.select(".price").first();

          if (parcela != null && priceElement != null) {
            String text = parcela.ownText().replaceAll("[^0-9]", "").trim();
            Float value = MathUtils.parseFloatWithComma(priceElement.ownText());

            if (!text.isEmpty() && value != null && value > 0) {
              installmentPriceMap.put(Integer.parseInt(text), value);
            } else {
              // this case we calculate the installments
              String textParcela = installmentsElement.attr("data-maximo_parcelas_sem_juros").replaceAll("[^0-9]", "");

              if (!textParcela.isEmpty()) {
                Integer parcelaNumber = Integer.parseInt(textParcela);
                Float valueParcela = MathUtils.normalizeTwoDecimalPlaces(price / parcelaNumber);

                installmentPriceMap.put(parcelaNumber, valueParcela);
              }

            }
          }
        }

        if (comJuros != null) {
          Element parcela = comJuros.select(".parcela").first();
          Element priceElement = comJuros.select(".price").first();

          if (parcela != null && priceElement != null) {
            String text = parcela.ownText().replaceAll("[^0-9]", "").trim();
            Float value = MathUtils.parseFloatWithComma(priceElement.text());

            if (!text.isEmpty() && value != null && value > 0) {
              installmentPriceMap.put(Integer.parseInt(text), value);
            } else {
              // this case we calculate the installments
              String textParcela = installmentsElement.attr("data-maximo_parcelas").replaceAll("[^0-9]", "");
              String textJuros = installmentsElement.attr("data-juros").replaceAll("[^0-9.]", "").trim();

              if (!textParcela.isEmpty() && !textJuros.isEmpty()) {
                Integer parcelaNumber = Integer.parseInt(textParcela);
                Float juros = Float.parseFloat(textJuros) / 100f;

                Float valueParcela = calcJuros(price, parcelaNumber, juros);

                installmentPriceMap.put(parcelaNumber, valueParcela);
              }
            }
          }
        } else {
          // this case we calculate the installments
          String textParcela = installmentsElement.attr("data-maximo_parcelas_sem_juros").replaceAll("[^0-9]", "");

          if (!textParcela.isEmpty()) {
            Integer parcelaNumber = Integer.parseInt(textParcela);
            Float valueParcela = price / parcelaNumber;
            installmentPriceMap.put(parcelaNumber, valueParcela);
          }
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }


    return prices;
  }

  /**
   * This value is calculated based in price table, I was found in this link:
   * https://pt.wikipedia.org/wiki/Tabela_Price
   * 
   * @param price
   * @param installment
   * @param juros
   * @return
   */
  private Float calcJuros(Float price, Integer installment, Float juros) {
    Double value = price * (juros / (1 - Math.pow(1 + juros, installment * -1)));

    return MathUtils.normalizeTwoDecimalPlaces(value.floatValue());
  }

}
