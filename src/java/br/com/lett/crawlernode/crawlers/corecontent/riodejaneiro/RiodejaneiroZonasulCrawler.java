package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class RiodejaneiroZonasulCrawler extends Crawler {

  private final String HOME_PAGE = "https://www.zonasul.com.br/";

  public RiodejaneiroZonasulCrawler(Session session) {
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

    if (!doc.select(".carrinho_vitrine div").isEmpty()) {
      RiodejaneiroNewZonasulCrawler zs = new RiodejaneiroNewZonasulCrawler();
      products.addAll(zs.extractInformation(doc, session, logger));
    } else if (isProductPage(this.session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // ID interno
      String internalId = null;
      Element elementID = doc.select(".codigo").first();
      if (elementID != null) {
        if (!elementID.text().trim().isEmpty()) {
          internalId = Integer.toString(Integer.parseInt(elementID.text().replaceAll("[^0-9,]+", ""))).trim();
        }
      }

      // Pid
      String internalPid = internalId;

      // Nome
      String name = null;
      Element elementName = doc.select(".top h3").first();
      if (elementName != null) {
        name = elementName.text().trim();
      }

      // Disponibilidade
      boolean available = true;
      Element elementUnavailable = doc.select("#ctl00_cphMasterPage1_pnlProdutoIndisponivel").first();
      if (elementUnavailable != null) {
        available = false;
      }

      // Preço
      Float price = null;
      if (available) {
        Elements elementPrice = doc.select("#ctl00_cphMasterPage1_pnlPreco p.preco");
        if (elementPrice.size() == 0) {
          elementPrice = doc.select("#ctl00_cphMasterPage1_pnlPrecoRebaixa p.preco_por");
        }

        try {
          price = Float.parseFloat(elementPrice.first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
        } catch (Exception e) {
          Logging.printLogError(logger, session, "Preço não encontrado para o produto: " + this.session.getOriginalURL());
        }
      }

      // Categoires
      CategoryCollection categories = crawlCategories(doc);

      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);

      // Descrição
      String description = "";

      Element elementInfoUL = doc.select(".info ul").first();
      if (elementInfoUL != null) {
        description = description + elementInfoUL.html();
      }

      Element elementInfoDET = doc.select(".info .det").first();
      if (elementInfoDET != null) {
        description = description + elementInfoDET.html();
      }

      // Estoque
      Integer stock = null;

      // Marketplace
      Marketplace marketplace = new Marketplace();

      // Prices
      Prices prices = crawlPrices(doc, price);

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


  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url) {
    return url.startsWith("https://www.zonasul.com.br/Produto/") || url.startsWith("http://www.zonasulatende.com.br/Produto/");
  }

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element elementPrimaryImage = doc.select(".img_principal").first();
    if (elementPrimaryImage != null) {
      String onClickAttr = elementPrimaryImage.attr("onclick").trim();

      if (!onClickAttr.isEmpty() && onClickAttr.contains("'")) {
        int beginIndex = onClickAttr.indexOf('\'');
        int endIndex = onClickAttr.lastIndexOf('\'');

        primaryImage = onClickAttr.substring(beginIndex + 1, endIndex);
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc) throws MalformedURLException {
    String primaryImage = crawlPrimaryImage(doc);
    String secondaryImages = null;

    JSONArray secondaryImagesArray = new JSONArray();
    Elements elementSecondaryImage = doc.select("#ctl00_cphMasterPage1_dlFotos td > a");

    if (elementSecondaryImage.size() > 1) {
      for (Element e : elementSecondaryImage) {
        String onClickAttr = e.attr("onclick");

        int beginIndex = onClickAttr.indexOf('\'');
        int endIndex = onClickAttr.lastIndexOf('\'');

        String imageUrl = onClickAttr.substring(beginIndex + 1, endIndex);

        if (imageUrl != null && !imageUrl.equals(primaryImage)) {

          String primaryImageSize = parseImageDimensions(primaryImage);
          if (primaryImageSize != null) {
            imageUrl = imageUrl.replaceFirst("280_280", primaryImageSize);
          }

          if (!imageUrl.equals(primaryImage)) { // check again, because now the imageUrl could have been modified
            secondaryImagesArray.put(imageUrl);
          }
        }
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String parseImageDimensions(String imageUrl) {
    String[] tokens = imageUrl.split("/"); // https://www.zonasul.com.br/ImgProdutos/280_280/2015422_37827.jpg
    if (tokens.length >= 5) {
      return tokens[4];
    }
    return null;
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

      Element priceFrom = doc.select(".top .prod_preco .preco_de").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDouble(priceFrom.text()));
      }

      installmentPriceMap.put(1, price);
      // prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
    }

    return prices;
  }

  /**
   * No momento que o crawler foi feito não foi achado produtos com categorias
   * 
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(Document doc) {
    CategoryCollection categories = new CategoryCollection();
    Element elementCategories = doc.select(".nomeComTopo h2").first();

    if (elementCategories != null) {
      categories.add(elementCategories.ownText());
    }

    return categories;
  }
}
