package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilEsalpetCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Esalpet";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.ELO.toString(), Card.HIPERCARD.toString(), Card.HIPER.toString(),
      Card.DINERS.toString(), Card.DISCOVER.toString(), Card.AURA.toString());

   public BrasilEsalpetCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   @Override
   protected Response fetchResponse() {
      String id = getId();
      String url = "https://esal.api.alleshub.com.br/api_ecommerce/item/getById/" + id;

      Map<String, String> headers = new HashMap<>();
      headers.put("BaseConn", "{\"base\":\"erp_esalpet\",\"emp_id\":1}");
      headers.put("content-type", "application/json");

      Request request = Request.RequestBuilder.create().setUrl(url)
         .setHeaders(headers)
         .build();

      return this.dataFetcher.get(session, request);
   }

   private String getId() {
      String id = null;

      Pattern pattern = Pattern.compile("id=([0-9]+)", Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(session.getOriginalURL());

      if (matcher.find()) {
         id = matcher.group(1);

      }

      return id;
   }

   @Override
   public List<Product> extractInformation(JSONObject jsonObject) throws Exception {
      List<Product> products = new ArrayList<>();

      JSONObject data = jsonObject != null ? jsonObject.optJSONObject("data") : null;

      if (data != null && !data.has("error")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = data.optString("id");
         String internalPid = JSONUtils.getValueRecursive(data, "item_linkWeb.ecommerce_id", String.class);
         String name = JSONUtils.getValueRecursive(data, "item_linkWeb.linkWeb.titulo", String.class);
         // Site has only one category
         List<String> images = crawlImages(data);
         String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
         String description = JSONUtils.getValueRecursive(data, "item_linkWeb.linkWeb.descricaoHtml", String.class);
         Integer stock = JSONUtils.getValueRecursive(data, "estoque.estAtual", Integer.class, 0);
         boolean available = stock > 0;
         List<String> eans = crawlEan(data);
         Offers offers = available ? scrapOffers(data) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setStock(stock)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> crawlEan(JSONObject data) {
      String ean = data.optString("ean");
      if (ean != null && !ean.isEmpty()) {
         return Collections.singletonList(ean);
      }
      return Collections.emptyList();
   }

   private List<String> crawlImages(JSONObject data) {
      List<String> images = new ArrayList<>();

      JSONArray imagesArray = data.optJSONArray("fotos");
      if (imagesArray != null) {
         for (Object obj : imagesArray) {
            if (obj instanceof String) {
               String image = (String) obj;
               String url = "https://esal.api.alleshub.com.br/images/erp_esalpet/item/" + image + ".jpg";
               images.add(url);
            } else {
               break;
            }
         }
      }

      return images;

   }

   private Offers scrapOffers(JSONObject data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(data);
      List<String> sales = new ArrayList<>();

      offers.add(OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getValueRecursive(data, "tabela.vVenda", Double.class, null);
      //  Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price-discount.mr-4 span", null, true, ',', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());


      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

}
