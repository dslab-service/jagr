/*
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * Copyright 1999 by dreamBean Software,
 * All rights reserved.
 */
package org.jnp.server;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.MarshalledObject;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import javax.naming.*;
import javax.net.ServerSocketFactory;

import org.apache.log4j.Category;
import org.apache.log4j.PropertyConfigurator;

import org.jnp.interfaces.Naming;
import org.jnp.interfaces.NamingContext;
import org.jnp.interfaces.java.javaURLContextFactory;
import org.jboss.net.sockets.QueuedClientSocketFactory;
import org.jboss.net.sockets.DefaultSocketFactory;

/** A main() entry point for running the jnp naming service implementation as
 a standalone process.
 
 @author oberg
 @author Scott.Stark@jboss.org
 @version $Revision: 1.1.1.1 $
 */
public class Main implements Runnable, MainMBean
{
   // Constants -----------------------------------------------------
   
   // Attributes ----------------------------------------------------
   /** The Naming interface server implementation */
   protected NamingServer theServer;
   protected MarshalledObject serverStub;
   /** The jnp server socket through which the NamingServer stub is vended */
   protected ServerSocket serverSocket;
   /** An optional custom client socket factory */
   protected RMIClientSocketFactory clientSocketFactory;
   /** An optional custom server socket factory */
   protected RMIServerSocketFactory serverSocketFactory;
   /** An optional custom server socket factory */
   protected ServerSocketFactory jnpServerSocketFactory;
   /** The class name of the optional custom client socket factory */
   protected String clientSocketFactoryName = null;
   /** The class name of the optional custom server socket factory */
   protected String serverSocketFactoryName = null;
   /** The class name of the optional custom JNP server socket factory */
   protected String jnpServerSocketFactoryName = null;
   /** The interface to bind to. This is useful for multi-homed hosts
    that want control over which interfaces accept connections.
    */
   protected InetAddress bindAddress;
   /** The serverSocket listen queue depth */
   protected int backlog = 50;
   /** max amount of concurrent client socket creates that can happen */
   protected long maxConcurrentClientConnections = 0;
   /** The jnp protocol listening port. The default is 1099, the same as
    the RMI registry default port. */
   protected int port = 1099;
   /** The RMI port on which the Naming implementation will be exported. The
    default is 0 which means use any available port. */
   protected int rmiPort = 0;
   protected Category log;

   // Static --------------------------------------------------------
   public static void main(String[] args)
      throws Exception
   {
      // Make sure the config file can be found
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      URL url = loader.getResource("log4j.properties");
      if( url == null )
         System.err.println("Failed to find log4j.properties");
      else
         PropertyConfigurator.configure(url);
      new Main().start();
   }

   // Constructors --------------------------------------------------
   public Main()
   {
      this("org.jboss.naming.Naming");
   }
   public Main(String categoryName)
   {
      // Load properties from properties file
      try
      {
         ClassLoader loader = getClass().getClassLoader();
         InputStream is = loader.getResourceAsStream("jnp.properties");
         System.getProperties().load(is);
      }
      catch (Exception e)
      {
         // Ignore
      }

      // Set configuration from the system properties
      setPort(Integer.getInteger("jnp.port",getPort()).intValue());
      setRmiPort(Integer.getInteger("jnp.rmiPort",getRmiPort()).intValue());
      log = Category.getInstance(categoryName);
   }

   // Public --------------------------------------------------------
   public Naming getServer()
   {
      return theServer;
   }

   public void setRmiPort(int p)
   {
      rmiPort = p;
   }
   public int getRmiPort()
   {
      return rmiPort;
   }

   public void setPort(int p)
   {
      port = p;
   }
   public int getPort()
   {
      return port;
   }

   public String getBindAddress()
   {
      String address = null;
      if( bindAddress != null )
         address = bindAddress.getHostAddress();
      return address;
   }
   public void setBindAddress(String host) throws UnknownHostException
   {
      bindAddress = InetAddress.getByName(host);
   }

   public int getBacklog()
   {
      return backlog;
   }
   public void setBacklog(int backlog)
   {
      if( backlog <= 0 )
         backlog = 50;
      this.backlog = backlog;
   }

