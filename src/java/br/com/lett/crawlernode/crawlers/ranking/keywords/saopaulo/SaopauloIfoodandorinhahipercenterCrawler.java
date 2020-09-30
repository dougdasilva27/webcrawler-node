package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilIfood;

public class SaopauloIfoodandorinhahipercenterCrawler extends BrasilIfood {


    public static final String REGION = "sao-paulo-sp";
    public static final String STORE_NAME = "andorinha-hiper-center-lauzane-paulista";
    public static final String STORE_ID = "4120f90c-45e8-4c31-aa8a-6c7a5ab1e5b2";

    public SaopauloIfoodandorinhahipercenterCrawler(Session session) {
        super(session);
    }

    @Override
    protected String getRegion() {
        return REGION;
    }

    @Override
    protected String getStoreName() {
        return STORE_NAME;
    }

    @Override
    protected String getStoreId() {
        return STORE_ID;
    }

}


