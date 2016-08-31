package br.com.lett.crawlernode.processor.models;

import org.bson.Document;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoDatabase;

import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;



/**
 * Processor model - Processed
 * 
 * Classe prepara conteúdo para unificação, portanto pronta para a unificação por conter informações já tratadas
 * @author doug
 *
 */

public class ProcessedModel {
	
	protected static final Logger logger = LoggerFactory.getLogger(ProcessedModel.class);

	private Long 		id;
	private String 		internalId;
	private String 		internalPid;
	private String 		_class;
	private String 		brand;
	private String 		recipient;
	private Double 		quantity;
	private Integer 	multiplier;
	private String 		unit;
	private String 		extra;
	private String 		pic;
	private String 		secondary_pics;
	private String 		cat1;
	private String 		cat2;
	private String 		cat3;
	private String 		url;
	private Integer 	market;
	private String 		originalName;
	private String 		originalDescription;
	private Float 		price;
	private Integer 	stock;
	private JSONObject 	digitalContent;
	private JSONArray 	behaviour;
	private JSONArray 	marketplace;
	private String 		lmt;
	private String 		ect;
	private String 		lat;
	private String 		lrt;
	private String 		lms;
	private String 		status;
	private Boolean 	available;
	private Boolean 	void_product;
	private JSONObject 	changes;
	private Long 		lettId;
	private JSONArray 	similars;
	private String 		sanitizedName;

	public ProcessedModel() {
		super();
	}

	public ProcessedModel(
			Long id, 
			String internalId, 
			String internalPid, 
			String originalName, 
			String _class, 
			String brand, 
			String recipient,
			Double quantity, 
			Integer multiplier, 
			String unit, 
			String extra, 
			String pic, 
			String secondary_pics, 
			String cat1, 
			String cat2, 
			String cat3, 
			String url,
			Integer market, 
			String ect, 
			String lmt, 
			String lat, 
			String lrt, 
			String lms, 
			String status, 
			JSONObject changes, 
			String originalDescription, 
			Float price, 
			JSONObject digitalContent, 
			Long lettId, 
			JSONArray similars, 
			Boolean available, 
			Boolean void_product, 
			Integer stock, 
			JSONArray behaviour, 
			JSONArray marketplace) {
		
		super();
		this.id = id;
		this.internalId = internalId;
		this.internalPid = internalPid;
		this.originalName = originalName;
		this._class = _class;
		this.brand = brand;
		this.recipient = recipient;
		this.quantity = quantity;
		this.multiplier = multiplier;
		this.unit = unit;
		this.extra = extra;
		this.pic = pic;
		this.secondary_pics = secondary_pics;
		this.cat1 = cat1;
		this.cat2 = cat2;
		this.url = url;
		this.market = market;
		this.ect = ect;
		this.lmt = lmt;
		this.lat = lat;
		this.lrt = lrt;
		this.lms = lms;
		this.status = status;
		this.changes = changes;
		this.originalDescription = originalDescription;
		this.price = price;
		this.stock = stock;
		this.digitalContent = digitalContent;
		this.lettId = lettId;
		this.similars = similars;
		this.available = available;
		this.void_product = void_product;
		this.behaviour = behaviour;
		this.marketplace = marketplace;
	}

