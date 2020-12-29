package br.com.lett.crawlernode.crawlers.corecontent.brasil;


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
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilAgroverdesrCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Agroverde sr brasil";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPER.toString(), Card.HIPERCARD.toString(),
      Card.JCB.toString(), Card.ELO.toString());

   public BrasilAgroverdesrCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".content.produto  #hdnProdutoId", "value");

      if (internalId != null) {
         String jsonString = CrawlerUtils.scrapScriptFromHtml(document, ".content.produto script[type=\"application/ld+json\"]");
         JSONObject json = JSONUtils.stringToJsonArray(jsonString).optJSONObject(0);
         JSONArray variation = ScrapVariantes(document);
         CategoryCollection category = ScrapCategory(document);

         for (int i = 0; i < variation.length(); i++) {
            String internalPid = JSONUtils.getValueRecursive(json, "offers." + i + ".mpn", String.class);
            String name = ScrapName( json, variation.optJSONObject(i));
            String description = JSONUtils.getStringValue(json, "description");
            String primaryImage = JSONUtils.getValueRecursive(json, "image." + i, String.class);
            String ean = JSONUtils.getValueRecursive(json, "offers." + i + ".gtin14", String.class);
            boolean available = JSONUtils.getValueRecursive(json, "offers." + i + ".availability", String.class).contains("InStock");
            Offers offers = available? ScrapOffers(json,i) : new Offers() ;


            products.add(ProductBuilder.create()
               .setInternalId(internalPid)
               .setInternalPid(internalId)
               .setUrl(session.getOriginalURL())
               .setName(name)
               .setDescription(description)
               .setPrimaryImage(primaryImage)
               .setCategories(category)
               .setOffers(offers)
               .setEans(Collections.singletonList(ean))
               .build());

         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers ScrapOffers(JSONObject json, int index) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = ScrapPricing(json,index);
      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setIsBuybox(false)
         .build());

      return offers;

   }

   private Pricing ScrapPricing( JSONObject json, int index) throws MalformedPricingException {
      Double price = Double.parseDouble(JSONUtils.getValueRecursive(json,"offers."+index+".price",String.class));
      CreditCards creditCards = ScrapCredtCard(price);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setBankSlip(BankSlip.BankSlipBuilder.create()
            .setFinalPrice(price)
            .build())
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards ScrapCredtCard(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setFinalPrice(price)
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
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


   private JSONArray ScrapVariantes(Document document) {
      JSONArray array = new JSONArray();
      String tag = "Fbits.Produto.AtributosProduto = ";
      Elements elements = document.select("#bodyProduto  script");
      for (Element element : elements) {
         for (DataNode dataNode : element.dataNodes()) {
            if (dataNode.getWholeData().contains(tag)) {
               String script = dataNode.getWholeData();
               int inicialIndex = script.indexOf(tag) + tag.length();
               int finalIndex = script.indexOf(";", inicialIndex);
               array = JSONUtils.stringToJsonArray(script.substring(inicialIndex, finalIndex));
            }
         }

      }
      return array;
   }


   private CategoryCollection ScrapCategory(Document document) {
      return CrawlerUtils.crawlCategories(document, "#fbits-breadcrumb");
   }

   private String ScrapName( JSONObject json, JSONObject variation) {
      StringBuilder name = new StringBuilder();
      name.append(JSONUtils.getStringValue(json, "name"));
      name.append("-");
      name.append(JSONUtils.getStringValue(variation, "Característica"));
      return name.toString();
   }

}
