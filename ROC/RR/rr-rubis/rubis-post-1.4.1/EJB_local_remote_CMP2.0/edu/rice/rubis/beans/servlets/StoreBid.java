package edu.rice.rubis.beans.servlets;

import java.io.IOException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.rice.rubis.beans.SB_StoreBid;
import edu.rice.rubis.beans.SB_StoreBidHome;

import roc.rr.ssmutil.SSMException;

/** This servlet records a bid in the database and display
 * the result of the transaction.
 * It must be called this way :
 * <pre>
 * http://..../StoreBid?itemId=aa&userId=bb&minBid=cc&maxQty=dd&bid=ee&maxBid=ff&qty=gg 
 *   where: aa is the item id 
 *          bb is the user id
 *          cc is the minimum acceptable bid for this item
 *          dd is the maximum quantity available for this item
 *          ee is the user bid
 *          ff is the maximum bid the user wants
 *          gg is the quantity asked by the user
 * </pre>
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class StoreBid extends HttpServlet
{

  private void printError(String errorMsg, ServletPrinter sp)
  {
    sp.printHTMLheader("RUBiS ERROR: StoreBid");
    sp.printHTML("<h2>Your request has not been processed due to the following error :</h2><br>");
    sp.printHTML(errorMsg);
    sp.printHTMLfooter();
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
   * Store the bid to the database and display resulting message.
   *
   * @param request a <code>HttpServletRequest</code> value
   * @param response a <code>HttpServletResponse</code> value
   * @exception IOException if an error occurs
   * @exception ServletException if an error occurs
   */
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException
  {
    ServletPrinter sp = null;
    Context initialContext = null;
    Integer userId; // item id
    Integer itemId; // user id
    float   minBid; // minimum acceptable bid for this item
    float   bid;    // user bid
    float   maxBid; // maximum bid the user wants
    int     maxQty; // maximum quantity available for this item
    int     qty;    // quantity asked by the user
    String  value;

    sp = new ServletPrinter(request, response, "StoreBid");

    /* Get and check all parameters */


    // extract userId from session object instead of URL parameter
    try {
	userId = Session.getUserId(request);
    } catch (SSMException e) {
	printError("Cannot read user id from SSM: "+e,sp);
	return;
    }
    if (userId == null || userId.intValue() < 0){
	printError("<h3>ERROR: Your Session is no longer active, please login and re-enter your bid.<br></h3>", sp);
      return ;
    }

    /*
      extract itemId from session object instead of argument parapeters
    */
    String itemStr;
    try {
	itemStr = Session.getItemId(request);
    } catch (SSMException e) {
	printError("Cannot get item id from SSM: "+e,sp);
	return;
    }
    
    itemId = new Integer(itemStr);
    if (itemId == null || itemId.equals("")) {
	printError("<h3>ERROR: Your Session is no longer active, please login and re-enter your bid.<br></h3>", sp);
	return ;
    }

    value = request.getParameter("minBid");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a minimum bid !<br></h3>", sp);
      return ;
    }
    else
    {
      Float foo = new Float(value);
      minBid = foo.floatValue();
    }

    value = request.getParameter("bid");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a bid !<br></h3>", sp);
      return ;
    }
    else
    {
      Float foo = new Float(value);
      bid = foo.floatValue();
    }

    value = request.getParameter("maxBid");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a maximum bid !<br></h3>", sp);
      return ;
    }
    else
    {
      Float foo = new Float(value);
      maxBid = foo.floatValue();
    }

    value = request.getParameter("maxQty");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a maximum quantity !<br></h3>", sp);
      return ;
    }
    else
    {
      Integer foo = new Integer(value);
      maxQty = foo.intValue();
    }

    value = request.getParameter("qty");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a quantity !<br></h3>", sp);
      return ;
    }
    else
    {
      Integer foo = new Integer(value);
      qty = foo.intValue();
    }

    /* Check for invalid values */

    if (qty > maxQty)
    {
      printError("<h3>You cannot request "+qty+" items because only "+maxQty+" are proposed !<br></h3>", sp);
      return ;
    }      
    if (bid < minBid)
    {
      printError("<h3>Your bid of $"+bid+" is not acceptable because it is below the $"+minBid+" minimum bid !<br></h3>", sp);
      return ;
    }      
    if (maxBid < minBid)
    {
      printError("<h3>Your maximum bid of $"+maxBid+" is not acceptable because it is below the $"+minBid+" minimum bid !<br></h3>", sp);
      return ;
    }      
    if (maxBid < bid)
    {
      printError("<h3>Your maximum bid of $"+maxBid+" is not acceptable because it is below your current bid of $"+bid+" !<br></h3>", sp);
      return ;
    }      

    try
    {
      initialContext = new InitialContext();
    } 
    catch (Exception e) 
    {
      printError("Cannot get initial context for JNDI: " + e+"<br>", sp);
      return ;
    }
    SB_StoreBidHome sbHome;
    SB_StoreBid sb_StoreBid;
    String jndiName  = "SB_StoreBidHome";
    Object jndiValue = null;

    try 
    {
	jndiValue = initialContext.lookup(jndiName);
	sbHome = (SB_StoreBidHome)PortableRemoteObject.narrow(jndiValue,SB_StoreBidHome.class);
	sb_StoreBid = sbHome.create();
    } 
    catch (ClassCastException e) {
        // Send service unavailable response to client
        // since microreboot of SB_ViewItem is in progress.
        sp.sendServiceUnavailable(jndiName, jndiValue);
        return;
    }
    catch (Exception e) {
	printError("Cannot lookup SB_StoreBid: " +e+"<br>", sp);
	return ;
    }

    try
    {
      sb_StoreBid.createBid(userId, itemId, bid, maxBid, qty);
      sp.printHTMLheader("RUBiS: Bidding result");
      sp.printHTML("<center><h2>Your bid has been successfully processed.</h2></center>\n");
    }
    catch (Exception e)
    {
      printError("Error while storing the bid (got exception: " +e+")<br>", sp);
      return ;
    }
		
    sp.printHTMLfooter();
  }

}
