/*
 * JORAM: Java(TM) Open Reliable Asynchronous Messaging
 * Copyright (C) 2002 INRIA
 * Contact: joram-team@objectweb.org
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 * 
 * Initial developer(s): Jeff Mesnil (jmesnil@inrialpes.fr)
 * Contributor(s): ______________________________________.
 */

package org.objectweb.jtests.jms.conform.message;

import org.objectweb.jtests.jms.framework.PTPTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import javax.jms.*;
import java.util.Vector;
import java.util.Enumeration;

/**
 * Test the different types of messages provided by JMS.
 * <br />
 * JMS provides 6 types of messages which differs by the type of their body:
 * <ol>
 *   <li><code>Message</code> which doesn't have a body</li>
 *   <li><code>TextMessage</code> with a <code>String</code> as body</li>
 *   <li><code>ObjectMessage</code> with any <code>Object</code> as body</li>
 *   <li><code>BytesMessage</code> with a body made of <code>bytes</code></li>
 *   <li><code>MapMessage</code> with name-value pairs of Java primitives in its body</li>
 *   <li><code>StreamMessage</code> with a stream of Java primitives as body</li>
 *  </ol>
 * <br />
 * For each of this type of message, we test that a message can be sent and received
 * with an empty body or not.
 * 
 * @author Jeff Mesnil (jmesnil@inrialpes.fr)
 * @version $Id: MessageTypeTest.java,v 1.1.1.1 2002/11/16 03:16:43 mikechen Exp $
 */
public class MessageTypeTest extends PTPTestCase {

   /**
    * Send a <code>StreamMessage</code> with 2 Java primitives in its body (a <code>
    * String</code> and a <code>double</code>).
    * <br />
    * Receive it and test that the values of the primitives of the body are correct
    */
   public void testStreamMessage_2() {
     try {
       StreamMessage message = senderSession.createStreamMessage();
       message.writeString("pi");
       message.writeDouble(3.14159);
       sender.send(message);

       Message m = receiver.receive();
       assertTrue("The message should be an instance of StreamMessage.\n",
  		 m instanceof StreamMessage);
       StreamMessage msg = (StreamMessage)m;
       assertEquals("pi", msg.readString());
       assertEquals(3.14159, msg.readDouble(), 0);
     } catch (JMSException e) {
       fail(e);
     }
   }

   /**
    * Send a <code>StreamMessage</code> with an empty body.
    * <br />
    * Receive it and test if the message is effectively an instance of 
    * <code>StreamMessage</code>
    */
   public void testStreamMessage_1() {
     try {
       StreamMessage message = senderSession.createStreamMessage();
       sender.send(message);

       Message msg = receiver.receive();
       assertTrue("The message should be an instance of StreamMessage.\n",
  		 msg instanceof StreamMessage);
     } catch (JMSException e) {
       fail(e);
     }
   }

   /**
    * Test in MapMessage the conversion between <code>getObject("foo")</code> and
    * <code>getDouble("foo")</code> (the later returning a java.lang.Double and the former a double)
    */
   public void testMapMessageConversion() {
     try {
       MapMessage message = senderSession.createMapMessage();
       message.setDouble("pi", 3.14159);
       sender.send(message);

       Message m = receiver.receive();
       assertTrue("The message should be an instance of MapMessage.\n",
  		 m instanceof MapMessage);
       MapMessage msg = (MapMessage)m;
       assertTrue(msg.getObject("pi") instanceof Double);
       assertEquals(3.14159, ((Double)msg.getObject("pi")).doubleValue(), 0);
       assertEquals(3.14159, msg.getDouble("pi"), 0);
     } catch (JMSException e) {
       fail(e);
     }
   }

   /**
    * Test that the <code>MapMessage.getMapNames()</code> method returns an
    * empty <code>Enumeration</code> when no map has been defined before.
    * <br />
    * Also test that the same method returns the correct names of the map.
    */
   public void testgetMapNames() {
     try {
       MapMessage message = senderSession.createMapMessage();
       Enumeration e = message.getMapNames();
       assertTrue("No map yet defined.\n",
  		 !e.hasMoreElements());
       message.setDouble("pi", 3.14159);
       e = message.getMapNames();
       assertEquals("pi", (String)(e.nextElement()));
     } catch (JMSException e) {
       fail(e);
     }
   }

   /**
    * Send a <code>MapMessage</code> with 2 Java primitives in its body (a <code>
    * String</code> and a <code>double</code>).
    * <br />
    * Receive it and test that the values of the primitives of the body are correct
    */
   public void testMapMessage_2() {
     try {
       MapMessage message = senderSession.createMapMessage();
       message.setString("name", "pi");
       message.setDouble("value",3.14159);
       sender.send(message);

       Message m = receiver.receive();
       assertTrue("The message should be an instance of MapMessage.\n",
    		 m instanceof MapMessage);
       MapMessage msg = (MapMessage)m;
       assertEquals("pi", msg.getString("name"));
       assertEquals(3.14159, msg.getDouble("value"), 0);
     } catch (JMSException e) {
       fail(e);
     }
   }

