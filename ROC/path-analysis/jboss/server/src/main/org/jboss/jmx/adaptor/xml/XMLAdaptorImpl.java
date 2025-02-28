/*
* JBoss, the OpenSource J2EE webOS
*
* Distributable under LGPL license.
* See terms of license at gnu.org.
*/
package org.jboss.jmx.adaptor.xml;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.ObjectName;
import javax.management.ObjectInstance;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.naming.InitialContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
* XML Adaptor Implementation interpreting the XML wrapped JMX commands.
*
* @author Andreas Schaefer (andreas.schaefer@madplanet.com)
* @created June 22, 2001
* @version $Revision: 1.1.1.1 $
*/
public class XMLAdaptorImpl {
  // Constants -----------------------------------------------------

  // Attributes ----------------------------------------------------
  MBeanServer mServer;


  // Static --------------------------------------------------------

  /** Primitive type name -> class map. */
  private static Hashtable mPrimitives = new Hashtable();
  
  /** Setup the primitives map. */
  static {
    mPrimitives.put( "boolean", Boolean.TYPE );
    mPrimitives.put( "byte", Byte.TYPE );
    mPrimitives.put( "short", Short.TYPE );
    mPrimitives.put( "int", Integer.TYPE );
    mPrimitives.put( "long", Long.TYPE );
    mPrimitives.put( "float", Float.TYPE );
    mPrimitives.put( "double", Double.TYPE );
    mPrimitives.put( "char", Character.TYPE );
  }

   // Constructors --------------------------------------------------

  /**
  *  Constructor for the XMLAdaptorImpl object
  *
  *@param pServer MBeanServer this adaptor executes its calls on
  */
  public XMLAdaptorImpl( MBeanServer pServer ) {
    super();
    mServer = pServer;
  }

  // Public --------------------------------------------------------

  /**
  * Performs a set of calls to the MBean Server and returns
  * it return values.
  *
  * @param pJmxOperations Complete XML Document (compliant to the
  *                       XMLAdaptor.dtd) containing a list of calls
  *                       to the MBean Server.
  *
  * @return List of return values, one for each call. If the call doesn't
  *         have a return value of the operation failed it will be null.
  **/
  public Object[] invokeXML( Document pJmxOperations ) {
    Vector lReturns = new Vector();
    NodeList lRoot = pJmxOperations.getChildNodes();
    if( lRoot.getLength() > 0 ) {
      Element lRootElement = (Element) lRoot.item( 0 );
      System.out.println( "XMLAdaptorImpl.invokeXML(), root: " + lRootElement );
      NodeList lOperations = lRootElement.getChildNodes();
      for( int i = 0; i < lOperations.getLength(); i++ ) {
        Element lChildElement = (Element) lOperations.item( i );
        lReturns.add( invokeXML( lChildElement ) );
      }
    }
    return (Object[]) lReturns.toArray( new Object[ 0 ] );
  }

