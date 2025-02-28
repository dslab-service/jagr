/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.metadata;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import org.jboss.deployment.DeploymentException;
import org.jboss.util.jmx.ObjectNameFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
/** The configuration information for an EJB container.
 *   @author <a href="mailto:sebastien.alborini@m4x.org">Sebastien Alborini</a>
 *   @author <a href="mailto:scott.stark@jboss.org">Scott Stark</a>
 *   @version $Revision: 1.1.1.1 $
 */
public class ConfigurationMetaData extends MetaData
{
   // Constants -----------------------------------------------------
   public static final String CMP_2x_13 = "Standard CMP 2.x EntityBean";
   public static final String CMP_1x_13 = "Standard CMP EntityBean";
   public static final String BMP_13 = "Standard BMP EntityBean";
   public static final String STATELESS_13 = "Standard Stateless SessionBean";
   public static final String STATEFUL_13 = "Standard Stateful SessionBean";
   public static final String MESSAGE_DRIVEN_13 = "Standard Message Driven Bean";
   public static final String CLUSTERED_STATELESS_13 = "Clustered Stateless SessionBean"; // we do not support JDK < 1.3
   public static final String CLUSTERED_STATEFUL_13 = "Clustered Stateful SessionBean"; // we do not support JDK < 1.3
   public static final String CLUSTERED_CMP_2x_13 = "Clustered CMP 2.x EntityBean"; // we do not support JDK < 1.3
   public static final String CLUSTERED_CMP_1x_13 = "Clustered CMP EntityBean"; // we do not support JDK < 1.3
   public static final String CLUSTERED_BMP_13 = "Clustered BMP EntityBean"; // we do not support JDK < 1.3

   public static final String IIOP_CMP_2x_13 = "IIOP CMP 2.x EntityBean";
   public static final String IIOP_CMP_1x_13 = "IIOP CMP EntityBean";
   public static final String IIOP_BMP_13 = "IIOP BMP EntityBean";
   public static final String IIOP_STATELESS_13 = "IIOP Stateless SessionBean";
   public static final String IIOP_STATEFUL_13 = "IIOP Stateful SessionBean";
   public static final byte A_COMMIT_OPTION = 0;
   public static final byte B_COMMIT_OPTION = 1;
   public static final byte C_COMMIT_OPTION = 2;
   /** D_COMMIT_OPTION is a lazy load option. By default it synchronizes every 30 seconds */
   public static final byte D_COMMIT_OPTION = 3;
   public static final String[] commitOptionStrings = { "A", "B", "C", "D"};
   /** The default invoker JMX names */
   public static final String DEFAULT_HOME_INVOKER = "jboss:service=invoker,type=jrmp";
   public static final String DEFAULT_BEAN_INVOKER = "jboss:service=invoker,type=jrmp";
   public static final String DEFAULT_CLUSTERED_HOME_INVOKER = "jboss:service=invoker,type=jrmpha";
   public static final String DEFAULT_CLUSTERED_BEAN_INVOKER = "jboss:service=invoker,type=jrmpha";

   // Attributes ----------------------------------------------------
   private String name;
   private String containerInvoker;
   private String instancePool;
   private String instanceCache;
   private String persistenceManager;
   private String transactionManager;
   private String webClassLoader = "org.jboss.web.WebClassLoader";
   // This is to provide backward compatibility with 2.4 series jboss.xml
   // but it should come from standardjboss alone
   // marcf:FIXME deprecate the "hardcoded string"
   private String lockClass = "org.jboss.ejb.plugins.lock.QueuedPessimisticEJBLock";
   private byte commitOption;
   private long optionDRefreshRate = 30000;
   private boolean callLogging;
   private boolean syncOnCommitOnly = false;
   private String securityDomain;
   private String authenticationModule;
   private String roleMappingManager;
    /** The default invokers to use for this container */
   private String homeInvoker = null;
   private String beanInvoker = null;

   private Element containerInvokerConf;
   private Element containerPoolConf;
   private Element containerCacheConf;
   private Element containerInterceptorsConf;
   private Element clientInterceptors;
   private HashMap clientInterceptorConfs = new HashMap();
   /** */
   private Collection depends = new LinkedList();
   /** The cluster-config element info */
   private ClusterConfigMetaData clusterConfig = null;
   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------
   public ConfigurationMetaData (String name)
   {
      this.name = name;
   }
   // Public --------------------------------------------------------

