/*
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.ha.framework.server;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.RemoteStub;

import org.jboss.invocation.MarshalledInvocation;
import org.jboss.invocation.Invocation;
import org.jboss.ha.framework.interfaces.DistributedReplicantManager.ReplicantListener;
import org.jboss.ha.framework.interfaces.LoadBalancePolicy;
import org.jboss.ha.framework.interfaces.HARMIServer;
import org.jboss.ha.framework.interfaces.HARMIClient;
import org.jboss.ha.framework.interfaces.HARMIResponse;
import org.jboss.ha.framework.interfaces.HARMIProxy;
import org.jboss.ha.framework.interfaces.HAPartition;

/** 
 *   This class is a "server-side" proxy for replicated RMI objects.
 *
 *   @author bill@burkecentral.com
 *   @author sacha.labourey@cogito-info.ch
 *   @version $Revision: 1.1.1.1 $
 *
 * <p><b>Revisions:</b><br>
 * <p><b>2001/11/09: Sacha Labourey</b>
 * <ol>
 *   <li>Added possibility to replace underlying HAPartition without impacting already living HARMIClient(s)</li>
 *   <li> => needed to changed the condition as to when to give a new view to the client.
 * </ol>
 * <p><b>2002/02/15: Bill Burke</b>
 * <ol>
 *   <li>Replicant management delegated to HATargety.  This class almost looks the same as JRMPInvokerHA, but is a tiny bit
 *   more lightweight.
 * </ol>
 */
public class HARMIServerImpl
   implements HARMIServer
{
   // Constants -----------------------------------------------------
   
   // Attributes ----------------------------------------------------
   
   protected Object handler;
   protected Map invokerMap = new HashMap();
   protected org.jboss.logging.Logger log;
   protected RemoteStub rmistub;
   protected Object stub;
   protected String key;
   protected Class intf;
   protected RefreshProxiesHATarget target;
   
   // Static --------------------------------------------------------
   
   // Constructors --------------------------------------------------
   
   public HARMIServerImpl(HAPartition partition, 
                          String replicantName, 
                          Class intf, 
                          Object handler,
                          int port, 
                          RMIClientSocketFactory csf, 
                          RMIServerSocketFactory ssf) 
      throws Exception
   {
      this.handler = handler;
      this.log = org.jboss.logging.Logger.getLogger(this.getClass());
      this.intf = intf;
      this.key = partition.getPartitionName() + "/" + replicantName;
      Method[] methods = handler.getClass().getMethods();
      
      for (int i = 0; i < methods.length; i++) {
         Long methodkey = new Long(MarshalledInvocation.calculateHash(methods[i]));
         invokerMap.put(methodkey, methods[i]);
      }
      
      this.rmistub = (RemoteStub)UnicastRemoteObject.exportObject(this, port, csf, ssf);// casting is necessary because interface has changed in JDK>=1.2
      this.target = new RefreshProxiesHATarget(partition, replicantName, rmistub);

      HARMIServer.rmiServers.put(key, this);
   }

   /**
    * Create a new HARMIServer implementation that will act as a RMI end-point for a specific server.
    * @param partition {@link HAPartition} that will determine the cluster member
    * @param replicantName Name of the service using this HARMIServer
    * @param intf Class type under which should appear the RMI server dynamically built
    * @param handler Target object to which calls will be forwarded
    * @throws Exception Thrown if any exception occurs during call forwarding
    */   
   public HARMIServerImpl(HAPartition partition, String replicantName, Class intf, Object handler) throws Exception
   {
      this(partition, replicantName, intf, handler, 0, null, null);
   }
   
   // Public --------------------------------------------------------
   
   /**
    * Once a HARMIServer implementation exists, it is possible to ask for a stub that can, for example,
    * be bound in JNDI for client use. Each client stub may incorpore a specific load-balancing
    * policy.
    * @param policy {@link org.jboss.ha.framework.interfaces.LoadBalancingPolicy} implementation to ues on the client.
    * @return
    */   
   public Object createHAStub(LoadBalancePolicy policy)
   {
      HARMIClient client = new HARMIClient(target.getReplicants(), target.getCurrentViewId (), 
                                           policy, key, handler);
      this.target.addProxy (client);
      
      return Proxy.newProxyInstance(
      intf.getClassLoader(),
      new Class[]
      { intf, HARMIProxy.class },
      client);
   }
   
   public void destroy()
   {
      try
      {
	 target.destroy();
         HARMIServer.rmiServers.remove(key);
         UnicastRemoteObject.unexportObject(this, true);
         
      } catch (Exception e)
      {
         e.printStackTrace();
      }
   }
   
   // HARMIServer implementation ----------------------------------------------

   public HARMIResponse invoke(long clientViewId, MarshalledInvocation mi) throws Exception
   {
      mi.setMethodMap(invokerMap);
      Method method = mi.getMethod();
      
      try
      {
         HARMIResponse rsp = new HARMIResponse();         
         if (clientViewId != target.getCurrentViewId())
         {
            rsp.newReplicants = new ArrayList(target.getReplicants());
            rsp.currentViewId = target.getCurrentViewId();
         } 
         
         rsp.response = method.invoke(handler, mi.getArguments());
         return rsp;
      }
      catch (IllegalAccessException iae)
      {
         throw iae;
      }
      catch (IllegalArgumentException iae)
      {
         throw iae;
      }
      catch (java.lang.reflect.InvocationTargetException ite)
      {
         throw (Exception)ite.getTargetException();
      }
   }

   public List getReplicants() throws Exception
   {
      return target.getReplicants();
   }

   public Object getLocal() throws Exception
   {
      return handler;
   }
   
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------
   public class RefreshProxiesHATarget extends HATarget
   {
      protected ArrayList generatedProxies;
      
      public RefreshProxiesHATarget(HAPartition partition, 
            String replicantName,
            java.io.Serializable target) 
         throws Exception
      {
         super (partition, replicantName, target);         
      }
      
      public void init() throws Exception
      {
         super.init ();
         generatedProxies = new ArrayList ();
      }
      
      
      public synchronized void addProxy (HARMIClient client)
      {
         SoftReference ref = new SoftReference(client);
         generatedProxies.add (ref);
      }
      
      public synchronized void replicantsChanged(String key, List newReplicants, int newReplicantsViewId)
      {
         super.replicantsChanged (key, newReplicants, newReplicantsViewId);
         
         // we now update all generated proxies
         //
         int max = generatedProxies.size ();
         ArrayList trash = new ArrayList();
         for (int i=0; i<max; i++)
         {
            SoftReference ref = (SoftReference)generatedProxies.get (i);
            HARMIClient proxy = (HARMIClient)ref.get ();
            if (proxy == null)
            {
               trash.add (ref);
            }
            else
            {
               proxy.updateClusterInfo (this.replicants, this.clusterViewId);
            }
         }
         
         if (trash.size () > 0)
            generatedProxies.removeAll (trash);
         
      }
   }
}