	/**
	 * Método para identificação de alterações de atributos dos produtos processados em alguma outra ocasião.
	 * Registra apenas mudanças que o Processor efetua, ou seja, campos que ele extrai. Caso uma dessas mudanças
	 * seja importante para o cliente, a url será escalonada para ter um screenshot capturada. Essa url vai para o
	 * Mongo, na collection Screenshots, juntamente com outros dados pertinentes. A instância do mongo passada de parâmetro
	 * está vindo do webcrawler, uma vez que este método registerChanges está sendo chamado no MarketCrawler.
	 * 
	 * @author Fabricio
	 * @category Comparação
	 * @param compareTo - Recepção de ProcessedModel a ser comparado 
	 */
	public void registerChanges(ProcessedModel compareTo, MongoDatabase mongo) {
		JSONObject newChanges = null;
		boolean mustScheduleUrlToScreenshot = false;

		// Se já foi registrado alguma mudança hoje, acumula no objeto changes

		// Verificando se foi criado agora
		if(compareTo == null) {
			newChanges = new JSONObject();
			newChanges.put("created", true);
		} else {

			// Verificando se já foi registrado alguma mudança hoje, a fim de acumular as próximas

			try {
				DateTimeFormatter f = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

				DateTime lrt = f.parseDateTime(compareTo.getLrt());

				if(compareTo.getChanges() != null && lrt.isAfter(new DateTime().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0))) {
					newChanges = new JSONObject(compareTo.getChanges(), JSONObject.getNames(compareTo.getChanges())); //clone
				} else {
					newChanges = new JSONObject();
				}
			} catch (Exception e) {
				newChanges = new JSONObject();
			}

			// Verificando se o preço se alterou
			if(
					(this.getPrice() == null ^ compareTo.getPrice() == null)
					||
					(this.getPrice() != null && !this.getPrice().equals(compareTo.getPrice()))
					) {
				newChanges.put("price", compareTo.getPrice());
			}

			// Verificando se mudou sua condição de disponibilidade
			if(this.getAvailable() != compareTo.getAvailable()) {
				newChanges.put("available", compareTo.getAvailable());
				mustScheduleUrlToScreenshot = true;
			}

			// Verificando se a foto primária se alterou, à partir do seu md5
			JSONObject pic = new JSONObject();
			String actualMd5 = null;
			String compareToMd5 = null;

			try {
				actualMd5 = this.getDigitalContent().getJSONObject("pic").getJSONObject("primary").getString("md5");
			} catch (Exception e1) { 

			}
			try {
				compareToMd5 = compareTo.getDigitalContent().getJSONObject("pic").getJSONObject("primary").getString("md5");
			} catch (Exception e1) { 

			}

			if(
					(actualMd5 == null ^ compareToMd5 == null)
					||
					(actualMd5 != null && compareToMd5 != null && !actualMd5.equals(compareToMd5))
					) {
				pic.put("primary", compareToMd5);
			}

			// Verificando se alguma foto secundária se alterou, à partir da(s) sua(s) url(s)
			if(
					(this.getSecondary_pics() == null ^ compareTo.getSecondary_pics() == null)
					||
					(this.getSecondary_pics() != null && !this.getSecondary_pics().equals(compareTo.getSecondary_pics()))
					) {

				JSONArray pic_secondary;
				try 					{ 	pic_secondary = new JSONArray(compareTo.getSecondary_pics());
				} catch (Exception e) 	{	pic_secondary = null; }

				if(pic_secondary != null) pic.put("secondary", pic_secondary);
			}

			if(pic.length() > 0) newChanges.put("pic", pic);

			// Verificando os campos originais se alteraram
			JSONObject originals = new JSONObject();

			if(
					(this.getOriginalName() == null ^ compareTo.getOriginalName() == null)
					||
					(this.getOriginalName() != null && !this.getOriginalName().equals(compareTo.getOriginalName()))
					) {
				originals.put("name", compareTo.getOriginalName());
			}

			if(
					(this.getOriginalDescription() == null ^ compareTo.getOriginalDescription() == null)
					||
					(this.getOriginalDescription() != null && !this.getOriginalDescription().equals(compareTo.getOriginalDescription()))
					) {
				originals.put("description", compareTo.getOriginalDescription());
			}

			if(originals.length() > 0) newChanges.put("originals", originals);

		}

		if(newChanges.length() == 0) {
			this.setChanges(null);
		} else {
			this.setChanges(newChanges);
		}

