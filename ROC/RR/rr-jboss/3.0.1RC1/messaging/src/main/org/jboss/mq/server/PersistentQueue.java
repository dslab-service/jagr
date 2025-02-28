package org.jboss.mq.server;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;

import org.jboss.mq.SpyDestination;
import org.jboss.mq.SpyMessage;

/**
 *  This class implements a persistent version of the basic queue.
 *
 * @author     David Maplesden (David.Maplesden@orion.co.nz)
 * @created    August 16, 2001
 */

public class PersistentQueue extends org.jboss.mq.server.BasicQueue {
   SpyDestination   destination;

   public PersistentQueue( JMSDestinationManager server, SpyDestination destination )
      throws JMSException {
      super( server, destination.toString() );
      this.destination = destination;
      //server.getPersistenceManager().restoreQueue(this, destination);
   }

   public SpyDestination getSpyDestination()
   {
      return destination;
   }


   public void addMessage( MessageReference mesRef, org.jboss.mq.pm.Tx txId )
      throws JMSException {
      	
      SpyMessage mes = mesRef.getMessage();
      // Since we are updating the messages.
      mesRef.invalidate();
      
      mes.setJMSDestination( destination );
      if ( mes.getJMSDeliveryMode() == DeliveryMode.PERSISTENT ) {
         server.getPersistenceManager().add( mesRef, txId );
      }

      super.addMessage( mesRef, txId );
   }
}
