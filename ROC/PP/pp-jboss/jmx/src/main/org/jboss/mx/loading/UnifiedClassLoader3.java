/***************************************
*                                     *
*  JBoss: The OpenSource J2EE WebOS   *
*                                     *
*  Distributable under LGPL license.  *
*  See terms of license at gnu.org.   *
*                                     *
***************************************/

package org.jboss.mx.loading;

import java.net.URL;

import org.jboss.mx.logging.Logger;

import EDU.oswego.cs.dl.util.concurrent.ReentrantLock;

/** An extension of UnifiedClassLoader that manages a thread based loading
 * strategy to work around the locking problems associated with the VM
 * initiated locking due to the synchronized loadClassInternal method of
 * ClassLoader which cannot be overriden.

 * @author <a href="scott.stark@jboss.org">Scott Stark</a>
 * @version $Revision: 1.1.1.1 $
*/
public class UnifiedClassLoader3 extends UnifiedClassLoader
   implements UnifiedClassLoader3MBean
{
   // Static --------------------------------------------------------
   private static final Logger log = Logger.getLogger(UnifiedClassLoader3.class);

   // Attributes ----------------------------------------------------
   protected ReentrantLock loadLock = new ReentrantLock();
   /** A debugging variable used to track the recursive depth of loadClass() */
   private int loadClassDepth;
   /** The relative order in which this class loader was added to the ULR */
   private int addedOrder;

   // Constructors --------------------------------------------------
   /**
    * Construct a <tt>UnifiedClassLoader</tt> without registering it to the
    * classloader repository.
    *
    * @param url   the single URL to load classes from.
    */
   public UnifiedClassLoader3(URL url)
   {
      this(url, null);
   }
   /**
    * Construct a <tt>UnifiedClassLoader</tt> without registering it to the
    * classloader repository.
    *
    * @param url   the single URL to load classes from.
    * @param origURL the possibly null original URL from which url may
    * be a local copy or nested jar.
    */
   public UnifiedClassLoader3(URL url, URL origURL)
   {
      super(url, origURL);
   }

   /**
    * Construct a <tt>UnifiedClassLoader</tt> and registers it to the given
    * repository.
    * @param url The single URL to load classes from.
    * @param origURL the possibly null original URL from which url may
    * be a local copy or nested jar.
    * @param repository the repository this classloader delegates to
    * be a local copy or nested jar.
    */
   public UnifiedClassLoader3(URL url, URL origURL, LoaderRepository repository)
   {
      this(url, origURL);

      // set the repository reference
      this.repository = repository;
   }

   // Public --------------------------------------------------------

   public void unregister()
   {
      repository.removeClassLoader(this);
   }

   public int getAddedOrder()
   {
      return addedOrder;
   }
   public void setAddedOrder(int addedOrder)
   {
      this.addedOrder = addedOrder;
   }

   /**
   * Retruns a string representaion of this UCL.
   */
   public String toString()
   {
      StringBuffer tmp = new StringBuffer(super.toString());
      tmp.setCharAt(tmp.length()-1, ',');
      tmp.append("addedOrder=");
      tmp.append(addedOrder);
      tmp.append('}');
      return tmp.toString();
   }

   // UnifiedClassLoader overrides --------------------------------------

   /** Called to load a class into the repository. The calling thread owns
    * the UCL monitor and handles class loadings tasks for which this UCL
    * is likely to be able to handle based on the pkg to URL mapping in the
    * repository.
    *
    */
   public synchronized Class loadClass(String name, boolean resolve)
      throws ClassNotFoundException
   {
      loadClassDepth ++;
      boolean trace = log.isTraceEnabled();

      /* Since loadClass can be called from loadClassInternal with the monitor
         already held, we need to determine if there is a ClassLoadingTask
         which requires this UCL. If there is, we release the UCL monitor
         so that the ClassLoadingTask can use the UCL.
       */
      boolean acquired = attempt(1);
      while( acquired == false )
      {
         /* Another thread needs this UCL to load a class so release the
          monitor acquired by the synchronized method. We loop until
          we can acquire the class loading lock.
         */
        try
         {
            if( trace )
               log.trace("Waiting for loadClass lock");
            this.wait();
         }
         catch(InterruptedException ignore)
         {
         }
         acquired = attempt(1);
      }

      ClassLoadingTask task = null;
      try
      {
         Thread t = Thread.currentThread();
         // Register this thread as owning this UCL
         if( loadLock.holds() == 1 )
            LoadMgr.registerLoaderThread(this, t);

         // Create a class loading task and submit it to the repository
         task = new ClassLoadingTask(name, this, t);
         /* Process class loading tasks needing this UCL until our task has
            been completed by the thread owning the required UCL(s).
          */
         UnifiedLoaderRepository3 ulr3 = (UnifiedLoaderRepository3) repository;
         if( LoadMgr.beginLoadTask(task, ulr3) == false )
         {
            while( task.threadTaskCount != 0 )
            {
               try
               {
                  LoadMgr.nextTask(t, task, ulr3);
               }
               catch(InterruptedException e)
               {
                  // Abort the load or retry?
                  break;
               }
            }
         }
      }
      finally
      {
         // Unregister as the UCL owner to reschedule any remaining load tasks
         if( loadLock.holds() == 1 )
            LoadMgr.endLoadTask(task);
         // Notify any threads waiting to use this UCL
         this.release();
         this.notifyAll();
         loadClassDepth --;
      }

      if( task.loadedClass == null )
      {
         if( task.loadException instanceof ClassNotFoundException )
            throw (ClassNotFoundException) task.loadException;
         else if( task.loadException != null )
         {
            if( log.isTraceEnabled() )
               log.trace("Unexpected error during load of:"+name, task.loadException);
            String msg = "Unexpected error during load of: "+name
               + ", msg="+task.loadException.getMessage();
            throw new ClassNotFoundException(msg);
         }
         // Assert that loadedClass is not null
         else
            throw new IllegalStateException("ClassLoadingTask.loadedTask is null, name: "+name);
      }

      return task.loadedClass;
   }

   /** Load the resource from the repository using the LoadMgr as the
    * synchronization point.
    */
   public URL getResource(String name)
   {
      UnifiedLoaderRepository3 ulr3 = (UnifiedLoaderRepository3) repository;
      URL u = repository.getResource(name, this);
      return u;
   }

   /** Attempt to acquire the class loading lock. This lock must be acquired
    * before a thread enters the class loading task loop in loadClass. This
    * method maintains any interrupted state of the calling thread.
    *@see #loadClass(String, boolean)
    */
   protected boolean attempt(long waitMS)
   {
      boolean acquired = false;
      boolean trace = log.isTraceEnabled();
      // Save and clear the interrupted state of the incoming thread
      boolean threadWasInterrupted = Thread.currentThread().interrupted();
      try
      {
         acquired = loadLock.attempt(waitMS);
      }
      catch(InterruptedException e)
      {
      }
      finally
      {
         // Restore the interrupted state of the thread
         if( threadWasInterrupted )
            Thread.currentThread().interrupt();
      }
      if( trace )
         log.trace("attempt("+loadLock.holds()+") was: "+acquired+" for :"+this);
      return acquired;
   }
   /** Acquire the class loading lock. This lock must be acquired
    * before a thread enters the class loading task loop in loadClass.
    *@see #loadClass(String, boolean)
    */
   protected void acquire()
   {
      // Save and clear the interrupted state of the incoming thread
      boolean threadWasInterrupted = Thread.currentThread().interrupted();
      try
      {
         loadLock.acquire();
      }
      catch(InterruptedException e)
      {
      }
      finally
      {
         // Restore the interrupted state of the thread
         if( threadWasInterrupted )
            Thread.currentThread().interrupt();
      }
      if( log.isTraceEnabled() )
         log.trace("acquired("+loadLock.holds()+") for :"+this);
   }
   /** Release the class loading lock previous acquired through the acquire
    * method.
    */
   protected void release()
   {
      if( log.isTraceEnabled() )
         log.trace("release("+loadLock.holds()+") for :"+this);
      loadLock.release();
      if( log.isTraceEnabled() )
         log.trace("released, holds: "+loadLock.holds());
   }
}
