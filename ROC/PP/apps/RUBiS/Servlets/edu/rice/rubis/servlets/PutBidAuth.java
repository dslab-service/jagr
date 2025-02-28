package edu.rice.rubis.servlets;

import edu.rice.rubis.*;
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

/** This servlets display the page authentifying the user 
 * to allow him to put a bid on an item.
 * It must be called this way :
 * <pre>
 * http://..../PutBidAuth?itemId=xx where xx is the id of the item
 * /<pre>
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */


public class PutBidAuth extends HttpServlet
{
  private ServletPrinter sp = null;

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
  {
    sp = new ServletPrinter(response, "PubBidAuth");
    
    String value = request.getParameter("itemId");
    if ((value == null) || (value.equals("")))
    {
      sp.printHTMLheader("RUBiS ERROR: Authentification for bidding");
      sp.printHTML("No item identifier received - Cannot process the request<br>");
      sp.printHTMLfooter();
      return ;
    }

    sp.printHTMLheader("RUBiS: User authentification for bidding");
    sp.printFile(Config.HTMLFilesPath+"/put_bid_auth_header.html");
    sp.printHTML("<input type=hidden name=\"itemId\" value=\""+value+"\">");
    sp.printFile(Config.HTMLFilesPath+"/auth_footer.html");
    sp.printHTMLfooter();
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
  {
    doGet(request, response);
  }
}
