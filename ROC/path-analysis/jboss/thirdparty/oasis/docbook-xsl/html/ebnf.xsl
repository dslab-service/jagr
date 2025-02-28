<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:doc="http://nwalsh.com/xsl/documentation/1.0"
                exclude-result-prefixes="doc"
                version='1.0'>

<!-- ********************************************************************
     $Id: ebnf.xsl,v 1.1.1.1 2002/11/16 03:16:39 mikechen Exp $
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://nwalsh.com/docbook/xsl/ for copyright
     and other information.

     ******************************************************************** -->

<doc:reference xmlns="">
<referenceinfo>
<releaseinfo role="meta">
$Id: ebnf.xsl,v 1.1.1.1 2002/11/16 03:16:39 mikechen Exp $
</releaseinfo>
<author><surname>Walsh</surname>
<firstname>Norman</firstname></author>
<copyright><year>1999</year><year>2000</year>
<holder>Norman Walsh</holder>
</copyright>
</referenceinfo>
<title>HTML EBNF Reference</title>

<partintro>
<section><title>Introduction</title>

<para>This is technical reference documentation for the DocBook XSL
Stylesheets; it documents (some of) the parameters, templates, and
other elements of the stylesheets.</para>

<para>This reference describes the templates and parameters relevant
to formatting EBNF markup.</para>

<para>This is not intended to be <quote>user</quote> documentation.
It is provided for developers writing customization layers for the
stylesheets, and for anyone who's interested in <quote>how it
works</quote>.</para>

<para>Although I am trying to be thorough, this documentation is known
to be incomplete. Don't forget to read the source, too :-)</para>
</section>
</partintro>
</doc:reference>

<!-- ==================================================================== -->

<xsl:param name="ebnf.table.bgcolor">#F5DCB3</xsl:param>

<doc:param name="ebnf.table.bgcolor" xmlns="">
<refpurpose>Background color for EBNF tables</refpurpose>
<refdescription>
<para>Sets the background color for EBNF tables. No <sgmltag>bgcolor</sgmltag>
attribute is output if <varname>ebnf.table.bgcolor</varname> is set to
the null string. The default value matches the value used in recent
online versions of the W3C's XML Spec productions.</para>
</refdescription>
</doc:param>

<xsl:param name="ebnf.table.border">1</xsl:param>

<doc:param name="ebnf.table.border" xmlns="">
<refpurpose>Selects border on EBNF tables</refpurpose>
<refdescription>
<para>Selects the border on EBNF tables. If non-zero, the tables have
borders, otherwise they don't.</para>
</refdescription>
</doc:param>

<!-- ==================================================================== -->

<xsl:template match="productionset">
  <table width="100%" cellpadding="5">
    <xsl:if test="$ebnf.table.bgcolor != ''">
      <xsl:attribute name="bgcolor">
	<xsl:value-of select="$ebnf.table.bgcolor"/>
      </xsl:attribute>
    </xsl:if>
    <xsl:if test="$ebnf.table.border != 0">
      <xsl:attribute name="border">1</xsl:attribute>
    </xsl:if>
    <xsl:attribute name="class">
      <xsl:value-of select="name(.)"/>
    </xsl:attribute>
    <xsl:attribute name="summary">
      <xsl:text>EBNF</xsl:text>
      <xsl:if test="title">
	<xsl:text> for </xsl:text>
	<xsl:value-of select="title"/>
      </xsl:if>
    </xsl:attribute>

    <xsl:if test="title">
      <tr>
	<th align="left" valign="top" class="{name(.)}">
	  <xsl:apply-templates select="title"/>
	</th>
      </tr>
    </xsl:if>
    <tr>
      <td>
	<table border="0" width="99%" cellpadding="0">
	  <xsl:if test="$ebnf.table.bgcolor != ''">
	    <xsl:attribute name="bgcolor">
	      <xsl:value-of select="$ebnf.table.bgcolor"/>
	    </xsl:attribute>
	  </xsl:if>
	  <xsl:attribute name="class">
	    <xsl:value-of select="name(.)"/>
	  </xsl:attribute>
	  <xsl:attribute name="summary">EBNF productions</xsl:attribute>
	  <xsl:apply-templates select="production|productionrecap"/>
	</table>
      </td>
    </tr>
  </table>
</xsl:template>

<xsl:template match="productionset/title">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="production">
  <xsl:param name="recap" select="false()"/>
  <xsl:variable name="id"><xsl:call-template name="object.id"/></xsl:variable>
  <tr>
    <td align="left" valign="top" width="3%">
      <xsl:text>[</xsl:text>
      <xsl:number count="production" level="any"/>
      <xsl:text>]</xsl:text>
    </td>
    <td align="right" valign="top" width="10%">
      <xsl:choose>
	<xsl:when test="$recap">
	  <a>
	    <xsl:attribute name="href">
	      <xsl:call-template name="href.target">
		<xsl:with-param name="object" select="."/>
	      </xsl:call-template>
	    </xsl:attribute>
	    <xsl:apply-templates select="lhs"/>
	  </a>
	</xsl:when>
	<xsl:otherwise>
	  <a name="{$id}"/>
	  <xsl:apply-templates select="lhs"/>
	</xsl:otherwise>
      </xsl:choose>
    </td>
    <td valign="top" width="5%" align="center"><tt>::=</tt></td>
    <td valign="top" width="52%">
      <xsl:apply-templates select="rhs"/>
    </td>
    <td align="left" valign="top" width="30%">
      <xsl:choose>
	<xsl:when test="rhs/lineannotation|constraint">
          <xsl:apply-templates select="rhs/lineannotation" mode="rhslo"/>
          <xsl:apply-templates select="constraint"/>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:text>&#160;</xsl:text>
	</xsl:otherwise>
      </xsl:choose>
    </td>
  </tr>
