package br.com.lett.crawlernode.core.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageBatchResultEntry;

import br.com.lett.crawlernode.core.session.Session;

import br.com.lett.crawlernode.queue.QueueHandler;
import br.com.lett.crawlernode.queue.QueueName;
import br.com.lett.crawlernode.queue.QueueService;
import br.com.lett.crawlernode.util.Logging;
import models.Processed;


public class Scheduler {

	protected static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);


	public static void scheduleImages(
			Session session,
			QueueHandler queueHandler,
			Processed processed,
			Long processedId) {
		Logging.printLogDebug(LOGGER, session, "Scheduling images to be downloaded...");

		List<SendMessageBatchRequestEntry> entries = new ArrayList<>(); // send messages batch to Amazon SQS
		//List<TaskDocumentModel> tasksDocuments = new ArrayList<TaskDocumentModel>();
		Integer counter = 0;
		Integer insideBatchId = 0;

		int marketId = session.getMarket().getNumber();
		String primaryPic = processed.getPic();
		String internalId = processed.getInternalId();
		String secondaryImages = processed.getSecondaryImages();
		JSONArray secondaryPicsJSON = null;

		// get the secondary pics json array
		if (secondaryImages != null && !secondaryImages.isEmpty()) {
			secondaryPicsJSON = new JSONArray(secondaryImages);
		} else {
			secondaryPicsJSON = new JSONArray();
		}

		// assemble the primary image message
		if (primaryPic != null && !primaryPic.isEmpty()) {
			Map<String, MessageAttributeValue> attrPrimary = assembleImageMessageAttributes(
					marketId, 
					QueueService.PRIMARY_IMAGE_TYPE_MESSAGE_ATTR, 
					internalId, 
					processedId, 
					1);

			SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry();
			entry.setId(String.valueOf(insideBatchId));	// the id must be unique in the batch
			entry.setMessageAttributes(attrPrimary);
			entry.setMessageBody(primaryPic);

			entries.add(entry);
			counter++;
			insideBatchId++;

			// when the batch reaches size 10, we send them all to sqs and empty the list
			if (entries.size() == 10) {
				Logging.printLogDebug(LOGGER, session, "Sending batch of " + entries.size() + " messages...");

				// send the batch
				SendMessageBatchResult result;
				result = QueueService.sendBatchMessages(queueHandler.getSqs(), QueueName.IMAGES, entries);

				// get send request results
				List<SendMessageBatchResultEntry> successResultEntryList = result.getSuccessful();

				entries.clear();
				insideBatchId = 0;
			}
		}

		//TODO comentando porque estava dando muita imagem
		// assemble the secondary images
//		for (int i = 0; i < secondaryPicsJSON.length(); i++) {
//			Map<String, MessageAttributeValue> attrSecondary = assembleImageMessageAttributes(
//					marketCity, 
//					marketName, 
//					QueueService.SECONDARY_IMAGES_MESSAGE_ATTR, 
//					internalId, 
//					processedId, 
//					i+2);
//
//			SendMessageBatchRequestEntry secondaryEntry = new SendMessageBatchRequestEntry();
//			secondaryEntry.setId(String.valueOf(insideBatchId));	// the id must be unique in the batch
//			secondaryEntry.setMessageAttributes(attrSecondary);
//			secondaryEntry.setMessageBody(secondaryPicsJSON.getString(i));
//
//			entries.add(secondaryEntry);
//			counter++;
//			insideBatchId++;
//
//			// when the batch reaches size 10, we send them all to sqs and empty the list
//			if (entries.size() == 10) {
//				Logging.printLogDebug(logger, session, "Sending batch of " + entries.size() + " messages...");
//
//				// send the batch
//				SendMessageBatchResult result = null;
//				if (session instanceof TestCrawlerSession) {
//					result = QueueService.sendBatchMessages(queueHandler.getQueue(QueueHandler.DEVELOPMENT), QueueHandler.DEVELOPMENT, entries);
//				} else {
//					result = QueueService.sendBatchMessages(queueHandler.getQueue(QueueHandler.IMAGES), QueueHandler.IMAGES, entries);
//				}
//
//				// get send request results
//				List<SendMessageBatchResultEntry> successResultEntryList = result.getSuccessful();
//
//				entries.clear();
//				insideBatchId = 0;
//			}
//		}

		if (entries.size() > 0) { // the left over
			Logging.printLogDebug(LOGGER, session, "Sending remaining batch of " + entries.size() + " messages...");

			SendMessageBatchResult result = null;
			result = QueueService.sendBatchMessages(queueHandler.getSqs(), QueueName.IMAGES, entries);

			List<SendMessageBatchResultEntry> successResultEntryList = result.getSuccessful();

			entries.clear();
		}

		Logging.printLogInfo(LOGGER, session, counter + " tasks scheduled.");

	}

	private static Map<String, MessageAttributeValue> assembleImageMessageAttributes(
			int marketId,
			String type,
			String internalId,
			Long processedId,
			int number) {

		Map<String, MessageAttributeValue> attr = new HashMap<>();
		attr.put(QueueService.MARKET_ID_MESSAGE_ATTR, new MessageAttributeValue().withDataType("String").withStringValue(String.valueOf(marketId)));
		attr.put(QueueService.IMAGE_TYPE, new MessageAttributeValue().withDataType("String").withStringValue(type));
		attr.put(QueueService.INTERNAL_ID_MESSAGE_ATTR, new MessageAttributeValue().withDataType("String").withStringValue(internalId));
		attr.put(QueueService.PROCESSED_ID_MESSAGE_ATTR, new MessageAttributeValue().withDataType("String").withStringValue(String.valueOf(processedId)));
		attr.put(QueueService.NUMBER_MESSAGE_ATTR, new MessageAttributeValue().withDataType("String").withStringValue(String.valueOf(number)));

		return attr;		
	}

}
