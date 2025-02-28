<?xml version='1.0' encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:fox="http://xml.apache.org/fop/extensions"
                version='1.0'>

<!-- ********************************************************************
     $Id: fop.xsl,v 1.1.1.1 2002/11/16 03:16:39 mikechen Exp $
     ********************************************************************
     (c) Stephane Bline Peregrine Systems 2001
     Driver file to allow pdf bookmarking (based on fop implementation).
     ******************************************************************** -->
<!--
In PDF bookmarks can't be used characters with code>255. This version of file
translates characters with code>255 back to ASCII.

   Pavel Zampach (zampach@volny.cz)
-->

<xsl:variable name="a-dia" select=
"'&#257;&#259;&#261;&#263;&#265;&#267;&#269;&#271;&#273;&#275;&#277;&#279;&#281;&#283;&#339;&#285;&#287;&#289;&#291;&#293;&#295;&#297;&#299;&#301;&#303;&#305;&#309;&#311;&#314;&#316;&#318;&#320;&#322;&#324;&#326;&#328;&#331;&#333;&#335;&#337;&#341;&#343;&#345;&#347;&#349;&#351;&#353;&#355;&#357;&#359;&#361;&#363;&#365;&#367;&#369;&#371;&#373;&#375;&#378;&#380;&#382;&#256;&#258;&#260;&#262;&#264;&#266;&#268;&#270;&#272;&#274;&#276;&#278;&#280;&#282;&#338;&#284;&#286;&#288;&#290;&#292;&#294;&#296;&#298;&#300;&#302;&#304;&#308;&#310;&#313;&#315;&#317;&#319;&#321;&#323;&#325;&#327;&#330;&#332;&#334;&#336;&#340;&#342;&#344;&#346;&#348;&#350;&#352;&#354;&#356;&#358;&#360;&#362;&#364;&#366;&#368;&#370;&#372;&#374;&#376;&#377;&#379;&#381;'"/>
<xsl:variable name="a-asc" select=
"'aaaccccddeeeeeegggghhiiiiijklllllnnnnooorrrsssstttuuuuuuwyzzzAAACCCCDDEEEEEEGGGGHHIIIIIJKLLLLLNNNNOOORRRSSSSTTTUUUUUUWYYZZZ'"/>


<xsl:template match="set" mode="outline">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="bookmark-label">
    <xsl:apply-templates select="." mode="label.markup"/>
    <xsl:apply-templates select="." mode="title.markup"/>
  </xsl:variable>

  <fox:outline internal-destination="{$id}">
    <fox:label>
      <xsl:value-of select="translate($bookmark-label, $a-dia, $a-asc)"/>
    </fox:label>
  <xsl:if test="book">
      <xsl:apply-templates select="book"
                           mode="outline"/>
  </xsl:if>
  </fox:outline>
</xsl:template>

<xsl:template match="book" mode="outline">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="bookmark-label">
    <xsl:apply-templates select="." mode="label.markup"/>
    <xsl:apply-templates select="." mode="title.markup"/>
  </xsl:variable>

  <fox:outline internal-destination="{$id}">
    <fox:label>
      <xsl:value-of select="translate($bookmark-label, $a-dia, $a-asc)"/>
    </fox:label>

  <xsl:if test="part|preface|chapter|appendix">
      <xsl:apply-templates select="part|preface|chapter|appendix"
                           mode="outline"/>
  </xsl:if>
  </fox:outline>
</xsl:template>


<xsl:template match="part" mode="outline">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="bookmark-label">
    <xsl:apply-templates select="." mode="label.markup"/>
    <xsl:apply-templates select="." mode="title.markup"/>
  </xsl:variable>

  <fox:outline internal-destination="{$id}">
    <fox:label>
      <xsl:value-of select="translate($bookmark-label, $a-dia, $a-asc)"/>
    </fox:label>

  <xsl:if test="chapter|appendix|preface|reference">
      <xsl:apply-templates select="chapter|appendix|preface|reference"
                           mode="outline"/>
  </xsl:if>
  </fox:outline>
</xsl:template>

<xsl:template match="preface|chapter|appendix"
              mode="outline">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="bookmark-label">
    <xsl:apply-templates select="." mode="label.markup"/>
    <xsl:apply-templates select="." mode="title.markup"/>
  </xsl:variable>

  <fox:outline internal-destination="{$id}">
    <fox:label>
      <xsl:value-of select="translate($bookmark-label, $a-dia, $a-asc)"/>
    </fox:label>

  <xsl:if test="section|sect1">
      <xsl:apply-templates select="section|sect1"
                           mode="outline"/>
  </xsl:if>
  </fox:outline>
</xsl:template>

<xsl:template match="section|sect1|sect2|sect3|sect4|sect5"
              mode="outline">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="bookmark-label">
    <xsl:apply-templates select="." mode="label.markup"/>
    <xsl:apply-templates select="." mode="title.markup"/>
  </xsl:variable>

  <fox:outline internal-destination="{$id}">
    <fox:label>
      <xsl:value-of select="translate($bookmark-label, $a-dia, $a-asc)"/>
    </fox:label>

  <xsl:if test="section|sect2|sect3|sect4|sect5">
      <xsl:apply-templates select="section|sect2|sect3|sect4|sect5"
                           mode="outline"/>
  </xsl:if>
  </fox:outline>
</xsl:template>

<!-- Added missing template for "article" -->
<xsl:template match="article"
              mode="outline">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="bookmark-label">
    <xsl:apply-templates select="." mode="label.markup"/>
    <xsl:apply-templates select="." mode="title.markup"/>
  </xsl:variable>

  <fox:outline internal-destination="{$id}">
    <fox:label>
      <xsl:value-of select="translate($bookmark-label, $a-dia, $a-asc)"/>
    </fox:label>

  <xsl:if test="section|sect1|appendix|bibliography|glossary|index">
      <xsl:apply-templates select="section|sect1|appendix|bibliography|glossary|index"
                           mode="outline"/>
  </xsl:if>
  </fox:outline>
</xsl:template>


<xsl:template match="bibliography|glossary|index"
              mode="outline">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="bookmark-label">
    <xsl:apply-templates select="." mode="label.markup"/>
    <xsl:apply-templates select="." mode="title.markup"/>
  </xsl:variable>

  <fox:outline internal-destination="{$id}">
    <fox:label>
      <xsl:value-of select="translate($bookmark-label, $a-dia, $a-asc)"/>
    </fox:label>
  </fox:outline>
</xsl:template>

<xsl:template match="title" mode="outline">
  <xsl:apply-templates/>
</xsl:template>

</xsl:stylesheet>

