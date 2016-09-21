package br.com.lett.crawlernode.crawlers.florianopolis;


import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class FlorianopolisBistekCrawler extends Crawler {

	public FlorianopolisBistekCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();           
		return !FILTERS.matcher(href).matches() && href.startsWith("https://www.bistekonline.com.br/") && !href.contains("popup_indicarproduto.asp");
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			String params = this.session.getUrl().split("\\?")[1];
			Map<String, String> paramsMap = new HashMap<String, String>();
			for(String s: params.split("&")) {
				paramsMap.put(s.split("=")[0], s.split("=")[1]);
			}

			// Id interno
			String internalID = Integer.toString(Integer.parseInt(paramsMap.get("produto")));

			// Nome
			Elements elementName = doc.select(".titulo_produtos");
			String name = elementName.text().replace("'", "").trim();

			// Preço
			Element elementPrice = doc.select(".ProdutoValor").first();
			Float price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

			// Disponibilidade
			boolean available = true;

			// Categorias
			String category1 = paramsMap.get("secao");
			if(category1 == null) {
				category1 = "";
			} else if( category1.equals("1")) {
				category1 = "mercearia";
			} else if( category1.equals("2")) {
				category1 = "açougue";
			} else if( category1.equals("13")) {
				category1 = "padaria";
			} else if( category1.equals("3")) {
				category1 = "frios";
			} else if( category1.equals("4")) {
				category1 = "laticínios";
			} else if( category1.equals("5")) {
				category1 = "hortifruti";
			} else if( category1.equals("6")) {
				category1 = "bebidas";
			} else if( category1.equals("7")) {
				category1 = "higiene";
			} else if( category1.equals("8")) {
				category1 = "limpeza";
			} else if( category1.equals("9")) {
				category1 = "beleza";
			} else if( category1.equals("10")) {
				category1 = "pet shop";
			} else if( category1.equals("11")) {
				category1 = "bazar";
			} else if( category1.equals("12")) {
				category1 = "eletro";
			} else {
				category1 = "";
			}

			Elements element_cat2 = doc.select("a[href=Index.asp?secao=" + paramsMap.get("secao") + "&grupo=" + paramsMap.get("grupo") + "]");
			String category2 = element_cat2.text().trim();

			Elements element_cat3 = doc.select("a[href=Index.asp?secao=" + paramsMap.get("secao") + "&grupo=" + paramsMap.get("grupo") + "&subgrupo=" + paramsMap.get("subgrupo") + "]");
			String category3 = element_cat3.text().trim();

			// Imagens
			Elements element_foto = doc.select("#imgLink");
			String primaryImage = (element_foto.size() > 0) ? element_foto.get(0).attr("href") : "";
			primaryImage = primaryImage.replace("../", "https://www.bistekonline.com.br/lojaeletronica/");
			if(!checkImage(primaryImage)) primaryImage = "";

			String secondaryImages = null;

			// Descrição
			String description = null;

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
			product.setUrl(session.getUrl());
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
			
			if(name != null && !name.equals("")) {
				products.add(product);
			}
			
		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getUrl());
		}

		return products;
	}

	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return (url.contains("&produto=") || url.contains("?produto="));
	}

	public boolean checkImage(String url) {
		try {
			URL u = new URL (url);
			HttpURLConnection huc = (HttpURLConnection) u.openConnection (); 
			huc.setRequestMethod("GET");  //OR huc.setRequestMethod("HEAD"); 
			huc.connect() ;   

			return huc.getResponseCode() != 404;
		} catch (Exception e) {
			return false;
		}
	}
}