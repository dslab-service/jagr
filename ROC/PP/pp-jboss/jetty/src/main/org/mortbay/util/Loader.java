// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id: Loader.java,v 1.1.1.1 2003/03/07 08:26:05 emrek Exp $
// ========================================================================

package org.mortbay.util;

/* ------------------------------------------------------------ */
/** ClassLoader Helper.
 * This helper class allows classes to be loaded either from the
 * Thread's ContextClassLoader, the classloader of the derived class
 * or the system ClassLoader.
 *
 * <B>Usage:</B><PRE>
 * public class MyClass {
 *     void myMethod() {
 *          ...
 *          Class c=Loader.loadClass(this.getClass(),classname);
 *          ...
 *     }
 * </PRE>          
 * @version $Id: Loader.java,v 1.1.1.1 2003/03/07 08:26:05 emrek Exp $
 * @author Greg Wilkins (gregw)
 */
public class Loader
{
    /* ------------------------------------------------------------ */
    public static Class loadClass(Class loadClass,String name)
        throws ClassNotFoundException
    {
        ClassLoader loader=Thread.currentThread().getContextClassLoader();
        if (loader==null)
            loader=loadClass.getClassLoader();
        if (loader==null)
            return Class.forName(name);
        return loader.loadClass(name);
    }
}

