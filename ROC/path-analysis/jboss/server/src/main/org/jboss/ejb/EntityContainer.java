/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.ejb;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.ejb.EJBObject;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBMetaData;
import javax.ejb.EJBException;
import javax.ejb.RemoveException;
import javax.management.j2ee.CountStatistic;
import javax.transaction.Transaction;
import javax.transaction.TransactionRolledbackException;

import org.jboss.deployment.DeploymentException;
import org.jboss.invocation.Invocation;
import org.jboss.invocation.MarshalledInvocation;
import org.jboss.logging.Logger;
import org.jboss.monitor.StatisticsProvider;
import org.jboss.util.collection.SerializableEnumeration;
import org.jboss.metadata.EntityMetaData;

/**
* This is a Container for EntityBeans (both BMP and CMP).
*
* @see Container
* @see EntityEnterpriseContext
*
* @author <a href="mailto:rickard.oberg@telkel.com">Rickard �berg</a>
* @author <a href="mailto:marc.fleury@telkel.com">Marc Fleury</a>
* @author <a href="mailto:sebastien.alborini@m4x.org">Sebastien Alborini</a>
* @author <a href="mailto:docodan@mvcsoft.com">Daniel OConnor</a>
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @author <a href="mailto:andreas.schaefer@madplanet.com">Andreas Schaefer</a>
* @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
* @version $Revision: 1.1.1.1 $
*
* <p><b>Revisions:</b>
*
* <p><b>20010701 marc fleury:</b>
* <ul>
* <li>Transaction to context wiring was moved to the instance interceptor
* </ul>
* <p><b>20010718 andreas schaefer:</b>
* <ul>
* <li>- Added statistics gathering
* </ul>
* <p><b>20010807 bill burke:</b>
* <ul>
* <li> Moved storeEntity from EntitySynchronization to here so other classes can use it.
* <li> Moved synchronizeEntitiesWithinTransaction to here from Application as a static method.
* </ul>

* <p><b>20011219 marc fleury:</b>
* <ul>
* <li> Moved to new invoker scheme, using Invocation and MBean invokers.
* </ul>
* <p><b>20020413 dain sundstrom:</b>
* <ul>
* <li> Broke cretion into 2 invocations.  The first is through the invokeHome
* chain, and calls the normal createEntity.  The second is throught the invoke
* chain, and this call the new postCreateEntity method.  
* </ul>
*/
public class EntityContainer
extends Container
implements ContainerInvokerContainer, InstancePoolContainer, StatisticsProvider
{
   // Constants -----------------------------------------------------

   private static Logger log = Logger.getLogger(EntityContainer.class);
   
   // Attributes ----------------------------------------------------
   
   /**
   * These are the mappings between the home interface methods and the
   * container methods.
   */
   protected Map homeMapping = new HashMap();
   
   /**
   * These are the mappings between the remote/local interface methods and the
   * bean methods.
   */
   protected Map beanMapping = new HashMap();
   
   
   /** This is the container invoker for this container */
   protected ContainerInvoker containerInvoker;
   
   /** This is the persistence manager for this container */
   protected EntityPersistenceManager persistenceManager;
   
   /** This is the instance cache for this container */
   protected InstanceCache instanceCache;
   
   /** This is the instancepool that is to be used */
   protected InstancePool instancePool;
   
   protected TxEntityMap txEntityMap = new TxEntityMap();
   
   /**
   * This is the first interceptor in the chain. The last interceptor must
   * be provided by the container itself.
   */
   protected Interceptor interceptor;
   
   // These members contains statistics variable
   protected long createCount = 0;
   protected long removeCount = 0;
   
   /**
   * <code>readOnly</code> determines if state can be written to resource manager.
   *
   */
   protected boolean readOnly = false;
   
   /**
   * This provides a way to find the entities that are part of a given
   * transaction EntitySynchronizationInterceptor and InstanceSynchronization
   * manage this instance.
   */
   protected static  GlobalTxEntityMap globalTxEntityMap = new GlobalTxEntityMap();
   
   
   public static GlobalTxEntityMap getGlobalTxEntityMap() { return globalTxEntityMap; }
   
   /**
   * Stores all of the entities associated with the specified transaction.
   * As per the spec 9.6.4, entities must be synchronized with the datastore
   * when an ejbFind<METHOD> is called.
   * Also, all entities within entire transaction should be synchronized before
   * a remove, otherwise there may be problems with 'cascade delete'.
   * @param tx the transaction that associated entites will be stored
   * @throws Exception if an problem occures while storing the entities
   */
   public static void synchronizeEntitiesWithinTransaction(Transaction tx)
      throws TransactionRolledbackException
   {
      // If there is no transaction, there is nothing to synchronize.
      if(tx != null)
      {
	 getGlobalTxEntityMap().syncEntities(tx);
      }
   }
   // Public --------------------------------------------------------
   
   public boolean isReadOnly()
   {
      return readOnly;
   }


   public void setContainerInvoker(ContainerInvoker ci)
   {
      if (ci == null)
         throw new IllegalArgumentException("Null invoker");
      
      this.containerInvoker = ci;
      ci.setContainer(this);
   }
   
   public ContainerInvoker getContainerInvoker()
   {
      return containerInvoker;
   }
   
   public LocalContainerInvoker getLocalContainerInvoker()
   {
      return localContainerInvoker;
   }
   
   public void setInstancePool(InstancePool ip)
   {
      if (ip == null)
         throw new IllegalArgumentException("Null pool");
      
      this.instancePool = ip;
      ip.setContainer(this);
   }
   
   public InstancePool getInstancePool()
   {
      return instancePool;
   }
   
   public TxEntityMap getTxEntityMap()
   {
      return txEntityMap;
   }
   
   public void setInstanceCache(InstanceCache ic)
   {
      if (ic == null)
         throw new IllegalArgumentException("Null cache");
      
      this.instanceCache = ic;
      ic.setContainer(this);
   }
   
   public InstanceCache getInstanceCache()
   {
      return instanceCache;
   }
   
   public EntityPersistenceManager getPersistenceManager()
   {
      return persistenceManager;
   }
   
   public void setPersistenceManager(EntityPersistenceManager pm)
   {
      if (pm == null)
         throw new IllegalArgumentException("Null persistence manager");
      
      persistenceManager = pm;
      pm.setContainer(this);
   }
   
   public void addInterceptor(Interceptor in)
   {
      if (interceptor == null)
      {
         interceptor = in;
      }
      else
      {
         
         Interceptor current = interceptor;
         while ( current.getNext() != null)
         {
            current = current.getNext();
         }
         
         current.setNext(in);
      }
   }
   
   public Interceptor getInterceptor()
   {
      return interceptor;
   }
   
   public Class getHomeClass()
   {
      return homeInterface;
   }
   
   public Class getRemoteClass()
   {
      return remoteInterface;
   }
   
   /**
   * Returns a new instance of the bean class or a subclass of the bean class.
   * If this is 1.x cmp, simply return a new instance of the bean class.
   * If this is 2.x cmp, return a subclass that provides an implementation
   * of the abstract accessors.
   *
   * @see java.lang.Class#newInstance
   *
   * @return   The new instance.
   */
   public Object createBeanClassInstance() throws Exception {
      return persistenceManager.createBeanClassInstance();
   }
   
   // Container implementation --------------------------------------
   
   public void create() throws Exception
   {
      log.trace("Creating");
      // Associate thread with classloader
      ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(getClassLoader());

      try
      {
         // Acquire classes from CL
         if (metaData.getHome() != null)
            homeInterface = classLoader.loadClass(metaData.getHome());
         if (metaData.getRemote() != null)
            remoteInterface = classLoader.loadClass(metaData.getRemote());

         // Call default init
         super.create();

         // Map the bean methods
         setupBeanMapping();

         // Map the home methods
         setupHomeMapping();

         // Map the interfaces to Long
         setupMarshalledInvocationMapping();

         // Initialize pool
         instancePool.create();

         // Init container invoker
         if (containerInvoker != null)
            containerInvoker.create();

         // Init instance cache
         instanceCache.create();

         // Init persistence
         persistenceManager.create();

         // Initialize the interceptor by calling the chain
         Interceptor in = interceptor;
         while (in != null)
         {
            in.setContainer(this);
            in.create();
            in = in.getNext();
         }
         readOnly = ((EntityMetaData)metaData).isReadOnly();
      }
      finally
      {
         // Reset classloader
         Thread.currentThread().setContextClassLoader(oldCl);
      }
   }
   
   public void start() throws Exception
   {
      log.trace("Starting");
      // Associate thread with classloader
      ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(getClassLoader());

      try
      {
         // Call default start
         super.start();

         // Start container invoker
         if (containerInvoker != null)
            containerInvoker.start();

         // Start instance cache
         instanceCache.start();

         // Start persistence
         persistenceManager.start();

         // Start the instance pool
         instancePool.start();

         // Start all interceptors in the chain
         Interceptor in = interceptor;
         while (in != null)
         {
            in.start();
            in = in.getNext();
         }
      }
      finally
      {
         // Reset classloader
         Thread.currentThread().setContextClassLoader(oldCl);
      }
   }

   public void stop()
   {
      log.trace("Stopping");
      // Associate thread with classloader
      ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(getClassLoader());

      try
      {
         //Stop items in reverse order from start
         //This assures that CachedConnectionInterceptor will get removed
         //from in between this and the pm before the pm is stopped.
         // Stop all interceptors in the chain
         Interceptor in = interceptor;
         while (in != null)
         {
            in.stop();
            in = in.getNext();
         }      

         // Stop the instance pool
         instancePool.stop();


         // Stop persistence
         persistenceManager.stop();

         // Stop instance cache
         instanceCache.stop();

         // Stop container invoker
         if (containerInvoker != null)
            containerInvoker.stop();

         // Call default stop
         super.stop();
      }
      finally
      {
         // Reset classloader
         Thread.currentThread().setContextClassLoader(oldCl);
      }
   }
   
   public void destroy()
   {
      log.trace("Destroying");
      // Associate thread with classloader
      ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(getClassLoader());

      try
      {
         // Destroy container invoker
         if (containerInvoker != null)
            containerInvoker.destroy();

         // Destroy instance cache
         instanceCache.destroy();
         instanceCache.setContainer(null);

         // Destroy persistence
         persistenceManager.destroy();
         persistenceManager.setContainer(null);

         // Destroy the pool
         instancePool.destroy();
         instancePool.setContainer(null);

         // Destroy all the interceptors in the chain
         Interceptor in = interceptor;
         while (in != null)
         {
            in.destroy();
            in.setContainer(null);
            in = in.getNext();
         }

         // Call default destroy
         super.destroy();
      }
      finally
      {
         // Reset classloader
         Thread.currentThread().setContextClassLoader(oldCl);
      }
   }

   public Object invokeHome(Invocation mi) throws Exception
   {
      
      return getInterceptor().invokeHome(mi);
   }
   
   public Object invoke(Invocation mi) throws Exception
   {
      // Invoke through interceptors
      return getInterceptor().invoke(mi);
   }
   
   // EJBObject implementation --------------------------------------
   
   public void remove(Invocation mi)
   throws RemoteException, RemoveException
   {
      // synchronize entities with the datastore before the bean is removed
      // this will write queued updates so datastore will be consistent before removal
      if (!getBeanMetaData().getContainerConfiguration().getSyncOnCommitOnly())
         synchronizeEntitiesWithinTransaction(mi.getTransaction());
      
      // Get the persistence manager to do the dirty work
      getPersistenceManager().removeEntity((EntityEnterpriseContext)mi.getEnterpriseContext());
      
      // We signify "removed" with a null id
      // There is no need to synchronize on the context since all the threads reaching here have
      // gone through the InstanceInterceptor so the instance is locked and we only have one thread
      // the case of reentrant threads is unclear (would you want to delete an instance in reentrancy)
      ((EnterpriseContext) mi.getEnterpriseContext()).setId(null);
      removeCount++;
   }
   
   /**
   * @throws Error    Not yet implemented.
   */
   public Handle getHandle(Invocation mi)
   throws RemoteException
   {
      // TODO
      throw new Error("Not yet implemented");
   }
   
   /**
   * @throws Error    Not yet implemented.
   */
   public Object getPrimaryKey(Invocation mi)
   throws RemoteException
   {
      // TODO
      throw new Error("Not yet implemented");
   }
   
   /**
   * @throws IllegalStateException     If container invoker is null.
   */
   public EJBHome getEJBHome(Invocation mi)
   throws RemoteException
   {
      if (containerInvoker == null) {
         throw new IllegalStateException();
      }
      return (EJBHome) containerInvoker.getEJBHome();
   }
   
   public boolean isIdentical(Invocation mi)
   throws RemoteException
   {
      return ((EJBObject)mi.getArguments()[0]).getPrimaryKey().equals(((EnterpriseContext) mi.getEnterpriseContext()).getId());
      // TODO - should also check type
   }
   
   /**
   * MF FIXME these are implemented on the client
   */
   public EJBLocalHome getEJBLocalHome(Invocation mi)
   {
      return localContainerInvoker.getEJBLocalHome();
   }
   
   /**
   * @throws Error    Not yet implemented.
   */
   public void removeLocalHome(Invocation mi)
   throws RemoteException, RemoveException
   {
      throw new Error("Not Yet Implemented");
   }
   
   /**
   * Local home interface implementation
   */
   public EJBLocalObject createLocalHome(Invocation mi)
   throws Exception
   {
      // The persistence manager takes care of the wiring and creating the EJBLocalObject
      getPersistenceManager().createEntity(mi.getMethod(),mi.getArguments(),
         (EntityEnterpriseContext) mi.getEnterpriseContext());
      
      // The context implicitely carries the EJBObject
      createCount++;
      return ((EntityEnterpriseContext)mi.getEnterpriseContext()).getEJBLocalObject();
   }
     
   /**
   * Delegates to the persistence manager postCreateEntityMethod.
   */
   public void postCreateLocalHome(Invocation mi) throws Exception
   {
      // The persistence manager takes care of the post create step
      getPersistenceManager().postCreateEntity(mi.getMethod(),mi.getArguments(),
         (EntityEnterpriseContext) mi.getEnterpriseContext());
   }
  
   public Object findLocal(Invocation mi)
   throws Exception
   {
      /**
      * As per the spec 9.6.4, entities must be synchronized with the datastore
      * when an ejbFind<METHOD> is called.
      */
      if (!getBeanMetaData().getContainerConfiguration().getSyncOnCommitOnly())
         synchronizeEntitiesWithinTransaction(mi.getTransaction());
      
      // Multi-finder?
      if (!mi.getMethod().getReturnType().equals(getLocalClass()))
      {
         // Iterator finder
         Collection c = getPersistenceManager().findEntities(mi.getMethod(), mi.getArguments(), (EntityEnterpriseContext)mi.getEnterpriseContext());
         
         // Get the EJBObjects with that
         Collection ec = localContainerInvoker.getEntityLocalCollection(c);
         
         // BMP entity finder methods are allowed to return java.util.Enumeration.
         try {
            if (mi.getMethod().getReturnType().equals(Class.forName("java.util.Enumeration")))
            {
               return java.util.Collections.enumeration(ec);
            }
            else
            {
               return ec;
            }
         } catch (ClassNotFoundException e)
         {
            // shouldn't happen
            return ec;
         }
      }
      else
      {
         // Single entity finder
         Object id = getPersistenceManager().findEntity(mi.getMethod(),
            mi.getArguments(),
            (EntityEnterpriseContext)mi.getEnterpriseContext());
         
         //create the EJBObject
         return (EJBLocalObject)localContainerInvoker.getEntityEJBLocalObject(id);
      }
   }
   
   // Home interface implementation ---------------------------------
   
   /**
   * This methods finds the target instances by delegating to the persistence
   * manager It then manufactures EJBObject for all the involved instances
   * found.
   */
   public Object find(Invocation mi) throws Exception
   {        
      /**
      * As per the spec 9.6.4, entities must be synchronized with the datastore
      * when an ejbFind<METHOD> is called.
      */
      if (!getBeanMetaData().getContainerConfiguration().getSyncOnCommitOnly())
         synchronizeEntitiesWithinTransaction(mi.getTransaction());
      
      // Multi-finder?
      if (!mi.getMethod().getReturnType().equals(getRemoteClass()))
      {
         // Iterator finder
         Collection c = getPersistenceManager().findEntities(mi.getMethod(), mi.getArguments(), (EntityEnterpriseContext)mi.getEnterpriseContext());
         
         // Get the EJBObjects with that
         Collection ec = containerInvoker.getEntityCollection(c);
         
         // BMP entity finder methods are allowed to return java.util.Enumeration.
         // We need a serializable Enumeration, so we can't use Collections.enumeration()
         try {
            if (mi.getMethod().getReturnType().equals(Class.forName("java.util.Enumeration")))
            {
               return new SerializableEnumeration(ec);
            }
            else
            {
               return ec;
            }
         } catch (ClassNotFoundException e)
         {
            // shouldn't happen
            return ec;
         }
      }
      else
      {
         // Single entity finder
         Object id = getPersistenceManager().findEntity(mi.getMethod(),
            mi.getArguments(),
            (EntityEnterpriseContext)mi.getEnterpriseContext());
         
         //create the EJBObject
         return (EJBObject)containerInvoker.getEntityEJBObject(id);
      }
   }
   
   
   /**
   * store entity
   */
   public void storeEntity(EntityEnterpriseContext ctx) throws Exception
   {
      if (ctx.getId() != null)
      {
         if(getPersistenceManager().isModified(ctx)) {
            getPersistenceManager().storeEntity(ctx);
         }
      }
   }
   
   /**
   * Delegates to the persistence manager postCreateEntityMethod.
   */
   public void postCreateHome(Invocation mi) throws Exception
   {
      // The persistence manager takes care of the post create step
      getPersistenceManager().postCreateEntity(mi.getMethod(),mi.getArguments(),
         (EntityEnterpriseContext) mi.getEnterpriseContext());
   }
 
   /**
   * This method takes care of the wiring of the "EJBObject" trio
   * (target, context, proxy).  It delegates to the persistence manager.
   */
   public EJBObject createHome(Invocation mi)
   throws Exception
   {
      // The persistence manager takes care of the wiring and creating the EJBObject
      getPersistenceManager().createEntity(mi.getMethod(),mi.getArguments(),
         (EntityEnterpriseContext) mi.getEnterpriseContext());
      
      // The context implicitely carries the EJBObject
      createCount++;
      return ((EntityEnterpriseContext)mi.getEnterpriseContext()).getEJBObject();
   }
   
   /**
   * A method for the getEJBObject from the handle
   */
   public EJBObject getEJBObject(Invocation mi)
   throws RemoteException
   {
      // All we need is an EJBObject for this Id;
      return (EJBObject)containerInvoker.getEntityEJBObject(((EntityCache) instanceCache).createCacheKey(mi.getId()));
   }
   
   // EJBHome implementation ----------------------------------------
   
   /**
   * @throws Error    Not yet implemented.
   */
   public void removeHome(Invocation mi)
   throws RemoteException, RemoveException
   {
      throw new Error("Not yet implemented");
   }
   
   public EJBMetaData getEJBMetaDataHome(Invocation mi)
   throws RemoteException
   {
      return getContainerInvoker().getEJBMetaData();
   }
   
   /**
   * @throws Error    Not yet implemented.
   */
   public HomeHandle getHomeHandleHome(Invocation mi)
   throws RemoteException
   {
      // TODO
      throw new Error("Not yet implemented");
   }
   
   // StatisticsProvider implementation ------------------------------------
   public Map retrieveStatistic()
   {
      // Loop through all Interceptors and add statistics
      Map lStatistics = new HashMap();
      StatisticsProvider lProvider = (StatisticsProvider) getPersistenceManager();
      lStatistics.putAll( lProvider.retrieveStatistic() );
      lProvider = (StatisticsProvider) getInstancePool();
      lStatistics.putAll( lProvider.retrieveStatistic() );
      return lStatistics;
   }
   public void resetStatistic()
   {
   }
   
   // Private -------------------------------------------------------
   
   private void setupHomeMappingImpl( 
      Method[] m,
      String finderName,
      String append )
   throws DeploymentException
   {
      // Adrian Brock: This should go away when we don't support EJB1x
      boolean isEJB1x = metaData.getApplicationMetaData().isEJB1x();

      for (int i = 0; i < m.length; i++)
      {
         String methodName = m[i].getName();
         try
         {
            try // Try home method
            {
               String ejbHomeMethodName = "ejbHome" + methodName.substring(0,1).toUpperCase() + methodName.substring(1);
               homeMapping.put(m[i], beanClass.getMethod(ejbHomeMethodName, m[i].getParameterTypes()));
               
               continue;
            } catch (NoSuchMethodException e)
            {
               // Ignore - just go on with other types of methods
            }
            
            // Implemented by container (in both cases)
            if (methodName.startsWith("find"))
            {
               homeMapping.put(m[i], this.getClass().getMethod(finderName, new Class[] { Invocation.class }));
            }
            else if (methodName.equals("create") ||
                  (isEJB1x == false && methodName.startsWith("create")))
            {
               homeMapping.put(m[i], this.getClass().getMethod("create"+append, new Class[] { Invocation.class }));
               beanMapping.put(m[i], this.getClass().getMethod("postCreate"+append, new Class[] { Invocation.class }));
            }
            else
            {
               homeMapping.put(m[i], this.getClass().getMethod(methodName+append, new Class[] { Invocation.class }));
            }
         } catch (NoSuchMethodException e)
         {
            throw new DeploymentException("Could not find matching method for "+m[i]);
         }
      }
   }
   
   protected void setupHomeMapping()
   throws DeploymentException
   {
      try {
         if (homeInterface != null)
         {
            Method[] m = homeInterface.getMethods();
            setupHomeMappingImpl( m, "find", "Home" );
         }
         if (localHomeInterface != null)
         {
            Method[] m = localHomeInterface.getMethods();
            setupHomeMappingImpl( m, "findLocal", "LocalHome" );
         }
      
         // Special methods
      
         // Get the One on Handle (getEJBObject), get the class
         Class handleClass = Class.forName("javax.ejb.Handle");
         
         // Get the methods (there is only one)
         Method[] handleMethods = handleClass.getMethods();
         
         //Just to make sure let's iterate
         for (int j=0; j<handleMethods.length ;j++)
         {
            
            try
            {
               
               //Get only the one called handle.getEJBObject
               if (handleMethods[j].getName().equals("getEJBObject"))
               {
                  
                  //Map it in the home stuff
                  homeMapping.put(handleMethods[j], this.getClass().getMethod("getEJBObject", new Class[]
                        {Invocation.class
                        }));
               }
            }
            catch (NoSuchMethodException e)
            {
               throw new DeploymentException("Couldn't find getEJBObject method on container");
            }
         }
      }
      catch (Exception e)
      {
         // ditch the half built mappings
         homeMapping.clear();
         beanMapping.clear();

         // DEBUG Logger.exception(e);

         if(e instanceof DeploymentException) {
            throw (DeploymentException)e;
         }
         throw new DeploymentException("Error setting up home mapping", e);
      }
   }
   
   private void setupBeanMappingImpl( Method[] m, String intfName )
   throws DeploymentException
   {
      for (int i = 0; i < m.length; i++)
      {
         try
         {
            if (!m[i].getDeclaringClass().getName().equals(intfName))
            {
               // Implemented by bean
               beanMapping.put(m[i], beanClass.getMethod(m[i].getName(), m[i].getParameterTypes()));
            }
            else
            {
               // Implemented by container
               beanMapping.put(m[i], getClass().getMethod(m[i].getName(), new Class[]
                     { Invocation.class
                     }));
            }
         } catch (NoSuchMethodException e)
         {
            throw new DeploymentException("Could not find matching method for "+m[i], e);
         }
      }
   }
   
   protected void setupBeanMapping()
   throws DeploymentException
   {
      try {
         if (remoteInterface != null)
         {
            Method[] m = remoteInterface.getMethods();
            setupBeanMappingImpl( m, "javax.ejb.EJBObject" );
         }
         if (localInterface != null)
         {
            Method[] m = localInterface.getMethods();
            setupBeanMappingImpl( m, "javax.ejb.EJBLocalObject" );
         }
      }
      catch (Exception e)
      {
         // ditch the half built mappings
         homeMapping.clear();
         beanMapping.clear();

         if(e instanceof DeploymentException) {
            throw (DeploymentException)e;
         }
         throw new DeploymentException("Error setting up bean mapping", e);
      }
   }
   
   protected void setupMarshalledInvocationMapping() 
   {
      try 
      {// Create method mappings for container invoker
         if (homeInterface != null)
         {
            Method [] m = homeInterface.getMethods();
            for (int i = 0 ; i<m.length ; i++)
            {
               marshalledInvocationMapping.put( new Long(MarshalledInvocation.calculateHash(m[i])), m[i]);
            }
         }
         if (remoteInterface != null)
         {
            Method [] m = remoteInterface.getMethods();
            for (int j = 0 ; j<m.length ; j++)
            {
               marshalledInvocationMapping.put( new Long(MarshalledInvocation.calculateHash(m[j])), m[j]);
            }
         }
         
         // Get the getEJBObjectMethod
         Method getEJBObjectMethod = Class.forName("javax.ejb.Handle").getMethod("getEJBObject", new Class[0]);
         
         // Hash it
         marshalledInvocationMapping.put(new Long(MarshalledInvocation.calculateHash(getEJBObjectMethod)),getEJBObjectMethod);
      }
      catch (Exception e)
      {
         log.error("getEJBObject failed", e);
      }
   }
   
   /**
   * Build the container MBean information on attributes, contstructors,
   * operations, and notifications. Currently there are no attributes, no
   * constructors, no notifications, and the following ops:
   * <ul>
   * <li>'home' -> invokeHome(Invocation);</li>
   * <li>'remote' -> invoke(Invocation);</li>
   * <li>'localHome' -> not implemented;</li>
   * <li>'local' -> not implemented;</li>
   * <li>'getHome' -> return EBJHome interface;</li>
   * <li>'getRemote' -> return EJBObject interface</li>
   * <li>'getCacheSize' -> return the entity's cache size</li>
   * <li>'flushCache' -> flush the entity's cache</li>
   * </ul>
   */
   public MBeanInfo getMBeanInfo()
   {
      MBeanParameterInfo miInfo = new MBeanParameterInfo("method", Invocation.class.getName(), "Invocation data");
      MBeanConstructorInfo[] ctorInfo = null;

      MBeanParameterInfo[] miInfoParams = new MBeanParameterInfo[] {new MBeanParameterInfo("method", Invocation.class.getName(), "Invocation data")};
      MBeanParameterInfo[] noParams = new MBeanParameterInfo[] {};

      MBeanInfo superInfo = super.getMBeanInfo();
      int superOpInfoCount = superInfo.getOperations().length;
      MBeanOperationInfo[] opInfo = new MBeanOperationInfo[superOpInfoCount +2];
      System.arraycopy(superInfo.getOperations(), 0, opInfo, 0, superOpInfoCount);
      opInfo[superOpInfoCount] = 
         new MBeanOperationInfo("getCacheSize", "Get the Container cache size.",
                                noParams,
                                "java.lang.Integer", MBeanOperationInfo.INFO);
      opInfo[superOpInfoCount + 1] = 
         new MBeanOperationInfo("flushCache", "Flush the Container cache.",
            noParams,
            "void", MBeanOperationInfo.ACTION);
      return new MBeanInfo(getClass().getName(), 
                           "EJB Entity Container MBean",
                           superInfo.getAttributes(),
                           superInfo.getConstructors(),                           opInfo,
                           superInfo.getNotifications());
   }
   
   /**
   * Handle a operation invocation.
   */
   public Object invoke(String actionName, Object[] params, String[] signature)
   throws MBeanException, ReflectionException
   {
      
      
      if( params != null && params.length == 1 && (params[0] instanceof Invocation) == false )
         throw new MBeanException(new IllegalArgumentException("Expected zero or single Invocation argument"));
      
      Object value = null;
      Invocation mi = null;
      if( params != null && params.length == 1 )
         mi = (Invocation) params[0];
    
      // marcf: FIXME: these should be exposed on the cache
      
      // Check against home, remote, localHome, local, getHome, getRemote, getLocalHome, getLocal
      if (actionName.equals("getCacheSize")) {
         return new Integer(((EntityCache)instanceCache).getCacheSize());
      }
      else if (actionName.equals("flushCache")) {
         log.info("flushing cache");
         ((EntityCache)instanceCache).flush();
         return null;
      }
      
      else return super.invoke(actionName, params, signature);
   }
   
   
   
   Interceptor createContainerInterceptor()
   {
      return new ContainerInterceptor();
   }
   
   // Inner classes -------------------------------------------------
   
   // This is the last step before invocation - all interceptors are done
   
   class ContainerInterceptor
   implements Interceptor
   {
      public void setContainer(Container con)
      {
      }
      
      public void setNext(Interceptor interceptor)
      {
      }
      
      public Interceptor getNext()
      {
         return null;
      }
      
      public void create()
      {
      }
      
      public void start()
      {
      }
      
      public void stop()
      {
      }
      
      public void destroy()
      {
      }
      
      public Object invokeHome(Invocation mi)
      throws Exception
      {
         // Invoke and handle exceptions
         Method miMethod = mi.getMethod();
         Method m = (Method) homeMapping.get(miMethod);
         if( m == null )
         {
            throw new EJBException("Invalid invocation, check your deployment packaging, method="+miMethod);
         }

         if (m.getDeclaringClass().equals(EntityContainer.class))
         {
            try
            {
               return m.invoke(EntityContainer.this, new Object[] { mi });
            } catch (IllegalAccessException e)
            {
               // Throw this as a bean exception...(?)
               throw new EJBException(e);
            } catch (InvocationTargetException e)
            {
               Throwable ex = e.getTargetException();
               if (ex instanceof EJBException)
                  throw (Exception)ex;
               else if (ex instanceof Exception)
                  throw (Exception)ex;
               else
                  throw (Error)ex;
            }
         } else // Home method
         {
            try
            {
               return m.invoke(((EnterpriseContext) mi.getEnterpriseContext()).getInstance(), mi.getArguments());
            } catch (IllegalAccessException e)
            {
               // Throw this as a bean exception...(?)
               throw new EJBException(e);
            } catch (InvocationTargetException e)
            {
               Throwable ex = e.getTargetException();
               if (ex instanceof EJBException)
                  throw (Exception)ex;
               else if (ex instanceof Exception)
                  throw (Exception)ex;
               else
                  throw (Error)ex;
            }
         }
      }
      
      public Object invoke(Invocation mi)
      throws Exception
      {
         // Get method
         Method miMethod = mi.getMethod();
         Method m = (Method) beanMapping.get(miMethod);
         if( m == null )
         {
            throw new EJBException("Invalid invocation, check your deployment packaging, method="+miMethod);
         }

         // Select instance to invoke (container or bean)
         if (m.getDeclaringClass().equals(EntityContainer.class))
         {
            // Invoke and handle exceptions
            try
            {
               return m.invoke(EntityContainer.this, new Object[]{ mi });
            } catch (IllegalAccessException e)
            {
               // Throw this as a bean exception...(?)
               throw new EJBException(e);
            } catch (InvocationTargetException e)
            {
               Throwable ex = e.getTargetException();
               if (ex instanceof EJBException)
                  throw (EJBException)ex;
               else if (ex instanceof Exception)
                  throw (Exception)ex;
               else
                  throw (Error)ex;
            }
         }
         else
         {
            // Invoke and handle exceptions
            try
            {
               return m.invoke(((EnterpriseContext) mi.getEnterpriseContext()).getInstance(), mi.getArguments());
            } catch (IllegalAccessException e)
            {
               // Throw this as a bean exception...(?)
               throw new EJBException(e);
            } catch (InvocationTargetException e)
            {
               Throwable ex = e.getTargetException();
               if (ex instanceof EJBException)
                  throw (EJBException)ex;
               else if (ex instanceof Exception)
                  throw (Exception)ex;
               else
                  throw (Error)ex;
            }
         }
      }
   }
}
