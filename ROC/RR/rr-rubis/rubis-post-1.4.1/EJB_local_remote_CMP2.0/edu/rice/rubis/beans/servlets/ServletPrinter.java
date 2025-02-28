package edu.rice.rubis.beans.servlets;

import edu.rice.rubis.beans.*;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.rmi.PortableRemoteObject;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.rmi.RemoteException;
import java.util.GregorianCalendar;
import java.util.Enumeration;
import java.net.URLEncoder;
import roc.rr.MicrorebootInProgress;

/** In fact, this class is not a servlet itself but it provides
 * output services to servlets to send back HTML files.
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class ServletPrinter
{
    private HttpServletRequest  request=null;
    private HttpServletResponse response=null;
    private PrintWriter         out;
    private String              servletName;
    private GregorianCalendar   startDate;

  /**
   * Creates a new <code>ServletPrinter</code> instance.
   *
   * @param toWebServer a <code>HttpServletResponse</code> value
   * @param callingServletName a <code>String</code> value
   */
  public ServletPrinter(HttpServletResponse toWebServer, String callingServletName)
  {
    startDate = new GregorianCalendar();
    toWebServer.setContentType("text/html");
    try
    {
      out = toWebServer.getWriter();
    }
    catch (IOException ioe)
    {
      ioe.printStackTrace();
    }
    servletName = callingServletName;
  }

  public ServletPrinter(HttpServletRequest request, 
			HttpServletResponse toWebServer, 
			String callingServletName)
  {
      this.request  = request;
      this.response = toWebServer;
      startDate = new GregorianCalendar();
      toWebServer.setContentType("text/html");
      try {
	  out = toWebServer.getWriter();
      }  catch (IOException ioe) {
	  ioe.printStackTrace();
      }
      servletName = callingServletName;
  }


  void printFile (String filename)
  {
      /*
    FileReader fis = null;
    try
    {
      fis = new FileReader(filename);
      char[] data = new char[4*1024]; // 4K buffer
      int    bytesRead;
      bytesRead = fis.read(data);
      while (bytesRead != -1)
      {
        out.write(data, 0, bytesRead);
	bytesRead = fis.read(data);
      }
    }
    catch (Exception e)
    {
      out.println("Unable to read file (exception: "+e+")<br>\n");
    }
    finally
    {
      if (fis != null)
        try
        {
          fis.close();
        }
      catch (Exception ex)
      {
        out.println("Unable to close the file reader (exception: "+ex+")<br>\n"); 
      }
    }
      */
      try {
	  request.getRequestDispatcher(filename).include(request,response);
      } catch (Exception e) {
	  out.println("Unable to include file "+filename+" : "+e);
      }
  }

  void printHTMLheader(String title) {
      String header = "/header-login.html";
      if ( request != null ){
	  HttpSession session = request.getSession();
	  if (session != null){
	      try {
		  //Integer io = (Integer)session.getAttribute("USERID");
		  Integer io = Session.getUserId(request);
		  Debug.println("[printHTMLheader] extracted userId: "+io);
		  if (io != null) {
		      header = "/header-logout.html";
		  }
	      } catch (Exception e){
		  out.println("Cannot get attribute USERID from session object.<br>");
	      }
	  }
      }
      //printFile(Config.HTMLFilesPath+header);
      try {
	  request.getRequestDispatcher(header).include(request,
						       response);
      } catch (Exception e){
	  out.println("Cannot include header.html file: "+e);
      }
      out.println("<title>"+title+"</title>\n");
  }

  void printLoginHTMLheader(String title) {
      String header = "/header-login.html";
      try {
	  request.getRequestDispatcher(header).include(request,
						       response);
      } catch (Exception e){
	  out.println("Cannot include header.html file: "+e);
      }
  }


  void printHTMLfooter()
  {
    GregorianCalendar endDate = new GregorianCalendar();

    out.println("<br><hr>RUBiS (C) Rice University/INRIA<br><br>\n");  // Page generated by "+servletName+" in "+TimeManagement.diffTime(startDate, endDate)
    out.println("</body>\n");
    out.println("</html>\n");	
  }

  void printHTML(String msg)
  {
    out.println(msg);
  }

  void printHTMLBody(String body){
      //printFile(Config.HTMLFilesPath+"/"+body);
      try {
	  request.getRequestDispatcher(body).include(request,
						     response);
      } catch (Exception e){
	  out.println("Cannot include "+body+": "+e);
      }
  }    

  void printHTMLHighlighted(String msg)
  {
    out.println("<TABLE width=\"100%\" bgcolor=\"#CCCCFF\">\n");
    out.println("<TR><TD align=\"center\" width=\"100%\"><FONT size=\"4\" color=\"#000000\"><B>"+msg+"</B></FONT></TD></TR>\n");
    out.println("</TABLE><p>\n");
  }

  void printItemHeader()
  {
    out.println("<TABLE border=\"1\" summary=\"List of items\">\n"+
                "<THEAD>\n"+
                "<TR><TH>Designation<TH>Price<TH>Bids<TH>End Date<TH>Bid Now\n"+
                "<TBODY>\n");
  }


  void printItemFooter(String URLprevious, String URLafter)
  {
    out.println("</TABLE>\n");
    out.println("<p><CENTER>\n"+URLprevious+"\n&nbsp&nbsp&nbsp"+URLafter+"\n</CENTER>\n");
  }

  /** 
   * Item footer printed function
   *
   * @since 1.1
   */
  void printItemFooter()
  {
    out.println("</TABLE>\n");
  }

  /**
   * Send service unavailable (http 503) response to client
   *
   */
    void sendServiceUnavailable(String jndiName, Object jndiValue)
  {
      if (jndiValue !=null && 
	  jndiValue.getClass() == MicrorebootInProgress.class) {
	  try {
	      response.addHeader("Retry-After", "2");
	      response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
				 "Microreboot of "+jndiName+ " is in progress. Retry later!");

	  } catch (Exception e) {
	      System.out.println("sendError failed: "+e);
	  }
      } else {
	  printHTMLheader("RUBiS ERROR: "+jndiName.substring(3));
	  printHTML("<h3>Your request has not been processed due to the following error :</h3><br>");
	  printHTML("Class cast error  JNDI name: "+jndiName
		    +" lookuped value: " + jndiValue);
	  printHTMLfooter();
      }
  }
}
