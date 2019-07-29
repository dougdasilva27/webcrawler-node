package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
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
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.prices.Prices;

public class BrasilColomboCrawler extends Crawler {

  public BrasilColomboCrawler(Session session) {
    super(session);
  }


  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith("https://www.colombo.com.br");
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    Element productElement = doc.select(".detalhe-produto").first();

    if (session.getOriginalURL().contains("www.colombo.com.br/produto/") && (productElement != null)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      Elements selections = doc.select(".dados-itens-table tr[data-item]");

      // Pid
      String internalPid = null;
      Element elementInternalPid = doc.select(".codigo-produto").first();
      if (elementInternalPid != null) {
        internalPid = elementInternalPid.attr("content").trim();
        if (internalPid.isEmpty()) {
          internalPid = elementInternalPid.text().replaceAll("[^0-9]", "").trim();
        }
      }

      // ID interno
      String internalId = null;
      Element elementInternalID = doc.select("input[type=radio][checked][value]").first();
      if (elementInternalID != null) {
        internalId = elementInternalID.attr("value").trim();
      } else {
        elementInternalID = doc.select("#itemAviso").first();
        if (elementInternalID != null) {
          internalId = elementInternalID.attr("value").trim();
        }
      }

      // Nome
      String name = null;
      Element elementName = doc.select("h1.nome-produto").first();
      if (elementName != null) {
        name = elementName.ownText().trim();
      }

      // Preço
      Float price = null;
      Element elementPrice = doc.select(".parcelas-produto-table .parcelas-produto-table-valor").first();
      if (elementPrice != null) {
        price = MathUtils.parseFloatWithComma(elementPrice.ownText());
      }

      Prices prices = crawlPrices(doc, price);

      // Categoria
      String category1 = "";
      String category2 = "";
      String category3 = "";
      Elements elementCategories = doc.select(".breadcrumb a");
      for (int i = 1; i < elementCategories.size(); i++) {
        if (category1.isEmpty()) {
          category1 = elementCategories.get(i).text().trim();
        } else if (category2.isEmpty()) {
          category2 = elementCategories.get(i).text().trim();
        } else if (category3.isEmpty()) {
          category3 = elementCategories.get(i).text().trim();
        }
      }

      // Descrição
      String description = scrapDescription(doc);

      Element elementPrimaryImage = doc.select("li.js_slide picture img[data-slide-position=0]").first();
      String primaryImage = null;
      if (elementPrimaryImage != null) {
        primaryImage = sanitizeImageURL(elementPrimaryImage.attr("src").trim().replace("400x400", "800x800"));
      } else {
        elementPrimaryImage = doc.select("li.js_slide picture img[data-slide-position=1]").first();

        if (elementPrimaryImage != null) {
          primaryImage = sanitizeImageURL(elementPrimaryImage.attr("src").trim().replace("400x400", "800x800"));
        }
      }

      // Imagens -- Caso principal
      Elements elementSecondaryImages = doc.select("li.js_slide picture img");
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      for (Element el : elementSecondaryImages) {
        String image = sanitizeImageURL(el.attr("src").trim().replace("400x400", "800x800"));
        if (image != null && !image.equals(primaryImage)) { // imagem primária
          secondaryImagesArray.put(image);
        }
      }

      if (secondaryImagesArray.length() > 0) {
        secondaryImages = secondaryImagesArray.toString();
      }

      // Estoque
      Integer stock = null;

      // Marketplace
      Marketplace marketplace = new Marketplace();

      // Disponibilidade
      boolean available = true;
      Element elementUnavailable = doc.select("#dados-produto-indisponivel.avisoIndisponivel:not(.hide)").first();
      if (elementUnavailable != null) {
        available = false;
      }

      if (selections.size() <= 1) { // sem variações
        Offers offers = available ? scrapBuyBox(doc, price) : new Offers();

        if (!available) {
          price = null;
          prices = new Prices();
        }

        // esse caso acontece quando o internalId não aparece, apenas o internalPid
        // isso acontece quando o produto está indisponível em alguns casos
        if (!available && (internalId == null || internalId.isEmpty())) {
          internalId = internalPid + "-i";
        }

        Product product = new Product();
        product.setUrl(session.getOriginalURL());
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
        product.setOffers(offers);

        products.add(product);

      }

      else { // múltiplas variações

        for (Element e : selections) {

          // ID interno
          String variationInternalId = null;
          Element variationElementInternalID = e.selectFirst("input[name=item]");
          if (variationElementInternalID != null) {
            variationInternalId = variationElementInternalID.val();
          }

          // Nome
          String variationName = null;
          Element variationElementName = e.selectFirst(".caracteristicasdados-itens-table-caracteristicas > label");
          if (variationElementName != null) {
            variationName = name + " " + variationElementName.ownText().split("- R")[0];
          }

          // Available
          boolean variationAvailable = available;
          Element variationElementAvailable = e.select(".dados-itens-table-estoque").first();
          if (variationElementAvailable != null && variationAvailable) {
            String tmp = variationElementAvailable.text().trim();
            if (tmp.contains("Esgotado")) {
              variationAvailable = false;
            }
          }

          Offers offers = available ? scrapBuyBox(e, price) : new Offers();

          if (!variationAvailable) {
            price = null;
            prices = new Prices();
          }

          Product product = new Product();
          product.setUrl(session.getOriginalURL());
          product.setInternalId(variationInternalId);
          product.setInternalPid(internalPid);
          product.setName(variationName);
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
          product.setAvailable(variationAvailable);
          product.setOffers(offers);

          products.add(product);

        }

      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private Offers scrapBuyBox(Element doc, Float price) {
    Offers offers = new Offers();
    try {
      Element nameElement = doc.selectFirst(".dados-itens-table-estoque .label-linha-item .btn-show-info-seller");
      Element sellerIdElement = doc.selectFirst("input[name=codigoSeller]");
      Element elementPrice = doc.selectFirst(".label-preco-item");
      Double mainPrice = price.doubleValue();
      String sellerFullName = null;
      String slugSellerName = null;
      String internalSellerId = null;

      if (nameElement != null) {
        sellerFullName = nameElement.text();
        slugSellerName = CrawlerUtils.toSlug(sellerFullName);
      }

      if (sellerIdElement != null) {
        internalSellerId = sellerIdElement.attr("value");
      }


      if (elementPrice != null) {
        mainPrice = MathUtils.parseDoubleWithComma(elementPrice.ownText());
      }

      Offer offer = new OfferBuilder().setSellerFullName(sellerFullName).setSlugSellerName(slugSellerName).setInternalSellerId(internalSellerId)
          .setMainPagePosition(1).setIsBuybox(false).setMainPrice(mainPrice).build();

      offers.add(offer);

    } catch (OfferException e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    }

    return offers;
  }


  private String sanitizeImageURL(String imageURL) {
    String sanitizedURL = imageURL;

    if (imageURL != null) {
      if (imageURL.contains("?")) { // removendo parâmetros da url da imagem, senão não passa no crawler de imagens
        int index = imageURL.indexOf('?');
        sanitizedURL = imageURL.substring(0, index);
      }
    }

    try {
      URI rawUri = new URI(sanitizedURL);
      if (rawUri.getScheme() == null) {
        return new URIBuilder().setScheme("https").setHost(rawUri.getHost()).setPath(rawUri.getPath()).build().toString();
      }

      return rawUri.toString();

    } catch (URISyntaxException uriSyntaxException) {
      Logging.printLogDebug(logger, session, "Not a valid image URL " + CommonMethods.getStackTrace(uriSyntaxException));
    }

    return sanitizedURL;
  }

  private String scrapDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Elements titles = doc.select(".ancoras .ancoras-item");
    for (Element e : titles) {
      if (e.select(".ci-duvidas, [class~=ci-star]").isEmpty()) {
        description.append(e.html());
      }
    }

    description.append(CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#produto-descricao", "#produto-caracteristicas")));

    return description.toString();
  }

  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      installmentPriceMap.put(1, price);

      Element bankPrice = doc.select(".dados-preco-valor").first();

      if (bankPrice != null) {
        Float bankTicketPrice = Float.parseFloat(bankPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
        prices.setBankTicketPrice(bankTicketPrice);
      }

      Elements parcelas = doc.select(".detalhe-produto-dados__dados-comprar .parcelas-produto-table tr");

      for (Element e : parcelas) {
        Element index = e.select(".parcelas-produto-table-index").first();

        if (index != null) {
          Integer installment = Integer.parseInt(index.text().replaceAll("[^0-9]", "").trim());

          Element valor = e.select(".parcelas-produto-table-valor").first();

          if (valor != null) {
            Float value = Float.parseFloat(valor.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

            installmentPriceMap.put(installment, value);
          }
        }

        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.HIPER.toString(), installmentPriceMap);
      }
    }

    return prices;
  }
}
