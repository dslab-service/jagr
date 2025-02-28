/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

// $Id: AxisTestCase.java,v 1.1.1.1 2003/03/07 08:26:04 emrek Exp $

package org.jboss.test.net;

import org.jboss.test.JBossTestCase;
import org.jboss.test.JBossTestSetup;

import org.jboss.net.axis.AxisInvocationHandler;
import org.jboss.net.axis.XMLResourceProvider;
import org.jboss.net.jmx.MBeanInvocationHandler;
import org.jboss.net.jmx.adaptor.RemoteAdaptor;
import org.jboss.net.jmx.adaptor.RemoteAdaptorInvocationHandler;

import org.apache.axis.client.Service;
import org.apache.axis.client.Call;
import org.apache.axis.AxisFault;
import org.apache.axis.client.AxisClient;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.rmi.RemoteException;

import java.net.URL;

import java.util.Map;

/**
 * Junit Test class with some Axis support
 * <br>
 * <h3>Change History</h3>
 * <ul>
 * <li> jung, 10.03.2002: axis alpha3. </li>
 * <li> jung, 06.02.2002: adapted to new test case structure. </li>
 * </ul>
 * @created 12. Oktober 2001, 11:20
 * @author <a href="mailto:Christoph.Jung@infor.de">Christoph G. Jung</a>
 * @version $Revision: 1.1.1.1 $
 */

public class AxisTestCase extends JBossTestCase {

   /** the protocol we use */
   protected String PROTOCOL = "http://";

   /** the address to which we forward the request */
   protected String ADDRESS = "localhost:8080/";

   /** where the axis servlet context is installed */
   protected String AXIS_CONTEXT = ADDRESS + "axis/";

   /** where the service port is located under */
   protected String SERVICE_PORT = AXIS_CONTEXT + "services";

   /** has an associated end point that may be configured once */
   protected String END_POINT = PROTOCOL + SERVICE_PORT;

   /** the call object */
   protected Service service;

   /** the map of methods to interface names */
   protected Map interfaceMap = new AxisInvocationHandler.DefaultInterfaceMap();
   ;

   /** the map of methods to method names */
   protected Map methodMap = new AxisInvocationHandler.DefaultMethodMap();

   /** Creates new AxisTestCase */
   public AxisTestCase(String name) {
      super(name);
   }

   public void setUp() throws Exception {
      URL resource = getClass().getClassLoader().getResource(getAxisConfiguration());
      System.out.println("Configuring axis with provider resource " + resource);
      service = new Service(new XMLResourceProvider(resource));
 	  service.setMaintainSession(true);
   }

   /** searches for the right configuration provider */
   protected String getAxisConfiguration() {
      return "client-config.xml";
   }

   /** creates a new Axis service using the test engine*/
   protected AxisInvocationHandler createAxisInvocationHandler(URL endpoint)
      throws AxisFault {
      return new AxisInvocationHandler(
         endpoint,service,
         methodMap,
         interfaceMap);
   }

   /** creates a new Axis service using the test engine*/
   protected MBeanInvocationHandler createMBeanInvocationHandler(URL endpoint)
      throws AxisFault {
      return new MBeanInvocationHandler(
         endpoint,
         service,
         methodMap,
         interfaceMap);
   }

   /** creates a new Axis service using the test engine*/
   protected Object createAxisService(Class _class, URL endpoint)
      throws AxisFault {
      return AxisInvocationHandler.createAxisService(
         _class,
         createAxisInvocationHandler(endpoint));
   }

   /** creates a new Axis service using the test engine*/
   protected Object createMBeanService(Class _class, URL endpoint)
      throws AxisFault {
      return MBeanInvocationHandler.createMBeanService(
         _class,
         createMBeanInvocationHandler(endpoint));
   }

   /** creates a new remote adaptor using the test engine*/
   protected RemoteAdaptor createRemoteAdaptor(URL endpoint)
      throws RemoteException {
      return RemoteAdaptorInvocationHandler.createRemoteAdaptor(
         createMBeanInvocationHandler(endpoint));
   }

   protected static Test getAxisSetup(final Test test, final String jarName)
      throws Exception {
      JBossTestSetup wrapper = new AxisTestSetup(test) {

         protected void setUp() throws Exception {
            this.deployAxis(jarName);
            this.getLog().debug("deployed package: " + jarName);
         }

         protected void tearDown() throws Exception {
            this.undeployAxis(jarName);
            this.getLog().debug("undeployed package: " + jarName);
         }
      };
      return wrapper;
   }

   protected static Test getAxisSetup(final Class clazz, final String jarName)
      throws Exception {
      TestSuite suite = new TestSuite();
      suite.addTest(new TestSuite(clazz));
      return getAxisSetup(suite, jarName);
   }
}