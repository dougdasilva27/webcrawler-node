package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilBalaodainformaticaCrawler extends CrawlerRankingKeywords{

	public BrasilBalaodainformaticaCrawler(Session session) {
		super(session);
	}


	private Elements id;

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 30;

		this.log("Página "+ this.currentPage);
		//monta a url com a keyword e a página
		String urlCrawler = "http://www.balaodainformatica.com.br/Produtos/Buscar/?filtro_Ordenar_Por=null&ispartial=true"
				+ "&busca="	+ this.keywordEncoded + "&pagina=" + this.currentPage;

		this.log("Link onde são feitos os crawlers: "+urlCrawler);	

		//chama função de pegar a url
		this.currentDoc = fetchDocument(urlCrawler);

		this.id =  this.currentDoc.select("div.produto-box-container > a[href]");

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(this.id.size() >=1)
		{
			for(Element e: this.id){
				//seta o id da classe pai com o id retirado do elements this.id
				String[] tokens = e.attr("href").split("/");

				String internalPid 	= tokens[tokens.length-2];
				String internalId 	= tokens[tokens.length-2];
				String productUrl	= e.attr("href");

				saveDataProduct(internalId, internalPid, productUrl);

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) break;
			}
		}
		else
		{	
			this.result = false;
			this.log("Keyword sem resultado!");
		}

		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		if(!(hasNextPage())) setTotalBusca();
	}


	@Override
	protected boolean hasNextPage() {
		Elements page = this.currentDoc.select("div.paginacao strong");

		if(page.size() > 0)
		{
			if(page.get(1).text().contains("Próximo"))	return true;
			else										return false;					
		}
		else
		{
			return false;
		}
	}	

}