		// Se for para escalonar a url para um screenshot, então inserir no Mongo
		if (mustScheduleUrlToScreenshot) {			
			Document urlDocument = new Document();
			urlDocument.append("url", this.url);
			urlDocument.append("marketId", this.market);
			urlDocument.append("internalId", this.internalId);
			urlDocument.append("screenshotProcessed", false);

			mongo.getCollection("Screenshots").insertOne(urlDocument);
		}
	}

	/**
	 * Método retorna uma cópia de informações do ProcessModel, diferente de apenas referência.
	 */
	public ProcessedModel clone () {
		return new ProcessedModel(
				this.id, 
				this.internalId, 
				this.internalPid, 
				this.originalName, 
				this._class, 
				this.brand, 
				this.recipient, 
				this.quantity, 
				this.multiplier,
				this.unit, 
				this.extra, 
				this.pic, 
				this.secondary_pics, 
				this.cat1, 
				this.cat2, 
				this.cat3, 
				this.url, 
				this.market, 
				this.ect, 
				this.lmt, 
				this.lat, 
				this.lrt, 
				this.lms, 
				this.status, 
				(this.changes == null) ? this.changes : new JSONObject(this.changes.toString()), 
				this.originalDescription, 
				this.price, 
				(this.digitalContent == null) ? this.digitalContent : new JSONObject(this.digitalContent.toString()), 
				this.lettId, this.similars, this.available, this.void_product, this.stock, 
				(this.behaviour == null) ? this.behaviour : new JSONArray(this.behaviour.toString()),
				(this.marketplace == null) ? this.marketplace : new JSONArray(this.marketplace.toString())
		);
	}
	
	/**
	 * Método que avalia o truco b
	 */
	public boolean compareHugeChanges(ProcessedModel truco, CrawlerSession session) {

		// Verificando se foi criado agora
		if(truco == null) {
			return false;
		} else {

			// price change
			if(
					(this.getPrice() == null ^ truco.getPrice() == null)
					||
					(this.getPrice() != null && !this.getPrice().equals(truco.getPrice()))
					) {
				
				Logging.printLogDebug(logger, session, "Change detected. [price][" + this.getPrice() + " -> " + truco.getPrice() + "]");
				return true;
			}

			// availability change
			if(this.getAvailable() != truco.getAvailable()) {
				Logging.printLogDebug(logger, session, "Change detected. [availability][" + this.getAvailable() + " -> " + truco.getAvailable() + "]");
				return true;
			}

			// void change
			if(this.getVoid() != truco.getVoid()) {
				Logging.printLogDebug(logger, session, "Change detected. [void][" + this.getVoid() + " -> " + truco.getVoid() + "]");
				return true;
			}

			// status change
			if(!this.getStatus().equals(truco.getStatus())) {
				Logging.printLogDebug(logger, session, "Change detected. [status][" + this.getStatus() + " -> " + truco.getStatus() + "]");
				return true;
			}

			// name change
			if(
					(this.getOriginalName() == null ^ truco.getOriginalName() == null)
					||
					(this.getOriginalName() != null && !this.getOriginalName().equals(truco.getOriginalName()))
					) {
				Logging.printLogDebug(logger, session, "Change detected. [name][" + this.getOriginalName() + " -> " + truco.getOriginalName() + "]");
				return true;
			}
			
			// internalPid change
			if(
					(this.getInternalPid() == null ^ truco.getInternalPid() == null)
					||
					(this.getInternalPid() != null && !this.getInternalPid().equals(truco.getInternalPid()))
					) {
				Logging.printLogDebug(logger, session, "Change detected. [internalPid][" + this.getInternalPid() + " -> " + truco.getInternalPid() + "]");
				return true;
			}
			
			// url change
			if(!this.getUrl().equals(truco.getUrl())) {
				Logging.printLogDebug(logger, session, "Change detected. [url][" + this.getUrl() + " -> " + truco.getUrl() + "]");
				return true;
			}

		}
		
		return false;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getInternalId() {
		return this.internalId;
	}

	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}

	public String getInternalPid() {
		return this.internalPid;
	}

	public void setInternalPid(String internalPid) {
		this.internalPid = internalPid;
	}

	public String getOriginalName() {
		return this.originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public String get_class() {
		return this._class;
	}

	public void set_class(String _class) {
		this._class = _class;
	}

	public String getBrand() {
		return this.brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public String getRecipient() {
		return this.recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public Double getQuantity() {
		return this.quantity;
	}

	public void setQuantity(Double quantity) {
		this.quantity = quantity;
	}

	public Integer getMultiplier() {
		return multiplier;
	}
	public void setMultiplier(Integer multiplier) {
		this.multiplier = multiplier;
	}

	public String getUnit() {
		return this.unit;
	}
	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getExtra() {
		return this.extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public String getPic() {
		return this.pic;
	}
	
	public void setPic(String pic) {
		this.pic = pic;
	}

	public String getCat1() {
		return this.cat1;
	}

	public void setCat1(String cat1) {
		this.cat1 = cat1;
	}

	public String getCat2() {
		return this.cat2;
	}
	
	public void setCat2(String cat2) {
		this.cat2 = cat2;
	}

	public String getCat3() {
		return this.cat3;
	}

	public void setCat3(String cat3) {
		this.cat3 = cat3;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Integer getMarket() {
		return this.market;
	}

	public void setMarket(Integer market) {
		this.market = market;
	}

	public String getLmt() {
		return this.lmt;
	}
	public void setLmt(String lmt) {
		this.lmt = lmt;
	}

	public String getEct() {
		return this.ect;
	}

	public void setEct(String ect) {
		this.ect = ect;
	}

	public String getLms() {
		return lms;
	}

	public void setLms(String lms) {
		this.lms = lms;
	}

	public String getSanitizedName() {
		return sanitizedName;
	}
	public void setSanitizedName(String sanitizedName) {
		this.sanitizedName = sanitizedName;
	}

	public String getOriginalDescription() {
		return originalDescription;
	}
	
	public void setOriginalDescription(String originalDescription) {
		this.originalDescription = originalDescription;
	}

	public String getSecondary_pics() {
		return secondary_pics;
	}
	
	public void setSecondary_pics(String secondary_pics) {
		this.secondary_pics = secondary_pics;
	}

	public Float getPrice() {
		return price;
	}

	public void setPrice(Float price) {
		this.price = price;
	}

	public JSONObject getChanges() {
		return changes;
	}

	public void setChanges(JSONObject changes) {
		this.changes = changes;
	}

	public JSONObject getDigitalContent() {
		return digitalContent;
	}

	public void setDigitalContent(JSONObject digitalContent) {
		this.digitalContent = digitalContent;
	}

	public Long getLettId() {
		return lettId;
	}

	public void setLettId(Long lettId) {
		this.lettId = lettId;
	}

	public String getLat() {
		return lat;
	}

	public void setLat(String lat) {
		this.lat = lat;
	}

	public Boolean getAvailable() {
		return available;
	}

	public void setAvailable(Boolean available) {
		this.available = available;
	}

	public String getLrt() {
		return lrt;
	}

	public void setLrt(String lrt) {
		this.lrt = lrt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getStock() {
		return stock;
	}
	public void setStock(Integer stock) {
		this.stock = stock;
	}

	public JSONArray getSimilars() {
		return similars;
	}

	public void setSimilars(JSONArray similars) {
		this.similars = similars;
	}

	public JSONArray getBehaviour() {
		return behaviour;
	}

	public void setBehaviour(JSONArray behaviour) {
		this.behaviour = behaviour;
	}

	public JSONArray getMarketplace() {
		return marketplace;
	}

	public void setMarketplace(JSONArray marketplace) {
		this.marketplace = marketplace;
	}

	public Boolean getVoid() {
		return void_product;
	}

	public void setVoid(Boolean void_product) {
		this.void_product = void_product;
	}
	
	@Override
	public String toString() {
		StringBuilder stringBuilder =new StringBuilder();
		
		stringBuilder.append("Processed id: " + this.id + "\n");
		stringBuilder.append("InternalId: " + this.internalId + "\n");
		stringBuilder.append("InternalPid: " + this.internalPid + "\n");
		stringBuilder.append("Class: " + this._class + "\n");
		stringBuilder.append("Brand: " + this.brand + "\n");
		stringBuilder.append("Recipient: " + this.recipient + "\n");
		stringBuilder.append("Quantity: " + this.quantity + "\n");
		stringBuilder.append("Multiplier: " + this.multiplier + "\n");
		stringBuilder.append("Unit: " + this.unit + "\n");
		stringBuilder.append("Extra: " + this.extra + "\n");
		stringBuilder.append("Picture: " + this.pic + "\n");
		stringBuilder.append("Secondary pics: " + this.secondary_pics + "\n");
		stringBuilder.append("URL: " + this.url + "\n");
		stringBuilder.append("LettId: " + this.lettId + "\n");
		stringBuilder.append("ECT: " + this.ect + "\n");
		stringBuilder.append("LMT: " + this.lmt + "\n");
		stringBuilder.append("LAT: " + this.lat + "\n");
		stringBuilder.append("LRT: " + this.lrt + "\n");
		stringBuilder.append("LMS: " + this.lms + "\n");
		stringBuilder.append("Status: " + this.status + "\n");
		stringBuilder.append("Available: " + this.available + "\n");
		stringBuilder.append("Void: " + this.void_product + "\n");
		stringBuilder.append("Stock: " + this.stock + "\n");
		stringBuilder.append("Marketplace: " + this.marketplace + "\n");
		
		return stringBuilder.toString();
	}

}
