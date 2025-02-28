package edu.rice.rubis.beans.servlets;

import java.io.IOException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.rice.rubis.beans.SB_ViewItem;
import edu.rice.rubis.beans.SB_ViewItemHome;

/** This servlets displays the full description of a given item
 * and allows the user to bid on this item.
 * It must be called this way :
 * <pre>
 * http://..../ViewItem?itemId=xx where xx is the id of the item
 * </pre>
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */


public class ViewItem extends HttpServlet
{

  private void printError(String errorMsg, ServletPrinter sp)
  {
    sp.printHTMLheader("RUBiS ERROR: View item");
    sp.printHTML("<h2>We cannot process your request due to the following error :</h2><br>");
    sp.printHTML(errorMsg);
    sp.printHTMLfooter();
  }

  /**
   * Display all available information on an item.
   *
   * @param request a <code>HttpServletRequest</code> value
   * @param response a <code>HttpServletResponse</code> value
   * @exception IOException if an error occurs
   * @exception ServletException if an error occurs
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
  {
    ServletPrinter sp = null;

    sp = new ServletPrinter(request, response, "ViewItem");
    
    String value = request.getParameter("itemId");
    if ((value == null) || (value.equals("")))
    {
      printError("No item identifier received - Cannot process the request<br>", sp);
      return ;
    }

    Context initialContext = null;
    try
    {
      initialContext = new InitialContext();
    } 
    catch (Exception e) 
    {
      printError("Cannot get initial context for JNDI: " + e+"<br>", sp);
      return ;
    }

    SB_ViewItemHome viewItemHome = null;
    SB_ViewItem viewItem = null;
    String jndiName="SB_ViewItemHome";
    String html;
    Object jndiValue=null;

    try 
    {
	jndiValue = initialContext.lookup(jndiName);
	viewItemHome = (SB_ViewItemHome)PortableRemoteObject.narrow(jndiValue, SB_ViewItemHome.class);
	viewItem = viewItemHome.create();
    } 
    catch (ClassCastException e) {
	// Send service unavailable response to client 
	// since microreboot of SB_ViewItem is in progress.
	sp.sendServiceUnavailable(jndiName, jndiValue);
	return;
    }
    catch (Exception e)
    {
      printError("Cannot lookup SB_ViewItem: " +e+"<br>", sp);
      return ;
    }
    try
    {
	sp.printHTMLheader("RUBiS: Viewing Item \n");
	Integer itemId = new Integer(value);
	html = viewItem.getItemDescription(itemId, -1);
	sp.printHTML(html);
	sp.printHTMLfooter();
    }
    catch (Exception e)
    {
      printError("Cannot get item description (got exception: " +e+")<br>", sp);
      return ;
    }
  }

  /**
   * Call the <code>doGet</code> method.
   *
   * @param request a <code>HttpServletRequest</code> value
   * @param response a <code>HttpServletResponse</code> value
   * @exception IOException if an error occurs
   * @exception ServletException if an error occurs
   */
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
  {
    doGet(request, response);
  }
}
