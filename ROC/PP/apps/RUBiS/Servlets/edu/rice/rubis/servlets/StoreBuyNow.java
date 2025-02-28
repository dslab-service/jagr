package edu.rice.rubis.servlets;

import edu.rice.rubis.*;
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

/** This servlet records a BuyNow in the database and display
 * the result of the transaction.
 * It must be called this way :
 * <pre>
 * http://..../StoreBuyNow?itemId=aa&userId=bb&minBuyNow=cc&maxQty=dd&BuyNow=ee&maxBuyNow=ff&qty=gg 
 *   where: aa is the item id 
 *          bb is the user id
 *          cc is the minimum acceptable BuyNow for this item
 *          dd is the maximum quantity available for this item
 *          ee is the user BuyNow
 *          ff is the maximum BuyNow the user wants
 *          gg is the quantity asked by the user
 * </pre>
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class StoreBuyNow extends RubisHttpServlet
{
  private ServletPrinter sp = null;
  private PreparedStatement stmt = null;
  private Connection conn = null;

  public int getPoolSize()
  {
    return Config.StoreBuyNowPoolSize;
  }


  private void closeConnection()
  {
    try 
    {
      if (stmt != null) stmt.close();	// close statement
    } 
    catch (Exception ignore) 
    {
    }
  }

  private void printError(String errorMsg)
  {
    sp.printHTMLheader("RUBiS ERROR: StoreBuyNow");
    sp.printHTML("<h2>Your request has not been processed due to the following error :</h2><br>");
    sp.printHTML(errorMsg);
    sp.printHTMLfooter();
    closeConnection();
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
   * Store the BuyNow to the database and display resulting message.
   *
   * @param request a <code>HttpServletRequest</code> value
   * @param response a <code>HttpServletResponse</code> value
   * @exception IOException if an error occurs
   * @exception ServletException if an error occurs
   */
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
  {
    Integer userId; // item id
    Integer itemId; // user id
    //     float   minBuyNow; // minimum acceptable BuyNow for this item
    //     float   BuyNow;    // user BuyNow
    //     float   maxBuyNow; // maximum BuyNow the user wants
    int     maxQty; // maximum quantity available for this item
    int     qty;    // quantity asked by the user

    sp = new ServletPrinter(response, "StoreBuyNow");

    /* Get and check all parameters */

    String value = request.getParameter("userId");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a user identifier !<br></h3>");
      return ;
    }
    else
      userId = new Integer(value);

    value = request.getParameter("itemId");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide an item identifier !<br></h3>");
      return ;
    }
    else
      itemId = new Integer(value);


    value = request.getParameter("maxQty");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a maximum quantity !<br></h3>");
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
      printError("<h3>You must provide a quantity !<br></h3>");
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
      printError("<h3>You cannot request "+qty+" items because only "+maxQty+" are proposed !<br></h3>");
      return ;
    }  
    String now = TimeManagement.currentDateToString();
    // Try to find the Item corresponding to the Item ID
    try 
    {
      int quantity;
      conn = getConnection();
      conn.setAutoCommit(false);
      stmt = conn.prepareStatement("SELECT quantity, end_date FROM items WHERE id=?");
      stmt.setInt(1, itemId.intValue());
      ResultSet irs = stmt.executeQuery();
      if (!irs.first())
      {
        printError("This item does not exist in the database.");
        return;
      }
      quantity = irs.getInt("quantity");
      quantity = quantity -qty;
      if (quantity == 0)
      {
        stmt = conn.prepareStatement("UPDATE items SET end_date=?, quantity=? WHERE id=?");
        stmt.setString(1, now);
        stmt.setInt(2, quantity);
        stmt.setInt(3, itemId.intValue());
        stmt.executeUpdate();
      }
      else
      {
        stmt = conn.prepareStatement("UPDATE items SET quantity=? WHERE id=?");
        stmt.setInt(1, quantity);
        stmt.setInt(2, itemId.intValue());
        stmt.executeUpdate();
      }
    }
    catch (SQLException e)
    {
      sp.printHTML("Failed to execute Query for the item: " +e+"<br>");
      try
      {
        conn.rollback();
      }
      catch (Exception se) 
      {
        printError("Transaction rollback failed: " + e);
      }
      return;
    }
    try 
    {
	
      stmt = conn.prepareStatement("INSERT INTO buy_now VALUES (NULL, \""+userId+
                                   "\", \""+itemId+"\", \""+qty+"\", \""+now+"\")");
      stmt.executeUpdate();
      conn.commit();
      sp.printHTMLheader("RUBiS: BuyNow result");
      if (qty == 1)
        sp.printHTML("<center><h2>Your have successfully bought this item.</h2></center>\n");
      else
        sp.printHTML("<center><h2>Your have successfully bought these items.</h2></center>\n");
    }
    catch (Exception e)
    {
      sp.printHTML("Error while storing the BuyNow (got exception: " +e+")<br>");
      try
      {
        conn.rollback();
      }
      catch (Exception se) 
      {
        printError("Transaction rollback failed: " + e +"<br>");
      }
      return ;
    }
		
    sp.printHTMLfooter();
  }

}
