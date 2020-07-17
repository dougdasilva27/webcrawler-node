package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SaopauloImigrantesbebidasCrawler extends Crawler {

    private static final String BASE_URL = "www.imigrantesbebidas.com.br";
    private static final String MAIN_SELLER_NAME = "Imigrantes Bebidas";
    private final Set<String> cards = Sets.newHashSet(Card.VISA.toString(),
            Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.ELO.toString(), Card.DINERS.toString());

    public SaopauloImigrantesbebidasCrawler(Session session) {
        super(session);
    }

    @Override
    public List<Product> extractInformation(Document doc) throws Exception {
        super.extractInformation(doc);
        List<Product> products = new ArrayList<>();

        if (isProductPage(doc)) {

            String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".skuId", true);
            String internalPId = internalId;
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".productPage__name", true);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".productGallery__nav__item > img", Collections.singletonList("src"), "https", BASE_URL);
            String secondaryImage = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".productGallery__nav__item > img", Collections.singletonList("src"), "https", BASE_URL, primaryImage);
            String description = CrawlerUtils.scrapElementsDescription(doc, Collections.singletonList(".tabs"));
            int stock = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, ".frmCartQuantity input[name=\"stock_unit\"]", "value", 0);
            Offers offers = stock > 0 ? scrapOffers(doc) : new Offers();
            List<String> eans = scrapEans(doc);

            Product product = ProductBuilder.create()
                    .setUrl(session.getOriginalURL())
                    .setInternalId(internalId)
                    .setInternalPid(internalPId)
                    .setName(name)
                    .setPrimaryImage(primaryImage)
                    .setSecondaryImages(secondaryImage)
                    .setDescription(description)
                    .setStock(stock)
                    .setOffers(offers)
                    .setEans(eans)
                    .build();

            products.add(product);

        }

        return products;
    }

    private boolean isProductPage(Document doc) {
        return doc.selectFirst(".productPage") != null;
    }

    private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
        Offers offers = new Offers();
        Pricing pricing = scrapPricing(doc);

        if (pricing != null) {
            offers.add(Offer.OfferBuilder.create()
                    .setUseSlugNameAsInternalSellerId(true)
                    .setSellerFullName(MAIN_SELLER_NAME)
                    .setSellersPagePosition(1)
                    .setIsBuybox(false)
                    .setIsMainRetailer(true)
                    .setPricing(pricing)
                    .build());
        }
        return offers;
    }

    private Pricing scrapPricing(Document doc) throws MalformedPricingException {
        Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".productPage__wholeprice", null, true, ',', this.session);
        Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".productPage__price", null, true, ',', this.session);

        return Pricing.PricingBuilder.create()
                .setSpotlightPrice(spotlightPrice)
                .setPriceFrom(priceFrom)
                .setCreditCards(scrapCreditCards(spotlightPrice))
                .setBankSlip(scrapBankSlip(spotlightPrice))
                .build();
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

    private BankSlip scrapBankSlip(Double spotlightPrice) throws MalformedPricingException {
        return BankSlip.BankSlipBuilder.create()
                .setFinalPrice(spotlightPrice)
                .build();
    }

    private List<String> scrapEans(Document doc) {
        //TODO
        /*JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", null, null, false, false);
        String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".productPage__tabs__content__text"));
        String ean = description.split("EAN:")[1].trim();
        return !ean.isEmpty() ? Collections.singletonList(ean) : null;*/
        return null;
    }
}
