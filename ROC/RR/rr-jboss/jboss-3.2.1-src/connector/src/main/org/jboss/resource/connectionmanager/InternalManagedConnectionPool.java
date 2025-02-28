
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import org.jboss.logging.Logger;

/**
 * JBossManagedConnectionPool.java
 *
 *
 * Created: Sun Dec 30 21:17:36 2001
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @version
 */

public class InternalManagedConnectionPool
{

   private final ManagedConnectionFactory mcf;
   private final Subject defaultSubject;
   private final ConnectionRequestInfo defaultCri;
   private final PoolParams poolParams;

   private final LinkedList mcs = new LinkedList();

   private final FIFOSemaphore permits;

   private final Logger log;

   private final Counter connectionCounter = new Counter();

   private final Map destroyed = Collections.synchronizedMap(new WeakHashMap());

   private final Set checkedOut = new HashSet();
   private final Set dispose = new HashSet();


   //used to fill pool after first connection returned.
   private boolean started = false;

   public InternalManagedConnectionPool (ManagedConnectionFactory mcf, Subject subject, ConnectionRequestInfo cri, PoolParams poolParams, Logger log)
   {
      this.mcf = mcf;
      defaultSubject = subject;
      defaultCri = cri;
      this.poolParams = poolParams;
      this.log = log;
      permits = new FIFOSemaphore(this.poolParams.maxSize);
      IdleRemover.registerPool(this, poolParams.idleTimeout);
   }

   public long getAvailableConnections()
   {
      return permits.permits();
   }

   /**
    * Describe <code>getConnection</code> method here.
    *
    * @param subject a <code>Subject</code> value
    * @param cri a <code>ConnectionRequestInfo</code> value
    * @return a <code>ManagedConnection</code> value
    * @exception ResourceException if an error occurs
    *
    * @todo distinguish between connection dying while match called
    * and bad match strategy.  In latter case we should put it back in
    * the pool.
    */
   public ManagedConnection getConnection(Subject subject, ConnectionRequestInfo cri)
      throws ResourceException
   {
      subject = (subject == null)? defaultSubject: subject;
      cri = (cri == null)? defaultCri: cri;
      try
      {
         if (permits.attempt(poolParams.blockingTimeout))
         {
            //We have a permit to get a connection. Is there one in the pool already?
            ManagedConnection mc = null;
            ManagedConnection matchedMc = null;
            int mcSize = 0;
            do
            {
               synchronized (mcs)
               {
                  mcSize = mcs.size();
                  if (mcSize > 0)
                  {
                     mc = ((MCHolder)mcs.removeFirst()).getMC();
                     checkedOut.add(mc);
                  } // end of if ()
               }
               if (mc != null)
               {
                  //Yes, we retrieved a ManagedConnection from the pool. Does it match?
                  try
                  {
                     matchedMc = mcf.matchManagedConnections(new SetOfOne(mc), subject, cri);
                  } catch (ResourceException re)
                  {
                     doDestroy(mc);
                     mc = null;
                  }
                  if (matchedMc != null)
                  {
                     if (log.isTraceEnabled())
                     {
                        log.trace("supplying ManagedConnection from pool: " + matchedMc);
                     } // end of if ()
                     return matchedMc;
                  } // end of if ()
                  //Match did not succeed but no exception was thrown.
                  //Either we have the matching strategy wrong or the
                  //connection died while being checked.  We need to
                  //distinguish these cases, but for now we always
                  //destroy the connection.
                  log.warn("Destroying connection that could not be successfully matched: " + mc);
                  doDestroy(mc);
               } // end of if ()
            }
            while (mcSize >0);
            //OK, we couldnt find a working connection from the pool.  Make a new one.
            try
            {
               //No, the pool was empty, so we have to make a new one.
               mc = createConnection(subject, cri);
               //lack of synch on "started" probably ok, if 2 reads occur we will just
               //run fillPool twice, no harm done.
               if (!started)
               {
                  started = true;
                  PoolFiller.fillPool(this);
               } // end of if ()
               if (log.isTraceEnabled())
               {
                  log.trace("supplying new ManagedConnection: " + mc);
               } // end of if ()
               synchronized (mcs)
               {
                  checkedOut.add(mc);
               }
               return mc;

            } catch (ResourceException re)
            {
               //return permit and rethrow
               synchronized (mcs)
               {
                  checkedOut.remove(mc);
               }
               permits.release();
               throw re;
            } // end of try-catch
         } // end of if ()
         else
         {
            //we timed out
            throw new ResourceException("No ManagedConnections Available!");
         } // end of else

      } catch (InterruptedException ie)
      {
         throw new ResourceException("Interrupted while requesting permit!");
      } // end of try-catch

   }

