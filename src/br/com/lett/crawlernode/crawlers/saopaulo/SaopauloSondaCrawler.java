package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloSondaCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.sondadelivery.com.br/";

	public SaopauloSondaCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// ID interno
			String internalID = Integer.toString(Integer.parseInt(this.session.getUrl().split("/")[this.session.getUrl().split("/").length-1]));

			// Nome
			Elements elementName = doc.select(".box-DetalhesProdMed h3");
			String name = elementName.text().replace("'", "").trim();

			// Preço
			Elements elementPrice = doc.select(".box-DetalhesProdMed h2");
			Float price = Float.parseFloat(elementPrice.last().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			// Disponibilidade
			boolean available = true;

			// Categorias
			Elements elementCategory1 = doc.select(".HeaderDetalhesdoProduto h2");
			String category1 = elementCategory1.text().trim();
			Elements elementCategory2 = doc.select(".HeaderDetalhesdoProduto .spnSubTitulo");
			String category2 = elementCategory2.text().trim();
			String category3 = "";

			// Imagem primária
			Elements elementPrimaryImage = doc.select(".box-Imagem img");
			String primaryImage = elementPrimaryImage.get(0).attr("src");
			if(primaryImage.contains("nome_da_imagem_do_sem_foto.gif")) primaryImage = ""; //TODO: Verificar o nome da foto genérica
			String secondaryImages = null;

			// Descrição
			String description = "";
			Elements elementDescription = doc.select("article.bx-detalhe-descricao");
			for(int i = 0; i < elementDescription.size(); i++) {
				String information = elementDescription.get(i).select("h2").text();
				if(information.equals("Ingredientes")) {
					description = description + elementDescription.get(i).select("p").html();
				}
				else if(information.equals("Tabela Nutricional")) {
					description = description + elementDescription.get(i).select("table tbody").html();
				}
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			product.setUrl(this.session.getUrl());
			product.setSeedId(this.session.getSeedId());
			product.setInternalId(internalID);
			product.setName(name);
			product.setPrice(price);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);
			product.setAvailable(available);

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}

		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.contains("/delivery.aspx/produto/");
	}
}