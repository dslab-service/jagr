/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
 
package org.jboss.iiop.rmi;

import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.AttributeMode;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.io.Externalizable;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Iterator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *  Common base class of ValueAnalysis and InterfaceAnalysis.
 *
 *  Routines here are conforming to the "Java(TM) Language to IDL Mapping
 *  Specification", version 1.1 (01-06-07).
 *      
 *  @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 *  @version $Revision: 1.1.1.1 $
 */
public abstract class ContainerAnalysis
   extends ClassAnalysis
{
   // Constants -----------------------------------------------------
    
   /** Flags a method as overloaded. */
   protected final byte M_OVERLOADED = 1;

   /** Flags a method as the accessor of a read-write property. */
   protected final byte M_READ = 2;

   /** Flags a method as the mutator of a read-write property. */
   protected final byte M_WRITE = 4;

   /** Flags a method as the accessor of a read-only property. */
   protected final byte M_READONLY = 8;

   /** Flags a method as being inherited. */
   protected final byte M_INHERITED = 16;

   /**
    *  Flags a method as being the writeObject() method
    *  used for serialization.
    */
   protected final byte M_WRITEOBJECT = 32;


   /** Flags a field as being a constant (public final static). */
   protected final byte F_CONSTANT = 1;

   /**
    *  Flags a field as being the special <code> public final static
    *  java.io.ObjectStreamField[] serialPersistentFields</code> field.
    */
   protected final byte F_SPFFIELD = 2;

   // Attributes ----------------------------------------------------

   /**
    *  Array of all java methods.
    */
   protected Method[] methods;

   /**
    *  Array with flags for all java methods.
    */
   protected byte[] m_flags;

   /**
    *  Index of the mutator for read-write attributes.
    *  Only entries <code>i</code> where <code>(m_flags[i]&M_READ) != 0</code>
    *  are used. These entries contain the index of the mutator method
    *  corresponding to the accessor method.
    */
   protected int[] mutators;

   /**
    *  Array of all java fields.
    */
   protected Field[] fields;

   /**
    *  Array with flags for all java fields.
    */
   protected byte[] f_flags;

   /**
    *  The class hash code, as specified in "The Common Object Request
    *  Broker: Architecture and Specification" (01-02-33), section 10.6.2.
    */
   protected long classHashCode = 0;

   /**
    *  The repository ID.
    *  This is in the RMI hashed format, like
    *  "RMI:java.util.Hashtable:C03324C0EA357270:13BB0F25214AE4B8".
    */
   protected String repositoryId;

   /**
    *  The prefix and postfix of members repository ID.
    *  These are used to calculate member repository IDs and are like
    *  "RMI:java.util.Hashtable." and ":C03324C0EA357270:13BB0F25214AE4B8".
    */
   protected String memberPrefix, memberPostfix;

   /**
    *  Array of analysis of the interfaces implemented/extended here.
    */
   protected InterfaceAnalysis[] interfaces;

   /**
    *  Array of attributes.
    */
   protected AttributeAnalysis[] attributes;

   /**
    *  Array of Constants.
    */
   protected ConstantAnalysis[] constants;

   /**
    *  Array of operations.
    */
   protected OperationAnalysis[] operations;


   // Static --------------------------------------------------------

   private static final org.jboss.logging.Logger logger = 
               org.jboss.logging.Logger.getLogger(ContainerAnalysis.class);

   // Constructors  -------------------------------------------------

   protected ContainerAnalysis(Class cls)
   {
      super(cls);

      if (cls == java.lang.Object.class ||
          cls == java.io.Serializable.class ||
          cls == java.io.Externalizable.class)
         throw new IllegalArgumentException("Cannot analyze special class: " +
                                            cls.getName());

      this.cls = cls;
   }

   protected void doAnalyze()
      throws RMIIIOPViolationException
   {
      analyzeInterfaces();
      analyzeMethods();
      analyzeFields();
      calculateClassHashCode();
      calculateRepositoryId();
      analyzeAttributes();
      analyzeConstants();
      analyzeOperations();
      fixupOverloadedOperationNames();
   }

   // Public --------------------------------------------------------

   /**
    *  Return the interfaces.
    */
   public InterfaceAnalysis[] getInterfaces()
   {
      logger.debug("Interface count: " + interfaces.length);
      return (InterfaceAnalysis[])interfaces.clone();
   }

   /**
    *  Return the attributes.
    */
   public AttributeAnalysis[] getAttributes()
   {
      logger.debug("Attribute count: " + attributes.length);
      return (AttributeAnalysis[])attributes.clone();
   }

   /**
    *  Return the attributes.
    */
   public ConstantAnalysis[] getConstants()
   {
      logger.debug("Constants count: " + constants.length);
      return (ConstantAnalysis[])constants.clone();
   }

   /**
    *  Return the operations.
    */
   public OperationAnalysis[] getOperations()
   {
      return (OperationAnalysis[])operations.clone();
   }

   /**
    *  Return the repository ID.
    */
   public String getRepositoryId()
   {
      return repositoryId;
   }

   /**
    *  Return a repository ID for a member.
    *
    *  @param memberName The Java name of the member.
    */
   public String getMemberRepositoryId(String memberName)
   {
      return memberPrefix + escapeIRName(memberName) + memberPostfix;
   }

   /**
    *  Return the fully qualified IDL module name that this
    *  analysis should be placed in.
    */
   public String getIDLModuleName()
   {
      if (idlModuleName == null) {
         String pkgName = cls.getPackage().getName();
         StringBuffer b = new StringBuffer();

         while (!"".equals(pkgName)) {
            int idx = pkgName.indexOf('.');
            String n = (idx == -1) ? pkgName : pkgName.substring(0, idx);

            b.append("::").append(Util.javaToIDLName(n));

            pkgName = (idx == -1) ? "" : pkgName.substring(idx+1);
         }
         idlModuleName = b.toString();
      }
      return idlModuleName;
   }

   // Protected -----------------------------------------------------

   /**
    *  Convert an integer to a 16-digit hex string.
    */
   protected String toHexString(int i)
   {
      String s = Integer.toHexString(i).toUpperCase();
 
      if (s.length() < 8)
         return "00000000".substring(0, 8 - s.length()) + s;
      else
         return s;
   }
   /**
    *  Convert a long to a 16-digit hex string.
    */
   protected String toHexString(long l)
   {
      String s = Long.toHexString(l).toUpperCase();
 
      if (s.length() < 16)
         return "0000000000000000".substring(0, 16 - s.length()) + s;
      else
         return s;
   }

   /**
    *  Check if a method is an accessor.
    */
   protected boolean isAccessor(Method m)
   {
     Class returnType = m.getReturnType();
 
     if (!m.getName().startsWith("get"))
        if (!m.getName().startsWith("is") || !(returnType == Boolean.TYPE))
           return false;
     if (returnType == Void.TYPE)
         return false;
     if (m.getParameterTypes().length != 0)
         return false;

     return hasNonAppExceptions(m);
   }
 
   /**
    *  Check if a method is a mutator.
    */
   protected boolean isMutator(Method m)
   {
     if (!m.getName().startsWith("set"))
         return false;
     if (m.getReturnType() != Void.TYPE)
         return false;
     if (m.getParameterTypes().length != 1)
         return false;
     return hasNonAppExceptions(m);
   }
 
   /**
    *  Check if a method throws anything checked other than
    *  java.rmi.RemoteException and its subclasses.
    */
   protected boolean hasNonAppExceptions(Method m)
   {
      Class[] ex = m.getExceptionTypes();
 
      for (int i = 0; i < ex.length; ++i)
         if (!java.rmi.RemoteException.class.isAssignableFrom(ex[i]))
            return false;
      return true;
   }

   /**
    *  Analyze the fields of the class.
    *  This will fill in the <code>fields</code> and <code>f_flags</code>
    *  arrays.
    */
   protected void analyzeFields()
   {
      //fields = cls.getFields();
      fields = cls.getDeclaredFields();
      f_flags = new byte[fields.length];

      for (int i = 0; i < fields.length; ++i) {
         int mods = fields[i].getModifiers();

         if (Modifier.isFinal(mods) &&
             Modifier.isStatic(mods) &&
             Modifier.isPublic(mods))
           f_flags[i] |= F_CONSTANT;
      }
   }

   /**
    *  Analyze the interfaces of the class.
    *  This will fill in the <code>interfaces</code> array.
    */
   protected void analyzeInterfaces()
      throws RMIIIOPViolationException
   {
      Class[] intfs = cls.getInterfaces();
      ArrayList a = new ArrayList();

      for (int i = 0; i < intfs.length; ++i) {
         if (cls.isInterface() &&
             java.rmi.Remote.class.isAssignableFrom(cls)) {
            // Ignore java.rmi.Remote for interfaces
            if (intfs[i] == java.rmi.Remote.class)
               continue;
         } else {
            // Ignore Serializable and Externalizable for values
            if (intfs[i] == java.io.Serializable.class)
               continue;
            if (intfs[i] == java.io.Externalizable.class)
               continue;
         }

         a.add(InterfaceAnalysis.getInterfaceAnalysis(intfs[i]));
      }

      interfaces = new InterfaceAnalysis[a.size()];
      interfaces = (InterfaceAnalysis[])a.toArray(interfaces);
   }

   /**
    *  Analyze the methods of the class.
    *  This will fill in the <code>methods</code> and <code>m_flags</code>
    *  arrays.
    */
   protected void analyzeMethods()
   {
      // The dynamic stub and skeleton strategy generation mechanism
      // requires the inclusion of inherited methods in the analysis of
      // remote interfaces. To speed things up, inherited methods are
      // not considered in the analysis of a class or non-remote interface.
      if (cls.isInterface() && java.rmi.Remote.class.isAssignableFrom(cls))
         methods = cls.getMethods();
      else
         methods = cls.getDeclaredMethods();
      m_flags = new byte[methods.length];
      mutators = new int[methods.length];
 
      // Find read-write properties
      for (int i = 0; i < methods.length; ++i)
         mutators[i] = -1; // no mutator here
      for (int i = 0; i < methods.length; ++i) {
         logger.debug("analyzeMethods(): method["+i+"].getName()=\"" +
                      methods[i].getName() + "\".");

         if (isAccessor(methods[i]) && (m_flags[i]&M_READ) == 0) {
            String attrName = attributeReadName(methods[i].getName());
            Class iReturn = methods[i].getReturnType();
            for (int j = i+1; j < methods.length; ++j) {
               if (isMutator(methods[j]) && (m_flags[j]&M_WRITE) == 0 &&
                   attrName.equals(attributeWriteName(methods[j].getName()))) {
                  Class[] jParams = methods[j].getParameterTypes();
                  if (jParams.length == 1 && jParams[0] == iReturn) {
                     m_flags[i] |= M_READ;
                     m_flags[j] |= M_WRITE;
                     mutators[i] = j;
                     break;
                  }
               }
            }
         } else if (isMutator(methods[i]) && (m_flags[i]&M_WRITE) == 0) {
            String attrName = attributeWriteName(methods[i].getName());
            Class[] iParams = methods[i].getParameterTypes();
            for (int j = i+1; j < methods.length; ++j) {
               if (isAccessor(methods[j]) && (m_flags[j]&M_READ) == 0 &&
                   attrName.equals(attributeReadName(methods[j].getName()))) {
                  Class jReturn = methods[j].getReturnType();
                  if (iParams.length == 1 && iParams[0] == jReturn) {
                     m_flags[i] |= M_WRITE;
                     m_flags[j] |= M_READ;
                     mutators[j] = i;
                     break;
                  }
               }
            }
         }
      }
 
      // Find read-only properties
      for (int i = 0; i < methods.length; ++i)
         if ((m_flags[i] & (M_READ|M_WRITE)) == 0 && isAccessor(methods[i]))
            m_flags[i] |= M_READONLY;

      // Check for overloaded and inherited methods
      for (int i = 0; i < methods.length; ++i) {
         if ((m_flags[i] & (M_READ|M_WRITE|M_READONLY)) == 0) {
            String iName = methods[i].getName();

            for (int j = i+1; j < methods.length; ++j) {
               if (iName.equals(methods[j].getName())) {
                  m_flags[i] |= M_OVERLOADED;
                  m_flags[j] |= M_OVERLOADED;
               }
            }
         }

         if (methods[i].getDeclaringClass() != cls)
            m_flags[i] |= M_INHERITED;
      }
   }

   /**
    *  Convert an attribute read method name in Java format to
    *  an attribute name in Java format.
    */
   protected String attributeReadName(String name)
   {
      if (name.startsWith("get"))
         name = name.substring(3);
      else if (name.startsWith("is"))
         name = name.substring(2);
      else
         throw new IllegalArgumentException("Not an accessor: " + name);

      return name;
   }

   /**
    *  Convert an attribute write method name in Java format to
    *  an attribute name in Java format.
    */
   protected String attributeWriteName(String name)
   {
      if (name.startsWith("set"))
         name = name.substring(3);
      else
         throw new IllegalArgumentException("Not an accessor: " + name);

      return name;
   }

   /**
    *  Analyse constants.
    *  This will fill in the <code>constants</code> array.
    */
   protected void analyzeConstants()
      throws RMIIIOPViolationException
   {
      ArrayList a = new ArrayList();

      for (int i = 0; i < fields.length; ++i) {
         logger.debug("f_flags["+i+"]=" + f_flags[i]);
         if ((f_flags[i] & F_CONSTANT) == 0)
            continue;

         Class type = fields[i].getType();

         // Only map primitives and java.lang.String
         if (!type.isPrimitive() && type != java.lang.String.class) {
            // It is an RMI/IIOP violation for interfaces.
            if (cls.isInterface())
               throw new RMIIIOPViolationException(
                      "Field \"" + fields[i].getName() + "\" of interface \"" +
                      cls.getName() + "\" is a constant, but not of one " +
                      "of the primitive types, or String.", "1.2.3");

            continue;
         }

         String name = fields[i].getName();

         Object value;
         try {
            value = fields[i].get(null);
         } catch (Exception ex) {
            throw new RuntimeException(ex.toString());
         }

         logger.debug("Constant["+i+"] name= " + name);
         logger.debug("Constant["+i+"] type= " + type.getName());
         logger.debug("Constant["+i+"] value= " + value);
            a.add(new ConstantAnalysis(name, type, value));
      }

      logger.debug("Constants count: " + a.size());
      constants = new ConstantAnalysis[a.size()];
      constants = (ConstantAnalysis[])a.toArray(constants);
   }

   /**
    *  Analyse attributes.
    *  This will fill in the <code>attributes</code> array.
    */
   protected void analyzeAttributes()
      throws RMIIIOPViolationException
   {
      ArrayList a = new ArrayList();

      for (int i = 0; i < methods.length; ++i) {
         logger.debug("m_flags["+i+"]=" + m_flags[i]);
         //if ((m_flags[i]&M_INHERITED) != 0)
         //  continue;

         if ((m_flags[i] & (M_READ|M_READONLY)) != 0) {
            // Read method of an attribute.
            String name = attributeReadName(methods[i].getName());

            logger.debug("Attribute["+i+"] name= " + name);
            if ((m_flags[i]&M_READONLY) != 0)
               a.add(new AttributeAnalysis(name, methods[i]));
            else
               a.add(new AttributeAnalysis(name, methods[i],
                                           methods[mutators[i]]));
         }
      }

      logger.debug("Attribute count: " + a.size());
      attributes = new AttributeAnalysis[a.size()];
      attributes = (AttributeAnalysis[])a.toArray(attributes);
   }

   /**
    *  Analyse operations.
    *  This will fill in the <code>operations</code> array.
    *  This implementation just creates an empty array; override
    *  in subclasses for a real analysis.
    */
   protected void analyzeOperations()
      throws RMIIIOPViolationException
   {
      operations = new OperationAnalysis[0];
   }

   /**
    *  Fixup overloaded operation names.
    *  As specified in section 1.3.2.6.
    */
   protected void fixupOverloadedOperationNames()
      throws RMIIIOPViolationException
   {
      for (int i = 0; i < methods.length; ++i) {
         if ((m_flags[i]&M_OVERLOADED) == 0)
            continue;

         // Find the operation
         OperationAnalysis oa = null;
         String javaName = methods[i].getName();
         for (int opIdx = 0; oa == null && opIdx < operations.length; ++opIdx)
            if (operations[opIdx].getMethod().equals(methods[i]))
               oa = operations[opIdx];

         if (oa == null)
            continue; // This method is not mapped.

         // Calculate new IDL name
         ParameterAnalysis[] parms = oa.getParameters();
         StringBuffer b = new StringBuffer(oa.getIDLName());
         if (parms.length == 0)
            b.append("__");
         for (int j = 0; j < parms.length; ++j) {
            String s = parms[j].getTypeIDLName();

            if (s.startsWith("::"))
               s = s.substring(2);

            b.append('_');

            while (!"".equals(s)) {
               int idx = s.indexOf("::");

               b.append('_');

               if (idx == -1) {
                  b.append(s);
                  s = "";
               } else {
                  b.append(s.substring(0, idx));
                  s = s.substring(idx + 2);
               }
            }
         }

         // Set new IDL name
         oa.setIDLName(b.toString());
      }
   }

   /**
    *  Fixup names differing only in case.
    *  As specified in section 1.3.2.7.
    */
   protected void fixupCaseNames()
      throws RMIIIOPViolationException
   {
      ArrayList entries = getContainedEntries();
      boolean[] clash = new boolean[entries.size()];
      String[] upperNames = new String[entries.size()];

      for (int i = 0; i < entries.size(); ++i) {
         AbstractAnalysis aa = (AbstractAnalysis)entries.get(i);

         clash[i] = false;
         upperNames[i] = aa.getIDLName().toUpperCase();

         for (int j = 0; j < i; ++j) {
            if (upperNames[i].equals(upperNames[j])) {
               clash[i] = true;
               clash[j] = true;
            }
         }
      }

      for (int i = 0; i < entries.size(); ++i) {
         if (!clash[i])
            continue;

         AbstractAnalysis aa = (AbstractAnalysis)entries.get(i);
         boolean noUpper = true;
         String name = aa.getIDLName();
         StringBuffer b = new StringBuffer(name);
         b.append('_');
         for (int j = 0; j < name.length(); ++j) {
            if (!Character.isUpperCase(name.charAt(j)))
               continue;
            if (noUpper)
               noUpper = false;
            else
               b.append('_');
            b.append(j);
         }

         aa.setIDLName(b.toString());
      }
   }

   /**
    *  Return a list of all the entries contained here.
    *
    *  @param entries The list of entries contained here. Entries in this list
    *                 must be subclasses of <code>AbstractAnalysis</code>.
    */
   abstract protected ArrayList getContainedEntries();

   /**
    *  Return the class hash code, as specified in "The Common Object
    *  Request Broker: Architecture and Specification" (01-02-33),
    *  section 10.6.2.
    */
   protected void calculateClassHashCode()
   {
      // The simple cases
      if (cls.isInterface())
         classHashCode = 0;
      else if (!Serializable.class.isAssignableFrom(cls))
         classHashCode = 0;
      else if (Externalizable.class.isAssignableFrom(cls))
         classHashCode = 1;
      else // Go ask Util class for the hash code
         classHashCode = Util.getClassHashCode(cls);
   }

   /**
    *  Escape non-ISO characters for an IR name.
    */
   protected String escapeIRName(String name)
   {
      StringBuffer b = new StringBuffer();

      for (int i = 0; i < name.length(); ++i) {
         char c = name.charAt(i);
 
         if (c < 256)
            b.append(c);
         else
            b.append("\\U").append(toHexString((int)c));
      }
      return b.toString();
   }

   /**
    *  Return the IR global ID of the given class or interface.
    *  This is described in section 1.3.5.7.
    *  The returned string is in the RMI hashed format, like
    *  "RMI:java.util.Hashtable:C03324C0EA357270:13BB0F25214AE4B8".
    */
   protected void calculateRepositoryId()
   {
      if (cls.isArray() || cls.isPrimitive())
         throw new IllegalArgumentException("Not a class or interface.");
 
      StringBuffer b = new StringBuffer("RMI:");
 
      b.append(escapeIRName(cls.getName()));

      memberPrefix = b.toString() + ".";
 
      String hashStr = toHexString(classHashCode);
 
      b.append(':').append(hashStr);
 
      ObjectStreamClass osClass = ObjectStreamClass.lookup(cls);
      if (osClass != null) {
         long serialVersionUID = osClass.getSerialVersionUID();
         String SVUID = toHexString(serialVersionUID);
 
         if (classHashCode != serialVersionUID)
            b.append(':').append(SVUID);
         memberPostfix = ":" + hashStr + ":" + SVUID;
      } else
         memberPostfix = ":" + hashStr;
 
      repositoryId = b.toString();
   }


   // Private  ------------------------------------------------------

   /**
    *  A cache for the fully qualified IDL name of the IDL module we
    *  belong to.
    */
   private String idlModuleName = null;

}