   /**
    * Describe <code>returnConnection</code> method here.
    *
    * @param mc a <code>ManagedConnection</code> value
    * @param kill a <code>boolean</code> value
    *
    * @todo wrap mc in IdentityWrapper for destroyed check.
    */
   public void returnConnection(ManagedConnection mc, boolean kill)
   {
      if (destroyed.containsKey(mc))
      {
         log.trace("ManagedConnection is being returned after it was destroyed" + mc);
         return;
      } // end of if ()

      log.trace("putting ManagedConnection back into pool");
      boolean wasInPool = false;
      try
      {
         try
         {
            mc.cleanup();
         }
         catch (ResourceException re)
         {
            log.warn("ResourceException cleaning up ManagedConnection:" + re);
            kill = true;
         }

         synchronized (mcs)
         {
            if (dispose.remove(mc))
            {
               kill = true;
            } // end of if ()
            checkedOut.remove(mc);
            if (kill)
            {
               for (Iterator i = mcs.iterator(); i.hasNext(); )
               {
                  MCHolder mch = (MCHolder)i.next();
                  if (mch.getMC() == mc)
                  {
                     i.remove();
                     wasInPool = true;
                     break;
                  } // end of if ()
               } // end of for ()
            } // end of if ()
            else
            {
               mcs.addFirst(new MCHolder(mc));
            } // end of else
         }
         if (kill)
         {
            doDestroy(mc);
         } // end of if ()

      }
      finally
      {
         if (!wasInPool)
         {
            permits.release();
         } // end of if ()
      } // end of try-catch
   }

   public void flush()
   {
      synchronized (mcs)
      {
         dispose.addAll(checkedOut);
         for (Iterator i = mcs.iterator(); i.hasNext(); )
         {
            ManagedConnection mc = ((MCHolder)i.next()).getMC();
            i.remove();
            doDestroy(mc);
         } // end of for ()
      }
   }


   public void removeTimedOut()
   {
      synchronized (mcs)
      {
         for (ListIterator i = mcs.listIterator(mcs.size()); i.hasPrevious(); )
         {
            MCHolder mch = (MCHolder)i.previous();
            if (mch.isTimedOut())
            {
               i.remove();
               doDestroy(mch.getMC());
            } // end of if ()
            else
            {
               //They were put in reverse chronologically, so if one isn't timed out, preceding ones won't be either.
               break;
            } // end of else
         } // end of for ()
      }
      //refill if necessary, asynchronously.
      PoolFiller.fillPool(this);
   }

   /**
    * The <code>shutdown</code> method
    * @todo deadlock possible on shutdown on this synchronized!
    */
   public void shutdown()
   {
      flush();
      IdleRemover.unregisterPool(this);
   }

   public void fillToMin()
   {
      ArrayList newMCs = new ArrayList();
      try
      {
         while (connectionCounter.getCount() < poolParams.minSize)
         {
            newMCs.add(getConnection(defaultSubject, defaultCri));
         } // end of while ()
      }
      catch (ResourceException re)
      {
         //Whatever the reason, stop trying to add more!
      } // end of try-catch
      for (Iterator i = newMCs.iterator(); i.hasNext(); )
      {
         returnConnection((ManagedConnection)i.next(), false);
      } // end of for ()

   }

   public int getConnectionCount()
   {
      return connectionCounter.getCount();
   }

   public int getConnectionCreatedCount()
   {
      return connectionCounter.getCreatedCount();
   }

   public int getConnectionDestroyedCount()
   {
      return connectionCounter.getDestroyedCount();
   }

   private ManagedConnection createConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException
   {
      try
      {
         connectionCounter.inc();
         return mcf.createManagedConnection(subject, cri);
      }
      catch (ResourceException re)
      {
         connectionCounter.dec();
         throw re;
      } // end of try-catch
   }

   /**
    * Describe <code>doDestroy</code> method here.
    *
    * @param mc a <code>ManagedConnection</code> value
    *
    * @todo wrap mc in identity wrapper!
    */
   private void doDestroy(ManagedConnection mc)
   {
      connectionCounter.dec();
      destroyed.put(mc, null);
      try
      {
         mc.destroy();
      }
      catch (ResourceException re)
      {
         log.info("Exception destroying ManagedConnection", re);
      } // end of try-catch
   }

