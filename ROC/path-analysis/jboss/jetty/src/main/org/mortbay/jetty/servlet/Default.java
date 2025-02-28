// ===========================================================================
// Copyright (c) 1996-2002 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id: Default.java,v 1.1.1.1 2002/11/16 03:16:49 mikechen Exp $
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.InclusiveByteRange;
import org.mortbay.http.MultiPartResponse;
import org.mortbay.util.Code;
import org.mortbay.util.IO;
import org.mortbay.util.Log;
import org.mortbay.util.URI;
import org.mortbay.util.CachedResource;
import org.mortbay.util.Resource;
import org.mortbay.util.ByteArrayISO8859Writer;


/* ------------------------------------------------------------ */
/** The default servlet.                                                 
 * This servlet, normally mapped to /, provides the handling for static 
 * content, OPTION and TRACE methods for the context.                   
 * The following initParameters are supported:                          
 * <PRE>                                                                      
 *   acceptRanges     If true, range requests and responses are         
 *                    supported                                         
 *                                                                      
 *   dirAllowed       If true, directory listings are returned if no    
 *                    welcome file is found. Else 403 Forbidden.        
 *                                                                      
 *   putAllowed       If true, the PUT method is allowed                
 *                                                                      
 *   delAllowed       If true, the DELETE method is allowed
 *
 *   redirectWelcome  If true, welcome files are redirected rather than
 *                    forwarded to.
 * </PRE>
 *                                                               
 * The MOVE method is allowed if PUT and DELETE are allowed             
 *
 * @version $Id: Default.java,v 1.1.1.1 2002/11/16 03:16:49 mikechen Exp $
 * @author Greg Wilkins (gregw)
 */
public class Default extends HttpServlet
{
    private HttpContext _httpContext;
    private ServletHandler _servletHandler;
    private String _AllowString="GET, POST, HEAD, OPTION, TRACE";
    
    private boolean _acceptRanges=true;
    private boolean _dirAllowed;
    private boolean _putAllowed;
    private boolean _delAllowed;
    private boolean _redirectWelcomeFiles;
    
    /* ------------------------------------------------------------ */
    public void init()
    {
        ServletContext config=getServletContext();
        _servletHandler=((ServletHandler.Context)config).getServletHandler();
        _httpContext=_servletHandler.getHttpContext();

        _acceptRanges=getInitBoolean("acceptRanges");
        _dirAllowed=getInitBoolean("dirAllowed");
        _putAllowed=getInitBoolean("putAllowed");
        _delAllowed=getInitBoolean("delAllowed");
        _redirectWelcomeFiles=getInitBoolean("redirectWelcome");

        if (_putAllowed)
            _AllowString+=", PUT";
        if (_delAllowed)
            _AllowString+=", DELETE";
        if (_putAllowed && _delAllowed)
            _AllowString+=", MOVE";
    }

    /* ------------------------------------------------------------ */
    private boolean getInitBoolean(String name)
    {
        String value=getInitParameter(name);
        return value.length()>0 &&
            (value.startsWith("t")||
             value.startsWith("T")||
             value.startsWith("y")||
             value.startsWith("Y")||
             value.startsWith("1"));
    }
    
