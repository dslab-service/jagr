/*
 * Copyright 1999 by dreamBean Software,
 * All rights reserved.
 */
package org.jboss.test.bankiiop.interfaces;

import java.rmi.*;
import javax.ejb.*;

/**
 *      
 *   @see <related>
 *   @author $Author: mikechen $
 *   @version $Revision: 1.1.1.1 $
 */
public interface TellerHome
   extends EJBHome
{
   // Constants -----------------------------------------------------
   public static final String COMP_NAME = "java:comp/env/ejb/Teller";
   public static final String JNDI_NAME = "bank/Teller";
    
   // Attributes ----------------------------------------------------
   
   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------
   
   // Public --------------------------------------------------------
   public Teller create()
      throws RemoteException, CreateException;
   
}

/*
 *   $Id: TellerHome.java,v 1.1.1.1 2002/11/16 03:16:42 mikechen Exp $
 *   Currently locked by:$Locker:  $
 *   Revision:
 *   $Log: TellerHome.java,v $
 *   Revision 1.1.1.1  2002/11/16 03:16:42  mikechen
 *   Import of JBoss v3.0.3
 *
 *   Revision 1.1  2002/03/15 22:36:29  reverbel
 *   Initial version of the bank test for JBoss/IIOP.
 *
 *   Revision 1.3  2001/01/20 16:32:52  osh
 *   More cleanup to avoid verifier warnings.
 *
 *   Revision 1.2  2001/01/07 23:14:36  peter
 *   Trying to get JAAS to work within test suite.
 *
 *   Revision 1.1.1.1  2000/06/21 15:52:38  oberg
 *   Initial import of jBoss test. This module contains CTS tests, some simple examples, and small bean suites.
 *
 *
 *  
 */
