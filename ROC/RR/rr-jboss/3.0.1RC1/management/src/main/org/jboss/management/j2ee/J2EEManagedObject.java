/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.management.j2ee;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.Hashtable;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.logging.Logger;
import org.jboss.system.ServiceMBeanSupport;

/**
 * Root class of the JBoss JSR-77 implementation of
 * {@link javax.management.j2ee.J2EEManagedObject J2EEManagedObject}.
 *
 * @author  <a href="mailto:andreas@jboss.org">Andreas Schaefer</a>.
 * @version $Revision: 1.3 $
 *   
 * <p><b>Revisions:</b>
 *
 * <p><b>20011123 Andreas Schaefer:</b>
 * <ul>
 * <li> Adjustments to the JBoss Guidelines as well as adding some
 *      static helper methods
 * </ul>
 *
 * @jmx:mbean
 **/
public abstract class J2EEManagedObject
   extends ServiceMBeanSupport
   implements J2EEManagedObjectMBean, Serializable
{
   
   // Constants -----------------------------------------------------
   
   public static final String TYPE = "j2eeType";
   public static final String NAME = "name";
   
   // Attributes ----------------------------------------------------
   
   /** Class logger. */
   private static final Logger log =
      Logger.getLogger(J2EEManagedObject.class);
   
   private ObjectName mParent = null;
   private ObjectName mName = null;
   
   // Static --------------------------------------------------------
   
   private static String sDomainName = null;
   
   public static String getDomainName() {
      return sDomainName;
   }
   
   /**
    * Retrieves the type out of an JSR-77 object name
    *
    * @param pName Object Name to check if null then
    *              it will be treated like NO type found
    *
    * @return The type of the given Object Name or an EMPTY
    *         string if either Object Name null or type not found
    **/
   protected static String getType( ObjectName pName ) {
      String lType = null;
      if( pName != null ) {
         lType = (String) pName.getKeyPropertyList().get( TYPE );
      }
      // Return an empty string if type not found
      return lType == null ? "" : lType;
   }
   
   protected static void removeObject( MBeanServer pServer, String pSearchCriteria )
      throws JMException
   {
      Set lNames = pServer.queryNames(
         new ObjectName( pSearchCriteria ),
         null
      );
      if( !lNames.isEmpty() ) {
         pServer.unregisterMBean( (ObjectName) lNames.iterator().next() );
      }
   }
   
   // Constructors --------------------------------------------------
   
   /**
   * Constructor for the root J2EEDomain object
   *
   * @param pDomainName Name of the domain
   * @param pType Type of the Managed Object which must be defined
   * @param pName Name of the Managed Object which must be defined
   *
   * @throws InvalidParameterException If the given Domain Name, Type or Name is null
   **/
   public J2EEManagedObject( String pDomainName, String pType, String pName )
      throws
         MalformedObjectNameException
   {
      if( pDomainName == null ) {
         throw new InvalidParameterException( "Domain Name must be set" );
      }
      sDomainName = pDomainName;
      Hashtable lProperties = new Hashtable();
      lProperties.put( TYPE, pType );
      lProperties.put( NAME, pName );
      mName = new ObjectName( getDomainName(), lProperties );

      log.debug("create root with name: " + mName );
   }
   
   /**
   * Constructor for any Managed Object except the root J2EEMangement.
   *
   * @param pType Type of the Managed Object which must be defined
   * @param pName Name of the Managed Object which must be defined
   * @param pParent Object Name of the parent of this Managed Object
   *                which must be defined
   *
   * @throws InvalidParameterException If the given Type, Name or Parent is null
   **/
   public J2EEManagedObject( String pType, String pName, ObjectName pParent )
      throws
         MalformedObjectNameException,
         InvalidParentException
   {
      Hashtable lProperties = getParentKeys( pParent );
      lProperties.put( TYPE, pType );
      lProperties.put( NAME, pName );
      mName = new ObjectName( getDomainName(), lProperties );
      setParent( pParent );
   }
   
   // Public --------------------------------------------------------
   
   // J2EEManagedObjectMBean implementation ----------------------------------------------
   
   /**
    * @jmx:managed-attribute
    **/
   public ObjectName getObjectName() {
      log.debug("getObjectName(), name: " + mName );
      return mName;
   }

   /**
    * @jmx:managed-attribute
    **/
   public ObjectName getParent() {
      return mParent;
   }
   
   /**
    * @jmx:managed-attribute
    **/
   public void setParent( ObjectName pParent )
      throws
         InvalidParentException
   {
      if( pParent == null ) {
         throw new InvalidParameterException( "Parent must be set" );
      }
      mParent = pParent;
   }
   
   /**
    * @jmx:managed-operation
    **/
   public void addChild( ObjectName pChild ) {
      //AS ToDo: Remove later is just here to compile
   }
   
   /**
    * @jmx:managed-operation
    **/
   public void removeChild( ObjectName pChild ) {
      //AS ToDo: Remove later is just here to compile
   }

   // J2EEManagedObject implementation ----------------------------------------------
   
   /**
    * @jmx:managed-attribute
    **/
   public boolean isStateManageable() {
      return this instanceof StateManageable;
   }

   /**
    * @jmx:managed-attribute
    **/
   public boolean isStatisticsProvider() {
      return this instanceof StatisticsProvider;
   }
   
   /**
    * @jmx:managed-attribute
    **/
   public boolean isEventProvider() {
      return this instanceof EventProvider;
   }
   
   // ServiceMBeanSupport overrides ---------------------------------------------------
   
   public ObjectName getObjectName( MBeanServer pServer, ObjectName pName ) {
      return getObjectName();
   }
   
   /**
    * Last steps to be done after MBean is registered on MBeanServer. This
    * method is made final because it contains vital steps mandatory to all
    * J2EEManagedObjects. To perform your own Post-Creation steps please
    * override {@link #postCreation postCreation()} method.
    **/
   public final void postRegister( java.lang.Boolean pRegistrationDone ) {
      try {
      log.debug("postRegister(), parent: " + mParent );
      if( pRegistrationDone.booleanValue() ) {
         // Let the subclass handle post creation steps
         postCreation();
         if( mParent != null ) {
            try {
               // Notify the parent about its new child
               if(mParent.getKeyProperty( "name" ).compareTo(" ") != 0) {
                  getServer().invoke(
                     mParent,
                     "addChild",
                     new Object[] { mName },
                     new String [] { ObjectName.class.getName() }
                  );
               }
               else 
               {
                  ObjectName lServer =  (ObjectName) getServer().queryNames(
                     new ObjectName(
                        J2EEManagedObject.getDomainName() + ":" +
                        J2EEManagedObject.TYPE + "=" + J2EEServer.J2EE_TYPE + "," + 
                        "name=" + mParent.getKeyProperty( J2EEServer.J2EE_TYPE ) + "," +
                        "*"
                     ),
                     null
                  ).iterator().next();
                  getServer().invoke(
                     lServer, 
                     "addChild",
                     new Object[] { mName },
                     new String [] { ObjectName.class.getName() }
                  );
               }
               super.postRegister( pRegistrationDone );
            }
            catch( JMException jme ) {
               // Stop it because of the error
               super.postRegister( new Boolean( false ) );
            }
            // CANDEA start
            catch( Exception e ) {
               throw new RuntimeException( "<<< RR-specific (J2EEManagedObject.postRegister) >>> " + e.toString() );
            }
            // CANDEA end
         }
      }
      }
      catch( RuntimeException re ) {
         log.error( "Caught Runtime Exception is postRegister()", re );
         throw re;
      }
   }
   
   /**
    * Last steps to be done before MBean is unregistered on MBeanServer. This
    * method is made final because it contains vital steps mandatory to all
    * J2EEManagedObjects. To perform your own Pre-Destruction steps please
    * override {@link #preDestruction preDestruction()} method.
    **/
   public final void preDeregister()
      throws Exception
   {
      log.debug("preDeregister(), parent: " + mParent );
      try {
         // Only remove child if it is a child (root has not parent)
         if( mParent != null ) {
            try {
               // Notify the parent about its new child
               getServer().invoke(
                  mParent,
                  "removeChild",
                  new Object[] { mName },
                  new String [] { ObjectName.class.getName() }
               );
            }
            catch( InstanceNotFoundException infe ) {
            }
         }
         // Let the subclass handle pre destruction steps
         preDestruction();
      }
      catch( JMException jme ) {
         // Ignore it
      }
   }
   
   // Object overrides ---------------------------------------------------
   
   public String toString() {
      return "J2EEManagedObject [ name: " + mName + ", parent: " + mParent + " ];";
   }
   
   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   protected void postCreation() {
   }
   
   protected void preDestruction() {
   }
   
   /**
    * This method can be overwritten by any subclass which must
    * return &lt;parent-j2eeType&gt; indicating its parents. By
    * default it returns an empty hashtable instance.
    *
    * @param pParent The direct parent of this class
    *
    * @return An empty hashtable
    **/
   protected Hashtable getParentKeys( ObjectName pParent ) {
      return new Hashtable();
   }
   
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------
}
