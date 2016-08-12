package br.com.lett.crawlernode.kernel.models;

import java.util.Map;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.queue.QueueService;

public class Market {
	
	private int id;
	private String city;
	private String name;
	private String preferredProxyService;
	
	public Market(
			int id, 
			String city, 
			String name,
			String preferredProxyService) {
		
		this.id = id;
		this.city = city;
		this.name = name;
		this.preferredProxyService = preferredProxyService;
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
		
		// market preferred proxy service
		if (attrMap.containsKey(QueueService.PROXY_SERVICE_MESSAGE_ATTR)) {
			this.preferredProxyService = attrMap.get(QueueService.PROXY_SERVICE_MESSAGE_ATTR).getStringValue();
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

	public String getPreferredProxyService() {
		return preferredProxyService;
	}

	public void setPreferredProxyService(String preferredProxyService) {
		this.preferredProxyService = preferredProxyService;
	}
	
	@Override
	public String toString() {
		return "Market [id=" + this.id + 
				", city=" + this.city + 
				", name=" + this.name + 
				", preferred proxy service=" + this.preferredProxyService + "]";
	}
}
