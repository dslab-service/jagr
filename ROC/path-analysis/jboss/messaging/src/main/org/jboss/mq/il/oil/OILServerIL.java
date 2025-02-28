/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.mq.il.oil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;

import java.net.Socket;
import javax.jms.Destination;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.Topic;
import javax.net.SocketFactory;

import org.jboss.logging.Logger;
import org.jboss.mq.AcknowledgementRequest;
import org.jboss.mq.ConnectionToken;
import org.jboss.mq.DurableSubscriptionID;
import org.jboss.mq.SpyDestination;
import org.jboss.mq.SpyMessage;
import org.jboss.mq.TransactionRequest;
import org.jboss.mq.il.ServerIL;

/**
 * The JVM implementation of the ServerIL object
 *
 * @author    Hiram Chirino (Cojonudo14@hotmail.com)
 * @author    Norbert Lataille (Norbert.Lataille@m4x.org)
 * @version   $Revision: 1.1.1.1 $
 * @created   August 16, 2001
 */
public final class OILServerIL
   implements java.io.Serializable, 
      java.lang.Cloneable,
      org.jboss.mq.il.ServerIL
{
   private static Logger log = Logger.getLogger(OILServerIL.class);

   /** The org.jboss.mq.il.oil2.localAddr system property allows a client to
    *define the local interface to which its sockets should be bound
    */
   private final static String LOCAL_ADDR = "org.jboss.mq.il.oil.localAddr";
   /** The org.jboss.mq.il.oil2.localPort system property allows a client to
    *define the local port to which its sockets should be bound
    */
   private final static String LOCAL_PORT = "org.jboss.mq.il.oil.localPort";

   /** The server host name/IP to connect to
    */
   private InetAddress addr;
   /** The server port to connect to.
    */
   private int port;
   /** The name of the class implementing the javax.net.SocketFactory to
    * use for creating the client socket.
    */
   private String socketFactoryName;

   /**
    * If the TcpNoDelay option should be used on the socket.
    */
   private boolean enableTcpNoDelay = false;
   /** The local interface name/IP to use for the client
    */
   private transient InetAddress localAddr;
   /** The local port to use for the client
    */
   private transient int localPort;

   /**
    * Description of the Field
    */
   private transient ObjectInputStream in;

   /**
    * Description of the Field
    */
   private transient ObjectOutputStream out;
   
   /**
    * Description of the Field
    */
   private transient Socket socket;

   /**
    * Constructor for the OILServerIL object
    *
    * @param a     Description of Parameter
    * @param port  Description of Parameter
    */
   public OILServerIL(InetAddress addr, int port, 
      String socketFactoryName, boolean enableTcpNoDelay)
   {
      this.addr = addr;
      this.port = port;
      this.socketFactoryName = socketFactoryName;
      this.enableTcpNoDelay = enableTcpNoDelay;
   }

   /**
    * Sets the ConnectionToken attribute of the OILServerIL object
    *
    * @param dest           The new ConnectionToken value
    * @exception Exception  Description of Exception
    */
   public synchronized void setConnectionToken(ConnectionToken dest)
          throws Exception
   {
      checkConnection();
      out.writeByte(OILConstants.SET_SPY_DISTRIBUTED_CONNECTION);
      out.writeObject(dest);
      waitAnswer();
   }

   /**
    * Sets the Enabled attribute of the OILServerIL object
    *
    * @param dc                The new Enabled value
    * @param enabled           The new Enabled value
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized void setEnabled(ConnectionToken dc, boolean enabled)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.SET_ENABLED);
      out.writeBoolean(enabled);
      waitAnswer();
   }

   /**
    * Gets the ID attribute of the OILServerIL object
    *
    * @return               The ID value
    * @exception Exception  Description of Exception
    */
   public synchronized String getID()
          throws Exception
   {
      checkConnection();
      out.writeByte(OILConstants.GET_ID);
      return (String)waitAnswer();
   }

   /**
    * Gets the TemporaryQueue attribute of the OILServerIL object
    *
    * @param dc                Description of Parameter
    * @return                  The TemporaryQueue value
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized TemporaryQueue getTemporaryQueue(ConnectionToken dc)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.GET_TEMPORARY_QUEUE);
      return (TemporaryQueue)waitAnswer();
   }

   /**
    * Gets the TemporaryTopic attribute of the OILServerIL object
    *
    * @param dc                Description of Parameter
    * @return                  The TemporaryTopic value
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized TemporaryTopic getTemporaryTopic(ConnectionToken dc)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.GET_TEMPORARY_TOPIC);
      return (TemporaryTopic)waitAnswer();
   }

   /**
    * #Description of the Method
    *
    * @param dc                Description of Parameter
    * @param item              Description of Parameter
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized void acknowledge(ConnectionToken dc, AcknowledgementRequest item)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.ACKNOWLEDGE);
      item.writeExternal(out);
      waitAnswer();
   }

   /**
    * Adds a feature to the Message attribute of the OILServerIL object
    *
    * @param dc             The feature to be added to the Message attribute
    * @param val            The feature to be added to the Message attribute
    * @exception Exception  Description of Exception
    */
   public synchronized void addMessage(ConnectionToken dc, SpyMessage val)
          throws Exception
   {
      checkConnection();
      out.writeByte(OILConstants.ADD_MESSAGE);
      SpyMessage.writeMessage(val, out);
      waitAnswer();
   }

   /**
    * #Description of the Method
    *
    * @param dc                Description of Parameter
    * @param dest              Description of Parameter
    * @param selector          Description of Parameter
    * @return                  Description of the Returned Value
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized SpyMessage[] browse(ConnectionToken dc, Destination dest, String selector)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.BROWSE);
      out.writeObject(dest);
      out.writeObject(selector);
      return (SpyMessage[])waitAnswer();
   }

   /**
    * #Description of the Method
    *
    * @param ID                Description of Parameter
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized void checkID(String ID)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.CHECK_ID);
      out.writeObject(ID);
      waitAnswer();
   }

   /**
    * #Description of the Method
    *
    * @param userName          Description of Parameter
    * @param password          Description of Parameter
    * @return                  Description of the Returned Value
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized String checkUser(String userName, String password)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.CHECK_USER);
      out.writeObject(userName);
      out.writeObject(password);
      return (String)waitAnswer();
   }

   /**
    * #Description of the Method
    *
    * @param userName          Description of Parameter
    * @param password          Description of Parameter
    * @return                  Description of the Returned Value
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized String authenticate(String userName, String password)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.AUTHENTICATE);
      out.writeObject(userName);
      out.writeObject(password);
      return (String)waitAnswer();
   }
   /**
    * #Description of the Method
    *
    * @return                                Description of the Returned Value
    * @exception CloneNotSupportedException  Description of Exception
    */
   public Object clone()
          throws CloneNotSupportedException
   {
      return super.clone();
   }

   /**
    * Need to clone because there are instance variables tha can get clobbered.
    * All Multiple connections can NOT share the same JVMServerIL object
    *
    * @return               Description of the Returned Value
    * @exception Exception  Description of Exception
    */
   public ServerIL cloneServerIL()
          throws Exception
   {
      return (ServerIL)clone();
   }

   /**
    * #Description of the Method
    *
    * @param dc                Description of Parameter
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized void connectionClosing(ConnectionToken dc)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.CONNECTION_CLOSING);
      waitAnswer();
      destroyConnection();
   }

   /**
    * #Description of the Method
    *
    * @param dc                Description of Parameter
    * @param dest              Description of Parameter
    * @return                  Description of the Returned Value
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized Queue createQueue(ConnectionToken dc, String dest)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.CREATE_QUEUE);
      out.writeObject(dest);
      return (Queue)waitAnswer();
   }

   /**
    * #Description of the Method
    *
    * @param dc                Description of Parameter
    * @param dest              Description of Parameter
    * @return                  Description of the Returned Value
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized Topic createTopic(ConnectionToken dc, String dest)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.CREATE_TOPIC);
      out.writeObject(dest);
      return (Topic)waitAnswer();
   }

   /**
    * #Description of the Method
    *
    * @param dc                Description of Parameter
    * @param dest              Description of Parameter
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized void deleteTemporaryDestination(ConnectionToken dc, SpyDestination dest)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.DELETE_TEMPORARY_DESTINATION);
      out.writeObject(dest);
      waitAnswer();
   }

   /**
    * #Description of the Method
    *
    * @param id                Description of Parameter
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized void destroySubscription(ConnectionToken dc,DurableSubscriptionID id)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.DESTROY_SUBSCRIPTION);
      out.writeObject(id);
      waitAnswer();
   }

   /**
    * #Description of the Method
    *
    * @param dc             Description of Parameter
    * @param clientTime     Description of Parameter
    * @exception Exception  Description of Exception
    */
   public synchronized void ping(ConnectionToken dc, long clientTime)
          throws Exception
   {
      checkConnection();
      out.writeByte(OILConstants.PING);
      out.writeLong(clientTime);
      waitAnswer();
   }

   /**
    * #Description of the Method
    *
    * @param dc             Description of Parameter
    * @param subscriberId   Description of Parameter
    * @param wait           Description of Parameter
    * @return               Description of the Returned Value
    * @exception Exception  Description of Exception
    */
   public synchronized SpyMessage receive(ConnectionToken dc, int subscriberId, long wait)
          throws Exception, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.RECEIVE);
      out.writeInt(subscriberId);
      out.writeLong(wait);
      return (SpyMessage)waitAnswer();
   }

   /**
    * #Description of the Method
    *
    * @param dc                Description of Parameter
    * @param s                 Description of Parameter
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized void subscribe(ConnectionToken dc, org.jboss.mq.Subscription s)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.SUBSCRIBE);
      out.writeObject(s);
      waitAnswer();
   }

   /**
    * #Description of the Method
    *
    * @param dc                Description of Parameter
    * @param t                 Description of Parameter
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized void transact(org.jboss.mq.ConnectionToken dc, TransactionRequest t)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.TRANSACT);
      t.writeExternal(out);
      waitAnswer();
   }

   /**
    * #Description of the Method
    *
    * @param dc                Description of Parameter
    * @param subscriptionId    Description of Parameter
    * @exception JMSException  Description of Exception
    * @exception Exception     Description of Exception
    */
   public synchronized void unsubscribe(ConnectionToken dc, int subscriptionId)
          throws JMSException, Exception
   {
      checkConnection();
      out.writeByte(OILConstants.UNSUBSCRIBE);
      out.writeInt(subscriptionId);
      waitAnswer();
   }

   /**
    * #Description of the Method
    *
    * @exception Exception  Description of Exception
    */
   private void checkConnection()
          throws Exception
   {
      if (socket == null)
      {
         createConnection();
      }
   }

   /**
    * Used to establish a new connection to the server
    *
    * @exception Exception  Description of Exception
    */
   private void createConnection()
          throws Exception
   {
      boolean tracing = log.isTraceEnabled();
      if( tracing )
         log.trace("Connecting to : "+addr+":"+port);

      /** Attempt to load the socket factory and if this fails, use the
       * default socket factory impl.
       */
      SocketFactory socketFactory = null;
      if( socketFactoryName != null )
      {
         try
         {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class factoryClass = loader.loadClass(socketFactoryName);
            socketFactory = (SocketFactory) factoryClass.newInstance();
         }
         catch(Exception e)
         {
            log.debug("Failed to load socket factory: "+socketFactoryName, e);
         }
      }
      // Use the default socket factory
      if( socketFactory == null )
      {
         socketFactory = SocketFactory.getDefault();
      }

      // Look for a local address and port as properties
      String tmp = System.getProperty(LOCAL_ADDR);
      if( tmp != null )
         this.localAddr = InetAddress.getByName(tmp);
      tmp = System.getProperty(LOCAL_PORT);
      if( tmp != null )
         this.localPort = Integer.parseInt(tmp);
      if( tracing )
      {
         log.trace("Connecting with addr="+addr+", port="+port
            + ", localAddr="+localAddr+", localPort="+localPort
            + ", socketFactory="+socketFactory);
      }

      if( localAddr != null )
         socket = socketFactory.createSocket(addr, port, localAddr, localPort);
      else
         socket = socketFactory.createSocket(addr, port);

      socket.setTcpNoDelay(enableTcpNoDelay);
      in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
      out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
      out.flush();
   }
   
   /**
    * Used to close the current connection with the server
    *
    * @exception Exception  Description of Exception
    */
   private void destroyConnection()
          throws Exception
   {
      out.close();
      in.close();
      socket.close();
   }

   /**
    * #Description of the Method
    *
    * @return               Description of the Returned Value
    * @exception Exception  Description of Exception
    */
   private Object waitAnswer()
          throws Exception
   {
      out.reset();
      out.flush();
      int val = in.readByte();
      if (val == OILConstants.SUCCESS)
      {
         return null;
      }
      if (val == OILConstants.SUCCESS_OBJECT)
      {
         return in.readObject();
      }
      else
      {
         Exception e = (Exception)in.readObject();
         throw e;
      }
   }
}
