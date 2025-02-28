/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.management.j2ee;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.AttributeChangeNotification;
import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.jboss.logging.Logger;
import org.jboss.system.ServiceMBean;

/**
 * Root class of the JBoss JSR-77 implementation of
 * {@link javax.management.j2ee.JDBCResource JDBCResource}.
 *
 * @author  <a href="mailto:andreas@jboss.org">Andreas Schaefer</a>.
 * @version $Revision: 1.3 $
 *   
 * <p><b>Revisions:</b>
 *
 * <p><b>20011126 Andreas Schaefer:</b>
 * <ul>
 * <li> Creation
 * </ul>
 *
 * @todo This resource should not implement state manageable because it
 *       has no MBean/Service associated but codes stays.
 *
 * @jmx:mbean extends="org.jboss.management.j2ee.StateManageable,org.jboss.management.j2ee.J2EEResourceMBean"
 **/
public class JDBCResource
   extends J2EEResource
   implements JDBCResourceMBean
{
   // Constants -----------------------------------------------------
   
   public static final String J2EE_TYPE = "JDBCResource";
   
   // Attributes ----------------------------------------------------
   
   private StateManagement mState;
   private ObjectName mService;
   
   private List mDataSources = new ArrayList();
   
   // Static --------------------------------------------------------
   
   public static ObjectName create( MBeanServer pServer, String pName ) {
      Logger lLog = Logger.getLogger( JDBCResource.class );
      if( !J2EEServer.sIsActive ) {
         return null;
      }
      ObjectName lServer = null;
      try {
         lServer = (ObjectName) pServer.queryNames(
            new ObjectName(
               J2EEManagedObject.getDomainName() + ":" +
               J2EEManagedObject.TYPE + "=" + J2EEServer.J2EE_TYPE + "," +
               "*"
            ),
            null
         ).iterator().next();
      }
      catch( Exception e ) {
//AS         lLog.error( "Could not find parent J2EEServer", e );
         return null;
      }
      try {
         // Now create the JDBC Representant
         return pServer.createMBean(
            "org.jboss.management.j2ee.JDBCResource",
            null,
            new Object[] {
               pName,
               lServer
            },
            new String[] {
               String.class.getName(),
               ObjectName.class.getName()
            }
         ).getObjectName();
      }
      catch( Exception e ) {
//AS         lLog.error( "Could not create JSR-77 JDBC Manager", e );
         return null;
      }
   }
   
   public static void destroy( MBeanServer pServer, String pName ) {
      Logger lLog = Logger.getLogger( JDBCResource.class );
      if( !J2EEServer.sIsActive ) {
         return;
      }
      try {
         // Find the Object to be destroyed
         ObjectName lSearch = new ObjectName(
            J2EEManagedObject.getDomainName() + ":" +
            J2EEManagedObject.TYPE + "=" + JDBCResource.J2EE_TYPE + "," +
            "name=" + pName + "," +
            "*"
         );
         Set lNames = pServer.queryNames(
            lSearch,
            null
         );
         if( !lNames.isEmpty() ) {
            ObjectName lJDBCResource = (ObjectName) lNames.iterator().next();
            // Now check if the JDBCResource Manager does not contains another DataSources
            ObjectName[] lDataSources = (ObjectName[]) pServer.getAttribute(
               lJDBCResource,
               "JdbcDataSources"
            );
            if( lDataSources.length == 0 ) {
               // Remove it because it does not reference any JDBC DataSources
               pServer.unregisterMBean( lJDBCResource );
            }
         }
      }
      catch( Exception e ) {
//AS       lLog.error( "Could not destroy JSR-77 JDBC Manager", e );
      }
   }
   
   // Constructors --------------------------------------------------
   
   /**
    * @param pName Name of the JDBCResource
    *
    * @throws InvalidParameterException If list of nodes or ports was null or empty
    **/
   public JDBCResource( String pName, ObjectName pServer )
      throws
         MalformedObjectNameException,
         InvalidParentException
   {
      super( J2EE_TYPE, pName, pServer );
      mState = new StateManagement( this );
   }
   
   // Public --------------------------------------------------------
   
   // javax.managment.j2ee.EventProvider implementation -------------
   
   public String[] getEventTypes() {
      return StateManagement.sTypes;
   }
   
   public String getEventType( int pIndex ) {
      if( pIndex >= 0 && pIndex < StateManagement.sTypes.length ) {
         return StateManagement.sTypes[ pIndex ];
      } else {
         return null;
      }
   }
   
   // javax.management.j2ee.StateManageable implementation ----------
   
   public long getStartTime() {
      return mState.getStartTime();
   }
   
   public int getState() {
      return mState.getState();
   }
   
   public void mejbStart() {
      // No component behind therefore just do it as it is started
      mState.setState( ServiceMBean.STARTING + 2 );
      mState.setState( ServiceMBean.STARTED + 2 );
   }
   
   public void mejbStartRecursive() {
      mState.setState( ServiceMBean.STOPPING + 2 );
      Iterator i = mDataSources.iterator();
      ObjectName lDataSource = null;
      while( i.hasNext() ) {
         lDataSource = (ObjectName) i.next();
         try {
            getServer().invoke(
               lDataSource,
               "mejbStart",
               new Object[] {},
               new String[] {}
            );
         }
         catch( JMException jme ) {
            getLog().error( "Could not start JSR-77 JDBC-DataSource: " + lDataSource, jme );
         }
         // CANDEA start
         catch( Exception e ) {
            throw new RuntimeException( "<<< RR-specific (JDBCResource.mejbStartRecursive) >>> " + e.toString() );
         }
         // CANDEA end
      }
      mState.setState( ServiceMBean.STOPPED + 2 );
   }
   
   public void mejbStop() {
      // No component behind therefore just do it as it is started
      mState.setState( 3 );
      Iterator i = mDataSources.iterator();
      while( i.hasNext() ) {
         ObjectName lDataSource = (ObjectName) i.next();
         try {
            getServer().invoke(
               lDataSource,
               "mejbStop",
               new Object[] {},
               new String[] {}
            );
         }
         catch( JMException jme ) {
            getLog().error( "Could not stop JSR-77 JDBC-DataSource: " + lDataSource, jme );
         }
         // CANDEA start
         catch( Exception e ) {
            throw new RuntimeException( "<<< RR-specific (JDBCResource.mejbStop) >>> " + e.toString() );
         }
         // CANDEA end
      }
      // No component behind therefore just do it as it is started
      mState.setState( 2 );
   }
   
   /**
    * @todo Listener cannot be used right now because there is no MBean associated
    *       to it and therefore no state management possible but currently it stays
    *       StateManageable to save the code.
    **/
   public void postCreation() {
/*AS 
      try {
         mListener = new Listener();
         getServer().addNotificationListener( mService, mListener, null, null );
      }
      catch( JMException jme ) {
         //AS ToDo: later on we have to define what happens when service is null or
         //AS ToDo: not found.
         getLog().error( "Could not add listener at target service", jme );
      }
*/
      sendNotification(
         new Notification(
            StateManagement.sTypes[ 0 ],
            getName(),
            1,
            System.currentTimeMillis(),
            "JDBC Resource created"
         )
      );
   }
   
   /**
    * @todo Listener cannot be used right now because there is no MBean associated
    *       to it and therefore no state management possible but currently it stays
    *       StateManageable to save the code.
    **/
   public void preDestruction() {
      sendNotification(
         new Notification(
            StateManagement.sTypes[ 1 ],
            getName(),
            1,
            System.currentTimeMillis(),
            "JDBC Resource deleted"
         )
      );
/*AS
      // Remove the listener of the target MBean
      try {
         getServer().removeNotificationListener( mService, mListener );
      }
      catch( JMException jme ) {
         // When the service is not available anymore then just ignore the exception
      }
*/
   }
   
   // javax.management.j2ee.JDBCResource implementation ---------------------
   
   /**
    * @jmx:managed-attribute
    **/
   public ObjectName[] getJdbcDataSources() {
      return (ObjectName[]) mDataSources.toArray( new ObjectName[ mDataSources.size() ] );
   }
   
   /**
    * @jmx:managed-operation
    **/
   public ObjectName getJdbcDataSource( int pIndex ) {
      if( pIndex >= 0 && pIndex < mDataSources.size() ) {
         return (ObjectName) mDataSources.get( pIndex );
      }
      else {
         return null;
      }
   }
   
   // J2EEManagedObjectMBean implementation -------------------------
   
   public void addChild( ObjectName pChild ) {
      String lType = J2EEManagedObject.getType( pChild );
      if( JDBCDataSource.J2EE_TYPE.equals( lType ) ) {
         mDataSources.add( pChild );
      }
   }
   
   public void removeChild( ObjectName pChild ) {
      String lType = J2EEManagedObject.getType( pChild );
      if( JDBCDataSource.J2EE_TYPE.equals( lType ) ) {
         mDataSources.remove( pChild );
      }
   }

   // java.lang.Object overrides ------------------------------------
   
   public String toString() {
      return "JDBCResource { " + super.toString() + " } [ " +
         "Datasources: " + mDataSources +
         " ]";
   }

   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------
}
