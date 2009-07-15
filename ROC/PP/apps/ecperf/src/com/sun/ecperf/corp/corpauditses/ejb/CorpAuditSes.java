
/*
 *
 * Copyright (c) 1999-2000 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 * @author Ramesh Ramachandran
 *
 *
 */
package com.sun.ecperf.corp.corpauditses.ejb;


import javax.ejb.EJBObject;

import java.rmi.RemoteException;

/**
 * This interface is the remote interface for the CorpAuditSes
 * session bean. This bean is stateless.
 *
 * @author Ramesh Ramachandran
 *
 *
 */
public interface CorpAuditSes extends EJBObject {
     boolean validateInitialValues(int txRate) throws RemoteException;
     int getCustomerCount() throws RemoteException;
}

