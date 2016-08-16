package br.com.lett.crawlernode.kernel.models;

import java.util.ArrayList;
import java.util.Map;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.server.QueueService;

public class Market {
	
	private int id;
	private String city;
	private String name;
	private ArrayList<String> proxies;
	
	public Market(
			int id, 
			String city, 
			String name,
			String preferredProxyService) {
		
		this.id = id;
		this.city = city;
		this.name = name;
		this.proxies = new ArrayList<String>();
	}
	
	public Market(Message message) {
		Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();
		
		// market id
		if (attrMap.containsKey(QueueService.MARKET_ID_MESSAGE_ATTR)) {
			this.id = Integer.parseInt(attrMap.get(QueueService.MARKET_ID_MESSAGE_ATTR).getStringValue());
		}
		
		// market city
		if (attrMap.containsKey(QueueService.CITY_MESSAGE_ATTR)) {
			this.city = attrMap.get(QueueService.CITY_MESSAGE_ATTR).getStringValue();
		}		
		
		// market name
		if (attrMap.containsKey(QueueService.MARKET_MESSAGE_ATTR)) {
			this.name = attrMap.get(QueueService.MARKET_MESSAGE_ATTR).getStringValue();
		}
		
		// proxies
		if (attrMap.containsKey(QueueService.PROXY_SERVICE_MESSAGE_ATTR)) {
			String proxiesFromMessage = attrMap.get(QueueService.PROXY_SERVICE_MESSAGE_ATTR).getStringValue().replace("[", "").replace("]", "");
			ArrayList<String> proxies = new ArrayList<String>();
			String[] tokens = proxiesFromMessage.split(",");
			for (String token : tokens) {
				proxies.add(token.trim());
			}
			this.setProxies(proxies);
		}
		
	}

	public int getNumber() {
		return id;
	}
	
	public void setNumber(int number) {
		this.id = number;
	}

	public String getCity() {
		return city;
	}
	
	public void setCity(String city) {
		this.city = city;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "Market [id=" + this.id + 
				", city=" + this.city + 
				", name=" + this.name;
	}

	public ArrayList<String> getProxies() {
		return proxies;
	}

	public void setProxies(ArrayList<String> proxies) {
		this.proxies = proxies;
	}
}
