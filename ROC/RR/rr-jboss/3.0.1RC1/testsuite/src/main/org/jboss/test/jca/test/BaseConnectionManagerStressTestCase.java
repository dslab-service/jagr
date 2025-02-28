
/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 */

package org.jboss.test.jca.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jboss.logging.Logger;
import org.jboss.resource.connectionmanager.BaseConnectionManager2;
import org.jboss.resource.connectionmanager.JBossManagedConnectionPool;
import org.jboss.resource.connectionmanager.CachedConnectionManager;
import org.jboss.resource.connectionmanager.NoTxConnectionManager;
import org.jboss.resource.connectionmanager.InternalManagedConnectionPool;
import org.jboss.resource.connectionmanager.ManagedConnectionPool;
import org.jboss.test.jca.adapter.TestConnectionRequestInfo;
import org.jboss.test.jca.adapter.TestManagedConnectionFactory;

/**
 *  Unit Test for class ManagedConnectionPool
 *
 *
 * Created: Wed Jan  2 00:06:35 2002
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @version
 */
public class BaseConnectionManagerStressTestCase extends TestCase 
{

   Logger log = Logger.getLogger(getClass());

   boolean failed;
   ResourceException error;
   int startedThreadCount;
   final Object startedLock = new Object();
   int finishedThreadCount;
   final Object finishedLock = new Object();
   int connectionCount;
   int errorCount;


   Subject subject = new Subject();
   ConnectionRequestInfo cri = new TestConnectionRequestInfo();
   CachedConnectionManager ccm = new CachedConnectionManager();


   /** 
    * Creates a new <code>BaseConnectionManagerStressTestCase</code> instance.
    *
    * @param name test name
    */
   public BaseConnectionManagerStressTestCase (String name)
   {
      super(name);
   }


   private BaseConnectionManager2 getCM(
      InternalManagedConnectionPool.PoolParams pp, 
      String poolingStrategyName)
      throws Exception
   {
      ManagedConnectionFactory mcf = new TestManagedConnectionFactory();
      JBossManagedConnectionPool mcp = new JBossManagedConnectionPool();
      mcp.setCriteria(poolingStrategyName);
      mcp.setMinSize(pp.minSize);
      mcp.setMaxSize(pp.maxSize);
      mcp.setBlockingTimeoutMillis(pp.blockingTimeout);
      mcp.setIdleTimeout(pp.idleTimeout);
      mcp.start();
      mcp.getManagedConnectionPool().setManagedConnectionFactory(mcf);
      BaseConnectionManager2 cm = new NoTxConnectionManager(mcf, ccm, mcp.getManagedConnectionPool());
      return cm;
   }


   /**
    * The testShortBlocking test tries to simulate extremely high load on the pool,
    * with a short blocking timeout.  It tests fairness in scheduling servicing 
    * requests. The work time is modeled by sleepTime. Allowing overhead of 
    * 15 ms/pool request, the blocking is calculated at 
    * (worktime + overhead) * (threadsPerConnection)
    *
    * @exception Exception if an error occurs
    */
   public void testShortBlocking() throws Exception
   {
      startedThreadCount = 0;
      finishedThreadCount = 0;
      connectionCount = 0;
      errorCount = 0;
      //totalTime = 0;

      final int reps = 50;
      final int threadsPerConnection = 50;
      final long sleepTime = 20;
      failed = false;
      InternalManagedConnectionPool.PoolParams pp = new InternalManagedConnectionPool.PoolParams();
      pp.minSize = 0;
      pp.maxSize = 5;
      pp.blockingTimeout =  (threadsPerConnection) * ((int)sleepTime + 15);
      pp.idleTimeout = 5000;
      final BaseConnectionManager2 cm = getCM(pp, "ByNothing");
      int totalThreads = pp.maxSize * threadsPerConnection;
      log.info("ShortBlocking test with totalThreads: " + totalThreads);
      long startTime = System.currentTimeMillis();
      for (int i = 0; i < totalThreads; i++)
      {
         Runnable t = new Runnable() {
               int id;
               public void run() 
               {
                  synchronized (startedLock)
                  {
                     id = startedThreadCount;
                     startedThreadCount++;
                     startedLock.notify();
                  }
                  //long startTime = System.currentTimeMillis();
                  for (int j = 0; j < reps; j++)
                  {
                     try 
                     {
                        ManagedConnection mc = cm.getManagedConnection(null, null);
                        //maybe should be synchronized
                        BaseConnectionManagerStressTestCase.this.connectionCount++;
                        //long started = System.currentTimeMillis();
                        // BaseConnectionManagerStressTestCase.this.log.info("got id: " + id + ", rep: " + j + ", mc: " + mc + ", time: " + started);
                        Thread.sleep(sleepTime);
                        cm.returnManagedConnection(mc, false);
                        //BaseConnectionManagerStressTestCase.this.log.info("end id: " + id + ", rep: " + j + ", mc: " + mc + ", elapsed: " + (System.currentTimeMillis() - started));
                     }
                      catch (ResourceException re)
                     {
                        BaseConnectionManagerStressTestCase.this.log.info("error: iterationCount: " + j + ", connectionCount: " + BaseConnectionManagerStressTestCase.this.connectionCount);
                        BaseConnectionManagerStressTestCase.this.errorCount++;
                        BaseConnectionManagerStressTestCase.this.error = re;
                        BaseConnectionManagerStressTestCase.this.failed = true;
                     } // end of try-catch
                     catch (InterruptedException ie)
                     {
                        break;
                     } // end of catch
                     
                     
                  } // end of for ()
                  //long endTime = System.currentTimeMillis();
                  synchronized (finishedLock)
                  {
                     //long elapsed = endTime - startTime;
                     //totalTime += elapsed;
                     //BaseConnectionManagerStressTestCase.this.log.info("id: " + id + ", elapsed: " + elapsed  + ", perRequest: " + elapsed/reps + ", overhead: " + (elapsed/reps - sleepTime));
                     finishedThreadCount++;
                     finishedLock.notify();
                  }
               }
            };
         new Thread(t).start();
         synchronized (startedLock)
         {
            while (startedThreadCount < i + 1)
            {
               startedLock.wait();
            } // end of while ()
            
         }
      } // end of for ()
      synchronized (finishedLock)
      {
         while (finishedThreadCount < totalThreads)
         {
            finishedLock.wait();
         } // end of while ()
      }
      long endTime = System.currentTimeMillis();
      log.info("completed short blocking test with connectionCount: " + connectionCount + ", expected : " + totalThreads * reps);
      log.info("errorCount: " + errorCount);
      //log.info("Total time for all threads: " + totalTime);
      long elapsed = endTime - startTime;
      log.info("Total time elapsed: " + elapsed  + ", perRequest: " + elapsed/(totalThreads * reps) + ", overhead: " + (elapsed * pp.maxSize/(totalThreads * reps) - sleepTime));
      assertTrue("Wrong number of connections counted: " + cm.getConnectionCount(), cm.getConnectionCount() == pp.maxSize);
      assertTrue("Blocking Timeout occurred in ShortBlocking test: " + error, !failed);
      cm.shutdown();
      
   }



}// 
