/*
 * Copyright 1999 by dreamBean Software,
 * All rights reserved.
 */
package org.jboss.test.bankiiop.interfaces;

import java.util.*;
import java.rmi.*;
import javax.ejb.*;

/**
 *      
 *   @see <related>
 *   @author $Author: emrek $
 *   @version $Revision: 1.1.1.1 $
 */
public interface CustomerHome
   extends EJBHome
{
   // Constants -----------------------------------------------------
   public static final String COMP_NAME = "java:comp/env/ejb/Customer";
   public static final String JNDI_NAME = "bank/Customer";
    
   // Attributes ----------------------------------------------------
   
   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------
   
   // Public --------------------------------------------------------
   public Customer create(String id, String name)
      throws RemoteException, CreateException;
   
   public Customer findByPrimaryKey(CustomerPK pk)
      throws RemoteException, FinderException;
      
   public Collection findAll()
      throws RemoteException, FinderException;
}

/*
 *   $Id: CustomerHome.java,v 1.1.1.1 2003/03/07 08:26:09 emrek Exp $
 *   Currently locked by:$Locker:  $
 *   Revision:
 *   $Log: CustomerHome.java,v $
 *   Revision 1.1.1.1  2003/03/07 08:26:09  emrek
 *
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
