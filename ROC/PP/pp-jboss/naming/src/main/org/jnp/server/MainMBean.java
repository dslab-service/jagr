/*
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jnp.server;

import java.net.UnknownHostException;

/** The Mbean interface for the jnp provider server.
 *      
 *   @author oberg
 *   @author Scott.Stark@jboss.org
 *   @version $Revision: 1.1.1.1 $
 */
public interface MainMBean
{
   // Constants -----------------------------------------------------
    
   // Public --------------------------------------------------------
   public void setRmiPort(int p);
   public int getRmiPort();
   
   public void setPort(int p);
   public int getPort();

   public String getBindAddress();
   public void setBindAddress(String host) throws UnknownHostException;

   public int getBacklog();
   public void setBacklog(int backlog);

   public long getMaxConcurrentClientConnections();
   public void setMaxConcurrentClientConnections(long max);

   /** Get the RMIClientSocketFactory implementation class */
   public String getClientSocketFactory();
   /** Set the RMIClientSocketFactory implementation class */
   public void setClientSocketFactory(String factoryClassName)
           throws ClassNotFoundException, InstantiationException, IllegalAccessException;
   /** Get the RMIServerSocketFactory implementation class */
   public String getServerSocketFactory();
   /** Set the RMIServerSocketFactory implementation class */
   public void setServerSocketFactory(String factoryClassName)
           throws ClassNotFoundException, InstantiationException, IllegalAccessException;
   
   /** Set the ServerSocketFactory implementation class */
   public void setJNPServerSocketFactory(String factoryClassName) 
           throws ClassNotFoundException, InstantiationException, IllegalAccessException;

   public void start()
      throws Exception;
   
   public void stop();

}