   public String getName() { return name; }
   public String getContainerInvoker() { return containerInvoker; }
   public String getInstancePool() { return instancePool; }
   public String getInstanceCache() { return instanceCache; }
   public String getPersistenceManager() { return persistenceManager; }
   public String getSecurityDomain() { return securityDomain; }
   public String getAuthenticationModule() { return authenticationModule; }
   public String getRoleMappingManager() { return roleMappingManager; }
   public String getTransactionManager() { return transactionManager; }
   public String getWebClassLoader() { return webClassLoader; }
   public String getLockClass() {return lockClass;}
   public String getHomeInvoker(boolean clustered)
   {
      if (homeInvoker != null)
         return homeInvoker;
      else if( clustered )
         return DEFAULT_CLUSTERED_HOME_INVOKER;
      else
         return DEFAULT_HOME_INVOKER;
   }
   public String getBeanInvoker(boolean clustered)
   {
      if (beanInvoker != null)
         return beanInvoker;
      else if( clustered )
         return DEFAULT_CLUSTERED_BEAN_INVOKER;
      else
         return DEFAULT_BEAN_INVOKER;
   }
   public Element getContainerInvokerConf() { return containerInvokerConf; }

   public Element getContainerPoolConf() { return containerPoolConf; }
   public Element getContainerCacheConf() { return containerCacheConf; }
   public Element getContainerInterceptorsConf() { return containerInterceptorsConf; }
   public Element getClientInterceptorConf(String interceptorName)
   {
      return (Element)clientInterceptorConfs.get(interceptorName);
   }
   public ClusterConfigMetaData getClusterConfigMetaData()
   {
	   return this.clusterConfig;
   }
   public boolean getCallLogging() { return callLogging; }
   public boolean getSyncOnCommitOnly() { return syncOnCommitOnly; }

