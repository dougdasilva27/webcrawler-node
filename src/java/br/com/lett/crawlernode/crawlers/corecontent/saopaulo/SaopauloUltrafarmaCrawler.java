package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

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

public class SaopauloUltrafarmaCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.ultrafarma.com.br/";

	public SaopauloUltrafarmaCrawler(Session session) {
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

			// ID interno
			String id = this.session.getOriginalURL().split("/")[4];
			String internalID = id.replaceAll("[^0-9,]+", "").replaceAll("\\.", "").trim();

			// Pid
			String internalPid = internalID;

			// Disponibilidade
			Element elementBuyButton = doc.select(".div_btn_comprar").first();
			boolean available = true;
			if(elementBuyButton == null || elementBuyButton.text().trim().toLowerCase().contains("produto indisponível")) {
				available = false;
			}

			// Nome
			Elements elementName = doc.select(".div_nome_produto");
			String name = elementName.text().trim();

			// Preço
			Float price = null;
			Elements elementPrice = doc.select(".div_preco_detalhe .txt_preco");
			if(elementPrice.size() > 0) {
				String priceText = elementPrice.first().text();
				price = Float.parseFloat(priceText.replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}				

			//Float price = 0f;

			// Categorias
			Elements elementCategories = doc.select(".breadCrumbs li"); 
			String category1 = ""; 
			String category2 = ""; 
			String category3 = "";

			String[] cat = new String[9];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			cat[3] = "";
			cat[4] = "";
			cat[5] = "";
			cat[6] = "";
			cat[7] = "";
			cat[8] = "";
			int j = 0;
			for(int i = 0; i < elementCategories.size(); i++) {
				Element e = elementCategories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace(">", "");
				j++;
			}
			category1 = cat[2];
			category2 = cat[4];
			if(elementCategories.size() > 5){
				category3 = cat[6];
			} else{
				category3 = null;
			}

			// Imagem primária
			Elements elementPrimaryImage = doc.select("#imagem-grande");
			String primaryImage = elementPrimaryImage.attr("src");

			// Imagens secundárias
			String secondaryImages = null;

			JSONArray secondaryImagesArray = new JSONArray();
			Elements element_fotosecundaria = doc.select(".cont_chama_produtos div img");
			if(element_fotosecundaria.size()>1){
				for(int i=1; i<element_fotosecundaria.size();i++){
					Element e = element_fotosecundaria.get(i);
					secondaryImagesArray.put(e.attr("src"));
				}
			}

			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";

			try {

				Element[] sections = new Element[] {
						doc.select(".div_informacoes_prod").first(),
						doc.select(".div_anvisa").first(),
				};
				for(Element e: sections) {
					if(e != null) {
						description = description + e.html(); 
					}
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
			product.setUrl(session.getOriginalURL());
			product.setInternalId(internalID);
			product.setInternalPid(internalPid);
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
			Logging.printLogDebug(logger, "Not a product page.");
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.startsWith("http://www.ultrafarma.com.br/produto/detalhes");
	}
}
