package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.*;

import br.com.lett.crawlernode.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;

public class BrasilEfacilCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "eFácil";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilEfacilCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      JSONObject dataJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, false);
      JSONObject productArray = JSONUtils.getValueRecursive(dataJson, "props.pageProps.staticProductData", JSONObject.class);

      if (productArray != null && !productArray.isEmpty()) {
         String internalId = JSONUtils.getValueRecursive(productArray, "idProduto", String.class);
         String internalPid = JSONUtils.getValueRecursive(productArray, "sku", String.class);
         String name = JSONUtils.getValueRecursive(productArray, "nome", String.class);
         JSONArray images = JSONUtils.getValueRecursive(productArray, "skus.0.imagens", JSONArray.class, new JSONArray());
         String primaryImage = JSONUtils.getValueRecursive(images, "0.url1000", String.class);
         List<String> secondaryImages = getSecondaryImages(images);
         String description = JSONUtils.getValueRecursive(productArray, "descricao", String.class);
         String categories = JSONUtils.getValueRecursive(productArray, "subcategoria", String.class, "");
         boolean available = JSONUtils.getValueRecursive(productArray, "skus.0.disponivel", Boolean.class);
         //Offers offers = stock > 0 ? scrapOffers(data) : new Offers();
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            //.setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setCategories(Collections.singleton(categories))
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }
      return products;
   }


   private List<String> getSecondaryImages(JSONArray images) {
      List<String> secondaryImages = new ArrayList<>();
      if (images != null) {
         for (Object o : images) {
            JSONObject data = (JSONObject) o;
            String image = JSONUtils.getValueRecursive(data, "url1000", String.class);

            if (image != null) {
               secondaryImages.add(image);
            }
         }
         if (secondaryImages.size() > 0) {
            secondaryImages.remove(0);
         }
         return secondaryImages;
      }
      return null;
   }


   private Offers scrapOffers(Document doc, String internalPid, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(internalPid, internalId);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(getSellerName(doc))
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(getSellerName(doc).equalsIgnoreCase(SELLER_FULL_NAME))
         .setSales(sales)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".line-through", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".priceby span span", null, false, ',', session);
      Double bankSlip = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#priceViewInCash > span.blue > span", null, false, ',', session);
      if (bankSlip == null) {
         bankSlip = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#ProductPriceView > div.priceby > span.large > span", null, false, ',', session);
      }
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(scrapBankSlip(bankSlip))
         .setCreditCards(creditCards)
         .build();

   }

   private BankSlip scrapBankSlip(Double bankSlipPrice) throws MalformedPricingException {
      return BankSlip.BankSlipBuilder.create()
         .setFinalPrice(bankSlipPrice)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(doc);
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   public Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();
      Element installmentsCard = doc.selectFirst(".container-price-installment span.blue");

      if (installmentsCard != null) {
         String installmentString = installmentsCard.ownText().replaceAll("[^0-9]", "").trim();
         Integer installment = !installmentString.isEmpty() ? Integer.parseInt(installmentString) : null;
         Element valueElement = doc.selectFirst("#valBold");

         if (valueElement != null && installment != null) {
            Double value = MathUtils.parseDoubleWithComma(valueElement.text());

            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(installment)
               .setInstallmentPrice(value)
               .build());
         }
      }

      return installments;
   }

   private List<String> scrapSales(String internalPid, String internalId) {

      String apiPrice = "https://www.efacil.com.br/webapp/wcs/stores/servlet/ProductPriceView?storeId=10154&catalogId=10051&productInfoPage=true&type=product&productIdPRD=" + internalPid + "&catalogEntryId=" + internalId;

      Map<String, String> headers = new HashMap<>();

      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.117 Safari/537.36");
      headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");

      Request request = RequestBuilder.create().setUrl(apiPrice).setHeaders(headers).mustSendContentEncoding(false).build();
      String response = this.dataFetcher.get(session, request).getBody();
      Document htmlPrice = Jsoup.parse(response);

      List<String> sales = new ArrayList<>();
      String sale = CrawlerUtils.scrapStringSimpleInfo(htmlPrice, "#entryPriceAtacarejo", false);
      if (sale != null) {
         sales.add(sale);
      }

      return sales;
   }

   private String getSellerName(Document doc) {

      return CrawlerUtils.scrapStringSimpleInfo(doc, "#nomeEntregue", false);
   }
}