   /**
    * Send a <code>MapMessage</code> with an empty body.
    * <br />
    * Receive it and test if the message is effectively an instance of 
    * <code>MapMessage</code>
    */
   public void testMapMessage_1() {
     try {
       MapMessage message = senderSession.createMapMessage();
       sender.send(message);

       Message msg = receiver.receive();
       assertTrue("The message should be an instance of MapMessage.\n",
 		 msg instanceof MapMessage);
     } catch (JMSException e) {
       fail(e);
     }
   }

   /**
    * Send an <code>ObjectMessage</code> with a <code>Vector</code> (composed of a <code>
    * String</code> and a <code>double</code>) in its body.
    * <br />
    * Receive it and test that the values of the primitives of the body are correct
    */
   public void testObjectMessage_2() {
     try {    
       Vector vector = new Vector();
       vector.add("pi");
       vector.add(new Double(3.14159));

       ObjectMessage message = senderSession.createObjectMessage();
       message.setObject(vector);
       sender.send(message);

       Message m = receiver.receive();
       assertTrue("The message should be an instance of ObjectMessage.\n",
  		 m instanceof ObjectMessage);
       ObjectMessage msg = (ObjectMessage)m;
       assertEquals(vector, msg.getObject());
     } catch (JMSException e) {
       fail(e);
     }
   }

   /**
    * Send a <code>ObjectMessage</code> with an empty body.
    * <br />
    * Receive it and test if the message is effectively an instance of 
    * <code>ObjectMessage</code>
    */
   public void testObjectMessage_1() {
     try {
       ObjectMessage message = senderSession.createObjectMessage();
       sender.send(message);

       Message msg = receiver.receive();
       assertTrue("The message should be an instance of ObjectMessage.\n",
  		 msg instanceof ObjectMessage);
     } catch (JMSException e) {
       fail(e);
     }
   }

   /**
    * Send a <code>BytesMessage</code> with 2 Java primitives in its body (a <code>
    * String</code> and a <code>double</code>).
    * <br />
    * Receive it and test that the values of the primitives of the body are correct
    */
   public void testBytesMessage_2() {
     try {
       byte[] bytes = new String("pi").getBytes();
       BytesMessage message = senderSession.createBytesMessage();
       message.writeBytes(bytes);
       message.writeDouble(3.14159);
       sender.send(message);

       Message m = receiver.receive();
       assertTrue("The message should be an instance of BytesMessage.\n",
  		 m instanceof BytesMessage);
       BytesMessage msg = (BytesMessage)m;
       byte[] receivedBytes = new byte[bytes.length];
       msg.readBytes(receivedBytes);
       assertEquals(new String(bytes), new String(receivedBytes));
       assertEquals(3.14159, msg.readDouble(), 0);  
     } catch (JMSException e) {
       fail(e);
     }
   }

   /**
    * Send a <code>BytesMessage</code> with an empty body.
    * <br />
    * Receive it and test if the message is effectively an instance of 
    * <code>BytesMessage</code>
    */
   public void testBytesMessage_1() {
     try {
       BytesMessage message = senderSession.createBytesMessage();
       sender.send(message);

       Message msg = receiver.receive();
       assertTrue("The message should be an instance of BytesMessage.\n",
 		 msg instanceof BytesMessage);
     } catch (JMSException e) {
       fail(e);
     }
   }

   /**
    * Send a <code>TextMessage</code> with a <code>String</code> in its body.
    * <br />
    * Receive it and test that the received <code>String</code> corresponds to
    * the sent one.
    */
   public void testTextMessage_2() {
     try {
       TextMessage message = senderSession.createTextMessage();
       message.setText("testTextMessage_2");
       sender.send(message);

       Message m = receiver.receiveNoWait();
       assertTrue("The message should be an instance of TextMessage.\n",
  		 m instanceof TextMessage);
       TextMessage msg = (TextMessage)m;
       assertEquals("testTextMessage_2", msg.getText());
     } catch (JMSException e) {
       fail(e);
     }
   }

  /**
   * Send a <code>TextMessage</code> with an empty body.
   * <br />
   * Receive it and test if the message is effectively an instance of 
   * <code>TextMessage</code>
   */
  public void testTextMessage_1() {
    try {
      TextMessage message = senderSession.createTextMessage();
      sender.send(message);

      Message msg = receiver.receive();
      assertTrue("The message should be an instance of TextMessage.\n",
   		 msg instanceof TextMessage);
    } catch (JMSException e) {
      fail(e);
    }
  }

  /** 
   * Method to use this class in a Test suite
   */
  public static Test suite() {
    return new TestSuite(MessageTypeTest.class);
  }
  
  public MessageTypeTest(String name) {
    super(name);
  }
}
