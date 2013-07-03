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

public class TopicCreationHandler implements Handler<Message<JsonObject>> 
{
	final Session session;
	final Logger logger;
	final EventBus eb;

	public TopicCreationHandler(Logger logger, Session session, EventBus eb) 
	{
		this.session = session;
		this.logger = logger;
		this.eb = eb;
	}

	@Override
	public void handle(Message<JsonObject> event) 
	{
		// New Queue to create and listen to
		final String topic = event.body().getString("topic");
		final boolean publish = event.body().getBoolean("publish", false);
		final String address = event.body().getString("address");
		
		logger.info("Creating queue listenr in bus : " + topic);
		
		// Create a Session
		try
		{
			// Create the destination (Topic or Queue)
			Destination destination = session.createTopic(topic);
	
			// Create a MessageConsumer from the Session to the Topic or Queue
			MessageConsumer consumer = session.createConsumer(destination);
	
			consumer.setMessageListener(new MessageListener() 
			{
				@Override
				public void onMessage(javax.jms.Message message) {
					
					logger.info("Recieving message...");
					
					if (message instanceof TextMessage) 
					{
						JsonObject jsonResponse = new JsonObject();
						
						jsonResponse.putString("topic", topic);
						
						TextMessage textMessage = (TextMessage) message;
						String text;
						try 
						{
							text = textMessage.getText();
							
							jsonResponse.putString("message", text);
							// Publish this message to all
							
							if(publish) {
								eb.publish("jms.topic." + address, jsonResponse);
							}
							else {
								eb.send("jms.topic." + address, jsonResponse);
							}
							
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