/*
 * JBossMQ, the OpenSource JMS implementation
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.mq.server;
import java.util.HashMap;
import java.util.Hashtable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import javax.jms.DeliveryMode;

import javax.jms.Destination;
import javax.jms.JMSException;

import org.jboss.logging.Logger;

import org.jboss.mq.SpyDestination;
import org.jboss.mq.SpyMessage;
import org.jboss.mq.Subscription;

/**
 *  This class is a message queue which is stored (hashed by Destination) on the
 *  JMS provider
 *
 * @author     Norbert Lataille (Norbert.Lataille@m4x.org)
 * @author     Hiram Chirino (Cojonudo14@hotmail.com)
 * @author     David Maplesden (David.Maplesden@orion.co.nz)
 * @created    August 16, 2001
 * @version    $Revision: 1.1.1.1 $
 */
public abstract class JMSDestination {

   //the Destination of this queue
   SpyDestination   destination;
   //If this is a temporaryDestination, temporaryDestination=ClientConsumer of the owner, otherwise it's null
   ClientConsumer   temporaryDestination;
   //The JMSServer object
   JMSDestinationManager        server;

   //Counter used to number incomming messages. (Used to order the messages.)
   long             messageIdCounter = 0;

   Logger cat;

   // Constructor ---------------------------------------------------
   JMSDestination( SpyDestination dest, ClientConsumer temporary, JMSDestinationManager server )
      throws JMSException {
      cat = Logger.getLogger( this.getClass().getName() + ":" + dest );
      destination = dest;
      temporaryDestination = temporary;
      this.server = server;
   }

   public SpyDestination getSpyDestination()
   {
      return destination;
   }


   public abstract void addSubscriber( Subscription sub )
      throws JMSException;

   public abstract void removeSubscriber( Subscription sub )
      throws JMSException;

   public abstract void nackMessages( Subscription sub )
      throws JMSException;

   public abstract SpyMessage receive( Subscription sub, boolean wait )
      throws JMSException;

   public abstract void addReceiver( Subscription sub );

   public abstract void removeReceiver( Subscription sub );

   public abstract void restoreMessage( MessageReference message );

   public abstract void clientConsumerStopped( ClientConsumer clientConsumer );
   public abstract boolean isInUse();
   
   public abstract void close() throws JMSException;
   public abstract void removeAllMessages() throws JMSException;

   /**
    * @param  req                         org.jboss.mq.AcknowledgementRequest
    * @param  sub                         org.jboss.mq.Subscription
    * @param  txId                        org.jboss.mq.pm.Tx
    * @exception  javax.jms.JMSException  The exception description.
    */
   public abstract void acknowledge( org.jboss.mq.AcknowledgementRequest req, org.jboss.mq.Subscription sub, org.jboss.mq.pm.Tx txId )
      throws javax.jms.JMSException;

   /**
    * @param  mes                         org.jboss.mq.SpyMessage
    * @param  txId                        org.jboss.mq.pm.Tx
    * @exception  javax.jms.JMSException  The exception description.
    */
   public abstract void addMessage( org.jboss.mq.SpyMessage mes, org.jboss.mq.pm.Tx txId )
      throws javax.jms.JMSException;
}
