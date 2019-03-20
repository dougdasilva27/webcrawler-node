package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcherNO;
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
 * Date: 09/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilNutriserviceCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.vitaesaude.com.br/";

  public BrasilNutriserviceCrawler(Session session) {
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

      String internalPid = crawlInternalPid(doc);
      String name = crawlName(doc);
      CategoryCollection categories = crawlCategories(doc);
      String description = crawlDescription(doc);
      Marketplace marketplace = crawlMarketplace();

      Elements variations = doc.select(".variante select option:not([value=0])");

      if (variations.size() > 1) {
        for (Element e : variations) {
          String internalId = e.val();
          String variationName = crawlVariationName(e, name);

          // Document dos preços da variação
          Document priceDocument = crawlPriceDocument(internalId, internalPid);

          Float price = crawlPrice(priceDocument, true);
          Prices prices = crawlPrices(price, priceDocument);
          boolean available = !e.hasClass("variante_select_indisponivel");
          Integer stock = null;

          // Document das imagens da variação
          Document imagesDocument = crawlImagesDocument(internalId, internalPid);

          String primaryImage = crawlPrimaryImage(imagesDocument);
          String secondaryImages = crawlSecondaryImages(imagesDocument, primaryImage);

          // Creating the product
          Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid)
              .setName(variationName).setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
              .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
              .setSecondaryImages(secondaryImages).setDescription(description).setStock(stock).setMarketplace(marketplace).build();

          products.add(product);
        }
      } else {
        String variationName = variations.size() == 1 ? crawlVariationName(variations.get(0), name) : name;
        String internalId = crawlInternalId(doc);
        Float price = crawlPrice(doc, false);
        Prices prices = crawlPrices(price, doc);
        Integer stock = crawlStock(doc);
        boolean available = stock != null && stock > 0;
        String primaryImage = crawlPrimaryImage(doc);
        String secondaryImages = crawlSecondaryImages(doc, primaryImage);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid)
            .setName(variationName).setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages).setDescription(description).setStock(stock).setMarketplace(marketplace).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    if (doc.select("#lblNome").first() != null) {
      return true;
    }
    return false;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Elements scripts = doc.select("script[type=text/javascript]");

    for (Element e : scripts) {
      String script = e.outerHtml().replace("<script type=\"text/javascript\">", "").replaceAll(" ", "");

      if (script.contains("variante[0][0]=")) {
        String[] tokens = script.split(";");

        for (String token : tokens) {
          if (token.trim().contains("variante[0][0]=")) {
            internalId = token.split("=")[1].replace("'", "").trim();
            break;
          }
        }

        break;
      }
    }

    return internalId;
  }

  private String crawlVariationName(Element e, String name) {
    String finalName;
    String variationName = e.ownText().trim();

    if (variationName.contains("(")) {
      int x = variationName.indexOf('(');
      finalName = name + " " + variationName.subSequence(0, x);
    } else {
      finalName = name + " " + variationName;
    }

    return finalName;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;
    Elements scripts = doc.select("script[type=text/javascript]");

    for (Element e : scripts) {
      String script = e.outerHtml().replace("<script type=\"text/javascript\">", "").replaceAll(" ", "");

      if (script.contains("produtoId=")) {
        String[] tokens = script.split(";");

        for (String token : tokens) {
          if (token.trim().contains("produtoId=")) {
            internalPid = token.split("=")[1].trim();
            break;
          }
        }

        break;
      }
    }
    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("#lblNome").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document, boolean isVariation) {
    Float price = null;

    Element salePriceElement;

    if (isVariation) {
      salePriceElement = document.select(".preco_por_texto strong").last();
    } else {
      salePriceElement = document.select(".preco_por").first();
    }

    if (salePriceElement != null) {
      price = MathUtils.parseFloatWithComma(salePriceElement.ownText());
    }

    return price;
  }

  private Document crawlPriceDocument(String internalId, String internalPid) {
    String url = "http://www.nutriservice.com.br/ParcelamentoVariante/CodVariante/" + internalId + "/produto_id/" + internalPid + "/t/1";

    return DataFetcherNO.fetchDocument(DataFetcherNO.GET_REQUEST, session, url, null, cookies);
  }

  private Document crawlImagesDocument(String internalId, String internalPid) {
    String url = "http://www.nutriservice.com.br/ImagensProduto/CodVariante/" + internalId + "/produto_id/" + internalPid + "/exibicao/produto/t/1";

    return DataFetcherNO.fetchDocument(DataFetcherNO.GET_REQUEST, session, url, null, cookies);
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element elementPrimaryImage = doc.select("#divImagemPrincipalZoom a").first();

    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("href");
    }

    return primaryImage;
  }

  /**
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(Document doc, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select("#thumblist li > a img");

    for (Element e : images) {
      String image = e.attr("src").replace("Pequenas", "SuperZoom").replace("Gigantes", "SuperZoom");

      if (!image.equals(primaryImage)) {
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
    Elements elementCategories = document.select(".migalha div[class^=migalha] > a");

    for (int i = 1; i < elementCategories.size(); i++) { // primeiro item é a home
      String cat = elementCategories.get(i).ownText().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element elementDescription = doc.select(".descricao").first();

    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    return description.toString();
  }

  private Integer crawlStock(Document doc) {
    Integer stock = null;
    Element stockElement = doc.select("#valorQuantidadeVariante").first();

    if (stockElement != null) {
      String stockString = stockElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!stockString.isEmpty()) {
        stock = Integer.parseInt(stockString);
      }
    }

    return stock;
  }

  /**
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      prices.setBankTicketPrice(price);

      Element installmentsElement = doc.select(".preco_por_texto label").first();

      if (installmentsElement != null) {
        Float aprazo = MathUtils.parseFloatWithComma(installmentsElement.ownText());
        if (aprazo != null) {
          installmentPriceMap.put(1, aprazo);
        }
      } else {
        installmentsElement = doc.select(".preco_por").first();

        if (installmentsElement != null) {
          Float aprazo = MathUtils.parseFloatWithComma(installmentsElement.ownText());
          if (aprazo != null) {
            installmentPriceMap.put(1, aprazo);
          }
        }
      }

      // prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      // prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      // prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      // prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    }

    return prices;
  }
}
