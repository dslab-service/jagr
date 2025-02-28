package edu.rice.rubis.beans.servlets;

import edu.rice.rubis.beans.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.transaction.UserTransaction;
import java.util.Enumeration;

/** This servlets displays the list of bids regarding an item.
 * It must be called this way :
 * <pre>
 * http://..../ViewUserInfo?itemId=xx where xx is the id of the item
 * </pre>
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class ViewBidHistory extends HttpServlet
{
  private ServletPrinter sp = null;
  private Context initialContext = null;
  private UserTransaction utx = null;

 
  /** List the bids corresponding to an item */
  private void listBids(Integer itemId) 
  {
    Enumeration bidList=null;
    ItemHome iHome;
    QueryHome   qHome = null;
    Query       q;
    BidHome bidHome;
    Bid bid;
    Item item;
    float price;
    String name;

    // Connecting to Query Home thru JNDI
    try 
    {
      qHome = (QueryHome)PortableRemoteObject.narrow(initialContext.lookup("QueryHome"),
                                                     QueryHome.class);
    } 
    catch (Exception e)
    {
      sp.printHTML("Cannot lookup Query: " +e+"<br>");
      return ;
    }
    // Get the list of the user's last bids
    try 
    {
      q = qHome.create();
      bidList = q.getItemBidHistory(itemId).elements();
    }
    catch (Exception e)
    {
      sp.printHTML("Exception getting bids list: " +e+"<br>");
      return ;
    }
    if ((bidList == null) || (!bidList.hasMoreElements()))
    {
      sp.printHTML("<h3>There is no bid corresponding to this item.</h3><br>");
      return ;
    }

    // Lookup bid home interface
    try 
    {
      bidHome = (BidHome)PortableRemoteObject.narrow(initialContext.lookup("BidHome"),
                                                     BidHome.class);
    } 
    catch (Exception e)
    {
      sp.printHTML("Cannot lookup Bid: " +e+"<br>");
      return ;
    }
 
    sp.printBidHistoryHeader();

    while (bidList.hasMoreElements()) 
    {
      // Get the bids
      try
      {
        bid = bidHome.findByPrimaryKey((BidPK)bidList.nextElement());
	      
      }
      catch (Exception e) 
      {
        sp.printHTML("Exception getting bid: " + e +"<br>");
        sp.printHTMLfooter();
        return;
      }

      sp.printBidHistory(bid);
    }
    sp.printBidHistoryFooter();
  }


  /**
   * Call the <code>doPost</code> method.
   *
   * @param request a <code>HttpServletRequest</code> value
   * @param response a <code>HttpServletResponse</code> value
   * @exception IOException if an error occurs
   * @exception ServletException if an error occurs
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
  {
    doPost(request, response);
  }

  /**
   * Display the bid history of an item
   *
   * @param request a <code>HttpServletRequest</code> value
   * @param response a <code>HttpServletResponse</code> value
   * @exception IOException if an error occurs
   * @exception ServletException if an error occurs
   */
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
  {
    String  value = request.getParameter("itemId");
    Integer itemId;
    
    sp = new ServletPrinter(response, "ViewBidHistory");

    if ((value == null) || (value.equals("")))
    {
      sp.printHTMLheader("RUBiS ERROR: View bids history");
      sp.printHTML("<h3>You must provide an item identifier !<br></h3>");
      sp.printHTMLfooter();
      return ;
    }
    else
      itemId = new Integer(value);

    if (itemId.intValue() == -1)
    {
      sp.printHTML("ERROR: ItemId is -1.<br>");
      return ;
    }


    sp.printHTMLheader("RUBiS: Bid history");

    try
    {
      initialContext = new InitialContext();
    } 
    catch (Exception e) 
    {
      sp.printHTML("Cannot get initial context for JNDI: " + e+"<br>");
      sp.printHTMLfooter();
      return ;
    }

    // We want to start transactions from client: get UserTransaction
    try
    {
      utx = (javax.transaction.UserTransaction)initialContext.lookup(Config.UserTransaction);
    } 
    catch (Exception e)
    {
      sp.printHTML("Cannot lookup UserTransaction: "+e+"<br>");
      return ;
    }

    // Try to find the item corresponding to the itemId
    ItemHome iHome;
    try 
    {
      iHome = (ItemHome)PortableRemoteObject.narrow(initialContext.lookup("ItemHome"),
                                                    ItemHome.class);
    } 
    catch (Exception e)
    {
      sp.printHTML("Cannot lookup item: " +e+"<br>");
      sp.printHTMLfooter();
      return ;
    }
    try
    {
      Item item = iHome.findByPrimaryKey(new ItemPK(itemId));
      sp.printHTML("<center><h3>Bid History for "+item.getName()+"<br></h3></center>");
    }
    catch (Exception e)
    {
      sp.printHTML("This item does not exist (got exception: " +e+")<br>");
      sp.printHTMLfooter();
      return ;
    }
		
    listBids(itemId);
    sp.printHTMLfooter();
  }

}