  /**
  * Performs a single call to the MBean Server and return its return value
  *
  * @param pJmxOperation Second level element (compliant to XMLAdaptor.dtd)
  *                      representing one call to the MBean Server
  *
  * @return Return value of the call or null if call failed or doesn't have
  *         a return value.
  **/
  public Object invokeXML( Element pJmxOperation ) {
    if( pJmxOperation == null ) {
      return null;
    }
    // Get the requested operation
    String lTag = pJmxOperation.getTagName();
    System.out.println( "XMLAdaptorImpl.invokeXML(), Tag: " + lTag );
    if( "create-mbean".equals( lTag ) ) {
      return createMBean(
        pJmxOperation.getAttribute( "code" ),
        getObjectName(
          pJmxOperation.getAttribute( "name" ),
          pJmxOperation.getElementsByTagName( "object-name" )
        ),
        pJmxOperation.getElementsByTagName( "constructor" ),
        pJmxOperation.getElementsByTagName( "attribute" )
      );
    }
    else if( "invoke".equals( lTag ) ) {
      // Get the operation, Object Name and attributes and invoke it
      String lOperation = pJmxOperation.getAttribute( "operation" );
      return invoke(
        lOperation,
        getObjectName( pJmxOperation.getElementsByTagName( "object-name" ) ),
        pJmxOperation.getElementsByTagName( "attribute" )
      );
    }
    else if( !"get-attribute".equals( lTag ) ) {
      return get(
        getObjectName( pJmxOperation.getElementsByTagName( "object-name" ) ),
        pJmxOperation.getElementsByTagName( "attribute" )
      );
    }
    else if( !"set-attribute".equals( lTag ) ) {
      return set(
        getObjectName( pJmxOperation.getElementsByTagName( "object-name" ) ),
        pJmxOperation.getElementsByTagName( "attribute" )
      );
    }
    else if( !"mbean-count".equals( lTag ) ) {
      return mServer.getMBeanCount();
    }
    else if( !"mbean-info".equals( lTag ) ) {
      return getMBeanInfo(
        getObjectName( pJmxOperation.getElementsByTagName( "object-name" ) )
      );
    }
    else if( !"object-instance".equals( lTag ) ) {
      return getObjectInstance(
        getObjectName( pJmxOperation.getElementsByTagName( "object-name" ) )
      );
    }
    else if( !"is-instance-of".equals( lTag ) ) {
      return isInstanceOf(
        getObjectName( pJmxOperation.getElementsByTagName( "object-name" ) ),
        pJmxOperation.getAttribute( "code" )
      );
    }
    else if( !"is-registered".equals( lTag ) ) {
      return isRegistered(
        getObjectName( pJmxOperation.getElementsByTagName( "object-name" ) )
      );
    }
    else if( !"unregister-mbean".equals( lTag ) ) {
      return unregisterMBean(
        getObjectName( pJmxOperation.getElementsByTagName( "object-name" ) )
      );
    }
    return null;
  }

  public ObjectName createMBean(
    String pCodebase,
    ObjectName pName,
    NodeList pConstructor,
    NodeList pAttributes
  ) {
    System.out.println( "XMLAdaptorImpl.createMBean(), code: " + pCodebase + ", name: " + pName );
    ObjectName lReturn = null;
    // Check Codebase
    if( pCodebase != null && !pCodebase.equals( "" ) ) {
      try {
        if( pName != null ) {
          ObjectInstance lNew = null;
          if( pConstructor.getLength() == 0 ) {
            System.out.println( "XMLAdaptorImpl.createMBean(), create w/o arguments" );
            lNew = mServer.createMBean( pCodebase, pName );
          }
          else {
            // Get the Constructor Values
            Object[][] lAttributes = getAttributes(
              ( (Element) pConstructor.item( 0 ) ).getElementsByTagName( "argument" )
            );
            System.out.println( "XMLAdaptorImpl.createMBean(), create with arguments" );
            lNew = mServer.createMBean(
              pCodebase,
              pName,
              lAttributes[ 0 ],
              (String[]) lAttributes[ 1 ]
            );
          }
          // Now loop over the attributes and set them
          Object[][] lAttributes = getAttributes(
            lNew.getObjectName(),
            pAttributes
          );
          applyAttributes(
            lNew.getObjectName(),
            (String[]) lAttributes[ 1 ],
            (Object[]) lAttributes[ 0 ]
          );
          
          lReturn = lNew.getObjectName();
          System.out.println( "XMLAdaptorImpl.createMBean(), Object Name to return: " + lReturn );
        }
      }
      catch( Exception e ) {
        e.printStackTrace();
      }
    }
    return lReturn;
  }

  public Object invoke( String pOperation, ObjectName pName, NodeList pAttributes ) {
    Object lReturn = null;
    System.out.println( "XMLAdaptorImpl.invoke(), Operation: " + pOperation );
    if( pOperation != null && !pOperation.equals( "" ) && pName != null  ) {
      try {
        if( pAttributes != null && pAttributes.getLength() > 0 ) {
          Object[][] lAttributes = getAttributes(
            pAttributes
          );
          // Invoke the method and return the value
          lReturn = mServer.invoke(
            pName,
            pOperation,
            lAttributes[ 0 ],
            (String[]) lAttributes[ 1 ]
          );
        }
        else {
          // Invoke the method and return the value
          lReturn = mServer.invoke(
            pName,
            pOperation,
            new Object[] {},
            new String[] {}
          );
        }
      }
      catch( Exception e ) {
        e.printStackTrace();
      }
    }
    return lReturn;
  }
  
