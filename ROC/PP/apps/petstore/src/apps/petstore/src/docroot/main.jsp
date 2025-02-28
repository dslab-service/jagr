<%--
 Copyright 2002 Sun Microsystems, Inc. All rights reserved.
 
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 
 - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 
 - Redistribution in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.
 
 Neither the name of Sun Microsystems, Inc. or the names of
 contributors may be used to endorse or promote products derived
 from this software without specific prior written permission.
 
 This software is provided "AS IS," without a warranty of any
 kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN
 OR ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR
 FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
 PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF
 LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE,
 EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 
 You acknowledge that Software is not designed, licensed or intended
 for use in the design, construction, operation or maintenance of
 any nuclear facility.
--%>

<%--
 % $Id: main.jsp,v 1.1.1.1 2003/03/07 08:30:30 emrek Exp $
 % Copyright 2001 Sun Microsystems, Inc. All rights reserved.
 % Copyright 2001 Sun Microsystems, Inc. Tous droits r�serv�s.
--%>

<map name="petmap">
<area href="category.screen?category_id=BIRDS" 
      alt="Birds" 
      coords="72,2,280,250">
<area href="category.screen?category_id=FISH" 
      alt="Fish" 
      coords="2,180,72,250">
<area href="category.screen?category_id=DOGS" 
      alt="Dogs" 
      coords="60,250,130,320">
<area href="category.screen?category_id=REPTILES" 
      alt="Reptiles" 
      coords="140,270,210,340">
<area href="category.screen?category_id=CATS" 
      alt="Cats" 
      coords="225,240,295,310">
<area href="category.screen?category_id=BIRDS" 
      alt="Birds" 
      coords="280,180,350,250">
</map>

<img src="images/splash.gif" 
     alt="Pet Selection Map"
     usemap="#petmap" 
     width="350" 
     height="355" 
     border="0">

