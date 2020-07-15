package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static models.pricing.BankSlip.BankSlipBuilder;

public class SaopauloSaudavelemcasaCrawler extends Crawler {
    private static final String BASE_URL = "https://www.saudavelemcasa.com.br/";
    private static final String MAIN_SELLER_NAME = "saudavel em casa";
    private final Set<String> cards = Sets.newHashSet(Card.VISA.toString(),
            Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.ELO.toString(), Card.DINERS.toString());

    public SaopauloSaudavelemcasaCrawler(Session session) {
        super(session);
    }

    @Override
    public List<Product> extractInformation(Document doc) throws Exception {
        super.extractInformation(doc);
        List<Product> products = new ArrayList<>();

        if (isProductPage(doc)) {

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#produto-id", "value");
            String internalPId = internalId;
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#produto-nome", true);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".thumbnails > a", Collections.singletonList("href"), "https", "static.saudavelemcasa.com.br");
            String secondaryImage = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".thumbnails > a", Collections.singletonList("href"), "https", "static.saudavelemcasa.com.br", primaryImage);
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > a", true);
            String description = CrawlerUtils.scrapElementsDescription(doc, Collections.singletonList(".tab:not(:last-child)"));
            boolean availability = scrapAvailability(doc);
            Offers offers = availability ? scrapOffers(doc) : new Offers();
            RatingsReviews ratingsReviews = scrapRatings(doc);

            Product product = ProductBuilder.create()
                    .setInternalId(internalId)
                    .setInternalPid(internalPId)
                    .setName(name)
                    .setPrimaryImage(primaryImage)
                    .setSecondaryImages(secondaryImage)
                    .setCategory1(categories.getCategory(0))
                    .setCategory2(categories.getCategory(1))
                    .setCategory3(categories.getCategory(2))
                    .setDescription(description)
                    .setOffers(offers)
                    .setRatingReviews(ratingsReviews)
                    .build();

            products.add(product);

        }

        return products;
    }

    private boolean isProductPage(Document doc) {
        return doc.selectFirst(".product") != null;
    }

    private boolean scrapAvailability(Document doc) {
        return doc.selectFirst(".btn-comprar.disabled") == null;
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
        Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#preco", null, true, ',', this.session);
        Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#preco-antigo", null, true, ',', this.session);

        return Pricing.PricingBuilder.create()
                .setSpotlightPrice(spotlightPrice)
                .setPriceFrom(priceFrom)
                .setCreditCards(scrapCreditCards(doc, spotlightPrice))
                .setBankSlip(scrapBankSlip(doc))
                .build();
    }

    private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
        CreditCards creditCards = scrapCardsInHtml(doc);
        Installments installments = new Installments();

        if (creditCards.getCreditCards().isEmpty()) {
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
        }

        return creditCards;
    }

    //TODO change name
    public CreditCards scrapCardsInHtml(Document doc) throws MalformedPricingException {
        CreditCards creditCards = new CreditCards();
        String cardName = null;

        Elements installmentsCard = doc.select("#parcelamento_info > div");
        for (Element element : installmentsCard) {

            if (element.selectFirst(".title") != null) {
                String creditCard = CrawlerUtils.scrapStringSimpleInfo(element, ".title", true);

                for (String card : cards) {
                    if (creditCard.contains(card)) {
                        cardName = card;
                    }
                }

            } else if (element.selectFirst(".parcelamentos") != null) {
                Installments installments = new Installments();

                Elements installmentsPrices = element.select(".parcelamentos");
                for (Element installmentsPrice : installmentsPrices) {
                    Integer number = CrawlerUtils.scrapIntegerFromHtml(installmentsPrice, ".parcelas", true, null);
                    Double value = CrawlerUtils.scrapDoublePriceFromHtml(installmentsPrice, ".valor", null, true, ',', this.session);
                    Double finalPrice = CrawlerUtils.scrapDoublePriceFromHtml(installmentsPrice, ".total", null, true, ',', this.session);

                    installments.add(Installment.InstallmentBuilder.create()
                            .setInstallmentNumber(number)
                            .setInstallmentPrice(value)
                            .setFinalPrice(finalPrice)
                            .build());
                }

                if (cardName != null) {
                    creditCards.add(CreditCard.CreditCardBuilder.create()
                            .setBrand(cardName)
                            .setInstallments(installments)
                            .setIsShopCard(false)
                            .build());
                }
            }
        }

        return creditCards;
    }

    private BankSlip scrapBankSlip(Document doc) throws MalformedPricingException {

        String bankSlipText = CrawlerUtils.scrapStringSimpleInfo(doc, "#preco_boleto", true);
        String[] split = bankSlipText.split("\\(");

        Double finalPrice = MathUtils.parseDoubleWithComma(split[0]);
        Double pageDiscount = MathUtils.parseDoubleWithComma(split[1]);

        finalPrice = finalPrice != null ? MathUtils.normalizeTwoDecimalPlaces(finalPrice) : null;
        pageDiscount = pageDiscount != null ? MathUtils.normalizeTwoDecimalPlaces(pageDiscount) / 100 : null;

        return BankSlipBuilder.create()
                .setFinalPrice(finalPrice)
                .setOnPageDiscount(pageDiscount)
                .build();
    }

    private RatingsReviews scrapRatings(Document doc) {
        // Quando o crawler for desenvolvido existia opção de ratings mas nenhuma produto havia sido avaliado
        return new RatingsReviews();
    }
}
