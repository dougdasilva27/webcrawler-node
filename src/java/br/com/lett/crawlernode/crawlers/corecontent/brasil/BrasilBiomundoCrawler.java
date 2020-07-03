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
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilBiomundoCrawler extends Crawler {

    private static final String HOST_PAGE = "www.lojabiomundo.com.br";
    private static final String MAIN_SELLER_NAME = "";
    protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
            Card.HIPER.toString(), Card.AMEX.toString());

    public BrasilBiomundoCrawler(Session session) {
        super(session);
    }

    @Override
    public List<Product> extractInformation(Document doc) throws Exception {
        super.extractInformation(doc);
        List<Product> products = new ArrayList<>();

        if (isProductPage(doc)) {
            Logging.printLogDebug(logger, session,
                    "Product page identified: " + this.session.getOriginalURL());

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".item > meta", "content");
            String internalPid = internalId;
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".item > .fn", true);
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > span > span > a", true);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#img-product", Collections.singletonList("src"), "https://", HOST_PAGE);
            String secondaryImages = null;
            String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".info-description"));
            //TODO get avaibility
            Offers offers = scrapOffers(doc);
            //TODO get advanced rating
            RatingsReviews ratings = scrapRatingReviews(doc);

            Product product = ProductBuilder.create()
                    .setUrl(session.getOriginalURL())
                    .setInternalId(internalId)
                    .setInternalPid(internalPid)
                    .setName(name)
                    .setCategory1(categories.getCategory(0))
                    .setCategory2(categories.getCategory(1))
                    .setCategory3(categories.getCategory(2))
                    .setPrimaryImage(primaryImage)
                    .setSecondaryImages(secondaryImages)
                    .setDescription(description)
                    .setOffers(offers)
                    .setRatingReviews(ratings)
                    .build();
            products.add(product);
        } else {
            Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
        }

        return products;
    }

    private boolean isProductPage(Document doc) {
        return doc.selectFirst(".product") != null;
    }

    private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
        Offers offers = new Offers();
        Pricing pricing = scrapPricing(doc);

        if (pricing != null) {
            offers.add(OfferBuilder.create()
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
        Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".sale", null, true, ',', session);
        Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".list-price > .list", null, true, ',', session);
        return PricingBuilder.create()
                .setPriceFrom(priceFrom)
                .setSpotlightPrice(spotlightPrice)
                .setCreditCards(scrapCreditCards(doc, spotlightPrice))
                .setBankSlip(scrapBankSlip(doc))
                .build();
    }

    private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
        CreditCards creditCards = new CreditCards();
        Installments installments = scrapInstallments(doc);
        if (installments.getInstallments().isEmpty()) {
            installments.add(
                    InstallmentBuilder.create()
                            .setInstallmentNumber(1)
                            .setInstallmentPrice(spotlightPrice)
                            .build());
        }
        for (String card : cards) {
            creditCards.add(
                    CreditCardBuilder.create()
                            .setBrand(card)
                            .setInstallments(installments)
                            .setIsShopCard(false).build());
        }
        return creditCards;
    }

    private Installments scrapInstallments(Document doc) throws MalformedPricingException {
        Installments installments = new Installments();
        if (doc.selectFirst(".condition") != null) {
            int parcels = CrawlerUtils.scrapIntegerFromHtml(doc, ".parcels", true, 0);
            Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".parcel-value", null, true, ',', session);
            installments.add(InstallmentBuilder.create()
                    .setInstallmentNumber(parcels)
                    .setInstallmentPrice(price)
                    .build());
        }
        return installments;
    }

    private BankSlip scrapBankSlip(Document doc) throws MalformedPricingException {
        if (doc.selectFirst(".savings") != null) {
            Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".savings > b", null, true, ',', session);
            return BankSlip.BankSlipBuilder.create()
                    .setFinalPrice(price)
                    .build();
        }
        return new BankSlip();
    }

    //TODO rename
    private BankSlip scrapBankSlip2(Document doc) throws MalformedPricingException {
        Element rows = doc.selectFirst("div.content > table:nth-child(5)");
        if(rows != null){
            for (Element line : doc.select("tbody > tr:last-child > td > div")){
                String price = line.text();
                Integer parcel = 1;
                Double installmentPrice = null;
                Double finalPrice = null;

                if(price.contains("x")){
                    String[] teste = price.split("x");
                    parcel = Integer.parseInt(teste[0]);
                    CrawlerUtils.scrapDoublePriceFromHtml()
                    price != null ? MathUtils.normalizeTwoDecimalPlaces(price.doubleValue()) : null;
                }
            }
        }

    }

    //TODO rename
    private Installments scrapInstallments2(Document doc) throws MalformedPricingException {
        Installments installments = new Installments();
        Element rows = doc.selectFirst("div.content > table:nth-child(5)");
        if(rows != null){
            for (Element line : doc.select("tbody > tr:last-child > td > div")){
                String price = line.text();
                Integer parcel = 1;
                Double installmentPrice = null;
                Double finalPrice = null;

                if(price.contains("x")){
                    String[] teste = price.split("x");
                    parcel = Integer.parseInt(teste[0]);
                    CrawlerUtils.scrapDoublePriceFromHtml()
                    price != null ? MathUtils.normalizeTwoDecimalPlaces(price.doubleValue()) : null;
                }
                else{
                    installmentPrice =
                }
            }
        }
    }
    private RatingsReviews scrapRatingReviews(Document doc) {
        RatingsReviews ratingReviews = new RatingsReviews();
        ratingReviews.setDate(session.getDate());

        Integer totalComments = CrawlerUtils.scrapIntegerFromHtml(doc, ".votes", true, 0);
        Double avgRating = Double.parseDouble(doc.selectFirst(".average").text());

        ratingReviews.setTotalRating(totalComments);
        ratingReviews.setTotalWrittenReviews(totalComments);
        ratingReviews.setAverageOverallRating(avgRating);

        return ratingReviews;
    }

}
