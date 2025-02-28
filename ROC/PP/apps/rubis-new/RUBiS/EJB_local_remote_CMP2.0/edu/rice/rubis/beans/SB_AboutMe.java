package edu.rice.rubis.beans;

import javax.ejb.*;
import java.rmi.*;

/**
 * This is the Remote Interface of the SB_AboutMe Bean
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.1
 */
public interface SB_AboutMe extends EJBObject, Remote {

   /**
   * Authenticate the user and get the information about the user.
   *
   * @return a string in html format
   * @since 1.1
   */
  public String getAboutMe(String username, String password) throws RemoteException;

  /** List items the user is currently selling and sold in the past 30 days */
  public String listItem(Integer userId, ItemLocalHome iHome) throws RemoteException;

  /** List items the user bought in the last 30 days*/
  public String listBoughtItems(Integer userId, ItemLocalHome iHome) throws RemoteException;

  /** List items the user won in the last 30 days*/
  public String listWonItems(Integer userId, UserLocal user) throws RemoteException;

 /** List comments about the user */
  public String listComments(CommentLocalHome home, Integer userId, UserLocalHome uHome) throws RemoteException;

  /** List items the user put a bid on in the last 30 days*/
  public String listBids(Integer userId, String username, String password, UserLocal user, ItemLocalHome iHome) throws RemoteException;

  /** 
   * user's bought items list header printed function
   *
   * @return a string in html format
   * @since 1.1
   */
  public String printUserBoughtItemHeader() throws RemoteException;

  /** 
   * user's won items list header printed function
   *
   * @return a string in html format
   * @since 1.1
   */
  public String printUserWonItemHeader() throws RemoteException;

  /** 
   * user's bids list header printed function
   *
   * @return a string in html format
   * @since 1.1
   */
  public String printUserBidsHeader() throws RemoteException;

  /** 
   * items list printed function
   *
   * @return a string in html format
   * @since 1.1
   */
  public String printItemUserHasBidOn(BidLocal bid, ItemLocal item, String username, String password) throws RemoteException;

  /** 
   * user's sellings header printed function
   *
   * @return a string in html format
   * @since 1.1
   */
 public String printSellHeader(String title) throws RemoteException;

  /** 
   * Item footer printed function
   *
   * @return a string in html format
   * @since 1.1
   */
 public String printItemFooter() throws RemoteException;

  /**
   * Construct a html highlighted string.
   * @param msg the message to display
   * @return a string in html format
   * @since 1.1
   */
  public String printHTMLHighlighted(String msg) throws RemoteException;

  /** 
   * Comment header printed function
   *
   * @return a string in html format
   * @since 1.1
   */
  public String printCommentHeader() throws RemoteException;

  /** 
   * Comment printed function
   *
   * @param userName the name of the user who is the subject of the comments
   * @param comment the comment to display
   * @return a string in html format
   * @since 1.1
   */
  public String printComment(String userName, CommentLocal comment) throws RemoteException;

  /** 
   * Comment footer printed function
   *
   * @return a string in html format
   * @since 1.1
   */
  public String printCommentFooter() throws RemoteException;


}
