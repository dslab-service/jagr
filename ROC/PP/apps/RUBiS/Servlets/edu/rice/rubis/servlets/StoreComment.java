package edu.rice.rubis.servlets;

import edu.rice.rubis.*;
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

/** This servlets records a comment in the database and display
 * the result of the transaction.
 * It must be called this way :
 * <pre>
 * http://..../StoreComment?itemId=aa&userId=bb&minComment=cc&maxQty=dd&comment=ee&maxComment=ff&qty=gg 
 *   where: aa is the item id 
 *          bb is the user id
 *          cc is the minimum acceptable comment for this item
 *          dd is the maximum quantity available for this item
 *          ee is the user comment
 *          ff is the maximum comment the user wants
 *          gg is the quantity asked by the user
 * /<pre>
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class StoreComment extends RubisHttpServlet
{
  private ServletPrinter sp = null;
  private PreparedStatement stmt = null;
  private Connection conn = null;

  public int getPoolSize()
  {
    return Config.StoreCommentPoolSize;
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
    sp.printHTMLheader("RUBiS ERROR: StoreComment");
    sp.printHTML("<h2>Your request has not been processed due to the following error :</h2><br>");
    sp.printHTML(errorMsg);
    sp.printHTMLfooter();
    closeConnection();
  }


  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
  {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
  {
    Integer toId;    // to user id
    Integer fromId;  // from user id
    Integer itemId;  // item id
    String  comment; // user comment
    Integer rating;  // user rating

    sp = new ServletPrinter(response, "StoreComment");

    /* Get and check all parameters */

    String value = request.getParameter("to");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a 'to user' identifier !<br></h3>");
      return ;
    }
    else
      toId = new Integer(value);

    value = request.getParameter("from");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a 'from user' identifier !<br></h3>");
      return ;
    }
    else
      fromId = new Integer(value);

    value = request.getParameter("itemId");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide an item identifier !<br></h3>");
      return ;
    }
    else
      itemId = new Integer(value);

    value = request.getParameter("rating");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a rating !<br></h3>");
      return ;
    }
    else
      rating = new Integer(value);

    comment = request.getParameter("comment");
    if ((comment == null) || (comment.equals("")))
    {
      printError("<h3>You must provide a comment !<br></h3>");
      return ;
    }

    try 
    {
      conn = getConnection();
      conn.setAutoCommit(false);	// faster if made inside a Tx
      // Try to create a new comment
      try 
      {
        String now = TimeManagement.currentDateToString();
        stmt = conn.prepareStatement("INSERT INTO comments VALUES (NULL, \""+
                                     fromId+
                                     "\", \""+toId+"\", \""+itemId+"\", \""+
                                     rating+"\", \""+now+"\",\""+comment+"\")");

        stmt.executeUpdate();
      }
      catch (SQLException e)
      {
        printError("Error while storing the comment (got exception: " +e+")<br>");
        return;
      }
      // Try to find the user corresponding to the 'to' ID
      try
      {
        ResultSet urs;
        stmt = conn.prepareStatement("SELECT rating FROM users WHERE id=?");
        stmt.setInt(1, toId.intValue());
        urs = stmt.executeQuery();
        if (urs.first())
        {
          int userRating = urs.getInt("rating");
          userRating = userRating + rating.intValue();
 
          stmt = conn.prepareStatement("UPDATE users SET rating=? WHERE id=?");
          stmt.setInt(1, userRating);
          stmt.setInt(2, toId.intValue());
          stmt.executeUpdate();
        }
      }
      catch (SQLException e)
      {
        printError("Error while updating user's rating (got exception: " +e+")<br>");
        return;
      }
      sp.printHTMLheader("RUBiS: Comment posting");
      sp.printHTML("<center><h2>Your comment has been successfully posted.</h2></center>");

      sp.printHTMLfooter();
      conn.commit();
    } 
    catch (Exception e) 
    {
      sp.printHTML("Exception getting comment list: " + e +"<br>");
      try
      {
        conn.rollback();
      }
      catch (Exception se) 
      {
        sp.printHTML("Transaction rollback failed: " + e +"<br>");
      }
      closeConnection();
    }
  }
}
