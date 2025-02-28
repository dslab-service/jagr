package edu.rice.rubis.servlets;

import edu.rice.rubis.*;
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

/**
 * This servlets displays general information about the user loged in
 * and about his current bids or items to sell.
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */
public class AboutMe extends RubisHttpServlet
{
  private ServletPrinter sp = null;
  private PreparedStatement stmt = null;
  private Connection conn = null;

  public int getPoolSize()
  {
    return Config.AboutMePoolSize;
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
    //sp.printHTMLheader("RUBiS ERROR: About me");
    sp.printHTML("<h3>Your request has not been processed due to the following error :</h3><br>");
    sp.printHTML(errorMsg);
    sp.printHTMLfooter();
    closeConnection();
  }


  /** List items the user is currently selling and sold in yhe past 30 days */
  private void listItem(Integer userId) 
  {
    ResultSet currentSellings = null;
    ResultSet pastSellings = null;

    String itemName, endDate, startDate;
    float currentPrice, initialPrice, buyNow, reservePrice;
    int quantity, itemId;
      

    // Retrieve ItemHome to get the names of the items the user sold
    try 
    {
      stmt = conn.prepareStatement("SELECT * FROM items WHERE items.seller=? AND items.end_date>=NOW()");
      stmt.setInt(1, userId.intValue());
      currentSellings = stmt.executeQuery();
    }
    catch (Exception e)
    {
      printError("Exception getting current sellings list: " +e+"<br>");
      return ;
    }
    try 
    {
      stmt = conn.prepareStatement("SELECT * FROM old_items WHERE old_items.seller=? AND TO_DAYS(NOW()) - TO_DAYS(old_items.end_date) < 30");
      stmt.setInt(1, userId.intValue());
      pastSellings = stmt.executeQuery();
    }
    catch (Exception e)
    {
      printError("Exception getting past sellings list: " +e+"<br>");
      return ;
    }

    try
    {
      if (!currentSellings.first())
      {
        sp.printHTML("<br>");
        sp.printHTMLHighlighted("<h3>You are currently selling no item.</h3>");
      
      }
      else
      {
        // display current sellings
        sp.printHTML("<br>");
        sp.printSellHeader("Items you are currently selling.");
        do
        {
          // Get the name of the items
          try
          {
            itemId = currentSellings.getInt("id");
            itemName = currentSellings.getString("name");
            endDate = currentSellings.getString("end_date");
            startDate = currentSellings.getString("start_date");
            initialPrice = currentSellings.getFloat("initial_price");
            reservePrice = currentSellings.getFloat("reserve_price");
            buyNow = currentSellings.getFloat("buy_now");
            quantity = currentSellings.getInt("quantity");

            currentPrice = currentSellings.getFloat("max_bid");
            if (currentPrice <initialPrice)
              currentPrice = initialPrice;

          }
          catch (Exception e) 
          {
            printError("Exception getting item: " + e +"<br>");
            return;
          }
          // display information about the item
          sp.printSell(itemId, itemName,  initialPrice, reservePrice, quantity, buyNow, startDate, endDate, currentPrice);
        }
        while (currentSellings.next());
        sp.printItemFooter();
      }
    }
    catch (Exception e) 
    {
      printError("Exception getting current items in sell: " + e +"<br>");
      return;
    }

    try
    {
      if (!pastSellings.first())
      {
        sp.printHTML("<br>");
        sp.printHTMLHighlighted("<h3>You didn't sell any item.</h3>");
        return ;
      }
      // display past sellings
      sp.printHTML("<br>");
      sp.printSellHeader("Items you sold in the last 30 days.");
      do 
      {
        // Get the name of the items
        try
        {
	  itemId = pastSellings.getInt("id");
	  itemName = pastSellings.getString("name");
	  endDate = pastSellings.getString("end_date");
	  startDate = pastSellings.getString("start_date");
	  initialPrice = pastSellings.getFloat("initial_price");
	  reservePrice = pastSellings.getFloat("reserve_price");
	  buyNow = pastSellings.getFloat("buy_now");
	  quantity = pastSellings.getInt("quantity");

	  currentPrice = pastSellings.getFloat("max_bid");
	  if (currentPrice <initialPrice)
            currentPrice = initialPrice;
        }
        catch (Exception e) 
        {
          printError("Exception getting sold item: " + e +"<br>");
          return;
        }
        // display information about the item
        sp.printSell(itemId, itemName,  initialPrice, reservePrice, quantity, buyNow, startDate, endDate, currentPrice);
      }
      while(pastSellings.next());
    }
    catch (Exception e) 
    {
      printError("Exception getting sold items: " + e +"<br>");
      return;
    }
    sp.printItemFooter();
  }

