package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author Paula
 */

public class BrasilCasadoprodutorCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.casadoprodutor.com.br/";

  public BrasilCasadoprodutorCrawler(Session session) {
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

    JSONObject dataLayer = CrawlerUtils
        .selectJsonFromHtml(doc, "script", "dataLayer[0]['product'] =", ";", false, true);

    if (dataLayer.has("id")) {
      Logging.printLogDebug(logger, session,
          "Product page identified: " + this.session.getOriginalURL());

      String internalIds = getMainProductId(dataLayer);
      String internalPid = crawlInternalPid(doc);

      CategoryCollection categories = CrawlerUtils
          .crawlCategories(doc, ".respiro_conteudo .migalha a");
      String description = CrawlerUtils
          .scrapSimpleDescription(doc,
              Collections.singletonList(".descricao_texto .descricao_texto_conteudo"));
      String mainProductId = getMainProductId(dataLayer);

      // sku data in json
      JSONArray arraySkus = dataLayer != null && dataLayer.has("variants") && dataLayer
          .get("variants") instanceof JSONArray ? dataLayer.getJSONArray("variants")
          : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String name = crawlName(jsonSku);
        String internalId = getMainProductId(jsonSku);

        Document productAPI = captureImageAndPricesInfo(internalIds, internalPid, mainProductId,
            doc);

        Offers offers = crawlAvailability(jsonSku) ? scrapOffers(jsonSku) : new Offers();
        String primaryImage = CrawlerUtils
            .scrapSimplePrimaryImage(productAPI, ".produto_foto #divImagemPrincipalZoom > a",
                Collections.singletonList("href"),
                "https:", "www.casadoprodutor.com.br");
        String secondaryImages = crawlSecondaryImages(doc);

        String ean = crawlEan(jsonSku);
        List<String> eans = ean != null && !ean.isEmpty() ? Collections.singletonList(ean) : null;

        // Creating the product
        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setEans(eans)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************
   * General methods *
   *******************/

  private String getMainProductId(JSONObject json) {
    String mainProductId = null;

    if (json.has("id") && !json.isNull("id")) {
      mainProductId = json.get("id").toString();
    }

    return mainProductId;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = CrawlerUtils
        .scrapStringSimpleInfo(doc, ".product-name .produto_codigo span", false);

    if (internalPid != null && internalPid.contains("-")) {
      int finalInternalPid = internalPid.indexOf("-");
      internalPid = internalPid.substring(0, finalInternalPid);
    }
    return internalPid;
  }

  private String crawlName(JSONObject skuJson) {
    String name = null;

    if (skuJson.has("name") && !skuJson.isNull("name")) {
      name = skuJson.get("name").toString();
    }
    return name;
  }

  private boolean crawlAvailability(JSONObject jsonSku) {
    return jsonSku.has("available") && jsonSku.get("available") instanceof Boolean && jsonSku
        .getBoolean("available") && jsonSku.optDouble("payInFullPrice", 0D) != 0D;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select("#thumblist li:not(:first-child) > a");
    for (Element e : images) {
      JSONObject rel = CrawlerUtils.stringToJson(e.attr("rel"));

      if (rel.has("largeimage")) {
        secondaryImagesArray.put(rel.get("largeimage"));
      } else if (rel.has("smallimage")) {
        secondaryImagesArray.put(rel.get("smallimage"));
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  /**
   * Access prices and images api's
   *
   * @param internalId
   * @param internalPid
   * @param mainProductId
   * @param mainPage
   * @return
   */
  private Document captureImageAndPricesInfo(String internalId, String internalPid,
      String mainProductId, Document mainPage) {
    Document doc = mainPage;

    if (mainProductId != null && !mainProductId.equals(internalId)) {
      doc = new Document("");
      String imagesUrl =
          "https://www.cassol.com.br/ImagensProduto/CodVariante/" + internalId + "/produto_id/"
              + internalPid + "/exibicao/produto/t/32";
      String pricesUrl = "https://www.cassol.com.br/ParcelamentoVariante/CodVariante/" + internalId
          + "/produto_id/" + internalPid + "/t/32";

      Request requestImages = RequestBuilder.create().setUrl(imagesUrl).setCookies(cookies).build();
      doc.append(Jsoup.parse(this.dataFetcher.get(session, requestImages).getBody()).toString());

      Request requestPrices = RequestBuilder.create().setUrl(pricesUrl).setCookies(cookies).build();
      doc.append(Jsoup.parse(this.dataFetcher.get(session, requestPrices).getBody()).toString());
    }

    return doc;
  }

  private Offers scrapOffers(JSONObject json) throws MalformedPricingException, OfferException {
    Offers offers = new Offers();
    Double price = JSONUtils.getDoubleValueFromJSON(json, "payInFullPrice", true);

    CreditCards creditCards = new CreditCards(
        Stream.of(Card.MASTERCARD, Card.VISA, Card.AMEX, Card.DINERS, Card.ELO,
            Card.HIPERCARD).map(card -> {
          int quantityInst = json.optInt("quantityOfInstallmentsNoInterest");
          double installment = json.optDouble("valueOfInstallmentsNoInterest");
          try {
            return CreditCardBuilder.create()
                .setBrand(card.toString())
                .setIsShopCard(false)
                .setInstallments(new Installments(Collections.singleton(InstallmentBuilder.create()
                    .setInstallmentPrice(installment)
                    .setInstallmentNumber(quantityInst)
                    .build())))
                .build();
          } catch (MalformedPricingException e) {
            throw new RuntimeException(e);
          }
        }).collect(Collectors.toList()));

    offers.add(OfferBuilder.create()
        .setIsBuybox(false)
        .setPricing(PricingBuilder.create()
            .setSpotlightPrice(price)
            .setPriceFrom(json.optDouble("price") != price ? json.optDouble("price") : null)
            .setBankSlip(BankSlipBuilder.create()
                .setFinalPrice(price)
                .build())
            .setCreditCards(creditCards)
            .build())
        .setSellerFullName("Casa do produtor")
        .setIsMainRetailer(true)
        .setUseSlugNameAsInternalSellerId(true)
        .build());

    return offers;
  }

  private String crawlEan(JSONObject json) {
    String ean = null;

    if (json.has("ean") && !json.isNull("ean")) {
      Object obj = json.get("ean");

      if (obj != null) {
        ean = obj.toString();
      }
    }
    return ean;
  }
}
