package com.c9.security;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

public class QueueCreationHandler implements Handler<Message<JsonObject>> 
{
	final Session session;
	final Logger logger;
	final EventBus eventBus;

	public QueueCreationHandler(Logger logger, Session session, EventBus eventBus) 
	{
		this.session = session;
		this.logger = logger;
		this.eventBus = eventBus;
	}

	@Override
	public void handle(Message<JsonObject> event) 
	{
		// New Queue to create and listen to
		final String queue = event.body().getString("queue");
		final String address = event.body().getString("address");
		
		logger.info("Creating queue listenr in bus : " + queue + " and forwarding requests to " + address);
		
		// Create a Session
		try
		{
			// Create the destination (Topic or Queue)
			Destination destination = session.createQueue(queue);
	
			// Create a MessageConsumer from the Session to the Topic or Queue
			MessageConsumer consumer = session.createConsumer(destination);
	
			consumer.setMessageListener(new MessageListener() {
				@Override
				public void onMessage(javax.jms.Message message) {
					
					System.out.println("Recieving message...");
					
					if (message instanceof TextMessage) 
					{
						JsonObject jsonResponse = new JsonObject();
						
						jsonResponse.putString("queue", queue);
						
						TextMessage textMessage = (TextMessage) message;
						String text;
						try 
						{
							text = textMessage.getText();
							
							jsonResponse.putString("message", text);
							// Publish this message to all
							
							eventBus.send("jms.queue." + address, jsonResponse);

							logger.info("Received: " + text);
						} catch (JMSException e) {
							e.printStackTrace();
						}
					} 
					else {
						logger.info("Received: " + message);
					}
				}
			});
			
			JsonObject response = new JsonObject();
			
			response.putString("staus", "OK");
			
			event.reply(response);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			
			JsonObject response = new JsonObject();
			
			response.putString("staus", "FAILED");
			
			response.putString("message", e.getMessage());
			
			event.reply(response);
		}
		
	}
}