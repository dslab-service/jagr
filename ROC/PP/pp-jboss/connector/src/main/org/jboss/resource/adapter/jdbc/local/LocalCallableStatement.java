/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.resource.adapter.jdbc.local;

import java.io.InputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URL;
import java.sql.Array;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Calendar;
import java.util.Map;
import java.util.Map;


/**
 * LocalCallableStatement.java
 *
 *
 * Created: Sat Apr 20 22:43:24 2002
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @version
 */

public class LocalCallableStatement 
   extends LocalPreparedStatement 
   implements CallableStatement
{

   private final CallableStatement cs;

   public LocalCallableStatement(final LocalConnection lc, final CallableStatement cs) 
   {
      super(lc, cs);
      this.cs = cs;
   }
   // implementation of java.sql.CallableStatement interface

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Object getObject(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getObject(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @param param2 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Object getObject(int parameterIndex, Map typeMap) throws SQLException
   {
      try 
      {
         return cs.getObject(parameterIndex, typeMap);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Object getObject(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getObject(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param param2 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Object getObject(String parameterName, Map typeMap) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getObject(parameterName, typeMap);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public boolean getBoolean(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getBoolean(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return false;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public boolean getBoolean(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getBoolean(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return false;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public byte getByte(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getByte(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return 0;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public byte getByte(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getByte(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return 0;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public short getShort(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getShort(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return 0;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public short getShort(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getShort(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return 0;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public int getInt(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getInt(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return 0;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public int getInt(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getInt(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return 0;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public long getLong(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getLong(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return 0;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public long getLong(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getLong(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return 0;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public float getFloat(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getFloat(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return 0;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public float getFloat(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getFloat(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return 0;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public double getDouble(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getDouble(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return 0;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public double getDouble(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getDouble(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return 0;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public byte[] getBytes(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getBytes(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public byte[] getBytes(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getBytes(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public URL getURL(int parameterIndex) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getURL(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public URL getURL(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getURL(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }


   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public String getString(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getString(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public String getString(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getString(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Ref getRef(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getRef(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Ref getRef(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getRef(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Time getTime(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getTime(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @param param2 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Time getTime(int parameterIndex, Calendar calendar) throws SQLException
   {
      try 
      {
         return cs.getTime(parameterIndex, calendar);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Time getTime(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getTime(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param param2 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Time getTime(String parameterName, Calendar calendar) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getTime(parameterName, calendar);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Date getDate(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getDate(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @param param2 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Date getDate(int parameterIndex, Calendar calendar) throws SQLException
   {
      try 
      {
         return cs.getDate(parameterIndex, calendar);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Date getDate(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getDate(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param param2 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Date getDate(String parameterName, Calendar calendar) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getDate(parameterName, calendar);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }


   /**
    *
    * @param param1 <description>
    * @param param2 <description>
    * @exception java.sql.SQLException <description>
    */
   public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException
   {
      try 
      {
         cs.registerOutParameter(parameterIndex, sqlType);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @param param2 <description>
    * @param param3 <description>
    * @exception java.sql.SQLException <description>
    */
   public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException
   {
      try 
      {
         cs.registerOutParameter(parameterIndex, sqlType, scale);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @param param2 <description>
    * @param param3 <description>
    * @exception java.sql.SQLException <description>
    */
   public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException
   {
      try 
      {
         cs.registerOutParameter(parameterIndex, sqlType, typeName);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @param param2 <description>
    * @exception java.sql.SQLException <description>
    */
   public void registerOutParameter(String parameterName, int sqlType) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.registerOutParameter(parameterName, sqlType);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param param2 <description>
    * @param param3 <description>
    * @exception java.sql.SQLException <description>
    */
   public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.registerOutParameter(parameterName, sqlType, scale);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param param2 <description>
    * @param param3 <description>
    * @exception java.sql.SQLException <description>
    */
   public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.registerOutParameter(parameterName, sqlType, typeName);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public boolean wasNull() throws SQLException
   {
      try 
      {
         return cs.wasNull();         
      }
      catch (SQLException e)
      {
         checkException(e);
         return false;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @param param2 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException
   {
      try 
      {
         return cs.getBigDecimal(parameterIndex, scale);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public BigDecimal getBigDecimal(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getBigDecimal(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public BigDecimal getBigDecimal(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getBigDecimal(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Timestamp getTimestamp(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getTimestamp(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @param param2 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Timestamp getTimestamp(int parameterIndex, Calendar calendar) throws SQLException
   {
      try 
      {
         return cs.getTimestamp(parameterIndex, calendar);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Timestamp getTimestamp(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getTimestamp(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param param2 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Timestamp getTimestamp(String parameterName, Calendar calendar) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getTimestamp(parameterName, calendar);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Blob getBlob(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getBlob(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Blob getBlob(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getBlob(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Clob getClob(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getClob(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Clob getClob(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getClob(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Array getArray(int parameterIndex) throws SQLException
   {
      try 
      {
         return cs.getArray(parameterIndex);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
   }

   /**
    *
    * @param param1 <description>
    * @return <description>
    * @exception java.sql.SQLException <description>
    */
   public Array getArray(String parameterName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         return cs.getArray(parameterName);         
      }
      catch (SQLException e)
      {
         checkException(e);
         return null;
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }


   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setBoolean(String parameterName, boolean value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setBoolean(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setByte(String parameterName, byte value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setByte(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setShort(String parameterName, short value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setShort(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setInt(String parameterName, int value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setInt(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setLong(String parameterName, long value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setLong(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setFloat(String parameterName, float value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setFloat(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setDouble(String parameterName, double value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setDouble(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setURL(String parameterName, URL value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setURL(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setTime(String parameterName, Time value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setTime(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @param param3 <description>
    * @exception java.sql.SQLException <description>
    */
   public void setTime(String parameterName, Time value, Calendar calendar) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setTime(parameterName, value, calendar);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setNull(String parameterName, int value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setNull(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @param param3 <description>
    * @exception java.sql.SQLException <description>
    */
   public void setNull(String parameterName, int sqlType, String typeName) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setNull(parameterName, sqlType, typeName);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setBigDecimal(String parameterName, BigDecimal value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setBigDecimal(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setString(String parameterName, String value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setString(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setBytes(String parameterName, byte[] value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setBytes(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setDate(String parameterName, Date value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setDate(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @param param3 <description>
    * @exception java.sql.SQLException <description>
    */
   public void setDate(String parameterName, Date value, Calendar calendar) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setDate(parameterName, value, calendar);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setTimestamp(String parameterName, Timestamp value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setTimestamp(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @param param3 <description>
    * @exception java.sql.SQLException <description>
    */
   public void setTimestamp(String parameterName, Timestamp value, Calendar calendar) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setTimestamp(parameterName, value, calendar);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @param param3 <description>
    * @exception java.sql.SQLException <description>
    */
   public void setAsciiStream(String parameterName, InputStream stream, int length) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setAsciiStream(parameterName, stream, length);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @param param3 <description>
    * @exception java.sql.SQLException <description>
    */
   public void setBinaryStream(String parameterName, InputStream stream, int length) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setBinaryStream(parameterName, stream, length);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @param param3 <description>
    * @param param4 <description>
    * @exception java.sql.SQLException <description>
    */
   public void setObject(String parameterName, Object value, int sqlType, int scale) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setObject(parameterName, value, sqlType, scale);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @param param3 <description>
    * @exception java.sql.SQLException <description>
    */
   public void setObject(String parameterName, Object value, int sqlType) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setObject(parameterName, value, sqlType);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @exception java.sql.SQLException <description>
    */
   public void setObject(String parameterName, Object value) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setObject(parameterName, value);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }

   /**
    *
    * @param param1 <description>
    * @param value <description>
    * @param param3 <description>
    * @exception java.sql.SQLException <description>
    */
   public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException
   {
@JDK1.4START@
      try 
      {
         cs.setCharacterStream(parameterName, reader, length);         
      }
      catch (SQLException e)
      {
         checkException(e);
      } // end of try-catch
@JDK1.4END@
@JDK1.3START@
   throw new SQLException("JDK1.4 method not available in JDK1.3");
@JDK1.3END@
   }



}// LocalCallableStatement
