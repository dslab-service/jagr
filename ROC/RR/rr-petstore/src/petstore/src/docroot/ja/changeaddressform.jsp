<%--
 % $Id: changeaddressform.jsp,v 1.1.1.1 2002/10/03 21:17:36 candea Exp $
 % Copyright 2001 Sun Microsystems, Inc. All rights reserved.
 % Copyright 2001 Sun Microsystems, Inc. Tous droits rεervε.
--%>
<%@ page contentType="text/html;charset=SJIS" %>
<%@ page import="com.sun.j2ee.blueprints.customer.util.ContactInformation" %>

<%
ContactInformation contact = (ContactInformation)request.getAttribute("contact");
%>
<table>
  <tr>
    <td align="right">Ό:</td>
    <td align="left" colspan="2">
      <input type="text" name="family_name" size="30" maxlength="30"
        value="<%=contact.getFamilyName()%>">
    </td>
  </tr>
  <tr>
    <td align="right">ΌO:</td>
    <td align="left" colspan="2">
      <input type="text" name="given_name" size="30" maxlength="30"
        value="<%=contact.getGivenName()%>">
    </td>
  </tr>
  <tr>
    <td align="right">¬ΌΤn:</td>
    <td align="left" colspan="2">
      <input type="text" name="address_1" size="55" maxlength="70"
        value="<%=contact.getAddress().getStreetName1()%>">
    </td>
  </tr>
  <tr>
    <td></td>
    <td align="left" colspan="2">
      <input type="text" name="address_2" size="55" maxlength="70"
        value="<%=contact.getAddress().getStreetName2()%>">
    </td>
  </tr>
  <tr>
    <td align="right">s¬ΊΌ:</td>
    <td align="left" colspan ="2">
      <input type="text" name="city" size="55" maxlength="70"
        value="<%=contact.getAddress().getCity()%>">
    </td>
  </tr>

  <% String state = contact.getAddress().getState(); %>

  <tr>
    <td>BΌ/sΉ{§Ό:</td>
    <td align="left">
      <select size="1" name="state_or_province">
        <option
          value="s"
          <% if (state.equals("s") ||
                 state.equals("s")) { %> selected <% } %>
          >s</option>
        <option
          value="εγ{"
          <% if (state.equals("εγ{") ||
                 state.equals("εγ{")) { %> selected <% } %>
          >εγ{</option>
        <option
          value="·μ§"
          <% if (state.equals("·μ§") |
                 state.equals("·μ§")) { %> selected <% } %>
          >·μ§</option>
      </select>
    </td>
    <td>XΦΤ:
      <input type="text" name="postal_code" size="12" maxlength="12"
        value="<%=contact.getAddress().getZipCode()%>">
    </td>
  </tr>

  <% String country = contact.getAddress().getCountry(); %>

  <tr>
    <td>Ό:</td>
    <td align="left" colspan ="2">
      <select size="1" name="country">
        <option
          value="USA"
          <% if (country.equals("USA")) { %> selected <% } %>
          >USA</option>
        <option
          value="Canada"
          <% if (country.equals("Canada")) { %> selected <% } %>
          >Canada</option>
        <option
          value="Japan"
          <% if (country.equals("Japan")) { %> selected <% } %>
          >Japan</option>
      </select>
    </td>
  </tr>
  <tr>
    <td>dbΤ:</td>
    <td align="left" colspan ="2">
      <input type="text" name="telephone_number" size="12" maxlength="70"
        value="<%=contact.getTelephone()%>">
    </td>
  </tr>
</table>
