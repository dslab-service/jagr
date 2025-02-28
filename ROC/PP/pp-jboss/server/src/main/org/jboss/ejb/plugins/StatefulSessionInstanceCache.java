/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.ejb.plugins;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import java.rmi.RemoteException;

import javax.transaction.Status;
import javax.transaction.SystemException;

import org.jboss.ejb.Container;
import org.jboss.ejb.StatefulSessionContainer;
import org.jboss.ejb.EnterpriseContext;
import org.jboss.ejb.StatefulSessionEnterpriseContext;
import org.jboss.ejb.StatefulSessionPersistenceManager;

/**
 * Cache for stateful session beans.
 *
 * @author <a href="mailto:simone.bordet@compaq.com">Simone Bordet</a>
 * @author <a href="mailto:sebastien.alborini@m4x.org">Sebastien Alborini</a>
 * @version $Revision: 1.1.1.1 $
 */
public class StatefulSessionInstanceCache
    extends AbstractInstanceCache
{
    // Constants -----------------------------------------------------

    // Attributes ----------------------------------------------------
    /* The container */
    private StatefulSessionContainer m_container;
    /* The map that holds passivated beans that will be removed */
    private HashMap m_passivated = new HashMap();
    /* Used for logging */
    private StringBuffer m_buffer = new StringBuffer();

    // Static --------------------------------------------------------

    // Constructors --------------------------------------------------

    // Public --------------------------------------------------------
    /* From ContainerPlugin interface */
    public void setContainer(Container c)
    {
        m_container = (StatefulSessionContainer)c;
    }

   public void destroy()
   {
      synchronized( this )
      {
         this.m_container = null;
      }
      m_passivated.clear();
      super.destroy();
   }

    // Z implementation ----------------------------------------------

    // Y overrides ---------------------------------------------------
    protected synchronized Container getContainer()
    {
       return m_container;
    }
    protected synchronized void passivate(EnterpriseContext ctx) throws RemoteException
    {
        if( m_container == null )
           return;

        m_container.getPersistenceManager().passivateSession((StatefulSessionEnterpriseContext)ctx);
        m_passivated.put(ctx.getId(), new Long(System.currentTimeMillis()));
    }
    protected void activate(EnterpriseContext ctx) throws RemoteException
    {
        m_container.getPersistenceManager().activateSession((StatefulSessionEnterpriseContext)ctx);
        m_passivated.remove(ctx.getId());
    }
    protected EnterpriseContext acquireContext() throws Exception
    {
        return m_container.getInstancePool().get();
    }
    protected synchronized void freeContext(EnterpriseContext ctx)
    {
        if( m_container == null )
           return;
        m_container.getInstancePool().free(ctx);
    }
    protected Object getKey(EnterpriseContext ctx)
    {
        return ctx.getId();
    }
    protected void setKey(Object id, EnterpriseContext ctx)
    {
        ctx.setId(id);
    }
    protected boolean canPassivate(EnterpriseContext ctx)
    {
        if (ctx.isLocked())
        {
            // The context is in the interceptor chain
            return false;
        }
        else if (m_container.getLockManager().canPassivate(ctx.getId())) 
        {
           return false;
        }
        else
        {
            if (ctx.getTransaction() != null)
            {
                try
                {
                   return (ctx.getTransaction().getStatus() == Status.STATUS_NO_TRANSACTION);
                }
                catch (SystemException e)
                {
                   // SA FIXME: not sure what to do here
                   return false;
                }
            }
        }
        return true;
    }

    // Package protected ---------------------------------------------
    void removePassivated(long maxLifeAfterPassivation)
    {
        StatefulSessionContainer con = (StatefulSessionContainer) getContainer();
        if( con == null )
           return;

        StatefulSessionPersistenceManager store = (StatefulSessionPersistenceManager)con.getPersistenceManager();
        String ejbName = con.getBeanMetaData().getEjbName();
        con = null;

        long now = System.currentTimeMillis();
        Iterator entries = m_passivated.entrySet().iterator();
        while (entries.hasNext())
        {
            Map.Entry entry = (Map.Entry)entries.next();
            Object key = entry.getKey();
            long passivationTime = ((Long)entry.getValue()).longValue();
            if (now - passivationTime > maxLifeAfterPassivation)
            {
                store.removePassivated(key);
                log(key, ejbName);
                // Must use the iterator to remove, otherwise
                // ConcurrentModificationException is thrown
                entries.remove();
//              removeLock(key);
            }
        }
    }

    // Protected -----------------------------------------------------

    // Private -------------------------------------------------------
    private void log(Object key, String ejbName)
    {
        if( log.isTraceEnabled() )
        {
           m_buffer.setLength(0);
           m_buffer.append("Removing from storage bean '");
           m_buffer.append(ejbName);
           m_buffer.append("' with id = ");
           m_buffer.append(key);
           log.trace(m_buffer.toString());
        }
    }

    // Inner classes -------------------------------------------------

}
