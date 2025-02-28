package edu.rice.rubis.beans.servlets;

import java.io.IOException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.rice.rubis.beans.SB_RegisterUser;
import edu.rice.rubis.beans.SB_RegisterUserHome;

/** This servlet register a new user in the database and display
 * the result of the transaction.
 * It must be called this way :
 * <pre>
 * http://..../RegisterUser?firstname=aa&lastname=bb&nickname=cc&email=dd&password=ee&region=ff
 *   where: aa is the user first name
 *          bb is the user last name
 *          cc is the user nick name (login name)
 *          dd is the email address of the user
 *          ee is the user password
 *          ff is the identifier of the region where the user lives
 * </pre>
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class RegisterUser extends HttpServlet
{

  private void printError(String errorMsg, ServletPrinter sp)
  {
    sp.printHTMLheader("RUBiS ERROR: Register user");
    sp.printHTML("<h2>Your registration has not been processed due to the following error :</h2><br>");
    sp.printHTML(errorMsg);
    sp.printHTMLfooter();
  }

  /**
   * Describe <code>doGet</code> method here.
   *
   * @param request a <code>HttpServletRequest</code> value
   * @param response a <code>HttpServletResponse</code> value
   * @exception IOException if an error occurs
   * @exception ServletException if an error occurs
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response) 
      throws IOException, ServletException
  {
    ServletPrinter sp = null;
    String firstname=null, lastname=null, nickname=null, email=null, password=null;
    int    regionId = 0;
    int    userId;
    String creationDate, regionName;

    sp = new ServletPrinter(request, response, "RegisterUser");
      
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

    String value = request.getParameter("firstname");
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide a first name!<br>", sp);
      return ;
    }
    else
      firstname = value;

    value = request.getParameter("lastname");
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide a last name!<br>", sp);
      return ;
    }
    else
      lastname = value;

    value = request.getParameter("nickname");
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide a nick name!<br>", sp);
      return ;
    }
    else
      nickname = value;

    value = request.getParameter("email");
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide an email address!<br>", sp);
      return ;
    }
    else
      email = value;

    value = request.getParameter("password");
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide a password!<br>", sp);
      return ;
    }
    else
      password = value;


    value = request.getParameter("region");
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide a valid region!<br>", sp);
      return ;
    }
    else
      regionName = value;

    // Try to create a new user
    SB_RegisterUserHome regHome;
    SB_RegisterUser reg;
    String jndiName  = "SB_RegisterUserHome";
    Object jndiValue = null;

    try {
	jndiValue = initialContext.lookup(jndiName);
	regHome = (SB_RegisterUserHome)PortableRemoteObject.narrow(jndiValue,SB_RegisterUserHome.class);
	reg = regHome.create();
    } 
    catch (ClassCastException e) {
        // Send service unavailable response to client
        // since microreboot of SB_ViewItem is in progress.
        sp.sendServiceUnavailable(jndiName, jndiValue);
        return;
    }
    catch (Exception e){
	printError("Cannot lookup SB_RegisterUser: " +e+"<br>", sp);
	return ;
    }

    String html;
    try {


      html = reg.createUser(firstname, lastname, nickname, email, password, regionName);
      
      sp.printHTMLheader("RUBiS: Welcome to "+nickname);
      sp.printHTML("<h2>Your registration has been processed successfully</h2><br>\n");
      sp.printHTML("<h3>Welcome "+nickname+"</h3>\n");
      sp.printHTML("RUBiS has stored the following information about you:<br>\n");
      sp.printHTML("First Name : "+firstname+"<br>\n");
      sp.printHTML("Last Name  : "+lastname+"<br>\n");
      sp.printHTML("Nick Name  : "+nickname+"<br>\n");
      sp.printHTML("Email      : "+email+"<br>\n");
      sp.printHTML("Password   : "+password+"<br>\n");
      sp.printHTML("Region     : "+regionName+"<br>\n");
      sp.printHTML("<br>The following information has been automatically generated by RUBiS:<br>\n");
      sp.printHTML(html);
      sp.printHTMLfooter();
    } 
    catch (Exception e)
    {
      printError("User registration failed: " +e+"<br>", sp);
    }
  }
    
 
  /**
   * Call the <code>doGet</code> method here.
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
