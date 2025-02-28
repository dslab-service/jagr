// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id: GUIDGenerator.java,v 1.1.1.1 2003/03/07 08:26:05 emrek Exp $
// ========================================================================

package org.mortbay.j2ee.session;

// shamelessly distilled from ClusteredHTTPSessionService.java

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import org.apache.log4j.Category;

public class
  GUIDGenerator
{
  Category _log=Category.getInstance(getClass().getName());

  protected final static int SESSION_ID_BYTES = 16; // We want 16 Bytes for the session-id
  protected final static String SESSION_ID_HASH_ALGORITHM = "MD5";
  protected final static String SESSION_ID_RANDOM_ALGORITHM = "SHA1PRNG";

  protected MessageDigest _digest=null;
  protected Random        _random=null;

  /**
     Generate a session-id that is not guessable
     @return generated session-id
  */
  public synchronized String generateSessionId()
    {
      if (_digest==null) {
	_digest=getDigest();
      }

      if (_random==null) {
	_random=getRandom();
      }

      byte[] bytes=new byte[SESSION_ID_BYTES];

      // get random bytes
      _random.nextBytes(bytes);

      // Hash the random bytes
      bytes=_digest.digest(bytes);

      // Render the result as a String of hexadecimal digits
      return encode(bytes);
    }

  /**
     Encode the bytes into a String with a slightly modified Base64-algorithm
     This code was written by Kevin Kelley <kelley@ruralnet.net>
     and adapted by Thomas Peuss <jboss@peuss.de>
     @param data The bytes you want to encode
     @return the encoded String
  */
  protected String encode(byte[] data)
    {
      char[] out = new char[((data.length + 2) / 3) * 4];
      char[] alphabet =  "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-*".toCharArray();

      //
      // 3 bytes encode to 4 chars.  Output is always an even
      // multiple of 4 characters.
      //
      for (int i=0, index=0; i<data.length; i+=3, index+=4) {
	boolean quad = false;
	boolean trip = false;

	int val = (0xFF & (int) data[i]);
	val <<= 8;
	if ((i+1) < data.length) {
	  val |= (0xFF & (int) data[i+1]);
	  trip = true;
	}
	val <<= 8;
	if ((i+2) < data.length) {
	  val |= (0xFF & (int) data[i+2]);
	  quad = true;
	}
	out[index+3] = alphabet[(quad? (val & 0x3F): 64)];
	val >>= 6;
	out[index+2] = alphabet[(trip? (val & 0x3F): 64)];
	val >>= 6;
	out[index+1] = alphabet[val & 0x3F];
	val >>= 6;
	out[index+0] = alphabet[val & 0x3F];
      }
      return new String(out);
    }

  /**
     get a random-number generator
     @return a random-number generator
  */
  protected synchronized Random getRandom()
    {
      long seed;
      Random random=null;

      // Mix up the seed a bit
      seed=System.currentTimeMillis();
      seed^=Runtime.getRuntime().freeMemory();

      try {
	random=SecureRandom.getInstance(SESSION_ID_RANDOM_ALGORITHM);
      } catch (NoSuchAlgorithmException e) {
	_log.error("Could not generate SecureRandom for session-id randomness",e);
	return null;
      }

      // set the generated seed for this PRNG
      random.setSeed(seed);

      return random;
    }

  /**
     get a MessageDigest hash-generator
     @return a hash generator
  */
  protected synchronized MessageDigest getDigest()
    {
      MessageDigest digest=null;

      try {
	digest=MessageDigest.getInstance(SESSION_ID_HASH_ALGORITHM);
      } catch (NoSuchAlgorithmException e) {
	_log.error("Could not generate MessageDigest for session-id hashing",e);
	return null;
      }

      return digest;
    }

}
