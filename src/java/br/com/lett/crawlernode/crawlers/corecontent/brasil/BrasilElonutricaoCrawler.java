package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
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
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 11/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilElonutricaoCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.elonutricao.com.br/";

  public BrasilElonutricaoCrawler(Session session) {
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
      String internalPid = null;
      String name = crawlName(doc);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price, doc);
      boolean available = crawlAvailability(doc);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
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
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    if (doc.select("#prod_padd").first() != null) {
      return true;
    }
    return false;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select("#ProdutoId").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    }

    return internalId;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(".prod_tit").first();

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;
    Element salePriceElement = document.select(".prod_box_info_top span[itemprop=price]").first();

    if (salePriceElement != null) {
      price = MathUtils.parseFloat(salePriceElement.text());
    }

    return price;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element elementPrimaryImage = doc.select(".zoomPad .jqzoom").first();

    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("href");
    }

    return primaryImage;
  }

  /**
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imgs = doc.select(".galeria-produto li img");

    for (int i = 1; i < imgs.size(); i++) {
      secondaryImagesArray.put(imgs.get(i).attr("src").replace("min", "zoom"));
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
    Elements elementCategories = document.select(".prod_nav li a");

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

    Element elementDescription = doc.select(".prod_box_descricao_conteudo").first();

    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    return description.toString();
  }

  private boolean crawlAvailability(Document doc) {
    return doc.select(".produto_detalhe_comprar_btn").first() != null;
  }

  /**
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    Element pricesUrl = doc.select("#btn-forma-pagamento-seta").first();

    Element priceFrom = doc.select(".prod_valor_de").first();
    if (priceFrom != null) {
      prices.setPriceFrom(MathUtils.parseDouble(priceFrom.text()));
    }

    if (pricesUrl != null) {
      String url = HOME_PAGE + pricesUrl.attr("data-ajax-post-url");

      Document pricesDoc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
      Elements cards = pricesDoc.select(".produto_list_pagamento li");

      for (Element e : cards) {
        String cardName = crawlCardName(e);
        Map<Integer, Float> installmentPriceMap = crawlInstallments(e);

        if (cardName != null && cardName.equals("boleto")) {

          if (installmentPriceMap.containsKey(1)) {
            prices.setBankTicketPrice(installmentPriceMap.get(1));
          }

        } else if (cardName != null) {
          prices.insertCardInstallment(cardName, installmentPriceMap);
        }
      }

    } else if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      prices.setBankTicketPrice(price);
      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    }

    return prices;
  }

  private Map<Integer, Float> crawlInstallments(Element card) {
    Map<Integer, Float> installmentPriceMap = new TreeMap<>();

    Elements installments = card.select(".parcelamento_tabela tr");

    for (Element x : installments) {
      String text = x.text();

      if (text.contains("x")) {
        int indexX = text.indexOf('x');
        int indexY = text.indexOf("(", indexX);

        String installmentText = text.substring(0, indexX).replaceAll("[^0-9]", "");
        Float value = MathUtils.parseFloat(text.substring(indexX, indexY).trim());

        if (!installmentText.isEmpty() && installmentText != "1" && value != null) {
          installmentPriceMap.put(Integer.parseInt(installmentText), value);
        }
      }
    }

    return installmentPriceMap;
  }

  private String crawlCardName(Element card) {
    String cardName = null;

    Element name = card.select("h4").first();

    if (name != null) {
      String option = name.ownText().toLowerCase();

      if (option.contains("visa")) {
        cardName = Card.VISA.toString();
      } else if (option.contains("mastercard")) {
        cardName = Card.MASTERCARD.toString();
      } else if (option.contains("elo")) {
        cardName = Card.ELO.toString();
      } else if (option.contains("diners")) {
        cardName = Card.DINERS.toString();
      } else if (option.contains("discover")) {
        cardName = Card.DISCOVER.toString();
      } else if (option.contains("boleto")) {
        cardName = "boleto";
      }
    }

    return cardName;
  }
}