   public static class PoolParams
   {
      public int minSize = 0;
      public int maxSize = 10;
      // public int maxSize = 100; // GEO: make this bigger
      public int blockingTimeout = 5000;//milliseconds
      public long idleTimeout = 1000*60*30;//milliseconds, 30 minutes.
   }


   private class MCHolder
   {
      private final ManagedConnection mc;
      private final long age;

      MCHolder(final ManagedConnection mc)
      {
         this.mc = mc;
         this.age = System.currentTimeMillis();
      }

      ManagedConnection getMC()
      {
         return mc;
      }

      boolean isTimedOut()
      {
         return System.currentTimeMillis() - age > poolParams.idleTimeout;
      }
   }

   private static class Counter
   {
      private int created = 0;
      private int destroyed = 0;

      synchronized int getCount()
      {
         return created - destroyed;
      }

      synchronized int getCreatedCount()
      {
         return created;
      }

      synchronized int getDestroyedCount()
      {
         return destroyed;
      }

      synchronized void inc()
      {
         created++;
      }

      synchronized void dec()
      {
         destroyed++;
      }
   }

   public static class SetOfOne  implements Set
   {
      private final Object object;

      public SetOfOne(Object object)
      {
         if (object == null)
         {
            throw new IllegalArgumentException("SetOfOne must contain a non-null object!");
         } // end of if ()

         this.object = object;
      }
      // implementation of java.util.Set interface

      /**
       *
       * @return <description>
       */
      public int hashCode() {
         return object.hashCode();
      }

      /**
       *
       * @param param1 <description>
       * @return <description>
       */
      public boolean equals(Object other) {
         if (other instanceof SetOfOne)
         {
            return this.object == ((SetOfOne)other).object;
         } // end of if ()

         return false;
      }

      /**
       *
       * @param param1 <description>
       * @return <description>
       */
      public boolean add(Object param1) {
         throw new UnsupportedOperationException("can't add to SetOfOne");
      }

      /**
       *
       * @return <description>
       */
      public int size() {
         return 1;
      }

      /**
       *
       * @return <description>
       */
      public Object[] toArray() {
         return new Object[] {object};
      }

      /**
       *
       * @param param1 <description>
       * @return <description>
       */
      public Object[] toArray(Object[] array) {
         if (array.length < 1)
         {
            array = (Object[])java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), 1);
         } // end of if ()
         array[0] = object;
         return array;
      }

      /**
       *
       * @param param1 <description>
       * @return <description>
       */
      public boolean contains(Object object) {
         return this.object.equals(object);
      }

      /**
       *
       */
      public void clear() {
         throw new UnsupportedOperationException("can't clear SetOfOne");
      }

      /**
       *
       * @param param1 <description>
       * @return <description>
       */
      public boolean remove(Object param1) {
         throw new UnsupportedOperationException("can't remove from SetOfOne");
      }

      /**
       *
       * @return <description>
       */
      public boolean isEmpty() {
         return false;
      }

      /**
       *
       * @return <description>
       */
      public Iterator iterator() {
         return new Iterator() {
               boolean done = false;

               public boolean hasNext()
               {
                  return !done;
               }

               public Object next()
               {
                  if (done)
                  {
                     throw new NoSuchElementException();
                  } // end of if ()
                  done = true;
                  return object;
               }

               public void remove()
               {
                  throw new UnsupportedOperationException();
               }

            };
      }

      /**
       *
       * @param param1 <description>
       * @return <description>
       */
      public boolean containsAll(Collection col)
      {
         if (col == null || col.size() != 1 )
         {
            return false;
         } // end of if ()

         return object.equals(col.iterator().next());
      }

      /**
       *
       * @param param1 <description>
       * @return <description>
       */
      public boolean addAll(Collection param1) {
         throw new UnsupportedOperationException();
      }

      /**
       *
       * @param param1 <description>
       * @return <description>
       */
      public boolean removeAll(Collection param1) {
         throw new UnsupportedOperationException();
      }

      /**
       *
       * @param param1 <description>
       * @return <description>
       */
      public boolean retainAll(Collection param1) {
         throw new UnsupportedOperationException();
      }

   }

}// ManagedConnectionPool
