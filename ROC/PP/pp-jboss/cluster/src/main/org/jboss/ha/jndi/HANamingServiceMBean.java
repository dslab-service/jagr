/*
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * Copyright 1999 by dreamBean Software,
 * All rights reserved.
 */

package org.jboss.ha.jndi;

import java.net.UnknownHostException;
import java.util.Map;

import javax.management.ObjectName;

import org.jboss.invocation.Invocation;
import org.jboss.util.jmx.ObjectNameFactory;

/**
 * HA-JNDI service that provides JNDI services in a clustered way.
 * Bindings are replicated cluster-wide.
 * Lookups are:
 *    - first resolved locally in the cluster-wide tree
 *    - if not available, resolved in the local underlying JNDI tree
 *    - if not available, the query is broadcast on the cluster and each node determines
 *      if it has one in its local JNDI tree
 *
 * The HA-JNDI service also provides an automatic-discovery feature that allow clients
 * to resolve the service through multicast.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sacha.labourey@cogito-info.ch">Sacha Labourey</a>
 * @version $Revision: 1.1.1.1 $
 *
 * <p><b>Revisions:</b><br>
 */

public interface HANamingServiceMBean
   extends org.jboss.system.ServiceMBean
{
   ObjectName OBJECT_NAME = ObjectNameFactory.create(":service=HANaming");
    
   /**
    * Name of the underlying partition that determine the cluster to use.
    */   
   String getPartitionName();
   void setPartitionName(String partitionName);

   /**
    * RmiPort to be used by the HA-JNDI service once bound. 0 => auto.
    */   
   void setRmiPort(int p);
   int getRmiPort();
   
   void setPort(int p);
   /**
    * Port on which the HA-JNDI stub is made available
    */   
   int getPort();

   /**
    * IP address on which the HA-JNDI stub is made available
    */   
   String getBindAddress();
   void setBindAddress(String host) throws UnknownHostException;

   /**
    * Backlog to be used for client-server RMI invocations during JNDI queries
    */   
   int getBacklog();
   void setBacklog(int backlog);

   /**
    * Client socket factory to be used for client-server RMI invocations during JNDI queries
    */   
   String getClientSocketFactory();
   void setClientSocketFactory(String factoryClassName)
           throws ClassNotFoundException, InstantiationException, IllegalAccessException;
   /**
    * Server socket factory to be used for client-server RMI invocations during JNDI queries
    */   
   String getServerSocketFactory();
   void setServerSocketFactory(String factoryClassName)
           throws ClassNotFoundException, InstantiationException, IllegalAccessException;

   /** Expose the Naming service interface mapping as a read-only attribute
    *
    * @jmx:managed-attribute
    *
    * @return A Map<Long hash, Method> of the Naming interface 
    */
   public Map getMethodMap();

   /** Expose the Naming service via JMX to invokers.
    *
    * @param invocation    A pointer to the invocation object
    * @return              Return value of method invocation.
    * 
    * @throws Exception    Failed to invoke method.
    */
   public Object invoke(Invocation invocation) throws Exception;
}
