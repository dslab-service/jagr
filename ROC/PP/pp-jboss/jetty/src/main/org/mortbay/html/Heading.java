// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id: Heading.java,v 1.1.1.1 2003/03/07 08:26:05 emrek Exp $
// ---------------------------------------------------------------------------

package org.mortbay.html;

/* -------------------------------------------------------------------- */
/** HTML Heading.
 */
public class Heading extends Block
{
    private static final String[] headerTags = {
        "h1", "h2", "h3", "h4", "h5", "h6"
    };

    /* ----------------------------------------------------------------- */
    /* Construct a heading and add Element, String or Object
     * @param level The level of the heading
     * @param o The Element, String or Object of the heading.
     */
    public Heading(int level,Object o)
    {
        super((level <= headerTags.length) ? headerTags[level-1] : "h"+level);
        add(o);
    }
}

