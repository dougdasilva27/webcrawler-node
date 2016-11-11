package br.com.lett.crawlernode.crawlers.corecontent.ribeiraopreto;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

public class RibeiraopretoMercadoribeiraoCrawler extends Crawler {
	
	private final String HOME_PAGE = "https://www.mercadoribeirao.com.br/";

	public RibeiraopretoMercadoribeiraoCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// Id interno
			String internalID = Integer.toString(Integer.parseInt(this.session.getOriginalURL().split("id_prod=")[1].split("&")[0]));

			// Nome
			Elements elementName = doc.select("div.entry-heading h1.first-word");
			String name = elementName.text();

			// Preço
			Elements elementPrice = doc.select("div.priceProduct div.boxPriceList");
			Float price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			// Disponibilidade
			boolean available = true;

			// Categorias
			String category1; 
			String category2; 
			String category3;
			Elements element_categories = doc.select("#breadcrumbs li"); 
			String[] cat = new String[3];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";

			int j=0;
			for(int i=0; i < element_categories.size(); i++) {
				Element e = element_categories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace(">", "");
				j++;
			}
			category1 = cat[1];
			category2 = cat[2];
			category3 = null;

			// Imagens
			String primaryImage = "";
			Elements element_foto = doc.select("div.post.entry-content.sp a img");
			primaryImage = element_foto.first().attr("src").trim();	
			if(primaryImage.startsWith("./")){
				primaryImage = "https://www.mercadoribeirao.com.br" + primaryImage.replaceFirst(".", "");
			}
			else{
				primaryImage = "https://www.mercadoribeirao.com.br/" + primaryImage;
			}
			if(primaryImage.contains("produto_sem_foto")) primaryImage = "";

			String secondaryImages = null;

			// Descrição
			String description = "";
			Elements element_informacoes = doc.select("div.post.entry-content.sp blockquote");
			if(element_informacoes.first() != null) description = element_informacoes.html();

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
			product.setUrl(session.getOriginalURL());
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
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}

		return products;
	}
	
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return (url.startsWith("https://www.mercadoribeirao.com.br/produto.php"));
	}

}