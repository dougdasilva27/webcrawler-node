package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offers;
import models.prices.Prices;
import models.pricing.*;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Date: 08/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilDrogafujiCrawler extends Crawler {

  private final String HOME_PAGE = "https://www.drogafuji.com.br/";
  private static final String SELLER_FULL_NAME = "droga-fuji-brasil";

  protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(), Card.DINERS.toString());

  public BrasilDrogafujiCrawler(Session session) {
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

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input#___rc-p-sku-ids", "value");
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".skuReference", false);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".productName", false);
      boolean isAvailable = crawlAvailability(doc);
      Offers offers = isAvailable ? crawlOffers(doc) : new Offers();
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#image-main", Collections.singletonList("src"), "https", "drogafuji.vteximg.com.br");
      List<String> secondaryImages = crawlSecondaryImages(doc);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".productDescription .productDescription"));
      Integer stock = null;
      Marketplace marketplace = crawlMarketplace();

      // Creating the product
      Product product = ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setCategories(categories)
         .setPrimaryImage(primaryImage)
         .setSecondaryImages(secondaryImages)
         .setDescription(description)
         .setStock(stock)
         .setOffers(offers)
         .setMarketplace(marketplace)
         .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return doc.select(".product-details").first() != null;
  }

   private Offers crawlOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      if(pricing != null){
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double[] prices = scrapPrices(doc);
      Double priceFrom = null;
      Double spotlightPrice = null;
      if (prices.length >= 2) {
         priceFrom = prices[0];
         spotlightPrice = prices[1];
      }

      if(spotlightPrice != null){
         CreditCards creditCards = scrapCreditCards(spotlightPrice);
         BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build();

         return Pricing.PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();
      }else{
         return null;
      }
   }

   private Double[] scrapPrices(Document doc){
      Double spotlightPrice;
      Double priceFrom;

      priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".skuListPrice", null, false, ',', session);
      if (priceFrom != null && priceFrom == 0) {
         priceFrom = null;
      }

      spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".skuBestPrice", null, false, ',', session);

      return new Double[]{priceFrom, spotlightPrice};
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());


      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String saleDiscount = CrawlerUtils.calculateSales(pricing);
      sales.add(saleDiscount);

      return sales;
   }

  private Float crawlPrice(Document document, String internalId) {
    Float price = null;

    String priceText = null;
    Element salePriceElement = document.select("#product-price-" + internalId).first();

    if (salePriceElement != null) {
      priceText = salePriceElement.text();
      price = MathUtils.parseFloatWithComma(priceText);
    }

    return price;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element elementPrimaryImage = doc.select(".product-img-box > a").first();

    if (elementPrimaryImage == null) {
      elementPrimaryImage = doc.select(".product-img-box > img").first();
    }

    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("href").trim();

      if (primaryImage.isEmpty()) {
        primaryImage = elementPrimaryImage.attr("src").trim();
      }
    }

    return primaryImage;
  }

  /**
   * In the time when this crawler was made, this market hasn't secondary Images
   * 
   * @param doc
   * @return
   */
  private List<String> crawlSecondaryImages(Document doc) {
    List<String> secondaryImages = new ArrayList<>();

    Elements imagesLi = doc.select(".thumbs img");
    for (Element imageLi : imagesLi) {
       secondaryImages.add(imageLi.attr("src"));
    }

    if (secondaryImages.size() > 0) {
       secondaryImages.remove(0);
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
    Elements elementCategories = document.select(".bread-crumb ul > li");

     for (Element elementCategory : elementCategories) {
        String category = CrawlerUtils.scrapStringSimpleInfo(elementCategory, "span[itemprop=name]", false);
        categories.add(category);
     }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element elementDescription = doc.select(".box-collateral.box-description").first();

    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    Element elementAditional = doc.select(".box-collateral.box-additional").first();

    if (elementAditional != null) {
      description.append(elementAditional.html());
    }

    return description.toString();
  }

  private boolean crawlAvailability(Document doc) {
    return doc.select(".skuBestPrice").first() != null;
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
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      Element priceFrom = doc.select(".old-price span[id]").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
      }

      Elements installmentsElement = doc.select("#parcelamento table tr");

      for (Element e : installmentsElement) {
        Elements values = e.select("td");

        if (values.size() > 1) {
          String textInstallment = values.get(0).ownText().replaceAll("[^0-9]", "").trim();
          Integer installment = !textInstallment.isEmpty() ? Integer.parseInt(textInstallment) : 1;

          String textValue = values.get(1).ownText().replaceAll("[^0-9,]", "").replaceAll(",", ".").trim();
          Float value = !textValue.isEmpty() ? Float.parseFloat(textValue) : null;

          if (value != null) {
            installmentPriceMap.put(installment, value);
          }
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }

}