   public long getMaxConcurrentClientConnections()
   {
      return maxConcurrentClientConnections;
   }
   public void setMaxConcurrentClientConnections(long max)
   {
      maxConcurrentClientConnections = max;
   }
   public String getClientSocketFactory()
   {
      return clientSocketFactoryName;
   }
   public void setClientSocketFactory(String factoryClassName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException
   {
      this.clientSocketFactoryName = factoryClassName;
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class clazz = loader.loadClass(clientSocketFactoryName);
      clientSocketFactory = (RMIClientSocketFactory) clazz.newInstance();
   }
   
   public String getServerSocketFactory()
   {
      return serverSocketFactoryName;
   }
   public void setServerSocketFactory(String factoryClassName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException
   {
      this.serverSocketFactoryName = factoryClassName;
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class clazz = loader.loadClass(serverSocketFactoryName);
      serverSocketFactory = (RMIServerSocketFactory) clazz.newInstance();
   }

   public void setJNPServerSocketFactory(String factoryClassName)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException
   {
      this.jnpServerSocketFactoryName = factoryClassName;
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class clazz = loader.loadClass(jnpServerSocketFactoryName);
      jnpServerSocketFactory = (ServerSocketFactory) clazz.newInstance();
   }

   public void start()
      throws Exception
   {
      log.info("Starting jnp server");
      // Create the local naming service instance if it does not exist
      if( theServer == null )
      {
         theServer = new NamingServer();
         // Set local server reference
         NamingContext.setLocal(theServer);
      }

      /* Only export server RMI interface and setup the listening socket if
        the port is >= 0. A value < 0 indicates the socket based access
      */
      if( this.port >= 0 )
         initJnpInvoker();
   }

   public void stop()
   {
      try
      {
         log.info("Stopping");        
         // Stop listener and unexport the RMI object
         if( serverSocket != null )
         {
            UnicastRemoteObject.unexportObject(theServer, false);
            ServerSocket s = serverSocket;
            serverSocket = null;
            s.close();
         }
         log.info("Stopped");
      }
      catch (Exception e)
      {
         log.error("Exception during shutdown", e);
      }
   }

   // Runnable implementation ---------------------------------------
   public void run()
   {
      Socket socket = null;
      
      // Accept a connection
      try
      {
         socket = serverSocket.accept();
      } catch (IOException e)
      {
         if (serverSocket == null)
            return; // Stopped by normal means

         log.error("Naming stopped", e);
         log.info("Restarting naming");
         try
         {
            start();
         } catch (Exception ex)
         {
            log.error("Restart failed", ex);
            return;
         }
      }

      // Create a new thread to accept the next connection
      listen();
      
      // Return the naming server stub
      try
      {
         ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
         out.writeObject(serverStub);
      }
      catch (IOException ex)
      {
         log.error("Error writing response", ex);
      }
      finally
      {
         try
         {
            socket.close();
         } catch (IOException e)
         {
         }
      }
   }
   
   // Y overrides ---------------------------------------------------
   
   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   protected void listen()
   {
      Thread t = new Thread(this, "JNP Server");
      t.start();
   }

   /** This code should be moved to a seperate invoker in the org.jboss.naming
    *package.
    */
   protected void initJnpInvoker() throws IOException
   {
      if (serverSocketFactory == null)
      {
         serverSocketFactory = new DefaultSocketFactory(backlog);
      }
      if (clientSocketFactory == null && maxConcurrentClientConnections > 0)
      {
         clientSocketFactory = new QueuedClientSocketFactory(maxConcurrentClientConnections);
      }
      Remote stub = UnicastRemoteObject.exportObject(theServer, rmiPort,
         clientSocketFactory, serverSocketFactory);
      log.debug("NamingServer stub: "+stub);
      serverStub = new MarshalledObject(stub);

      // Start listener
      try
      {
         // Get the default ServerSocketFactory is one was not specified
         if( jnpServerSocketFactory == null )
            jnpServerSocketFactory = ServerSocketFactory.getDefault();
         serverSocket = jnpServerSocketFactory.createServerSocket(port, backlog, bindAddress);
         // If an anonymous port was specified get the actual port used
         if( port == 0 )
            port = serverSocket.getLocalPort();
         String msg = "Started jnpPort=" + port +", rmiPort=" + rmiPort
            + ", backlog="+backlog+", bindAddress="+bindAddress
            + ", Client SocketFactory="+clientSocketFactory
            + ", Server SocketFactory="+serverSocketFactory;
         log.info(msg);
         listen();
      }
      catch (IOException e)
      {
         log.error("Could not start on port " + port, e);
      }
   }
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------
}