</xsl:template>

<xsl:template match="productionrecap">
  <xsl:variable name="targets" select="id(@linkend)"/>
  <xsl:variable name="target" select="$targets[1]"/>

  <xsl:if test="count($targets)=0">
    <xsl:message>
      <xsl:text>Error: no ID for productionrecap linkend: </xsl:text>
      <xsl:value-of select="@linkend"/>
      <xsl:text>.</xsl:text>
    </xsl:message>
  </xsl:if>

  <xsl:if test="count($targets)>1">
    <xsl:message>
      <xsl:text>Warning: multiple "IDs" for productionrecap linkend: </xsl:text>
      <xsl:value-of select="@linkend"/>
      <xsl:text>.</xsl:text>
    </xsl:message>
  </xsl:if>

  <xsl:apply-templates select="$target">
    <xsl:with-param name="recap" select="true()"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="lhs">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="rhs">
  <xsl:apply-templates/>
  <xsl:if test="following-sibling::rhs">
    <xsl:text> |</xsl:text>
    <br/>
  </xsl:if>
</xsl:template>

<xsl:template match="nonterminal">
  <xsl:variable name="linkend">
    <xsl:call-template name="xpointer.idref">
      <xsl:with-param name="xpointer" select="@def"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:call-template name="check.id.unique">
    <xsl:with-param name="linkend" select="$linkend"/>
  </xsl:call-template>

  <xsl:call-template name="check.idref.targets">
    <xsl:with-param name="linkend" select="$linkend"/>
    <xsl:with-param name="element-list">production</xsl:with-param>
  </xsl:call-template>

  <!-- If you don't provide content, you can't point outside this doc. -->
  <xsl:choose>
    <xsl:when test="*|text()"><!--nop--></xsl:when>
    <xsl:otherwise>
      <xsl:if test="$linkend = ''">
	<xsl:message>
	  <xsl:text>Non-terminals with no content must point to </xsl:text>
	  <xsl:text>production elements in the current document.</xsl:text>
	</xsl:message>
	<xsl:message>
	  <xsl:text>Invalid xpointer for empty nt: </xsl:text>
	  <xsl:value-of select="@def"/>
	</xsl:message>
      </xsl:if>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:variable name="href">
    <xsl:choose>
      <xsl:when test="$linkend != ''">
	<xsl:variable name="targets" select="id($linkend)"/>
	<xsl:variable name="target" select="$targets[1]"/>
	<xsl:call-template name="href.target">
	  <xsl:with-param name="object" select="$target"/>
	</xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="@def"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <a href="{$href}">
    <xsl:choose>
      <xsl:when test="*|text()">
	<xsl:apply-templates/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:choose>
	  <xsl:when test="$linkend != ''">
	    <xsl:variable name="targets" select="id($linkend)"/>
	    <xsl:variable name="target" select="$targets[1]"/>
	    <xsl:apply-templates select="$target/lhs"/>
	  </xsl:when>
	  <xsl:otherwise>
	    <xsl:text>???</xsl:text>
	  </xsl:otherwise>
	</xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </a>
</xsl:template>

<xsl:template match="rhs/lineannotation">
  <!--nop-->
</xsl:template>

<xsl:template match="rhs/lineannotation" mode="rhslo">
  <xsl:text>/*&#160;</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>&#160;*/</xsl:text>
  <br/>
</xsl:template>

<xsl:template match="constraint">
  <xsl:call-template name="check.id.unique">
    <xsl:with-param name="linkend" select="@linkend"/>
  </xsl:call-template>

  <xsl:call-template name="check.idref.targets">
    <xsl:with-param name="linkend" select="@linkend"/>
    <xsl:with-param name="element-list">constraintdef</xsl:with-param>
  </xsl:call-template>

  <xsl:variable name="href">
    <xsl:variable name="targets" select="id(@linkend)"/>
    <xsl:variable name="target" select="$targets[1]"/>
    <xsl:call-template name="href.target">
      <xsl:with-param name="object" select="$target"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:text>[&#160;</xsl:text>

  <xsl:choose>
    <xsl:when test="@role">
      <xsl:value-of select="@role"/>
      <xsl:text>: </xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="targets" select="id(@linkend)"/>
      <xsl:variable name="target" select="$targets[1]"/>
      <xsl:if test="$target/@role">
	<xsl:value-of select="$target/@role"/>
	<xsl:text>: </xsl:text>
      </xsl:if>
    </xsl:otherwise>
  </xsl:choose>

  <a href="{$href}">
    <xsl:variable name="targets" select="id(@linkend)"/>
    <xsl:variable name="target" select="$targets[1]"/>
    <xsl:apply-templates select="$target" mode="title.markup"/>
  </a>
  <xsl:text>&#160;]</xsl:text>
  <xsl:if test="following-sibling::constraint">
    <br/>
  </xsl:if>
</xsl:template>

<xsl:template match="constraintdef">
  <xsl:variable name="id"><xsl:call-template name="object.id"/></xsl:variable>
  <div class="{name(.)}">
    <a name="{$id}"/>
    <xsl:apply-templates/>
  </div>
</xsl:template>

<xsl:template match="constraintdef/title">
  <p><b><xsl:apply-templates/></b></p>
</xsl:template>

<!-- ==================================================================== -->

</xsl:stylesheet>