  /** List items the user bought in the last 30 days*/
  private void listBoughtItems(Integer userId) 
  {
    ResultSet buy = null;
    String itemName, sellerName;
    int quantity, sellerId, itemId;
    float buyNow;

    // Get the list of items the user bought
    try 
    {
      stmt = conn.prepareStatement("SELECT * FROM buy_now WHERE buy_now.buyer_id=? AND TO_DAYS(NOW()) - TO_DAYS(buy_now.date)<=30");
      stmt.setInt(1, userId.intValue());
      buy = stmt.executeQuery();
      if (!buy.first())
      {
        sp.printHTML("<br>");
        sp.printHTMLHighlighted("<h3>You didn't buy any item in the last 30 days.</h3>");
        sp.printHTML("<br>");
        return ;
      }
    }
    catch (Exception e)
    {
      printError("Exception getting bought items list: " +e+"<br>");
      return ;
    }

    sp.printUserBoughtItemHeader();
 
    try
    {
      do
      {
	itemId = buy.getInt("item_id");
	quantity = buy.getInt("qty");
        // Get the name of the items
        try
        {
	  try
	  {
            ResultSet itemRS = null;
            PreparedStatement itemStmt = conn.prepareStatement("SELECT * FROM items WHERE id=?");
            itemStmt.setInt(1, itemId);
            itemRS = itemStmt.executeQuery();
            if (!itemRS.first())
            {
              sp.printHTML("Couldn't find bought item.<br>");
              return;
            }
            itemName = itemRS.getString("name");
            sellerId = itemRS.getInt("seller");
            buyNow = itemRS.getFloat("buy_now");
	  }
	  catch (SQLException e)
          {
            sp.printHTML("Failed to execute Query for item (buy now): " +e);
            closeConnection();
            return;
          }
	  try 
          {
            PreparedStatement sellerStmt = conn.prepareStatement("SELECT nickname FROM users WHERE id=?");
            sellerStmt.setInt(1, sellerId);
            ResultSet sellerResult = sellerStmt.executeQuery();
            // Get the seller's name		 
            if (sellerResult.first()) 
              sellerName = sellerResult.getString("nickname");
            else
            {	
              sp.printHTML("Unknown seller");
              sellerStmt = null;
              closeConnection();
              return;
            }

          }
	  catch (SQLException e)
          {
            sp.printHTML("Failed to execute Query for seller (buy now): " +e);
            closeConnection();
            return;
          }
        }
        catch (Exception e) 
        {
          printError("Exception getting buyNow: " + e +"<br>");
          return;
        }
        // display information about the item
        sp.printUserBoughtItem(itemId, itemName, buyNow, quantity, sellerId, sellerName);
      }
      while(buy.next());
    }
    catch (Exception e)
    {
      printError("Exception getting bought items: " +e+"<br>");
      closeConnection();
      return ;
    }
    sp.printItemFooter();

  }

  /** List items the user won in the last 30 days*/
  private void listWonItems(Integer userId) 
  {
    int sellerId, itemId;  
    float currentPrice, initialPrice;
    String itemName, sellerName;
    ResultSet won = null;

    // Get the list of the user's won items
    try 
    {
      stmt = conn.prepareStatement("SELECT item_id FROM bids, old_items WHERE bids.user_id=? AND bids.item_id=old_items.id AND TO_DAYS(NOW()) - TO_DAYS(old_items.end_date) < 30 GROUP BY item_id");
      stmt.setInt(1, userId.intValue());
      won = stmt.executeQuery();
      if (!won.first())
      {
        sp.printHTML("<br>");
        sp.printHTMLHighlighted("<h3>You didn't win any item in the last 30 days.</h3>");
        sp.printHTML("<br>");
        return ;
      }
    }
    catch (Exception e)
    {
      sp.printHTML("Exception getting won items list: " +e+"<br>");
      return ;
    }

    sp.printUserWonItemHeader();
    try
    {
      do 
      {
	itemId = won.getInt("item_id");
        // Get the name of the items
        try
        {
	  try
	  {
            ResultSet itemRS = null;
            PreparedStatement itemStmt = conn.prepareStatement("SELECT * FROM old_items WHERE id=?");
            itemStmt.setInt(1, itemId);
            itemRS = itemStmt.executeQuery();
            if (!itemRS.first())
            {
              sp.printHTML("Couldn't find won item.<br>");
              return;
            }
            itemName = itemRS.getString("name");
            sellerId = itemRS.getInt("seller");
            initialPrice = itemRS.getFloat("initial_price");

            currentPrice = itemRS.getFloat("max_bid");
            if (currentPrice <initialPrice)
              currentPrice = initialPrice;
	  }
	  catch (SQLException e)
          {
            sp.printHTML("Failed to execute Query for item (won items): " +e);
            closeConnection();
            return;
          }
	  PreparedStatement sellerStmt =null;
	  try 
          {
            sellerStmt = conn.prepareStatement("SELECT nickname FROM users WHERE id=?");
            sellerStmt.setInt(1, sellerId);
            ResultSet sellerResult = sellerStmt.executeQuery();
            // Get the seller's name		 
            if (sellerResult.first()) 
              sellerName = sellerResult.getString("nickname");
            else
            {	
              sp.printHTML("Unknown seller");
              sellerStmt = null;
              closeConnection();
              return;
            }

          }
	  catch (SQLException e)
          {
            sp.printHTML("Failed to execute Query for seller (won items): " +e);
            closeConnection();
            sellerStmt = null;
            return;
          }
          // 	  PreparedStatement currentPriceStmt = null;
          // 	  try 
          // 	      {
          // 		  currentPriceStmt = conn.prepareStatement("SELECT MAX(bid) AS bid FROM bids WHERE item_id=?");
          // 		  currentPriceStmt.setInt(1, itemId);
          // 		  ResultSet currentPriceResult = currentPriceStmt.executeQuery();
          // 		  // Get the current price (max bid)		 
          // 		  if (currentPriceResult.first()) 
          // 		      currentPrice = currentPriceResult.getFloat("bid");
          // 		  else
          // 		      currentPrice = initialPrice;
          // 	      }
          // 	  catch (SQLException e)
          // 	      {
          // 		  sp.printHTML("Failed to executeQuery for current price: " +e);
          // 		  closeConnection();
          // 		  if (currentPriceStmt!=null) currentPriceStmt.close();
          // 		  return;
          // 	      }
        }
        catch (Exception e) 
        {
          printError("Exception getting item: " + e +"<br>");
          return;
        }
        // display information about the item
        sp.printUserWonItem(itemId, itemName, currentPrice, sellerId, sellerName);
      }
      while(won.next());
    }
    catch (Exception e)
    {
      sp.printHTML("Exception getting won items: " +e+"<br>");
      closeConnection();
      return ;
    }
    sp.printItemFooter();

  }


