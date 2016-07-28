package br.com.lett.crawlernode.models;

import java.util.Map;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.queue.QueueService;

public class Market {
	
	private int id;
	private String city;
	private String name;
	
	public Market(int number, String city, String name) {
		super();
		this.id = number;
		this.city = city;
		this.name = name;
	}
	
	public Market(Message message) {
		Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();
		
		if (attrMap.containsKey(QueueService.MARKET_ID_MESSAGE_ATTR)) {
			this.id = Integer.parseInt(attrMap.get(QueueService.MARKET_ID_MESSAGE_ATTR).getStringValue());
		}
		
		if (attrMap.containsKey(QueueService.CITY_MESSAGE_ATTR)) {
			this.city = attrMap.get(QueueService.CITY_MESSAGE_ATTR).getStringValue();
		}		
		
		if (attrMap.containsKey(QueueService.MARKET_MESSAGE_ATTR)) {
			this.name = attrMap.get(QueueService.MARKET_MESSAGE_ATTR).getStringValue();
		}
	}
	
	@Override
	public String toString() {
		return "Market [number=" + id + ", city=" + city + ", name=" + name
				+ ", production=" + "]";
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
}
