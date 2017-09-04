package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilLojasredeCrawler extends CrawlerRankingKeywords{

	public BrasilLojasredeCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página "+ this.currentPage);

		this.pageSize = 24;

		String url = "http://busca.lojasrede.com.br/busca?q=" + this.keywordEncoded + "&page="+this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	

		this.currentDoc = fetchDocument(url);
		Elements products =  this.currentDoc.select("li.nm-product-item");

		if(products.size() >= 1) {
			if(this.totalProducts == 0) {
				setTotalProducts();
			}

			for(Element e: products) {
				String internalPid 	= crawlInternalPid(e);
				String internalId = crawlInternalId(e);
				String productUrl  = crawlProductUrl(e);

				saveDataProduct(internalId, internalPid, productUrl);

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) {
					break;
				}

			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");

	}

	@Override
	protected boolean hasNextPage() {
		return this.totalProducts > this.arrayProducts.size();
	}

	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select(".neemu-total-products-container span").first();
		if(totalElement != null) {
			try {
				this.totalProducts = Integer.parseInt(totalElement.ownText().replaceAll("[^0-9]", ""));
			} catch(Exception e) {
				this.logError(CommonMethods.getStackTrace(e));
			}
		}

		this.log("Total da busca: " + this.totalProducts);
	}
	
	private String crawlInternalId(Element e) {
		String id = null;
		
		Element comprar = e.select(".nm-add a[data-produtovarianteid]").first();
		Element indisponivel = e.select(".spotIndisponivel .input").first();
		
		if(comprar != null) {
			id = comprar.attr("data-produtovarianteid");
		} else if(indisponivel != null) {
			String[] tokens = indisponivel.attr("id").split("-");
			id = tokens[tokens.length-1].trim();
		}

		return id;
	}
	
	private String crawlInternalPid(Element e) {
		String[] tokens = e.attr("id").split("-");
		
		return tokens[tokens.length-1].trim();
	}

	private String crawlProductUrl(Element e){
		String productUrl = null;
		Element url = e.select(".nm-product-name a").first();
		
		if(url != null) {
			productUrl = url.attr("href");
			
			if(productUrl.contains("lojasrede") && !productUrl.startsWith("http")) {
				productUrl = "http:" + productUrl;
			}
			
			if(!productUrl.startsWith("http://www.lojasrede.com.br/")) {
				productUrl = ("http://www.lojasrede.com.br/" + productUrl).replace("br//", "br/");
			}
		}
		
		return productUrl;
	}
}
