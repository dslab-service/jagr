
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
package com.sun.ecperf.mfg.mfgauditses.ejb;


import java.rmi.RemoteException;

import javax.ejb.*;


/**
 * This interface is the home interface for the MfgAudit
 * session bean. This bean is stateless
 *
 * @author Ramesh Ramachandran
 *
 *
 */
public interface MfgAuditSesHome extends EJBHome {

    /**
     * Method create
     *
     *
     * @return
     *
     * @throws CreateException
     * @throws RemoteException
     *
     */
    public MfgAuditSes create() throws RemoteException, CreateException;
}

