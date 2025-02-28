/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.invocation.pooled.interfaces;

import java.io.IOException;
import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.rmi.MarshalledObject;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.rmi.ConnectException;
import javax.transaction.TransactionRolledbackException;
import javax.transaction.SystemException;

import org.jboss.invocation.Invocation;
import org.jboss.invocation.Invoker;
import org.jboss.invocation.MarshalledInvocation;
import org.jboss.invocation.local.LocalInvoker;
import org.jboss.security.SecurityAssociation;
import org.jboss.tm.TransactionPropagationContextFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Client socket connections are pooled to avoid the overhead of
 * making a connection.  RMI seems to do a new connection with each
 * request.
 * 
 * 
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
public class PooledInvokerProxy
   implements Invoker, Externalizable
{
   // Attributes ----------------------------------------------------
   
   /**
    * Factory for transaction propagation contexts.
    *
    * @todo: marcf remove all transaction spill from here
    * 
    * When set to a non-null value, it is used to get transaction
    * propagation contexts for remote method invocations.
    * If <code>null</code>, transactions are not propagated on
    * remote method invocations.
    */
   protected static TransactionPropagationContextFactory tpcFactory = null;
   
   //  @todo: MOVE TO TRANSACTION
   // 
   // TPC factory
   public static void setTPCFactory(TransactionPropagationContextFactory tpcf) {
      tpcFactory = tpcf;
   }

   // Performance measurements
   public static long getSocketTime = 0;
   public static long readTime = 0;
   public static long writeTime = 0;
   public static long serializeTime = 0;
   public static long deserializeTime = 0;
   public static long usedPooled = 0;


   /**
    * Set number of retries in getSocket method
    */
   public static int MAX_RETRIES = 10;


   protected static HashMap connectionPools = new HashMap();

   /**
    * connection information
    */
   protected ServerAddress address;
   
   /**
    * Pool for this invoker.  This is shared between all
    * instances of proxies attached to a specific invoker
    */
   protected LinkedList pool = null;

   protected static class ClientSocket
   {
      public ObjectOutputStream out;
      public ObjectInputStream in;
      public Socket socket;
      public boolean pooled = false;
      public int timeout;
      public ClientSocket(Socket socket, int timeout) throws Exception
      {
         this.socket = socket;
         socket.setSoTimeout(timeout);
         this.timeout = timeout;
         out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
         out.flush();
         in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
      }
   }

   /**
    * Exposed for externalization.
    */
   public PooledInvokerProxy()
   {
      super();
   }

   
   /**
    * Create a new Proxy.
    *
    */
   public PooledInvokerProxy(ServerAddress sa)
   {
      this.address = sa;
   }

   /**
    * Close all sockets in a specific pool.
    */
   public static void clearPool(ServerAddress sa)
   {
      try
      {
         LinkedList thepool = (LinkedList)connectionPools.get(sa);
         if (thepool == null) return;
         synchronized (thepool)
         {
            int size = thepool.size();
            for (int i = 0; i < size; i++)
            {
               ClientSocket socket = (ClientSocket)thepool.removeFirst();
               try
               {
                  socket.socket.close();
               }
               catch (Exception ignored)
               {
               }
            }
         }
      }
      catch (Exception ex)
      {
         // ignored
      }
   }
   /**
    * Close all sockets in all pools
    */
   public static void clearPools()
   {
      synchronized (connectionPools)
      {
         Iterator it = connectionPools.keySet().iterator();
         while (it.hasNext())
         {
            ServerAddress sa = (ServerAddress)it.next();
            clearPool(sa);
         }
      }
   }

   protected void initPool()
   {
      synchronized (connectionPools)
      {
         pool = (LinkedList)connectionPools.get(address);
         if (pool == null)
         {
            pool = new LinkedList();
            connectionPools.put(address, pool);
         }
      }
   }

   protected ClientSocket getConnection() throws Exception
   {
      ClientSocket socket = null;
      do
      {
         socket = getSocket();
         try
         {
            // Test to see if socket is alive by send ACK message
            final byte ACK = 1;
            socket.out.writeByte(ACK);
            socket.out.flush();
            socket.in.readByte();
            return socket;
         }
         catch (Exception ex)
         {
            if (socket.pooled)
            {
               // since the socket was pooled and the ACK failed
               // try again because the failed pooled socket may be stale
               continue;
            }
            else
            {
               // Ok, the socket is a new connection so there is something
               // probably wrong.
               throw ex;
            }
         }
      } while (true);

      // return blah; not reached
   }

   protected ClientSocket getSocket() throws Exception
   {
      ClientSocket pooled = null;
      Exception failed = null;
      Socket socket = null;


      //
      // Need to retry a few times
      // on socket connection because, at least on Windoze,
      // if too many concurrent threads try to connect
      // at same time, you get ConnectionRefused
      //
      // Retrying seems to be the most performant.
      //
      // This problem always happens with RMI and seems to
      // have nothing to do with backlog or number of threads
      // waiting in accept() on the server.
      // 
      for (int i = 0; i < MAX_RETRIES; i++)
      {
         synchronized(pool)
         {
            while (pool.size() > 0)
            {
               pooled = (ClientSocket)pool.removeFirst();
               usedPooled++;
               return pooled;
            }
         }
         
         try
         {
            socket = new Socket(address.address, address.port);
            break;
         }
         catch (Exception ex)
         {
            if (i + 1 < MAX_RETRIES)
            {
               Thread.sleep(1);
               continue;
            }
            throw ex;
         }
      }
      socket.setTcpNoDelay(address.enableTcpNoDelay);
      return new ClientSocket(socket, address.timeout); 
   }
 
   /**
    * The name of of the server.
    */
   public String getServerHostName() throws Exception
   {
      return address.address;
   }
   
   /**
    * ???
    *
    * @todo: MOVE TO TRANSACTION
    *  
    * @return the transaction propagation context of the transaction
    *         associated with the current thread.
    *         Returns <code>null</code> if the transaction manager was never
    *         set, or if no transaction is associated with the current thread.
    */
   public Object getTransactionPropagationContext()
      throws SystemException
   {
      return (tpcFactory == null) ? null : tpcFactory.getTransactionPropagationContext();
   }


   /**
    * The invocation on the delegate, calls the right invoker.  Remote if we are remote, 
    * local if we are local. 
    */
   public Object invoke(Invocation invocation)
      throws Exception
   {
      // We are going to go through a Remote invocation, switch to a Marshalled Invocation
      MarshalledInvocation mi = new MarshalledInvocation(invocation);
         
      // Set the transaction propagation context
      //  @todo: MOVE TO TRANSACTION
      mi.setTransactionPropagationContext(getTransactionPropagationContext());


      Object response = null;
      long start = System.currentTimeMillis();
      ClientSocket socket = getConnection();
      long end = System.currentTimeMillis() - start;
      getSocketTime += end;
      try
      {
         socket.out.writeObject(mi);
         socket.out.flush();
         end = System.currentTimeMillis() - start;
         writeTime += end;
         start = System.currentTimeMillis();
         response = socket.in.readObject();
         end = System.currentTimeMillis() - start;
         readTime += end;
      }
      catch (Exception ex)
      {
         try
         {
            socket.socket.close();
         }
         catch (Exception ignored) {}
         //System.out.println("got read exception, exiting");
         throw new ConnectException("Failed to communicate", ex);
      }
      
      //System.out.println("put back in pool");
      // Put socket back in pool for reuse
      synchronized (pool)
      {
         socket.pooled = true;
         pool.add(socket);
      }
      
      // Return response
      //System.out.println("return response");

      try
      { 
         if (response instanceof Exception)
         {
            throw ((Exception)response);
         }
         if (response instanceof MarshalledObject)
         {
            return ((MarshalledObject)response).get();
         }
         return response; 
      }
      catch (ServerException ex)
      {
         // Suns RMI implementation wraps NoSuchObjectException in
         // a ServerException. We cannot have that if we want
         // to comply with the spec, so we unwrap here.
         if (ex.detail instanceof NoSuchObjectException)
         {
            throw (NoSuchObjectException) ex.detail;
         }
         //likewise
         if (ex.detail instanceof TransactionRolledbackException)
         {
            throw (TransactionRolledbackException) ex.detail;
         }
         throw ex;
      }  
   }
   
   /**
    * Externalize this instance and handle obtaining the remoteInvoker stub
    */
   public void writeExternal(final ObjectOutput out)
      throws IOException
   { 
      out.writeObject(address);
   }
 
   /**
    * Un-externalize this instance.
    *
    */
   public void readExternal(final ObjectInput in)
      throws IOException, ClassNotFoundException
   {
      address = (ServerAddress)in.readObject();
      initPool();
   }
}

