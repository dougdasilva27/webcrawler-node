package br.com.lett.crawlernode.crawlers.corecontent.chapeco;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ChapecoSuperroyalCrawler extends Crawler {

    private static final String HOME_PAGE = "https://www.superroyal.com.br/";
    private static final String MAIN_SELLER_NAME = "super royal";
    protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
            Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

    public ChapecoSuperroyalCrawler(Session session) {
        super(session);
    }

    @Override
    public boolean shouldVisit() {
        String href = session.getOriginalURL().toLowerCase();
        return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE) && href.contains("product/"));
    }

    @Override
    public List<Product> extractInformation(Document doc) throws Exception {
        List<Product> products = new ArrayList<>();

        if (isProductPage(doc)) {
            Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

            String internalId = scrapId();
            String internalPid = internalId;
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".description", true);
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".box-breadcrumb > li > a", false);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".box-product-details .box-img > img", Collections.singletonList("src"), "https://", "d1fk7i3duur4ft.cloudfront.net");
            String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".content", true);

            Offers offers = scrapOffers(doc);

            Product product = ProductBuilder.create()
                    .setUrl(session.getOriginalURL())
                    .setInternalId(internalId)
                    .setInternalPid(internalPid)
                    .setName(name)
                    .setCategory1(categories.getCategory(0))
                    .setCategory2(categories.getCategory(1))
                    .setCategory3(categories.getCategory(2))
                    .setPrimaryImage(primaryImage)
                    .setDescription(description)
                    .setOffers(offers)
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

    private String scrapId() {
        return this.session.getOriginalURL().split("/")[4];
    }

    private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
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
        Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".sale-price", null, true, ',', this.session);

        if (spotlightPrice != null) {
            Double priceFrom = null;
            CreditCards creditCards = scrapCreditCards(spotlightPrice);

            return PricingBuilder.create()
                    .setSpotlightPrice(spotlightPrice)
                    .setPriceFrom(priceFrom)
                    .setCreditCards(creditCards)
                    .build();
        }

        return null;
    }

    private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
        CreditCards creditCards = new CreditCards();

        Installments installments = new Installments();
        installments.add(Installment.InstallmentBuilder.create()
                .setInstallmentNumber(1)
                .setInstallmentPrice(spotlightPrice)
                .build());

        for (String brand : cards) {
            creditCards.add(CreditCardBuilder.create()
                    .setBrand(brand)
                    .setIsShopCard(false)
                    .setInstallments(installments)
                    .build());
        }

        return creditCards;
    }
}