  public Object[] get( ObjectName pName, NodeList pAttributes ) {
    try {
      if( pName != null ) {
        Object[][] lAttributes = getAttributes( pName, pAttributes );
        String[] lNames = (String[]) lAttributes[ 1 ];
        return getAttributeValues( mServer.getAttributes( pName, lNames ) );
      }
    }
    catch( Exception e ) {
      e.printStackTrace();
    }
    return null;
  }
  
  public Object[] set( ObjectName pName, NodeList pAttributes ) {
    try {
      if( pName != null ) {
        Object[][] lAttributes = getAttributes( pName, pAttributes );
        return applyAttributes( pName, (String[]) lAttributes[ 1 ], lAttributes[ 0 ] );
      }
    }
    catch( Exception e ) {
      e.printStackTrace();
    }
    return null;
  }
  
  public MBeanInfo getMBeanInfo( ObjectName pName ) {
    try {
      return mServer.getMBeanInfo( pName );
    }
    catch( Exception e ) {
      e.printStackTrace();
    }
    return null;
  }
  
  public ObjectInstance getObjectInstance( ObjectName pName ) {
    try {
      return mServer.getObjectInstance( pName );
    }
    catch( Exception e ) {
      e.printStackTrace();
    }
    return null;
  }
  
  public Boolean isInstanceOf( ObjectName pName, String pCodebase ) {
    try {
      return new Boolean( mServer.isInstanceOf( pName, pCodebase ) );
    }
    catch( Exception e ) {
      e.printStackTrace();
    }
    return null;
  }
  
  public Boolean isRegistered( ObjectName pName ) {
    try {
      return new Boolean( mServer.isRegistered( pName ) );
    }
    catch( Exception e ) {
      e.printStackTrace();
    }
    return null;
  }
  
  public Object unregisterMBean( ObjectName pName ) {
    try {
      mServer.unregisterMBean( pName );
    }
    catch( Exception e ) {
      e.printStackTrace();
    }
    return null;
  }
  
  // Protected -----------------------------------------------------
  
  protected ObjectName getObjectName( NodeList pObjectName ) {
    return getObjectName( null, pObjectName );
  }
  
  protected ObjectName getObjectName( String pName, NodeList pObjectName ) {
    ObjectName lName = null;
    try {
      // Create ObjectName
      if( pName != null && !pName.equals( "" ) ) {
        System.out.println( "XMLAdaptorImpl.getObjectName(), name: " + pName );
        lName = createObjectName( pName );
      }
      else if( pObjectName != null && pObjectName.getLength() > 0 ) {
        System.out.println( "XMLAdaptorImpl.getObjectName(), name element: " + pObjectName.item( 0 ) );
        lName = createObjectName( (Element) pObjectName.item( 0 ) );
      }
    }
    catch( Exception e ) {
      e.printStackTrace();
    }
    return lName;
  }
  
  protected ObjectName createObjectName( String pName )
    throws
      MalformedObjectNameException
  {
    return new ObjectName( pName );
  }
  
  protected ObjectName createObjectName( Element pObjectName )
    throws
      MalformedObjectNameException
  {
    if( pObjectName.hasAttribute( "name" ) ) {
      return new ObjectName( pObjectName.getAttribute( "name" ) );
    }
    else {
      String lDomain = null;
      if( pObjectName.hasAttribute( "domain" ) ) {
        lDomain = pObjectName.getAttribute( "domain" );
      }
      Hashtable lProperties = new Hashtable();
      NodeList lPropertyList = pObjectName.getElementsByTagName( "property" );
      for( int i = 0; i < lPropertyList.getLength(); i++ ) {
        Element lProperty = (Element) lPropertyList.item( i );
        if( lProperty.hasAttribute( "key" ) && lProperty.hasChildNodes() ) {
          lProperties.put( lProperty.getAttribute( "key" ), ( (Text) lProperty.getFirstChild()).getData() );
        }
      }
      return new ObjectName( lDomain, lProperties );
    }
  }

