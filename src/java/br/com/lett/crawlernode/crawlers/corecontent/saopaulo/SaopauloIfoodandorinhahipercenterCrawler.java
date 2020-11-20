package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilIfood;

public class SaopauloIfoodandorinhahipercenterCrawler extends BrasilIfood {

    public static final String region = "sao-paulo-sp";
    public static final String store_name = "andorinha-hiper-center-lauzane-paulista";
    public static final String seller_full_name = "Andorinha Hiper Center";

    public SaopauloIfoodandorinhahipercenterCrawler(Session session) {
        super(session);
    }

    @Override protected String getRegion() {
        return region;
    }

    @Override protected String getStore_name() {
        return store_name;
    }

    @Override protected String getSellerFullName() {
        return seller_full_name;
    }
}
