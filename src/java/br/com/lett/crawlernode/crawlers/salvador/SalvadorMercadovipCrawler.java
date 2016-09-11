package br.com.lett.crawlernode.crawlers.salvador;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class SalvadorMercadovipCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.mercadovip.com.br/";

	public SalvadorMercadovipCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// Id interno
			String internalID = Integer.toString(Integer.parseInt(this.session.getUrl().split("prod=")[1].split("&")[0]));

			// Nome
			Elements element_nome = doc.select(".nome_produto");
			String name = element_nome.text();

			// Preço
			Elements element_preco = doc.select(".valor_por_grande span");
			Float price = Float.parseFloat(element_preco.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			// Disponibilidade
			boolean available = true;

			// Categorias
			String category1; 
			String category2; 
			String category3;
			Elements element_categories = doc.select(".categoria_navegacao"); 
			String categorias = element_categories.text().trim();

			category1 = categorias.split(">")[1];
			category2 = categorias.split(">")[2];
			category3 = null;

			// Imagens
			String primaryImage = "";
			Elements element_foto = doc.select("#foto_produto");
			primaryImage = element_foto.first().attr("src").trim();	
			if(primaryImage.contains("produto_sem_foto")) primaryImage = "";

			String secondaryImages = null;

			// Descrição
			String description = "";
			Element element_descricao = doc.select("#box_descricao i").first();
			if (element_descricao!=null){
				description = element_descricao.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			product.setSeedId(this.session.getSeedId());
			product.setUrl(this.session.getUrl());
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
			Logging.printLogDebug(logger, session, "Not a product page" + session.getSeedId());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/
	private boolean isProductPage(String url) {
		return url.startsWith("http://www.mercadovip.com.br/app/sc/gui/Produto");
	}
}