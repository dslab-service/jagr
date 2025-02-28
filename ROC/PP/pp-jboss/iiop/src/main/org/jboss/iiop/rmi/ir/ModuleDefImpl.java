/*
* JBoss, the OpenSource J2EE webOS
*
* Distributable under LGPL license.
* See terms of license at gnu.org.
*/
package org.jboss.iiop.rmi.ir;

import org.omg.CORBA.ModuleDefOperations;
import org.omg.CORBA.ModuleDefPOATie;
import org.omg.CORBA.ModuleDefHelper;
import org.omg.CORBA.Any;
import org.omg.CORBA.IRObject;
import org.omg.CORBA.Contained;
import org.omg.CORBA.ContainedOperations;
import org.omg.CORBA.ContainedPackage.Description;
import org.omg.CORBA.Container;
import org.omg.CORBA.IDLType;
import org.omg.CORBA.DefinitionKind;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.UnionMember;
import org.omg.CORBA.InterfaceDef;
import org.omg.CORBA.ConstantDef;
import org.omg.CORBA.EnumDef;
import org.omg.CORBA.ValueDef;
import org.omg.CORBA.ValueBoxDef;
import org.omg.CORBA.Initializer;
import org.omg.CORBA.StructDef;
import org.omg.CORBA.UnionDef;
import org.omg.CORBA.ModuleDef;
import org.omg.CORBA.AliasDef;
import org.omg.CORBA.NativeDef;
import org.omg.CORBA.ExceptionDef;
import org.omg.CORBA.ModuleDescription;
import org.omg.CORBA.ModuleDescriptionHelper;
import org.omg.PortableServer.POA;


/**
 *  Abstract base class for all contained IR entities.
 *
 *  @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 *  @version $Revision: 1.1.1.1 $
 */
class ModuleDefImpl
   extends ContainedImpl
   implements ModuleDefOperations, LocalContainer
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   ModuleDefImpl(String id, String name, String version,
                 LocalContainer defined_in, RepositoryImpl repository)
   {
      super(id, name, version, defined_in,
            DefinitionKind.dk_Module, repository);

      this.delegate = new ContainerImplDelegate(this);
   }

   // Public --------------------------------------------------------

   // LocalContainer implementation ---------------------------------

   public LocalContained _lookup(String search_name)
   {
      return delegate._lookup(search_name);
   }
 
   public LocalContained[] _contents(DefinitionKind limit_type,
                                    boolean exclude_inherited)
   {
      return delegate._contents(limit_type, exclude_inherited);
   }
 
   public LocalContained[] _lookup_name(String search_name,
                                        int levels_to_search,
                                        DefinitionKind limit_type,
                                        boolean exclude_inherited)
   {
      return delegate._lookup_name(search_name, levels_to_search, limit_type,
                                   exclude_inherited);
   }
 
   public void add(String name, LocalContained contained)
      throws IRConstructionException
   {
      delegate.add(name, contained);
   }

   // ContainerOperations implementation ----------------------------

   public Contained lookup(String search_name)
   {
      return delegate.lookup(search_name);
   }

   public Contained[] contents(DefinitionKind limit_type,
                               boolean exclude_inherited)
   {
      return delegate.contents(limit_type, exclude_inherited);
   }

   public Contained[] lookup_name(String search_name, int levels_to_search,
                                  DefinitionKind limit_type,
                                  boolean exclude_inherited)
   {
      return delegate.lookup_name(search_name, levels_to_search, limit_type,
                                  exclude_inherited);
   }

   public org.omg.CORBA.ContainerPackage.Description[]
                        describe_contents(DefinitionKind limit_type,
                                          boolean exclude_inherited,
                                          int max_returned_objs)
   {
      return delegate.describe_contents(limit_type, exclude_inherited,
                                        max_returned_objs);
   }

   public ModuleDef create_module(String id, String name, String version)
   {
      return delegate.create_module(id, name, version);
   }

   public ConstantDef create_constant(String id, String name, String version,
                                      IDLType type, Any value)
   {
      return delegate.create_constant(id, name, version, type, value);
   }

   public StructDef create_struct(String id, String name, String version,
                                  StructMember[] members)
   {
      return delegate.create_struct(id, name, version, members);
   }

   public UnionDef create_union(String id, String name, String version,
                                IDLType discriminator_type,
                                UnionMember[] members)
   {
      return delegate.create_union(id, name, version, discriminator_type,
                                   members);
   }

   public EnumDef create_enum(String id, String name, String version,
                              String[] members)
   {
      return delegate.create_enum(id, name, version, members);
   }

   public AliasDef create_alias(String id, String name, String version,
                                IDLType original_type)
   {
      return delegate.create_alias(id, name, version, original_type);
   }

   public InterfaceDef create_interface(String id, String name, String version,
                                        InterfaceDef[] base_interfaces,
                                        boolean is_abstract)
   {
      return delegate.create_interface(id, name, version,
                                       base_interfaces, is_abstract);
   }

   public ValueDef create_value(String id, String name, String version,
                                boolean is_custom, boolean is_abstract,
                                ValueDef base_value, boolean is_truncatable,
                                ValueDef[] abstract_base_values,
                                InterfaceDef[] supported_interfaces,
                                Initializer[] initializers)
   {
      return delegate.create_value(id, name, version, is_custom, is_abstract,
                                   base_value, is_truncatable,
                                   abstract_base_values, supported_interfaces,
                                   initializers);
   }

   public ValueBoxDef create_value_box(String id, String name, String version,
                                       IDLType original_type_def)
   {
      return delegate.create_value_box(id, name, version, original_type_def);
   }

   public ExceptionDef create_exception(String id, String name, String version,
                                        StructMember[] members)
   {
      return delegate.create_exception(id, name, version, members);
   }

   public NativeDef create_native(String id, String name, String version)
   {
      return delegate.create_native(id, name, version);
   }

   // LocalIRObject implementation ----------------------------------

   public IRObject getReference()
   {
      if (ref == null) {
         ref = org.omg.CORBA.ModuleDefHelper.narrow(
                               servantToReference(new ModuleDefPOATie(this)) );
      }
      return ref;
   }

   public void allDone()
      throws IRConstructionException
   {
      getReference();
      delegate.allDone();
   }

   public void shutdown()
   {
      delegate.shutdown();
      super.shutdown();
   }

   // ContainedImpl implementation ----------------------------------

   public Description describe()
   {
      String defined_in_id = "IR";
 
      if (defined_in instanceof ContainedOperations)
         defined_in_id = ((ContainedOperations)defined_in).id();
 
      ModuleDescription md = new ModuleDescription(name, id, defined_in_id,
                                                   version);
 
      Any any = getORB().create_any();

      ModuleDescriptionHelper.insert(any, md);

      return new Description(DefinitionKind.dk_Module, any);
   }

   // Y overrides ---------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   /**
    *  My delegate for Container functionality.
    */
   private ContainerImplDelegate delegate;

   /**
    *  My CORBA reference.
    */
   private ModuleDef ref = null;

   // Inner classes -------------------------------------------------
}

