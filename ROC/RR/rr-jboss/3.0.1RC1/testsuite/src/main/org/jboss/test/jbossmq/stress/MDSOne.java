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
 * @author     <a href="mailto:pra@tim.se">Peter Antman</a>
 * @version $Revision: 1.1.1.1 $
 */

public class MDSOne extends MultipleDurableSubscribers{
   
   public MDSOne(String name) {
      super(name);
   }
     public static junit.framework.Test suite() throws Exception{
      
      TestSuite suite= new TestSuite();
      suite.addTest(new MDSOne("runDurableSubscriberPartOne"));
      
      //suite.addTest(new DurableSubscriberTest("testBadClient"));
      return suite;
   }
   public static void main(String[] args) {
      
   }
   
} // MDSOne