    /* ------------------------------------------------------------ */
    protected void service(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
    {
        String pathInContext=(String)request.getAttribute(Dispatcher.__PATH_INFO);
        if (pathInContext==null)
            pathInContext=request.getPathInfo();
        
        boolean endsWithSlash= pathInContext.endsWith("/");
        Resource resource=_httpContext.getResource(pathInContext);
        String method=request.getMethod();

        // Is the method allowed?        
        if (_AllowString.indexOf(method)<0)
        {
            Code.debug("Method not allowed: ",method);
            if (resource!=null && resource.exists())
            {
                response.setHeader(HttpFields.__Allow,_AllowString);    
                response.sendError(HttpResponse.__405_Method_Not_Allowed);
            }
            else
                response.sendError(HttpResponse.__404_Not_Found);
            return;
        }

        // Handle the request
        try
        {
            if (Code.debug())Code.debug(method," PATH=",pathInContext," RESOURCE=",resource);
            
            // check filename
            if (method.equals(HttpRequest.__GET) ||
                method.equals(HttpRequest.__POST) ||
                method.equals(HttpRequest.__HEAD))
                handleGet(request, response,pathInContext,resource,endsWithSlash);   
            else if (method.equals(HttpRequest.__PUT))
                handlePut(request, response, pathInContext, resource); 
            else if (method.equals(HttpRequest.__DELETE))
                handleDelete(request, response, pathInContext, resource); 
            else if (method.equals(HttpRequest.__MOVE))
                handleMove(request, response, pathInContext, resource);
            else if (method.equals(HttpRequest.__OPTIONS))
                handleOptions(request, response);
            else if (method.equals(HttpRequest.__TRACE))
                _servletHandler.handleTrace(request, response);
            else
            {
                Code.debug("Unknown action:"+method);
                // anything else...
                try{
                    if (resource.exists())
                        response.sendError(HttpResponse.__501_Not_Implemented);
                    else
                        _servletHandler.notFound(request,response);
                }
                catch(Exception e) {Code.ignore(e);}
            }
        }
        catch(IllegalArgumentException e)
        {
            Code.ignore(e);
        }
        finally
        {
            if (resource!=null && !(resource instanceof CachedResource))
                resource.release();
        }
        
    }
    
    /* ------------------------------------------------------------------- */
    public void handleGet(HttpServletRequest request,
                          HttpServletResponse response,
                          String pathInContext,
                          Resource resource,
                          boolean endsWithSlash)
        throws ServletException,IOException
    {
        Code.debug("handleGet ",resource);

        if (resource==null || !resource.exists())
            _servletHandler.notFound(request,response);
        else
        {
            // Check modified dates
            if (!passConditionalHeaders(request,response,resource))
                return;

            // check if directory
            if (resource.isDirectory())
            {
                if (!endsWithSlash && !pathInContext.equals("/"))
                {
                    Code.debug("Redirect to directory/");
                    
                    String q=request.getQueryString();
                    StringBuffer buf=request.getRequestURL();
                    if (q!=null&&q.length()!=0)
                    {
                        buf.append('?');
                        buf.append(q);
                    }
                    response.sendRedirect(response.encodeRedirectURL(URI.addPaths(buf.toString(),"/")));
                    return;
                }
  
                // See if index file exists
                String welcome=_httpContext.getWelcomeFile(resource);
                if (welcome!=null)
                {
                    String ipath=URI.addPaths(pathInContext,welcome);
                    if (_redirectWelcomeFiles)
                    {
                        // Redirect to the index
                        response.sendRedirect(URI.addPaths( _httpContext.getContextPath(),ipath));
                    }
                    else
                    {
                        // Forward to the index
                        RequestDispatcher dispatcher=_servletHandler.getRequestDispatcher(ipath);
                        dispatcher.forward(request,response);
                    }
                    return;
                }

                // If we got here, no forward to index took place
                sendDirectory(request,response,resource,pathInContext.length()>1);
            }
            else // just send it
                sendData(request,response,resource,true);
        }
    }
    
    /* ------------------------------------------------------------------- */
    public void handlePut(HttpServletRequest request,
                          HttpServletResponse response,
                          String pathInContext,
                          Resource resource)
        throws ServletException,IOException
    {
        Code.debug("handlePut ",resource);

        boolean exists=resource!=null && resource.exists();
        if (exists && !passConditionalHeaders(request,response,resource))
            return;

        if (pathInContext.endsWith("/"))
        {
            if (!exists)
            {
                if (!resource.getFile().mkdirs())
                    response.sendError(HttpResponse.__403_Forbidden,
                                       "Directories could not be created");
                else
                {
                    response.setStatus(HttpResponse.__201_Created);
                    response.flushBuffer();
                }
            }
            else
            {
                response.setStatus(HttpResponse.__200_OK);
                response.flushBuffer();
            }
        }
        else
        {
            try
            {
                int toRead = request.getContentLength();
                InputStream in = request.getInputStream();
                OutputStream out = resource.getOutputStream();
                if (toRead>=0)
                    IO.copy(in,out,toRead);
                else
                    IO.copy(in,out);
                out.close();
                
                response.setStatus(exists
                                   ?HttpResponse.__200_OK
                                   :HttpResponse.__201_Created);
                response.flushBuffer();
            }
            catch (Exception ex)
            {
                Code.warning(ex);
                response.sendError(HttpResponse.__403_Forbidden,
                                   ex.getMessage());
            }
        }
    }
    
    /* ------------------------------------------------------------------- */
    public void handleDelete(HttpServletRequest request,
                             HttpServletResponse response,
                             String pathInContext,
                             Resource resource)
        throws ServletException,IOException
    {
        Code.debug("handleDelete ",resource);
        if (!resource.exists() ||
            !passConditionalHeaders(request,response,resource))
            return;
        try
        {
            // delete the file
            if (resource.delete())
            {
                response.setStatus(HttpResponse.__204_No_Content);
                response.flushBuffer();
            }
            else
                response.sendError(HttpResponse.__403_Forbidden);
        }
        catch (SecurityException sex)
        {
            Code.warning(sex);
            response.sendError(HttpResponse.__403_Forbidden, sex.getMessage());
        }
    }
    
    /* ------------------------------------------------------------------- */
    public void handleMove(HttpServletRequest request,
                           HttpServletResponse response,
                           String pathInContext,
                           Resource resource)
        throws ServletException,IOException
    {
        Code.debug("handleMove ",resource);
        if (!resource.exists() || !passConditionalHeaders(request,response,resource))
            return;
        
        String newPath = URI.canonicalPath(request.getHeader("new-uri"));
        if (newPath==null)
        {
            response.sendError(HttpResponse.__400_Bad_Request,"No new-uri");
            return;
        }

        String contextPath = _httpContext.getContextPath();
        if (contextPath!=null && !newPath.startsWith(contextPath))
        {
            response.sendError(HttpResponse.__405_Method_Not_Allowed,"Not in context");
            return;
        }

        try
        {
            String newInfo=newPath;
            if (contextPath!=null)
                newInfo=newInfo.substring(contextPath.length());
            Resource newFile = _httpContext.getBaseResource().addPath(newInfo);
     
            Code.debug("Moving "+resource+" to "+newFile);
            resource.renameTo(newFile);
            response.setStatus(HttpResponse.__204_No_Content);
            response.flushBuffer();
        }
        catch (Exception ex)
        {
            Code.warning(ex);
            response.sendError(HttpResponse.__500_Internal_Server_Error,"Error:"+ex);
            return;
        }

    }
    
    /* ------------------------------------------------------------ */
    public void handleOptions(HttpServletRequest request,
                              HttpServletResponse response)
        throws IOException
    {
        // Handle OPTIONS request for entire server
        if ("*".equals(request.getRequestURI()))
        {
            // 9.2
            response.setIntHeader(HttpFields.__ContentLength,0);
            response.setHeader(HttpFields.__Allow,_AllowString);                
            response.flushBuffer();
        }
        else
            response.sendError(HttpResponse.__404_Not_Found);
    }
    
    /* ------------------------------------------------------------ */
    /* Check modification date headers.
     */
    protected boolean passConditionalHeaders(HttpServletRequest request,
                                           HttpServletResponse response,
                                           Resource resource)
        throws IOException
    {
        if (!request.getMethod().equals(HttpRequest.__HEAD))
        {
            // check any modified headers.
            long date=0;
            
            if ((date=request.getDateHeader(HttpFields.__IfUnmodifiedSince))>0)
            {
                if (resource.lastModified() > date)
                {
                    response.sendError(HttpResponse.__412_Precondition_Failed);
                    return false;
                }
            }
            
            if ((date=request.getDateHeader(HttpFields.__IfModifiedSince))>0)
            {
                if (resource.lastModified() <= date)
                {
                    response.setStatus(HttpResponse.__304_Not_Modified);
                    response.flushBuffer();
                    return false;
                }
            }
        }
        return true;
    }
    
    
    /* ------------------------------------------------------------------- */
    protected void sendDirectory(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Resource resource,
                                 boolean parent)
        throws IOException
    {
        if (!_dirAllowed)
        {
            response.sendError(HttpResponse.__403_Forbidden);
            return;
        }
        
        Code.debug("sendDirectory: "+resource);
        String base = URI.addPaths(request.getRequestURI(),"/");
        ByteArrayISO8859Writer dir = _httpContext
            .getDirectoryListing(resource,base,parent);
        if (dir==null)
        {
            response.sendError(HttpResponse.__403_Forbidden,
                               "No directory");
            return;
        }
        response.setContentType("text/html");
        response.setContentLength(dir.length());
        
        if (!request.getMethod().equals(HttpRequest.__HEAD))
            dir.writeTo(response.getOutputStream());
    }


    /* ------------------------------------------------------------ */
    protected void sendData(HttpServletRequest request,
                            HttpServletResponse response,
                            Resource resource,
                            boolean writeHeaders)
        throws IOException
    {
        long resLength=resource.length();
        
        //  see if there are any range headers
        Enumeration reqRanges = request.getHeaders(HttpFields.__Range);
        
        if (!writeHeaders || reqRanges == null || !reqRanges.hasMoreElements())
        {
            //  if there were no ranges, send entire entity
            if (writeHeaders)
                writeHeaders(response,resource,resLength);
            OutputStream out = response.getOutputStream();
            resource.writeTo(out,0,resLength);            
            return;
        }
            
        // Parse the satisfiable ranges
        List ranges =InclusiveByteRange.satisfiableRanges(reqRanges,resLength);
        if (Code.debug())
            Code.debug("ranges: " + reqRanges + " == " + ranges);
        
        //  if there are no satisfiable ranges, send 416 response
        if (ranges==null || ranges.size()==0)
        {
            Code.debug("no satisfiable ranges");
            writeHeaders(response, resource, resLength);
            response.setStatus(HttpResponse.__416_Requested_Range_Not_Satisfiable,
                               "Requested Range Not Satisfiable");
            response.setHeader(HttpFields.__ContentRange, 
                               InclusiveByteRange.to416HeaderRangeString(resLength));
            
            OutputStream out = response.getOutputStream();
            resource.writeTo(out,0,resLength);
            return;
        }

        
        //  if there is only a single valid range (must be satisfiable 
        //  since were here now), send that range with a 216 response
        if ( ranges.size()== 1)
        {
            InclusiveByteRange singleSatisfiableRange =
                (InclusiveByteRange)ranges.get(0);
            Code.debug("single satisfiable range: " + singleSatisfiableRange);
            long singleLength = singleSatisfiableRange.getSize(resLength);
            writeHeaders(response,resource,singleLength);
            response.setStatus(HttpResponse.__206_Partial_Content,"Partial Content");
            response.setHeader(HttpFields.__ContentRange, 
                               singleSatisfiableRange.toHeaderRangeString(resLength));
            OutputStream out = response.getOutputStream();
            resource.writeTo(out,singleSatisfiableRange.getFirst(resLength),singleLength);
            return;
        }
        
        
        //  multiple non-overlapping valid ranges cause a multipart
        //  216 response which does not require an overall 
        //  content-length header
        //
        HttpContext.ResourceMetaData metaData =
            (HttpContext.ResourceMetaData)resource.getAssociate();
        String encoding = metaData.getEncoding();
        MultiPartResponse multi = new MultiPartResponse(response.getOutputStream());
        response.setStatus(HttpResponse.__206_Partial_Content,"Partial Content");

	// If the request has a "Request-Range" header then we need to
	// send an old style multipart/x-byteranges Content-Type. This
	// keeps Netscape and acrobat happy. This is what Apache does.
	String ctp;
	if (request.getHeader(HttpFields.__RequestRange)!=null)
	    ctp = "multipart/x-byteranges; boundary=";
	else
	    ctp = "multipart/byteranges; boundary=";
	response.setContentType(ctp+multi.getBoundary());

        InputStream in=(resource instanceof CachedResource)
            ?null:resource.getInputStream();
        OutputStream out = response.getOutputStream();
        long pos=0;
            
        for (int i=0;i<ranges.size();i++)
        {
            InclusiveByteRange ibr = (InclusiveByteRange) ranges.get(i);
            String header=HttpFields.__ContentRange+": "+
                ibr.toHeaderRangeString(resLength);
            Code.debug("multi range: ",encoding," ",header);
            multi.startPart(encoding,new String[]{header});

            long start=ibr.getFirst(resLength);
            long size=ibr.getSize(resLength);
            if (in!=null)
            {
                // Handle non cached resource
                if (start<pos)
                {
                    in.close();
                    in=resource.getInputStream();
                    pos=0;
                }
                if (pos<start)
                {
                    in.skip(start-pos);
                    pos=start;
                }
                IO.copy(in,out,size);
                pos+=size;
            }
            else
                // Handle cached resource
                ((CachedResource)resource).writeTo(out,start,size);
            
        }
        if (in!=null)
            in.close();
        multi.close();
        
        return;
    }

    /* ------------------------------------------------------------ */
    protected void writeHeaders(HttpServletResponse response,
                                Resource resource,
                                long count)
        throws IOException
    {
        HttpContext.ResourceMetaData metaData =
            (HttpContext.ResourceMetaData)resource.getAssociate();

        response.setContentType(metaData.getEncoding());
        if (count != -1)
        {
            if (count==resource.length())
                response.setHeader(HttpFields.__ContentLength,metaData.getLength());
            else
                response.setContentLength((int)count);
        }

        response.setHeader(HttpFields.__LastModified,metaData.getLastModified());
        
        if (_acceptRanges)
            response.setHeader(HttpFields.__AcceptRanges,"bytes");
    }

}
