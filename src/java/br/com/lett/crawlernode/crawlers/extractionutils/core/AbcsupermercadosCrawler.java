package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AbcsupermercadosCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "abcsupermercados";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.AMEX.toString(), Card.DINERS.toString(), Card.HIPERCARD.toString());

   public AbcsupermercadosCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   private final String idLoja = getIdLoja();
   protected String getIdLoja() {
      return session.getOptions().optString("id_loja");
   }

   @Override
   protected Response fetchResponse() {
      String apiProductName = scrapUrlProductName();

      Map<String, String> headers = new HashMap<>();
      headers.put("authorization", "Basic YjQ5Y2ZlYTEtMTI4OS00YmNmLWE3M2UtMDkxMTVhZjQ4ZWNlOjY4MDZhZGY4Y2QyNGZmZGU2MGFhNGUwY2FmZDdmM2Qx");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://apiofertas.superabc.com.br/api/app/v3/selecionarprodutosnovo/?loja=" + idLoja + "&url=" + apiProductName)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR
         ))
         .build();

      Response response = this.dataFetcher.get(session, request);
      return response;
   }

   private String scrapUrlProductName() {
      String regex = "br\\/(.*)\\/p";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(session.getOriginalURL());
      if (matcher.find()) {
         return matcher.group(1);
      }
      return null;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();

      JSONObject productJson = JSONUtils.getValueRecursive(json, "Produtos.0", JSONObject.class);
      if (productJson != null) {
         String internalId = productJson.optString("Codigo");
         String internalPid = internalId;
         String name = productJson.optString("Descricao");
         JSONArray imagesJson = productJson.optJSONArray("Urls");
         List<String> images = imagesJson != null ? CrawlerUtils.scrapImagesListFromJSONArray(imagesJson, "url", null, "", "", session) : null;
         String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;

         boolean available = JSONUtils.getIntegerValueFromJSON(productJson, "EstoqueDisponivel", 0) > 0;
         Offers offers = available ? scrapOffers(productJson) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(JSONObject product) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject product) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(product, "PrecoDesconto", true);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(product, "PrecoVenda", true);
      if (spotlightPrice == 0) {
         spotlightPrice = JSONUtils.getDoubleValueFromJSON(product, "PrecoVenda", true);
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build())
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
}

