package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilRrmaquinasCrawler extends CrawlerRankingKeywords{

	public BrasilRrmaquinasCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 32;

		this.log("Página "+ this.currentPage);

		//monta a url com a keyword e a página
		String url = "http://busca.rrmaquinas.com.br/?Busca=" + this.keywordEncoded + "&pagina=" + this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	

		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);

		Element list = this.currentDoc.select(".lista").first();

		// há casos que retorna sugestões de produto na busca.
		if(list != null){
			Elements products =  list.select(".products-grid .item > a");

			//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
			if(products.size() >= 1) {			
				//se o total de busca não foi setado ainda, chama a função para setar
				if(this.totalBusca == 0) setTotalBusca();
				for(Element e : products) {

					// InternalPid
					String internalPid 	= crawlInternalPid(e);

					// InternalId
					String internalId 	= crawlInternalId(e);

					// Url do produto
					String urlProduct = crawlProductUrl(e);

					saveDataProduct(internalId, internalPid, urlProduct);
				
					this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
					if(this.arrayProducts.size() == productsLimit) break;

				}
			} else {
				this.result = false;
				this.log("Keyword sem resultado!");
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {

		//se  elemeno page obtiver algum resultado
		if(this.arrayProducts.size() < this.totalBusca){
			//tem próxima página
			return true;
		} 

		return false;

	}

	@Override
	protected void setTotalBusca()	{
		Element totalElement = this.currentDoc.select(".amount").first();

		if(totalElement != null) { 	
			try	{			
				String text = totalElement.text();
				int x = text.indexOf("de") + 2;
				int y = text.indexOf("para", x);

				this.totalBusca = Integer.parseInt(text.substring(x, y).replaceAll("[^0-9]", "").trim());
			} catch(Exception e) {
				this.logError(e.getMessage());
			}

			this.log("Total da busca: "+this.totalBusca);
		}
	}

	private String crawlInternalId(Element e){
		String internalId = null;

		String url = e.attr("href");
		if(url.contains("/p/")){
			String[] tokens = url.split("/");
			internalId = tokens[tokens.length-1].replaceAll("[^0-9]", "");
		}

		return internalId;
	}

	private String crawlInternalPid(Element e){
		String internalPid = null;
		String text = e.attr("href");

		String[] tokens = text.split("=");
		internalPid = tokens[tokens.length-1];

		return internalPid;
	}

	private String crawlProductUrl(Element e){
		String urlProduct = null;
		String text = e.attr("href");

		String[] tokens = text.split("%2f");
		urlProduct = "http://www.rrmaquinas.com.br/" + tokens[tokens.length-1].split("&")[0];

		return urlProduct;
	}
}
