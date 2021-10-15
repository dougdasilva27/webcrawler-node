package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CariacicaObaCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Oba";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public CariacicaObaCrawler(Session session) {
      super(session);
   }

   @Override
   protected JSONObject fetch() {

      String internalId = getProductId();

      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "ls.uid_armazem=" + session.getOptions().optString("id_armazen"));

      String url = "https://www.superoba.com.br/api/produto?id=" + internalId;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      String content = this.dataFetcher.get(session, request).getBody();

      return CrawlerUtils.stringToJson(content);

   }

   private String getProductId() {
      String url = null;
      Pattern pattern = Pattern.compile("\\/([0-9]+)\\/");
      Matcher matcher = pattern.matcher(session.getOriginalURL());
      if (matcher.find()) {
         url = matcher.group(1);
      }
      return url;
   }

   @Override
   public List<Product> extractInformation(JSONObject jsonObject) throws Exception {
      super.extractInformation(jsonObject);
      List<Product> products = new ArrayList<>();

      if (jsonObject != null && jsonObject.has("Produtos")) {
         JSONObject productJson = JSONUtils.getValueRecursive(jsonObject, "Produtos.0", JSONObject.class);
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalId = productJson.optString("id_produto");
         String name = productJson.optString("str_nom_produto");
         String primaryImage = crawlPrimaryImage(productJson);
         boolean availableToBuy = productJson.optString("str_disponibilidade").equals("Em Estoque");
         Offers offers = availableToBuy ? scrapOffer(productJson) : new Offers();
         CategoryCollection categories = scrapCategories(productJson);
         List<String> eans = new ArrayList<>();
         eans.add(productJson.optString("str_cod_barras_produto"));


         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setEans(eans)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffer(JSONObject productJson) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productJson);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
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

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(productJson, "mny_vlr_promo_tabela_preco", true);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(productJson, "mny_vlr_produto_tabela_preco", true);
      if (priceFrom != null && priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
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

   private String crawlPrimaryImage(JSONObject productJson) {
      String primaryImage = productJson.optString("str_img_path_cdn");
      return CrawlerUtils.completeUrl(primaryImage + "-g.jpg", "https", "obasuperatacado-img.azureedge.net");

   }

   private CategoryCollection scrapCategories(JSONObject product) {
      CategoryCollection categories = new CategoryCollection();

      String categoria = product.optString("str_categoria");
      if (categoria != null) {
         categories.add(categoria);
      }
      String subCategoria = product.optString("str_subcategoria");
      if (subCategoria != null) {
         categories.add(subCategoria);
      }
      return categories;
   }

}
