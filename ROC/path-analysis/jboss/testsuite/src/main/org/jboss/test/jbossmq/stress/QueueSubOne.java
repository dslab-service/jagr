/*
 * JBoss, the OpenSource EJB server
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.jbossmq.stress;

import junit.framework.TestSuite;
/**
 *
 *
 * @author     <a href="mailto:pra@tim.se">Peter Antman</a>
 * @version $Revision: 1.1.1.1 $
 */

public class QueueSubOne extends QueueTest{
   
   public QueueSubOne(String name) {
      super(name);
   }
   
      public static junit.framework.Test suite() throws Exception{
      
      TestSuite suite= new TestSuite();
      suite.addTest(new QueueSubOne("runQueueSubscriberPartOne"));
      
      return suite;
   }
   public static void main(String[] args) {
      
   }
   
} // QueueSubOne
