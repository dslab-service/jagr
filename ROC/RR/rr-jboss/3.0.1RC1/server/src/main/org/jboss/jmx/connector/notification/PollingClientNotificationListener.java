/*
* JBoss, the OpenSource J2EE webOS
*
* Distributable under LGPL license.
* See terms of license at gnu.org.
*/
package org.jboss.jmx.connector.notification;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import javax.management.JMException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.jboss.jmx.connector.RemoteMBeanServer;

/**
* Local Polling Listener to receive the message and send to the listener
**/
public class PollingClientNotificationListener
   extends ClientNotificationListener
   implements Runnable
{

   private RemoteMBeanServer                mConnector;
   private int                         mSleepingPeriod = 2000;
   
   public PollingClientNotificationListener(
      ObjectName pSender,
      NotificationListener pClientListener,
      Object pHandback,
      NotificationFilter pFilter,
      int pSleepingPeriod,
      int pMaximumListSize,
      RemoteMBeanServer pConnector
   ) throws
      JMException
   {
      super( pSender, pClientListener, pHandback );
      if( pSleepingPeriod > 0 ) {
         mSleepingPeriod = pSleepingPeriod;
      }
      mConnector = pConnector;
      // Register the listener as MBean on the remote JMX server
      createListener(
         pConnector,
         "org.jboss.jmx.connector.notification.PollingNotificationListener",
         new Object[] { new Integer( pMaximumListSize ), new Integer( pMaximumListSize ) },
         new String[] { Integer.TYPE.getName(), Integer.TYPE.getName() }
      );
      addNotificationListener( pConnector, pFilter );
      new Thread( this ).start();
   }

   public void run() {
      while( true ) {
         try {
            try {
               List lNotifications = (List) mConnector.getAttribute(
                  getRemoteListenerName(),
                  "Notifications"
               );
               Iterator i = lNotifications.iterator();
               while( i.hasNext() ) {
                  Notification lNotification = (Notification) i.next();
                  mClientListener.handleNotification(
                     lNotification,
                     mHandback
                  );
               }
            }
            catch( Exception e ) {
               System.out.println( "PollingClientNotificationListener.getNotifications() got exception " + e );
            }
            Thread.sleep( mSleepingPeriod );
         }
         catch( InterruptedException ie ) {
         }
      }
   }
}
