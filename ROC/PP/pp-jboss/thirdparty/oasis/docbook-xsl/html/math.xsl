<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'>

<!-- ********************************************************************
     $Id: math.xsl,v 1.1.1.1 2003/03/07 08:26:16 emrek Exp $
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://nwalsh.com/docbook/xsl/ for copyright
     and other information.

     ******************************************************************** -->

<xsl:template match="inlineequation">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="alt">
</xsl:template>

</xsl:stylesheet>