  /**
  * Returns a list of attribute objects and types of a given Node List
  * which must contain a attribute "type" and a text child.
  *
  * @param pAttributes List of nodes containing the attribute types and values
  *
  * @return First Array contains the objects created from the type and value string,
  *         Second Array contains the types as String
  **/
  protected Object[][] getAttributes( NodeList pAttributes ) {
    Object[][] lReturn = new Object[ 2 ][ 0 ];
    Object[] lValues = new Object[ pAttributes.getLength() ];
    String[] lTypes = new String[ pAttributes.getLength() ];
    // Loop through argument list and create type and values
    for( int i = 0; i < pAttributes.getLength(); i++ ) {
      try {
        Element lArgument = (Element) pAttributes.item( i );
        String lTypeString = lArgument.getAttribute( "type" );
        String lValueString = "";
        if( lArgument.hasChildNodes() ) {
          lValueString = ( (Text) lArgument.getFirstChild() ).getData();
        }
        Class lClass = null;
        if( mPrimitives.containsKey( lTypeString ) ) {
          lClass = (Class) mPrimitives.get( lTypeString );
        }
        else {
         lClass = Thread.currentThread().getContextClassLoader().loadClass( lTypeString );
        }
        PropertyEditor lEditor = PropertyEditorManager.findEditor( lClass );
        lEditor.setAsText( lValueString );
        lValues[ i ] = lEditor.getValue();
        lTypes[ i ] = lClass.getName();
      }
      catch( Exception e ) {
        e.printStackTrace();
      }
    }
    lReturn[ 0 ] = lValues;
    lReturn[ 1 ] = lTypes;
    return (Object[][]) lReturn;
  }

  /**
  * Returns a list of attribute objects and name of a given Node List
  * which must contain a attribute "name" and a text child.
  *
  * @param pAttributes List of nodes containing the attribute types and values
  *
  * @return First Array contains the objects created from the type and value string,
  *         Second Array contains the Attribute Names as String
  **/
  protected Object[][] getAttributes( ObjectName pName, NodeList pAttributes ) {
    Object[][] lReturn = new Object[ 2 ][ 0 ];
    Object[] lValues = new Object[ pAttributes.getLength() ];
    String[] lNames = new String[ pAttributes.getLength() ];

    try {
      MBeanAttributeInfo[] attributes = mServer.getMBeanInfo( pName ).getAttributes();
      // Loop through argument list and create type and values
      for( int i = 0; i < pAttributes.getLength(); i++ ) {
        Element lArgument = (Element) pAttributes.item( i );
        String lNameString = lArgument.getAttribute( "name" );
        String lValueString = "";
        if( lArgument.hasChildNodes() ) {
          lValueString = ( (Text) lArgument.getFirstChild() ).getData();
        }
        for( int k = 0; k < attributes.length; k++ ) {
          if( attributes[ k ].getName().equals( lNameString ) ) { 
            String lTypeString = attributes[ k ].getType();
            Class lClass;
            if( mPrimitives.containsKey( lTypeString ) ) {
               lClass = (Class) mPrimitives.get( lTypeString );
            }
            else {
              lClass = Thread.currentThread().getContextClassLoader().loadClass( lTypeString );
            }
            PropertyEditor lEditor = PropertyEditorManager.findEditor( lClass );
            lEditor.setAsText( lValueString );
            lValues[ i ] = lEditor.getValue();
            lNames[ i ] = lClass.getName();
          }
        }
      }
    }
    catch( Exception e ) {
      e.printStackTrace();
    }
    lReturn[ 0 ] = lValues;
    lReturn[ 1 ] = lNames;
    return (Object[][]) lReturn;
  }
  
  protected Object[] applyAttributes(
    ObjectName pName,
    String[] pNames,
    Object[] pValues
  ) {
    try {
      if( pName != null && pNames != null && pValues != null ) {
        if( pNames.length == pValues.length ) {
          AttributeList lList = new AttributeList();
          for( int i = 0; i < pNames.length; i++ ) {
            String lName = pNames[ i ];
            if( lName != null && !lName.equals( "" ) ) {
              // Create Value from attribute type and given string representation
              lList.add( new Attribute( lName, pValues[ i ] ) );
            }
          }
          return getAttributeValues( mServer.setAttributes( pName, lList ) );
        }
      }
    }
    catch( Exception e ) {
      e.printStackTrace();
    }
    return null;
  }
  
  protected Object[] getAttributeValues( AttributeList pList ) {
    Object[] lReturn = new Object[ pList.size() ];
    for( int i = 0; i < pList.size(); i++ ) {
      lReturn[ i ] = ( (Attribute) pList.get( i ) ).getValue();
    }
    return lReturn;
  }

}
