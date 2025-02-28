
/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 */

package org.jboss.resource.connectionmanager; 

import EDU.oswego.cs.dl.util.concurrent.FIFOSemaphore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import org.jboss.logging.Logger;
import org.jboss.system.ServiceMBeanSupport;
import org.jboss.deployment.DeploymentException;


/**
 * The JBossManagedConnectionPool mbean configures and supplies pooling of 
 * ManagedConnections to the BaseConnectionManager2 mbean.  It may be 
 * replaced by any mbean with a readable ManagedConnectionPool attribute 
 * of type ManagedConnectionPool.  Normal pooling parameters are supplied, 
 * and the criteria to distinguish ManagedConnections is set in the Criteria attribute.
 *
 *
 * Created: Sun Dec 30 21:17:36 2001
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @version
 * @jmx:mbean name="jboss.jca:service=JBossManagedConnectionPool"
 *            extends="org.jboss.system.ServiceMBean"
 */

public class JBossManagedConnectionPool 
   extends ServiceMBeanSupport
   implements JBossManagedConnectionPoolMBean 
{

   private String criteria;

   private ManagedConnectionPool poolingStrategy;

   private final InternalManagedConnectionPool.PoolParams poolParams = new InternalManagedConnectionPool.PoolParams();

   /**
    * Default managed JBossManagedConnectionPool constructor for mbeans.
    *
    * @jmx.managed-constructor
    */
   public JBossManagedConnectionPool()
   {
   }

   /**
    * ManagedConnectionPool is a read only attribute returning the pool
    * set up by this mbean.
    * 
    * @return the ManagedConnectionPool implementing the pool configured by this mbean.
    * @jmx.managed-attribute access="READ"
    * @todo decide if this should be an attribute or an operation.
    */
   public ManagedConnectionPool getManagedConnectionPool()
   {
      return poolingStrategy;
   }


   /**
    * Get number of available free connections
    *
    * @return number of available connections
    * @jmx:managed-attribute
    */
   public long getAvailableConnectionCount()
   {
      return poolingStrategy.getAvailableConnectionCount();
   }

   /**
    * The MinSize attribute indicates the minimum number of connections this
    * pool should hold.  These are not created until a Subject is known from a
    * request for a connection.  MinSize connections will be created for 
    * each sub-pool.
    *
    * @return the MinSize value.
    * @jmx:managed-attribute
    */
   public int getMinSize()
   {
      return poolParams.minSize;
   }

   /**
    * Set the MinSize value.
    * @param newMinSize The new MinSize value.
    * @jmx:managed-attribute
    */
   public void setMinSize(int newMinSize)
   {
      poolParams.minSize = newMinSize;
   }


   /**
    * The MaxSize attribute indicates the maximum number of connections for a 
    * pool. No more than MaxSize connections will be created in each 
    * sub-pool.
    *
    * @return the MaxSize value.
    * @jmx:managed-attribute
    */
   public int getMaxSize()
   {
      return poolParams.maxSize;
   }

   /**
    * Set the MaxSize value.
    * @param newMaxSize The new MaxSize value.
    * @jmx:managed-attribute
    */
   public void setMaxSize(int newMaxSize)
   {
      poolParams.maxSize = newMaxSize;
   }


   /**
    * The BlockingTimeoutMillis attribute indicates the maximum time to block
    * while waiting for a connection before throwing an exception.  Note that 
    * this blocks only while waiting for a permit for a connection, and will
    * never throw an exception if creating a new connection takes an 
    * inordinately long time.
    *
    * @return the BlockingTimeout value.
    * @jmx:managed-attribute
    */
   public int getBlockingTimeoutMillis()
   {
      return poolParams.blockingTimeout;
   }

   /**
    * Set the BlockingTimeout value.
    * @param newBlockingTimeout The new BlockingTimeout value.
    * @jmx:managed-attribute
    */
   public void setBlockingTimeoutMillis(int newBlockingTimeout)
   {
      poolParams.blockingTimeout = newBlockingTimeout;
   }


   /**
    * The IdleTimeoutMinutes attribute indicates the maximum time a connection
    * may be idle before being closed.  The actual maximum time depends also 
    * on the IdleRemover scan time, which is 1/2 the smallest IdleTimeout of 
    * any pool.
    *
    * @return the IdleTimeoutMinutes value.
    * @jmx:managed-attribute
    */
   public long getIdleTimeoutMinutes()
   {
      return poolParams.idleTimeout / (1000 * 60);
   }

   /**
    * Set the IdleTimeoutMinutes value.
    * @param newIdleTimeoutMinutes The new IdleTimeoutMinutes value.
    * @jmx:managed-attribute
    */
   public void setIdleTimeoutMinutes(long newIdleTimeoutMinutes)
   {
      poolParams.idleTimeout = newIdleTimeoutMinutes * 1000 * 60;
   }

   long idleTimeout;

   /**
    * Get the IdleTimeout value.
    *TESTING ONLY
    * @return the IdleTimeout value.
    */
   public long getIdleTimeout()
   {
      return poolParams.idleTimeout;
   }

   /**
    * Set the IdleTimeout value.
    *TESTING ONLY
    * @param newIdleTimeout The new IdleTimeout value.
    */
   public void setIdleTimeout(long newIdleTimeout)
   {
      poolParams.idleTimeout = newIdleTimeout;
   }

   
   /**
    * The Criteria attribute indicates if Subject (from security domain) or app supplied
    * parameters (such as from getConnection(user, pw)) are used to distinguish
    * connections in the pool. Choices are 
    *   ByContainerAndApplication (use both), 
    *   ByContainer (use Subject),
    *   ByApplication (use app supplied params only),
    *   ByNothing (all connections are equivalent, usually if adapter supports
    *     reauthentication)
    * @return the Criteria value.
    * @jmx:managed-attribute
    */
   public String getCriteria()
   {
      return criteria;
   }

   /**
    * Set the Criteria value.
    * @param newCriteria The new Criteria value.
    * @jmx:managed-attribute
    */
   public void setCriteria(String newCriteria)
   {
      this.criteria = newCriteria;
   }

   
   //serviceMBeanSupport

   public String getName()
   {
      return "JBossManagedConnectionPool";
   }


   protected void startService() throws Exception
   {
      if ("ByContainerAndApplication".equals(criteria))
      {
         poolingStrategy = new PoolBySubjectAndCri(poolParams, log);
      } // end of if ()
      else if ("ByContainer".equals(criteria))
      {
         poolingStrategy = new PoolBySubject(poolParams, log);
      } // end of if () else
      else if ("ByApplication".equals(criteria))
      {
         poolingStrategy = new PoolByCri(poolParams, log);
      } // end of if ()
      else if ("ByNothing".equals(criteria))
      {
         poolingStrategy = new OnePool(poolParams, log);
      } // end of if () else
      else
      {
         throw new DeploymentException("Unknown pooling criteria: " + criteria);         
      } // end of else
   }

   protected void stopService()
   {
   }


   //pooling strategies   
   //ManagedConnectionPool implementations

   //base class


   private abstract static class BasePool implements ManagedConnectionPool
   {
      private final Map pools = new HashMap();
      private final Map mcToPoolMap = new HashMap();

      private ManagedConnectionFactory mcf;

      private final InternalManagedConnectionPool.PoolParams poolParams;
      private final Logger log;

      public BasePool(final InternalManagedConnectionPool.PoolParams poolParams, 
                                 final Logger log)
      {
         this.poolParams = poolParams;
         this.log = log;
      }

      protected abstract Object getKey(Subject subject, ConnectionRequestInfo cri) throws ResourceException;

      public void setManagedConnectionFactory(ManagedConnectionFactory mcf)
      {
         this.mcf = mcf;
      }

      public ManagedConnection getConnection(Subject subject, ConnectionRequestInfo cri) 
         throws ResourceException
      {
         InternalManagedConnectionPool mcp = null;
         Object key = getKey(subject, cri);
         synchronized (pools)
         {
            mcp = (InternalManagedConnectionPool)pools.get(key);
            if (mcp == null) 
            {
               mcp = new InternalManagedConnectionPool(mcf, subject, cri, poolParams, log);
               pools.put(key, mcp);
            }   
         } 
         ManagedConnection mc = mcp.getConnection(subject, cri);
         mcToPoolMap.put(mc, mcp);
         return mc;
      }

      public void returnConnection(ManagedConnection mc, boolean kill) throws ResourceException
      {
         InternalManagedConnectionPool mcp = (InternalManagedConnectionPool)mcToPoolMap.get(mc);
         if (mcp == null) 
         {
            throw new ResourceException("Returned to wrong Pool!!");
         } // end of if ()
         mcp.returnConnection(mc, kill);
      }
      public int getConnectionCount()
      {
         int count = 0;
	 synchronized (pools)
	 {
	    for (Iterator i = pools.values().iterator(); i.hasNext(); )
	    {
	       count += ((InternalManagedConnectionPool)i.next()).getConnectionCount();
	    } // end of for ()
	 }
         return count;
      }

      public int getConnectionCreatedCount()
      {
         int count = 0;
	 synchronized (pools)
	 {
	    for (Iterator i = pools.values().iterator(); i.hasNext(); )
	    {
	       count += ((InternalManagedConnectionPool)i.next()).getConnectionCreatedCount();
	    } // end of for ()
	 }
         return count;
      }

      public int getConnectionDestroyedCount()
      {
         int count = 0;
	 synchronized (pools)
	 {
	    for (Iterator i = pools.values().iterator(); i.hasNext(); )
	    {
	       count += ((InternalManagedConnectionPool)i.next()).getConnectionDestroyedCount();
	    } // end of for ()
	 }
         return count;
      }

      public long getAvailableConnectionCount()
      {
         long count = 0;
         int idx = 0;
	 synchronized (pools)
	 {
	    for (Iterator i = pools.values().iterator(); i.hasNext(); idx++)
	    {
	       count += ((InternalManagedConnectionPool)i.next()).getAvailableConnections();
	    } // end of for ()
	 }
         return count;
      }

      public void shutdown()
      {
	 synchronized (pools)
	 {
	    for (Iterator i = pools.values().iterator(); i.hasNext(); )
	    {
	       ((InternalManagedConnectionPool)i.next()).shutdown();
	    } // end of for ()
	    pools.clear();
	 }
         mcf = null;
      }
   }


   private static class PoolBySubjectAndCri 
      extends BasePool
   {

      public PoolBySubjectAndCri(final InternalManagedConnectionPool.PoolParams poolParams, 
                                 final Logger log)
      {
         super(poolParams, log);
      }

      protected Object getKey(final Subject subject, final ConnectionRequestInfo cri) throws ResourceException
      {
         return new SubjectCriKey(subject, cri);
      }
   }

   private static class SubjectCriKey
   {
      private Subject subject;
      private ConnectionRequestInfo cri;

      SubjectCriKey(Subject subject, ConnectionRequestInfo cri) throws ResourceException
      {
         if (subject == null) 
         {
            throw new ResourceException("Need non-null subject for subject/cri based pooling");
         } // end of if ()
            
         if (cri == null) 
         {
            throw new ResourceException("Need non-null cri for subject/cri based pooling");
         } // end of if ()
         this.subject = subject;
         this.cri = cri;
      }

      public int hashCode()
      {
         return subject.hashCode() ^ cri.hashCode();
      }

      public boolean equals(Object other)
      {
         if (!(other instanceof SubjectCriKey)) 
         {
            return false;
         } // end of if ()
         return subject.equals(((SubjectCriKey)other).subject) 
            && cri.equals(((SubjectCriKey)other).cri);
      }
   }

   private static class PoolBySubject
      extends BasePool
   {

      public PoolBySubject(final InternalManagedConnectionPool.PoolParams poolParams, 
                                 final Logger log)
      {
         super(poolParams, log);
      }

      protected Object getKey(final Subject subject, final ConnectionRequestInfo cri)
      {
         return subject;
      }
   }

   private static class PoolByCri
      extends BasePool
   {

      public PoolByCri(final InternalManagedConnectionPool.PoolParams poolParams, 
                                 final Logger log)
      {
         super(poolParams, log);
      }

      protected Object getKey(final Subject subject, final ConnectionRequestInfo cri)
      {
         return cri;
      }
   }

   private static class OnePool implements ManagedConnectionPool
   {
      private InternalManagedConnectionPool mcp;

      private ManagedConnectionFactory mcf;

      private final InternalManagedConnectionPool.PoolParams poolParams;
      private final Logger log;

      public OnePool(final InternalManagedConnectionPool.PoolParams poolParams, 
                                 final Logger log)
      {
         this.poolParams = poolParams;
         this.log = log;
      }

      public long getAvailableConnectionCount()
      {
         return mcp.getAvailableConnections();
      }
      public void setManagedConnectionFactory(ManagedConnectionFactory mcf)
      {
         this.mcf = mcf;
      }

      public  ManagedConnection getConnection(Subject subject, ConnectionRequestInfo cri) 
         throws ResourceException
      {
         synchronized (this)
         {
            if (mcp == null) 
            {
               mcp = new InternalManagedConnectionPool(mcf, subject, cri, poolParams, log);
            } // end of if ()
         }
         return mcp.getConnection(subject, cri);
      }

      public void returnConnection(ManagedConnection mc, boolean kill)
      {
         mcp.returnConnection(mc, kill);
      }

      public int getConnectionCount()
      {
         return (mcp == null)? 0: mcp.getConnectionCount();
      }

      public int getConnectionCreatedCount()
      {
         return (mcp == null)? 0: mcp.getConnectionCreatedCount();
      }

      public int getConnectionDestroyedCount()
      {
         return (mcp == null)? 0: mcp.getConnectionDestroyedCount();
      }

      public void shutdown()
      {
         if (mcp != null ) 
            mcp.shutdown();
         mcp = null;
         mcf = null;
      }
   }

}// JBossManagedConnectionPool
