/*
 * JBoss, the OpenSource EJB server
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.ejb.plugins.jms;

import java.util.Hashtable;
import java.util.Enumeration;

import javax.naming.InitialContext;
import javax.naming.Context;

import javax.jms.Session;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.QueueSender;
import javax.jms.Queue;
import javax.jms.Message;
import javax.jms.JMSException;

import org.w3c.dom.Element;

import org.jboss.logging.Logger;
import org.jboss.deployment.DeploymentException;
import org.jboss.metadata.MetaData;
import org.jboss.jms.jndi.JMSProviderAdapter;

import org.jboss.system.ServiceMBeanSupport;

/**
 * Places redeliveded messages on a Dead Letter Queue.
 *
 *<p>The Dead Letter Queue handler is used to not set JBoss in an endles loop
 * when a message is resent on and on due to transaction rollback for
 * message receipt.
 *
 *<p>It sends message to a dead letter queue (configurable, defaults to
 * queue/DLQ) when the message has been resent a configurable amount of times,
 * defaults to 10.
 *
 * <p>The handler is configured through the element MDBConfig in
 * container-invoker-conf.
 *
 * <p>The JMS property JBOSS_ORIG_DESTINATION in the resent message is set
 * to the name of the original destination (Destionation.toString()).
 *
 * <p>The JMS property JBOSS_ORIG_MESSAGEID in the resent message is set
 * to the id of the original message.
 *
 * Created: Thu Aug 23 21:17:26 2001
 *
 * @version <tt>$Revision: 1.1.1.1 $ $Date: 2003/03/07 08:26:09 $</tt>
 * @author ???
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public class DLQHandler
   extends ServiceMBeanSupport
{
   /** JMS property name holding original destination. */
   public static final String JBOSS_ORIG_DESTINATION ="JBOSS_ORIG_DESTINATION";
   
   /** JMS property name holding original JMS message id. */
   public static final String JBOSS_ORIG_MESSAGEID="JBOSS_ORIG_MESSAGEID";
   
   // Configuratable stuff

   /**
    *  Destination to send dead letters to.
    *<p>Defaults to queue/DLQ,
    * configurable through DestinationQueue element.
    */
   private String destinationJNDI = "queue/DLQ";
   
   /**
    * Maximum times a message is alowed to be resent.
    *
    * <p>Defaults to 10, configurable through MaxTimesRedelivered element.
    */
   private int maxResent = 10;
   
   /**
    * Time to live for the message.
    *
    *<p>Defaults to Message.DEFAULT_TIME_TO_LIVE, configurable through
    * the TimeToLive element.
    */
   private long timeToLive = Message.DEFAULT_TIME_TO_LIVE;
   
   // May become configurable
   /** Delivery mode for message, Message.DEFAULT_DELIVERY_MODE. */
   private int deliveryMode = Message.DEFAULT_DELIVERY_MODE;
   /** Priority for the message, Message.DEFAULT_PRIORITY */
   private int priority = Message.DEFAULT_PRIORITY;
   
   // Private stuff
   private QueueConnection connection;
   private Queue dlq;
   private Hashtable resentBuffer = new Hashtable();
   private JMSProviderAdapter providerAdapter;

   public DLQHandler(final JMSProviderAdapter providerAdapter)
   {
      this.providerAdapter = providerAdapter;
   }

   //--- Service
   
   /**
    * Initalize the service.
    *
    * @throws Exception    Service failed to initalize.
    */
   protected void createService() throws Exception
   {
      Context ctx = providerAdapter.getInitialContext();
      
      try {
         QueueConnectionFactory factory = (QueueConnectionFactory)
            ctx.lookup(providerAdapter.getQueueFactoryRef());
         log.debug("Using factory: " + factory);
         
         connection = factory.createQueueConnection();
         log.debug("Created connection: " + connection);

         dlq = (Queue)ctx.lookup(destinationJNDI);
         log.debug("Using Queue: " + dlq);
      }
      finally {
         ctx.close();
      }
   }

   protected void startService() throws Exception
   {
      connection.start();
   }

   protected void stopService() throws Exception
   {
      connection.stop();
   }
   
   /**
    * Destroy the service.
    */
   protected void destroyService() throws Exception
   {
      connection = null;
   }
   
   //--- Logic
   /**
    * Check if a message has been redelivered to many times.
    *
    * If message has been redelivered to many times, send it to the
    * dead letter queue (default to queue/DLQ).
    *
    * @return true if message is handled, i.e resent, false if not.
    */
   public boolean handleRedeliveredMessage(Message msg)
   {
      try
      {
         String id = msg.getJMSMessageID();
         if (id == null)
         {
            // if we can't get the id we are basically fucked
            log.error("Message id is null, can't handle message");
         }
         else if(incrementResentCount(id) > maxResent)
         {
            log.warn("Message resent too many times; sending it to DLQ. Id: " + id);
            sendMessage(msg);
            deleteFromBuffer(id);
            return true;
         }
      }
      catch(JMSException ex)
      {
         // If we can't send it ahead, we do not dare to just drop it...or?
         log.error("Could not send message to Dead Letter Queue", ex);
      }
      
      return false;
   }

   //--- Private helper stuff
   /**
    * Increment the counter for the specific JMS message id.
    *
    * @return the new counter value.
    */
   protected int incrementResentCount(String id)
   {
      BufferEntry entry = null;
      boolean trace = log.isTraceEnabled();
      if(!resentBuffer.containsKey(id))
      {
         if (trace)
         log.trace("Making new entry for id " + id);
         entry = new BufferEntry();
         entry.id = id;
         entry.count = 1;
         resentBuffer.put(id,entry);
      } else
      {
         entry = (BufferEntry)resentBuffer.get(id);
         entry.count++;
         if (trace)
         log.trace("Incremented old entry for id " + id + " count " + entry.count);
      }
      return entry.count;
   }
   
   /**
    * Delete the entry in the message counter buffer for specifyed JMS id.
    */
   protected void deleteFromBuffer(String id)
   {
      resentBuffer.remove(id);
   }
   
   /**
    * Send message to the configured dead letter queue, defaults to queue/DLQ.
    */
   protected void sendMessage(Message msg) throws JMSException
   {
      // Set the properties
      QueueSession ses = null;
      QueueSender sender = null;
      try
      {
         msg = makeWritable(msg);//Don't know yet if we are gona clone or not
         msg.setStringProperty(JBOSS_ORIG_MESSAGEID,
         msg.getJMSMessageID());
         msg.setStringProperty(JBOSS_ORIG_DESTINATION,
         msg.getJMSDestination().toString());
         
         ses = connection.createQueueSession(false,Session.AUTO_ACKNOWLEDGE);
         sender = ses.createSender(dlq);
         if (log.isTraceEnabled())
            log.trace("Resending DLQ message to destination" + dlq);
         sender.send(msg,deliveryMode,priority,timeToLive);
      }
      finally
      {
         try
         {
            sender.close();
            ses.close();
         }
         catch(Exception ex)
         {
         }
      }
      
   }
   
   /**
    * Make the Message properties writable.
    *
    * @return the writable message.
    */
   protected Message makeWritable(Message msg) throws JMSException
   {
      Hashtable tmp = new Hashtable();
      // Save properties
      for(Enumeration en = msg.getPropertyNames();en.hasMoreElements();)
      {
         String key = (String) en.nextElement();
         tmp.put(key,msg.getStringProperty(key));
      }
      // Make them writable
      msg.clearProperties();
      
      Enumeration keys = tmp.keys();
      while(keys.hasMoreElements())
      {
         String key = (String) keys.nextElement();
         msg.setStringProperty(key,(String)tmp.get(key));
      }
      return msg;
   }
   
   /**
    * Takes an MDBConfig Element
    */
   public void importXml(Element element) throws DeploymentException
   {
      destinationJNDI = MetaData.getElementContent
         (MetaData.getUniqueChild(element, "DestinationQueue"));
      
      try
      {
         String mr = MetaData.getElementContent
            (MetaData.getUniqueChild(element, "MaxTimesRedelivered"));
         maxResent = Integer.parseInt(mr);
      }
      catch (Exception ignore) {}

      try {
         String ttl = MetaData.getElementContent
            (MetaData.getUniqueChild(element, "TimeToLive"));
         timeToLive = Long.parseLong(ttl);
         
         if (timeToLive < 0) {
            log.warn("Invalid TimeToLive: " + timeToLive + "; using default");
            timeToLive = Message.DEFAULT_TIME_TO_LIVE;
         }
      }
      catch (Exception ignore) {}
   }
   
   public String toString()
   {
      StringBuffer buff = new StringBuffer();
      buff.append("DLQHandler: {");
      buff.append("destinationJNDI=").append(destinationJNDI);
      buff.append(";maxResent=").append(maxResent);
      buff.append(";timeToLive=").append(timeToLive);
      buff.append("}");
      return buff.toString();
   }
   
   private class BufferEntry
   {
      int count;
      String id;
   }
} // DLQHandler












