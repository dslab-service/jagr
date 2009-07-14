package edu.rice.rubis.beans;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/** This is the Home interface of the SB_SearchItemsByRegion Bean */

public interface SB_SearchItemsByRegionHome extends EJBHome {

  /**
   * This method is used to create a new Bean.
   *
   * @return session bean
   */
  public SB_SearchItemsByRegion create() throws CreateException, RemoteException;

}