   public byte getCommitOption() { return commitOption; }
   public long getOptionDRefreshRate() { return optionDRefreshRate; }
   public Collection getDepends()
   {
      return depends;
   }
   public void importJbossXml(Element element) throws DeploymentException
   {
      // everything is optional to allow jboss.xml to modify part of a configuration
      // defined in standardjboss.xml
      // set call logging
      callLogging = Boolean.valueOf(getElementContent(getOptionalChild(element, "call-logging"), String.valueOf(callLogging))).booleanValue();
      // set synchronize on commit only
      syncOnCommitOnly = Boolean.valueOf(getElementContent(getOptionalChild(element, "sync-on-commit-only"), String.valueOf(syncOnCommitOnly))).booleanValue();
   
      // set the container invoker
      containerInvoker = getElementContent(getOptionalChild(element, "container-invoker"), containerInvoker);
      // set the instance pool
      instancePool = getElementContent(getOptionalChild(element, "instance-pool"), instancePool);
      // set the instance cache
      instanceCache = getElementContent(getOptionalChild(element, "instance-cache"), instanceCache);
      // set the persistence manager
      persistenceManager = getElementContent(getOptionalChild(element, "persistence-manager"), persistenceManager);
      // set the transaction manager
      transactionManager = getElementContent(getOptionalChild(element, "transaction-manager"), transactionManager);
      // set the web classloader
      webClassLoader = getElementContent(getOptionalChild(element, "web-class-loader"), webClassLoader);
      // set the lock class
      lockClass = getElementContent(getOptionalChild(element, "locking-policy"), lockClass);
      // set the security domain
      securityDomain = getElementContent(getOptionalChild(element, "security-domain"), securityDomain);
      // set the authentication module
      authenticationModule = getElementContent(getOptionalChild(element, "authentication-module"), authenticationModule);
      // set the role mapping manager
      roleMappingManager = getElementContent(getOptionalChild(element, "role-mapping-manager"), roleMappingManager);
      /* If the authenticationModule == roleMappingManager just set the securityDomain
         as this is the combination of the authentication-module and role-mapping-manager
         behaviors
      */
      if( authenticationModule != null && roleMappingManager != null &&
          roleMappingManager.equals(authenticationModule) )
      {
         securityDomain = authenticationModule;
         authenticationModule = null;
         roleMappingManager = null;
      }
      // Don't allow only a authentication-module or role-mapping-manager
      else if( (authenticationModule == null && roleMappingManager != null)
               || (authenticationModule != null && roleMappingManager == null) )
      {
         String msg = "Either a security-domain or both authentication-module "
            + "and role-mapping-manager must be specified";
         throw new DeploymentException(msg);
      }

      // set the commit option
      String commit = getElementContent(getOptionalChild(element, "commit-option"), commitOptionToString(commitOption));
      commitOption = stringToCommitOption(commit);
      //get the refresh rate for option D
      String refresh = getElementContent(getOptionalChild(element, "optiond-refresh-rate"), Long.toString(optionDRefreshRate));
      optionDRefreshRate = stringToRefreshRate(refresh);
      // the classes which can understand the following are dynamically loaded during deployment :
      // We save the Elements for them to use later
      // The configuration for the container interceptors
      containerInterceptorsConf = getOptionalChild(element, "container-interceptors", containerInterceptorsConf);
      clientInterceptors = getOptionalChild(element, "client-interceptors", clientInterceptors);
      if (clientInterceptors != null)
      {
         NodeList children = clientInterceptors.getChildNodes();
         for (int i = 0; i < children.getLength(); i++)
         {
            Node currentChild = children.item(i);
            if (currentChild.getNodeType() == Node.ELEMENT_NODE)
            {
               Element interceptor = (Element)children.item(i);
               clientInterceptorConfs.put(interceptor.getTagName(), interceptor);
            }
         }
      }
      // configuration for container invoker
      containerInvokerConf = getOptionalChild(element, "container-invoker-conf", containerInvokerConf);
      // configuration for instance pool
      containerPoolConf = getOptionalChild(element, "container-pool-conf", containerPoolConf);
      // configuration for instance cache
      containerCacheConf = getOptionalChild(element, "container-cache-conf", containerCacheConf);

      //Get depends object names
      for (Iterator dependsElements = getChildrenByTagName(element, "depends"); dependsElements.hasNext();)
      {
         Element dependsElement = (Element)dependsElements.next();
         String dependsName = getElementContent(dependsElement);
         depends.add(ObjectNameFactory.create(dependsName));
      } // end of for ()
      // DEPRECATED: Remove this in JBoss 4.0
      if (containerInvoker.equals("org.jboss.ejb.plugins.jrmp12.server.JRMPContainerInvoker") ||
          containerInvoker.equals("org.jboss.ejb.plugins.jrmp13.server.JRMPContainerInvoker"))
      {
         System.out.println("Deprecated container invoker. Change to org.jboss.ejb.plugins.jrmp.server.JRMPContainerInvoker");
         containerInvoker = "org.jboss.ejb.plugins.jrmp.server.JRMPContainerInvoker";
      }

      // Check for container specified invokers
      homeInvoker = getElementContent(getOptionalChild(element, "home-invoker"), homeInvoker);
      beanInvoker = getElementContent(getOptionalChild(element, "bean-invoker"), beanInvoker);
      // Check for clustering configuration
      Element clusterConfigElement = getOptionalChild(element, "cluster-config");
      if (clusterConfigElement != null)
      {
		  this.clusterConfig = new ClusterConfigMetaData();
		  clusterConfig.importJbossXml(clusterConfigElement);
      }
   }
   // Package protected ---------------------------------------------
   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------
   private static String commitOptionToString(byte commitOption)
      throws DeploymentException
   {
      try
      {
         return commitOptionStrings[commitOption];
      } catch( ArrayIndexOutOfBoundsException e )
      {
         throw new DeploymentException("Invalid commit option: " + commitOption);
      }
   }
   private static byte stringToCommitOption(String commitOption)
      throws DeploymentException
   {
      for( byte i=0; i<commitOptionStrings.length; ++i )
         if( commitOptionStrings[i].equals(commitOption) )
            return i;
      throw new DeploymentException("Invalid commit option: '" + commitOption + "'");
   }
   private static long stringToRefreshRate(String refreshRate) throws DeploymentException
   {
      try
      {
         long rate = Long.parseLong(refreshRate);
         // Convert from seconds to milliseconds
         rate *= 1000;
         return rate;
      } catch ( Exception e)
      {
         throw new DeploymentException("Invalid optiond-refresh-rate \"" + refreshRate + "\". Should be a number");
      }
   }
   // Inner classes -------------------------------------------------
}
