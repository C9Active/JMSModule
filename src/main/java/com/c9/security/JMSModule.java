package com.c9.security;

import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class JMSModule extends BusModBase 
{
	String activeMQHost;
	Connection connection;
	Session session;
	ActiveMQConnectionFactory connectionFactory;
	
	boolean connected = false;

	public void start() {
		
		super.start();
		
		activeMQHost = getOptionalStringConfig("activeMQHost", "tcp://localhost:61616"); 

		eb.registerLocalHandler("jms.connected", new Handler<Message<String>>() {
			@Override
			public void handle(Message<String> event) {
				setupHandlers();
			}
		});
		
		eb.registerLocalHandler("jms.reconnect", new Handler<Message<String>>() {
			@Override
			public void handle(Message<String> event) {
				if(!connected)
				{
					createConnection();
				}
			}
		});
		
		eb.registerHandler("jms.stats", new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject stats = new JsonObject();
				
				event.reply(stats);
			}
		});
		
		createConnection();
	}

	protected void setupHandlers() {
		
		logger.info("Setting up handlers");
		Handler<Message<JsonObject>> unregisterHandler = new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				logger.info("Unregistered!");
			}
		};
		
		eb.unregisterHandler("jms.queue.create", unregisterHandler);
		eb.unregisterHandler("jms.topic.create", unregisterHandler);
		eb.unregisterHandler("jms.queue.receive", unregisterHandler);
		eb.unregisterHandler("jms.topic.receive", unregisterHandler);
		
		logger.info("Register Handler jms.queue.create and jms.topic.create ");
		eb.registerHandler("jms.queue.create", new QueueCreationHandler(logger, session, eb));
		eb.registerHandler("jms.topic.create", new TopicCreationHandler(logger, session, eb));

		// Move these to the handler - request should tell this service where to forward the notification to
		eb.registerHandler("jms.queue.receive", new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) 
			{
				container.logger().info("JMS QUEUE RESPONSE FROM BUS : " + event.body());
			}
		});
		eb.registerHandler("jms.topic.receive", new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) 
			{
				container.logger().info("JMS TOPIC RESPONSE FROM BUS : " + event.body());
			}
		});
		
	}

	private void createConnection() {
//		Create new listener's by registering what topic/queue you care about
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(activeMQHost);
		
		try
		{
			// Create a Connection
			connection = connectionFactory.createConnection();
			
			connection.start();
	
			connection.setExceptionListener(new ExceptionListener() {
				@Override
				public void onException(JMSException arg0) {
					
					arg0.printStackTrace();
					
					logger.debug("ERROR!!" + arg0.getMessage());
					
					eb.send("error", new JsonObject().putString("message", "JMS Exception : " + arg0.getMessage()));
					
					eb.send("jms.reconnect", "");
					
					connected = false;
				}
			});
			
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			
			connected = true;
			
            logger.info("jms.connected");
            
			eb.send("jms.connected", "");
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			eb.send("error", new JsonObject().putString("message", "JMS Exception : " + e.getMessage()));
			
			eb.send("jms.reconnect", "");
			
			connected = false;
		}
	}
}