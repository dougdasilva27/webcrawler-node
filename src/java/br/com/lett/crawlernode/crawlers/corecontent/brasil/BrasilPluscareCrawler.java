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
 * Date: 10/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilPluscareCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.pluscare.com.br";

  public BrasilPluscareCrawler(Session session) {
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

    if (isProductPage(session.getOriginalURL())) {
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

  private boolean isProductPage(String url) {
    if (url.startsWith(HOME_PAGE + "/produto/")) {
      return true;
    }
    return false;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select("#IdProduto").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    }

    return internalId;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("#NomeProduto").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;
    Element salePriceElement = document.select(".precoPor").first();

    if (salePriceElement != null) {
      price = MathUtils.parseFloat(salePriceElement.ownText());
    }

    return price;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element elementPrimaryImage = doc.select("#zoomProduto img").first();

    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("src").trim();
    }

    return primaryImage;
  }

  /**
   * Quando o crawler foi feito não havia imagens secundárias
   * 
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select("#ListarMultiFotos li a");

    for (int i = 1; i < images.size(); i++) { // primeira imagem é a imagem primária
      String image = images.get(i).attr("href");

      if (image.isEmpty() || !image.toLowerCase().endsWith("jpg")) {
        Element img = images.get(i).select("img").first();

        if (img != null) {
          image = img.attr("src");
        }
      }

      secondaryImagesArray.put(image);
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
    Elements elementCategories = document.select(".breadCrumbs a");

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

    Element shortDescription = doc.select(".descResumida").first();

    if (shortDescription != null) {
      description.append(shortDescription.html());
    }

    Element descriptionElement = doc.select(".descricao").first();

    if (descriptionElement != null) {
      description.append(descriptionElement.html());
    }

    return description.toString();
  }

  private boolean crawlAvailability(Document doc) {
    return doc.select("#SemEstoque[style=display:block;]").first() == null;
  }

  /**
   * Quando fiz o crawler não achei produto com informações de parcelamento
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

      Element parc = doc.select("#ParcelamentoProduto").first();

      if (parc != null) {
        String parcText = parc.ownText().replaceAll("[^0-9]", "").trim();
        Element valueElement = parc.select("span").first();

        if (parcText.isEmpty() && valueElement != null) {
          Float value = MathUtils.parseFloat(valueElement.ownText());
          Integer parcel = Integer.parseInt(parcText);

          if (parcel > 1 && value != null) {
            installmentPriceMap.put(parcel, value);
          }
        }
      }

      prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }

}
