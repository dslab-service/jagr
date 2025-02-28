<?xml version='1.0'?>

<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>

<xsl:param name="thedate"/>
<xsl:param name="java_version"/>
<xsl:param name="java_vendor"/>
<xsl:param name="java_vm_version"/>
<xsl:param name="java_vm_name"/>
<xsl:param name="java_vm_info"/>
<xsl:param name="os_name"/>
<xsl:param name="os_arch"/>
<xsl:param name="os_version"/>


<xsl:output method='text'/> 

<xsl:template match='/'>

<xsl:variable name="numberOfTests" select="sum(//@tests)"/>
<xsl:variable name="numberOfErrors" select="sum(//@errors)"/>
<xsl:variable name="numberOfFailures" select="sum(//@failures)"/>
<xsl:variable name="numberOfSuccesses" select="$numberOfTests - $numberOfErrors - $numberOfFailures"/>

JBoss daily test results

SUMMARY

Number of tests run:   <xsl:value-of select="$numberOfTests"/>

--------------------------------------------

Successful tests:      <xsl:value-of select="$numberOfSuccesses"/>

Errors:                <xsl:value-of select="$numberOfErrors"/>

Failures:              <xsl:value-of select="$numberOfFailures"/>

--------------------------------------------



[time of test: <xsl:value-of select="$thedate"/> GMT]
[java.version: <xsl:value-of select="$java_version"/>]
[java.vendor: <xsl:value-of select="$java_vendor"/>]
[java.vm.version: <xsl:value-of select="$java_vm_version"/>]
[java.vm.name: <xsl:value-of select="$java_vm_name"/>]
[java.vm.info: <xsl:value-of select="$java_vm_info"/>]
[os.name: <xsl:value-of select="$os_name"/>]
[os.arch: <xsl:value-of select="$os_arch"/>]
[os.version: <xsl:value-of select="$os_version"/>]

See http://lubega.com for full details

NOTE: If there are any errors shown above - this mail is only highlighting 
them - it is NOT indicating that they are being looked at by anyone.

It is assumed that whoever makes change(s) to jboss that 
break the test will be fixing the test or jboss, as appropriate!

--------------------------------------------

<xsl:if test='$numberOfFailures!=0 or $numberOfErrors!=0'>

DETAILS OF ERRORS

[details not shown - as this makes the mail too big to reach the sf mailing list]

</xsl:if>


</xsl:template>

</xsl:stylesheet>
