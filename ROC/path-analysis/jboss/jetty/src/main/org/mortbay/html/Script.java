// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd., Sydney
// $Id: Script.java,v 1.1.1.1 2002/11/16 03:16:49 mikechen Exp $
// ========================================================================

package org.mortbay.html;


/* -------------------------------------------------------------------- */
/** HTML Script Block.
 */
public class Script extends Block
{
    public static final String javascript = "JavaScript";

    /* ------------------------------------------------------------ */
    /** Construct a script element.
     * @param lang Language of Script */
    public Script(String script, String lang)
    {
        super("script");
        attribute("language",lang);
        add(script);
    }

    /* ------------------------------------------------------------ */
    /** Construct a JavaScript script element */
    public Script(String script)
    {
        this(script, javascript);
    }
};