  /** List comments about the user */
  private void listComment(Integer userId) 
  {
    ResultSet rs = null;
    String date, comment;
    int authorId;
      
    try 
    {
      conn.setAutoCommit(false);	// faster if made inside a Tx

      // Try to find the comment corresponding to the user
      try
      {
        stmt = conn.prepareStatement("SELECT * FROM comments WHERE to_user_id=?");
        stmt.setInt(1, userId.intValue());
        rs = stmt.executeQuery();
      }
      catch (Exception e)
      {
        sp.printHTML("Failed to execute Query for list of comments: " +e);
        closeConnection();
        return;
      }
      if (!rs.first()) 
      {
        sp.printHTML("<br>");
        sp.printHTMLHighlighted("<h3>There is no comment yet for this user.</h3>");
        sp.printHTML("<br>");
        return;
      }
      else
        sp.printHTML("<br><hr><br><h3>Comments for this user</h3><br>");

      sp.printCommentHeader();
      // Display each comment and the name of its author
      do 
      {
        comment = rs.getString("comment");
        date = rs.getString("date");
        authorId = rs.getInt("from_user_id");

        String authorName = "none";
	ResultSet authorRS = null;
	try
	{
          stmt = conn.prepareStatement("SELECT nickname FROM users WHERE id=?");
          stmt.setInt(1, authorId);
          authorRS = stmt.executeQuery();
          if (authorRS.first())
            authorName = authorRS.getString("nickname");
	}
	catch (Exception e)
	{
          sp.printHTML("Failed to execute Query for the comment author: " +e);
          closeConnection();
          return;
	}
        sp.printComment(authorName, authorId, date, comment);
      }
      while (rs.next());
      sp.printCommentFooter();
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

  /** List items the user put a bid on in the last 30 days*/
  private void listBids(Integer userId, String username, String password) 
  {
 
    float currentPrice, initialPrice, maxBid;
    String itemName, sellerName, startDate, endDate;
    int sellerId, quantity, itemId;
    ResultSet bid = null;

    // Get the list of the user's last bids
    try 
    {
      stmt = conn.prepareStatement("SELECT item_id, bids.max_bid FROM bids, items WHERE bids.user_id=? AND bids.item_id=items.id AND items.end_date>=NOW() GROUP BY item_id");
      stmt.setInt(1, userId.intValue());
      bid = stmt.executeQuery();
      if (!bid.first())
      {
        sp.printHTMLHighlighted("<h3>You didn't put any bid.</h3>");
        sp.printHTML("<br>");
        return ;
      }
    }
    catch (Exception e)
    {
      sp.printHTML("Exception getting bids list: " +e+"<br>");
      return ;
    }

    sp.printUserBidsHeader();
    ResultSet rs = null;
    PreparedStatement itemStmt = null;
    try
    {
      do 
      {
	itemId = bid.getInt("item_id");
	maxBid = bid.getFloat("max_bid");
	try
	{   
          itemStmt = conn.prepareStatement("SELECT * FROM items WHERE id=?");
          itemStmt.setInt(1, itemId);
          rs = itemStmt.executeQuery();
	}
	catch (Exception e)
	{
          sp.printHTML("Failed to execute Query for item the user has bid on: " +e);
          closeConnection();
          return;
	}

        // Get the name of the items
        try
        {
	  if (!rs.first()) 
	  {
            sp.printHTML("<h3>Failed to get items.</h3><br>");
            return;
	  }
 	  itemName = rs.getString("name");
	  initialPrice = rs.getFloat("initial_price");
	  quantity = rs.getInt("quantity");
	  startDate = rs.getString("start_date");
	  endDate = rs.getString("end_date");  
 	  sellerId = rs.getInt("seller");

	  currentPrice = rs.getFloat("max_bid");
	  if (currentPrice <initialPrice)
            currentPrice = initialPrice;

	  PreparedStatement sellerStmt = null;
	  try 
          {
            sellerStmt = conn.prepareStatement("SELECT nickname FROM users WHERE id=?");
            sellerStmt.setInt(1, sellerId);
            ResultSet sellerResult = sellerStmt.executeQuery();
            // Get the seller's name		 
            if (sellerResult.first()) 
              sellerName = sellerResult.getString("nickname");
            else
            {	
              sp.printHTML("Unknown seller");
              closeConnection();
              if (sellerStmt != null) sellerStmt.close();
              return;
            }

          }
	  catch (Exception e)
          {
            sp.printHTML("Failed to execute Query for seller (bids): " +e);
            closeConnection();
            if (sellerStmt != null) sellerStmt.close();
            return;
          }
        }
        catch (Exception e) 
        {
          printError("Exception getting item: " + e +"<br>");
          return;
        }
        //  display information about user's bids
        sp.printItemUserHasBidOn(itemId, itemName, initialPrice, quantity, startDate, endDate, sellerId, sellerName, currentPrice, maxBid, username, password);
      }
      while(bid.next());
    }
    catch (Exception e) 
    {
      printError("Exception getting items the user has bid on: " + e +"<br>");
      return;
    }
    sp.printItemFooter();
  }


  /**
   * Call <code>doPost</code> method.
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
   * Check username and password and build the web page that display the information about
   * the loged in user.
   *
   * @param request a <code>HttpServletRequest</code> value
   * @param response a <code>HttpServletResponse</code> value
   * @exception IOException if an error occurs
   * @exception ServletException if an error occurs
   */
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
  {
    String  password=null, username=null; 
    Integer userId=null;
    ResultSet rs = null;

    sp = new ServletPrinter(response, "About me");

    username = request.getParameter("nickname");
    password = request.getParameter("password");    
    conn = getConnection();
    // Authenticate the user
    if ((username != null && username !="") || (password != null && password !=""))
    {
      Auth auth = new Auth(conn, sp);
      int id = auth.authenticate(username, password);
      if (id == -1)
      {
        printError("You don't have an account on RUBiS!<br>You have to register first.<br>");
        return ;	
      }
      userId = new Integer(id);
    }
    else
    {
      printError(" You must provide valid username and password.");
      return ;
    }
    // Try to find the user corresponding to the userId
    try
    {
      stmt = conn.prepareStatement("SELECT * FROM users WHERE id=?");
      stmt.setInt(1, userId.intValue());
      rs = stmt.executeQuery();
    }
    catch (Exception e)
    {
      sp.printHTML("Failed to execute Query for user: " +e);
      closeConnection();
      sp.printHTMLfooter();
      return;
    }
    try 
    {
      if (!rs.first())
      {
        sp.printHTML("<h2>This user does not exist!</h2>");		    
        closeConnection();
        sp.printHTMLfooter();
        return;
      }
      String firstname = rs.getString("firstname");
      String lastname = rs.getString("lastname");
      String nickname = rs.getString("nickname");
      String email = rs.getString("email");
      String date = rs.getString("creation_date");
      int rating = rs.getInt("rating");

      String result = new String();

      result = result+"<h2>Information about "+nickname+"<br></h2>";
      result = result+"Real life name : "+firstname+" "+lastname+"<br>";
      result = result+"Email address  : "+email+"<br>";
      result = result+"User since     : "+date+"<br>";
      result = result+"Current rating : <b>"+rating+"</b><br>";
      sp.printHTMLheader("RUBiS: About "+nickname);
      sp.printHTML(result);

    }
    catch (SQLException s)
    {
      sp.printHTML("Failed to get general information about the user: " +s);
      closeConnection();
      sp.printHTMLfooter();
      return;
    }

    listBids(userId, username, password);
    listItem(userId);
    listWonItems(userId);
    listBoughtItems(userId);
    listComment(userId);

    sp.printHTMLfooter();
  }

}
