/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.ejb.plugins.cmp.jdbc.metadata;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.deployment.DeploymentException;
import org.jboss.metadata.EntityMetaData;
import org.jboss.metadata.MetaData;
import org.jboss.metadata.QueryMetaData;
import org.w3c.dom.Element;

/**
 * This immutable class contains information about an entity
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="sebastien.alborini@m4x.org">Sebastien Alborini</a>
 * @author <a href="mailto:dirk@jboss.de">Dirk Zimmermann</a>
 * @version $Revision: 1.1.1.1 $
 */
public final class JDBCEntityMetaData {
   /**
    * application metadata in which this entity is defined
    */
   private final JDBCApplicationMetaData jdbcApplication;
   
   /**
    * data source name in jndi
    */
   private final String dataSourceName;
   
   /**
    * type mapping used for this entity
    */
   private final JDBCTypeMappingMetaData datasourceMapping;
   
   /**
    * the name of this entity
    */
   private final String entityName;
   
   /**
    * the abstract schema name of this entity
    */
   private final String abstractSchemaName;

   /**
    * the implementation class of this entity
    */
   private final Class entityClass;
   
   /**
    * the home class of this entity
    */
   private final Class homeClass;
   
   /**
    * the remote class of this entity
    */
   private final Class remoteClass;
   
   /**
    * the local home class of this entity
    */
   private final Class localHomeClass;
   
    /**
    * the local class of this entity
    */
   private final Class localClass;
   
   /**
    * Does this entity use cmp 1.x?
    */
   private final boolean isCMP1x;
   
   /**
    * the name of the table to which this entity is persisted
    */
   private final String tableName;
   
   /**
    * Should we try and create the table when deployed?
    */
   private final boolean createTable;
   
   /** 
    * Should we drop the table when undeployed?
    */
   private final boolean removeTable;
   
   /**
    * Should we use 'SELECT ... FOR UPDATE' syntax when loading?
    */
   private final boolean rowLocking;
   
   /**
    * Is this entity read-only?
    */
   private final boolean readOnly;
   
   /**
    * how long is a read valid
    */
   private final int readTimeOut;
   
   /**
    * Should the table have a primary key constraint?
    */
   private final boolean primaryKeyConstraint;

   /**
    * the java class of the primary key
    */
   private final Class primaryKeyClass;

   /**
    * the name of the primary key field or null if the primary key field 
    * is multivalued
    */
   private final String primaryKeyFieldName;

   /**
    * Map of the cmp fields of this entity by field name.
    */
   private final Map cmpFieldsByName = new HashMap();
   private final List cmpFields = new ArrayList();

   /**
    * A map of all the load groups by name.
    */
   private final Map loadGroups = new HashMap();

   /**
    * The fields which should always be loaded when an entity of this type
    * is loaded.
    */
   private final String eagerLoadGroup;

   /**
    * A list of groups (also lists) of the fields that should be lazy
    * loaded together.
    */
   private final List lazyLoadGroups = new ArrayList();

   /**
    * Map of the queries on this entity by the Method that invokes the query.
    */
   private final Map queries = new HashMap();

   /**
    * The factory used to used to create query meta data
    */
   private final JDBCQueryMetaDataFactory queryFactory;

   /**
    * The read ahead meta data
    */
   private final JDBCReadAheadMetaData readAhead;

   /**
    * The maximum number of read ahead lists that can be tracked for this
    * entity.
    */
   private final int listCacheMax;

   /**
    * The number of entities to read in one round-trip to the 
    * underlying data store.
    */
   private final int fetchSize;

   /**
    * Constructs jdbc entity meta data defined in the jdbcApplication and 
    * with the data from the entity meta data which is loaded from the
    * ejb-jar.xml file.
    *
    * @param jdbcApplication the application in which this entity is defined
    * @param entity the entity meta data for this entity that is loaded
    *    from the ejb-jar.xml file
    * @throws DeploymentException if an problem occures while loading the
    *    classes or if data in the ejb-jar.xml is inconsistent with data 
    *    from jbosscmp-jdbc.xml file
    */
   public JDBCEntityMetaData(
         JDBCApplicationMetaData jdbcApplication,
         EntityMetaData entity) throws DeploymentException {

      this.jdbcApplication = jdbcApplication;
      entityName = entity.getEjbName();
      abstractSchemaName = entity.getAbstractSchemaName();
      listCacheMax = 1000;
      fetchSize = 0;

      try {
         entityClass = getClassLoader().loadClass(entity.getEjbClass());
      } catch (ClassNotFoundException e) {
         throw new DeploymentException("entity class not found: " + entityName);
      }

      try {
         primaryKeyClass = 
               getClassLoader().loadClass(entity.getPrimaryKeyClass());
      } catch (ClassNotFoundException e) {
         throw new DeploymentException("could not load primary key class: " +
               entity.getPrimaryKeyClass());
      }

      isCMP1x = entity.isCMP1x();
      primaryKeyFieldName = entity.getPrimKeyField();

      String home = entity.getHome();
      if(home != null) {
         try {
            homeClass = getClassLoader().loadClass(home);
         } catch (ClassNotFoundException e) {
            throw new DeploymentException("home class not found: " + home);
         }
         try {
            remoteClass = getClassLoader().loadClass(entity.getRemote());
         } catch (ClassNotFoundException e) {
            throw new DeploymentException("remote class not found: " + 
                  entity.getRemote());
         }
      } else {
         homeClass = null;
         remoteClass = null;
      }

      String localHome = entity.getLocalHome();
      if(localHome != null) {
         try {
            localHomeClass = getClassLoader().loadClass(localHome);
         } catch (ClassNotFoundException e) {
            throw new DeploymentException("local home class not found: " +
                  localHome);
         }
         try {
            localClass = getClassLoader().loadClass(entity.getLocal());
         } catch (ClassNotFoundException e) {
            throw new DeploymentException("local class not found: " +
                  entity.getLocal());
         }
      } else {
         // we must have a home or local home
         if(home == null) {
            throw new DeploymentException("Entity must have atleast a home " +
                  "or local home: " + entityName);
         }

         localHomeClass = null;
         localClass = null;
      }

      // we replace the . by _ because some dbs die on it...
      // the table name may be overridden in importXml(jbosscmp-jdbc.xml)
      tableName = entityName.replace('.', '_');

      // Warn: readTimeOut must be set before cmp fields are created
      // otherwise cmp fields will have readTimeOut set to zero by default
      // while default is -1
      dataSourceName = null;
      datasourceMapping = null;
      createTable = false;
      removeTable = false;
      rowLocking = false;
      primaryKeyConstraint = false;
      readOnly = false;
      readTimeOut = -1;

      // build the metadata for the cmp fields now in case there is
      // no jbosscmp-jdbc.xml
      List nonPkFieldNames = new ArrayList();
      for(Iterator i = entity.getCMPFields(); i.hasNext(); ) {
         String cmpFieldName = (String)i.next();
         JDBCCMPFieldMetaData cmpField = 
               new JDBCCMPFieldMetaData(this, cmpFieldName);         
         cmpFields.add(cmpField);
         cmpFieldsByName.put(cmpFieldName, cmpField);
         if(!cmpField.isPrimaryKeyMember()) {
            nonPkFieldNames.add(cmpFieldName);
         }
      }

      // set eager load fields to all group.
      eagerLoadGroup = "*";

      // Create no lazy load groups. By default every thing is eager loaded.
      // build the metadata for the queries now in case there is no 
      // jbosscmp-jdbc.xml
      queryFactory = new JDBCQueryMetaDataFactory(this);

      for(Iterator queriesIterator = entity.getQueries();
            queriesIterator.hasNext();) {

         QueryMetaData queryData = (QueryMetaData)queriesIterator.next();
         Map newQueries = queryFactory.createJDBCQueryMetaData(queryData);

         // overrides defaults added above
         queries.putAll(newQueries);
      }

      // Create no relationship roles for this entity, will be added 
      // by the relation meta data

      readAhead = JDBCReadAheadMetaData.DEFAULT;
   }

   /**
    * Constructs entity meta data with the data contained in the entity xml
    * element from a jbosscmp-jdbc xml file. Optional values of the xml element 
    * that are not present are loaded from the defalutValues parameter.
    *
    * @param jdbcApplication the application in which this entity is defined
    * @param element the xml Element which contains the metadata about 
    *       this entity
    * @param defaultValues the JDBCEntityMetaData which contains the values
    *       for optional elements of the element
    * @throws DeploymentException if the xml element is not semantically correct
    */
   public JDBCEntityMetaData(
         JDBCApplicationMetaData jdbcApplication, 
         Element element, 
         JDBCEntityMetaData defaultValues) throws DeploymentException {

      // store passed in application... application in defaultValues may 
      // be different because jdbcApplication is imutable
      this.jdbcApplication = jdbcApplication;

      // set default values
      entityName = defaultValues.getName();
      abstractSchemaName = defaultValues.getAbstractSchemaName();
      entityClass = defaultValues.getEntityClass();
      primaryKeyClass = defaultValues.getPrimaryKeyClass();
      isCMP1x = defaultValues.isCMP1x;
      primaryKeyFieldName = defaultValues.getPrimaryKeyFieldName();
      homeClass = defaultValues.getHomeClass();
      remoteClass = defaultValues.getRemoteClass();
      localHomeClass = defaultValues.getLocalHomeClass();
      localClass = defaultValues.getLocalClass();
      queryFactory = new JDBCQueryMetaDataFactory(this);

      // datasource name
      String dataSourceNameString = 
            MetaData.getOptionalChildContent(element, "datasource");
      if(dataSourceNameString != null) {
         dataSourceName = dataSourceNameString;
      } else {
         dataSourceName = defaultValues.getDataSourceName();
      }
      
      // get the datasource mapping for this datasource (optional, but always 
      // set in standardjbosscmp-jdbc.xml)
      String datasourceMappingString = 
            MetaData.getOptionalChildContent(element, "datasource-mapping");
      if(datasourceMappingString != null) {
         datasourceMapping = 
               jdbcApplication.getTypeMappingByName(datasourceMappingString);
      
         if(datasourceMapping == null) {
            throw new DeploymentException("Error in jbosscmp-jdbc.xml : " +
                  "datasource-mapping " + datasourceMappingString + 
                  " not found");
         }
      } else {
         datasourceMapping = defaultValues.getTypeMapping();
      }

      // get table name
      String tableStr = MetaData.getOptionalChildContent(element, "table-name");
      if(tableStr != null) {
         tableName = tableStr;
      } else {
         tableName = defaultValues.getDefaultTableName();
      }

      // create table?  If not provided, keep default.
      String createStr =
            MetaData.getOptionalChildContent(element, "create-table");
      if(createStr != null) {
         createTable = Boolean.valueOf(createStr).booleanValue();
      } else {
         createTable = defaultValues.getCreateTable();
      }

       // remove table?  If not provided, keep default.
      String removeStr = 
            MetaData.getOptionalChildContent(element, "remove-table");
      if(removeStr != null) {
         removeTable = Boolean.valueOf(removeStr).booleanValue();
      } else {
         removeTable = defaultValues.getRemoveTable();
      }

      // read-only
      String readOnlyStr =
            MetaData.getOptionalChildContent(element, "read-only");
      if(readOnlyStr != null) {
         readOnly = Boolean.valueOf(readOnlyStr).booleanValue();
      } else {
         readOnly = defaultValues.isReadOnly();
      }

      // read-time-out
      String readTimeOutStr =
            MetaData.getOptionalChildContent(element, "read-time-out");
      if(readTimeOutStr != null) {
         try {
            readTimeOut = Integer.parseInt(readTimeOutStr);
         } catch (NumberFormatException e) {
            throw new DeploymentException("Invalid number format in " +
                  "read-time-out '" + readTimeOutStr + "': " + e);
         }
      } else {
         readTimeOut = defaultValues.getReadTimeOut();
      }

      String sForUpStr = 
            MetaData.getOptionalChildContent(element, "row-locking");
      if(sForUpStr != null) {
         rowLocking = 
               !isReadOnly() && (Boolean.valueOf(sForUpStr).booleanValue());
      } else {
         rowLocking = defaultValues.hasRowLocking();
      }

      // primary key constraint?  If not provided, keep default.
      String pkStr = MetaData.getOptionalChildContent(element, "pk-constraint");
      if(pkStr != null) {
         primaryKeyConstraint = Boolean.valueOf(pkStr).booleanValue();
      } else {
         primaryKeyConstraint = defaultValues.hasPrimaryKeyConstraint();
      }

      // list-cache-max
      String listCacheMaxStr = 
            MetaData.getOptionalChildContent(element, "list-cache-max");
      if(listCacheMaxStr != null) {
         try {
            listCacheMax = Integer.parseInt(listCacheMaxStr);
         } catch (NumberFormatException e) {
            throw new DeploymentException("Invalid number format in read-" +
                  "ahead list-cache-max '" + listCacheMaxStr + "': " + e);
         }
         if(listCacheMax < 0) {
            throw new DeploymentException("Negative value for read ahead " +
                  "list-cache-max '" + listCacheMaxStr + "'.");
         }
      } else {
         listCacheMax = defaultValues.getListCacheMax();
      }

      // fetch-size
      String fetchSizeStr = 
      MetaData.getOptionalChildContent(element, "fetch-size");
      if(fetchSizeStr != null) {
         try {
            fetchSize = Integer.parseInt(fetchSizeStr);
         } catch (NumberFormatException e) {
            throw new DeploymentException("Invalid number format in " +
                  "fetch-size '" + fetchSizeStr + "': " + e);
         }
         if(fetchSize < 0) {
            throw new DeploymentException("Negative value for fetch size " +
                  "fetch-size '" + fetchSizeStr + "'.");
         }
      } else {
         fetchSize = defaultValues.getFetchSize();
      }

      // 
      // cmp fields
      // 

      // update all existing queries with the new read ahead value
      for(Iterator cmpFieldIterator = defaultValues.cmpFields.iterator();
            cmpFieldIterator.hasNext(); ) {
         
         JDBCCMPFieldMetaData cmpField = new JDBCCMPFieldMetaData(
               this,
               (JDBCCMPFieldMetaData)cmpFieldIterator.next());
         cmpFields.add(cmpField);
         cmpFieldsByName.put(cmpField.getFieldName(), cmpField);
      }

      // apply new configurations to the cmpfields
      for(Iterator i = MetaData.getChildrenByTagName(element, "cmp-field"); 
            i.hasNext(); ) {

         Element cmpFieldElement = (Element)i.next();
         String fieldName =
               MetaData.getUniqueChildContent(cmpFieldElement, "field-name");

         JDBCCMPFieldMetaData oldCMPField =
               (JDBCCMPFieldMetaData)cmpFieldsByName.get(fieldName);
         if(oldCMPField == null) {
            throw new DeploymentException("CMP field not found : fieldName=" +
                  fieldName);
         }
         JDBCCMPFieldMetaData cmpFieldMetaData = new JDBCCMPFieldMetaData(
               this, 
               cmpFieldElement, 
               oldCMPField);

         // replace the old cmp meta data with the new
         cmpFieldsByName.put(fieldName, cmpFieldMetaData);
         int index = cmpFields.indexOf(oldCMPField);
         cmpFields.remove(oldCMPField);
         cmpFields.add(index, cmpFieldMetaData);
      }

      // load-loads
      loadGroups.putAll(defaultValues.loadGroups);
      loadLoadGroupsXml(element);

      // eager-load
      Element eagerLoadGroupElement = MetaData.getOptionalChild(
            element, "eager-load-group");
      if(eagerLoadGroupElement != null) {
         String eagerLoadGroupTmp = 
               MetaData.getElementContent(eagerLoadGroupElement);
         if(eagerLoadGroupTmp != null && eagerLoadGroupTmp.trim().length()==0) {
            eagerLoadGroupTmp = null;
         }
         if(eagerLoadGroupTmp != null 
               && !eagerLoadGroupTmp.equals("*")
               && !loadGroups.containsKey(eagerLoadGroupTmp)) {
            throw new DeploymentException("Eager load group not found: " +
                  "eager-load-group=" + eagerLoadGroupTmp);
         }
         eagerLoadGroup = eagerLoadGroupTmp;
      } else {
         eagerLoadGroup = defaultValues.getEagerLoadGroup();
      }

      // lazy-loads
      lazyLoadGroups.addAll(defaultValues.lazyLoadGroups);
      loadLazyLoadGroupsXml(element);

      // read-ahead
      Element readAheadElement =
            MetaData.getOptionalChild(element, "read-ahead");
      if(readAheadElement != null) {
         readAhead = new JDBCReadAheadMetaData(
               readAheadElement, defaultValues.getReadAhead());
      } else {
         readAhead = defaultValues.readAhead;
      }

      // queries

      // update all existing queries with the new read ahead value
      for(Iterator queriesIterator = defaultValues.queries.values().iterator();
            queriesIterator.hasNext(); ) {
         
         JDBCQueryMetaData query = queryFactory.createJDBCQueryMetaData(
               (JDBCQueryMetaData)queriesIterator.next(),
               readAhead);
         queries.put(query.getMethod(), query);
      }

      // apply new configurations to the queries
      for(Iterator queriesIterator = 
            MetaData.getChildrenByTagName(element, "query"); 
            queriesIterator.hasNext(); ) {

         Element queryElement = (Element)queriesIterator.next();
         Map newQueries = queryFactory.createJDBCQueryMetaData(
               queryElement,
               defaultValues.queries,
               readAhead);

         // overrides defaults added above
         queries.putAll(newQueries);
      }
   }

   /**
    * Loads the load groups of cmp fields from the xml element
    */
   private void loadLoadGroupsXml(Element element)
         throws DeploymentException {

      Element loadGroupsElement = 
            MetaData.getOptionalChild(element, "load-groups");
      if(loadGroupsElement == null) {
         // no info, default work already done in constructor
         return;
      }
      
      // only allowed for cmp 2.x
      if(isCMP1x) {
         throw new DeploymentException("load-groups are only allowed " +
               "for CMP 2.x");
      }
      
      // load each group
      Iterator groups = MetaData.getChildrenByTagName(
            loadGroupsElement, "load-group");
      while(groups.hasNext()) {
         Element groupElement = (Element)groups.next();

         // get the load-group-name
         String loadGroupName = MetaData.getUniqueChildContent(
               groupElement, "load-group-name");
         if(loadGroups.containsKey(loadGroupName)) {
            throw new DeploymentException("Load group already defined: " +
                  " load-group-name=" + loadGroupName);
         }
         if(loadGroupName.equals("*")) {
            throw new DeploymentException("The * load group is automatically " +
                  "defined and can't be overriden");
         }
         ArrayList group = new ArrayList();

         // add each field
         Iterator fields =
               MetaData.getChildrenByTagName(groupElement, "field-name");
         while(fields.hasNext()) {
            String fieldName = 
                  MetaData.getElementContent((Element)fields.next());

            // check if the field is a cmp field that it is not a pk memeber
            JDBCCMPFieldMetaData field = getCMPFieldByName(fieldName);
            if(field != null && field.isPrimaryKeyMember()) {
               throw new DeploymentException("Primary key fields can not be" +
                     " a member of a load group: " +
                     " load-group-name=" + loadGroupName +
                     " field-name=" + fieldName);
            }

            group.add(fieldName);
         }
         
         loadGroups.put(
               loadGroupName,
               Collections.unmodifiableList(group));
      }
   }

   /**
    * Loads the list of lazy load groups from the xml element.
    */
   private void loadLazyLoadGroupsXml(Element element)
         throws DeploymentException {

      Element lazyLoadGroupsElement = 
            MetaData.getOptionalChild(element, "lazy-load-groups");

      // If no info, we're done. Default work was already done in constructor.
      if(lazyLoadGroupsElement == null) {
         return;
      }

      // only allowed for cmp 2.x
      if(isCMP1x) {
         throw new DeploymentException("lazy-load-groups is only allowed " +
               "for CMP 2.x");
      }

      // get the fields
      Iterator loadGroupNames = MetaData.getChildrenByTagName(
            lazyLoadGroupsElement, "load-group-name");
      while(loadGroupNames.hasNext()) {
         String loadGroupName = MetaData.getElementContent(
               (Element)loadGroupNames.next());
         if(!loadGroupName.equals("*")
               && !loadGroups.containsKey(loadGroupName)) {
            throw new DeploymentException("Lazy load group not found: " +
                  "load-group-name=" + loadGroupName);
         }

         lazyLoadGroups.add(loadGroupName);
      }

   }

   /**
    * Gets the meta data for the application of which this entity is a member.
    * @return the meta data for the application that this entity is a memeber
    */
   public JDBCApplicationMetaData getJDBCApplication() {
      return jdbcApplication;
   }

   /**
    * Gets the name of the datasource in jndi for this entity
    * @return the name of datasource in jndi
    */
   public String getDataSourceName() {
      return dataSourceName;
   }

   /**
    * Gets the jdbc type mapping for this entity
    * @return the jdbc type mapping for this entity
    */
   public JDBCTypeMappingMetaData getTypeMapping() {
      return datasourceMapping;
   }
   
   /**
    * Gets the name of this entity. The name come from the ejb-jar.xml file.
    * @return the name of this entity
    */
   public String getName() {
      return entityName;
   }

   /**
    * Gets the abstract shcema name of this entity. The name come from 
    * the ejb-jar.xml file.
    * @return the abstract schema name of this entity
    */
   public String getAbstractSchemaName() {
      return abstractSchemaName;
   }

   /**
    * Gets the class loaded which is used to load all classes used by this
    * entity
    * @return the class loader which is used to load all classes used by
    *    this entity
    */
   public ClassLoader getClassLoader() {
      return jdbcApplication.getClassLoader();
   }
   
   /**
    * Gets the implementation class of this entity
    * @return the implementation class of this entity
    */
   public Class getEntityClass() {
      return entityClass;
   }
   
   /**
    * Gets the home class of this entity
    * @return the home class of this entity
    */
   public Class getHomeClass() {
      return homeClass;
   }
   
   /**
    * Gets the remote class of this entity
    * @return the remote class of this entity
    */
   public Class getRemoteClass() {
      return remoteClass;
   }
   
   /**
    * Gets the local home class of this entity
    * @return the local home class of this entity
    */
   public Class getLocalHomeClass() {
      return localHomeClass;
   }
   
   /**
    * Gets the local class of this entity
    * @return the local class of this entity
    */
   public Class getLocalClass() {
      return localClass;
   }
   
   /**
    * Does this entity use CMP version 1.x
    * @return true if this entity used CMP version 1.x; otherwise false
    */
   public boolean isCMP1x() {
      return isCMP1x;
   }
   
   /**
    * Does this entity use CMP version 2.x
    * @return true if this entity used CMP version 2.x; otherwise false
    */
   public boolean isCMP2x() {
      return !isCMP1x;
   }

   /**
    * Gets the cmp fields of this entity
    * @return an unmodifiable collection of JDBCCMPFieldMetaData objects
    */
   public List getCMPFields() {
      return Collections.unmodifiableList(cmpFields);
   }
   
   /**
    * Gets the name of the eager load group. This name can be used to 
    * look up the load group.
    * @return the name of the eager load group
    */
   public String getEagerLoadGroup() {
      return eagerLoadGroup;
   }
   
   /**
    * Gets the collection of lazy load group names.
    * @return an unmodifiable collection of load group names
    */
   public List getLazyLoadGroups() {
      return Collections.unmodifiableList(lazyLoadGroups);
   }

   /**
    * Gets the map from load grou name to a List of field names, which
    * forms a logical load group.
    * @return an unmodifiable map of load groups (Lists) by group name.
    */
   public Map getLoadGroups() {
      return Collections.unmodifiableMap(loadGroups);
   }

   /**
    * Gets the load group with the specified name.
    * @return the load group with the specified name
    * @throws EJBException if group with the specified name is not found
    */
   public List getLoadGroup(String name) throws DeploymentException {
      List group = (List)loadGroups.get(name);
      if(group == null) {
         throw new DeploymentException("Unknown load group: name=" + name);
      }
      return group;
   }


   /**
    * Gets the cmp field with the specified name
    * @param name the name of the desired field
    * @return the cmp field with the specified name or null if not found
    */
   public JDBCCMPFieldMetaData getCMPFieldByName(String name) {
      return (JDBCCMPFieldMetaData)cmpFieldsByName.get(name);
   }

   /**
    * Gets the cmp field with the specified name
    * @param name the name of the desired field
    * @return the cmp field with the specified name
    * @throws DeploymentException if the field is not found
    */
   private JDBCCMPFieldMetaData getExistingFieldByName(String name) 
         throws DeploymentException {

      JDBCCMPFieldMetaData field = getCMPFieldByName(name);
      if(field == null)
      {
         throw new DeploymentException( "Entity Bean '" + entityName +
            "': CMP field '" + name + "' found in jbosscmp-jdbc.xml but " +
            "not in ejb-jar.xml" );
      }
      return field;
   }

   /**
    * Gets the name of the table to which this entity is persisted
    * @return the name of the table to which this entity is persisted
    */
   public String getDefaultTableName() {
      return tableName;
   }
   
   /**
    * Gets the flag used to determine if the store manager should attempt to
    * create database table when the entity is deployed.
    * @return true if the store manager should attempt to create the table
    */
   public boolean getCreateTable() {
      return createTable;
   }

   /**
    * Gets the flag used to determine if the store manager should attempt to
    * remove database table when the entity is undeployed.
    * @return true if the store manager should attempt to remove the table
    */
   public boolean getRemoveTable() {
      return removeTable;
   }
   
   /**
    * Gets the flag used to determine if the store manager should add a 
    * priary key constraint when creating the table
    * @return true if the store manager should add a primary key constraint to
    *       the create table sql statement
    */
   public boolean hasPrimaryKeyConstraint() {
      return primaryKeyConstraint;
   }

   /**
    * Gets the flag used to determine if the store manager should do row locking
    * when loading entity beans
    * @return true if the store manager should add a row locking
    *       clause when selecting data from the table
    */
   public boolean hasRowLocking() {
      return rowLocking;
   }

   /**
    * The maximum number of qurey result lists that will be tracked.
    */
   public int getListCacheMax() {
      return listCacheMax;
   }

   /**
    * The number of rows that the database driver should get in a single
    * trip to the database.
    */
   public int getFetchSize() {
      return fetchSize;
   }


   /**
    * Gets the queries defined on this entity
    * @return an unmodifiable collection of JDBCQueryMetaData objects
    */
   public Collection getQueries() {
      return Collections.unmodifiableCollection(queries.values());
   }
   
   /**
    * Get the relationsip roles of this entity. 
    * Items are instance of JDBCRelationshipRoleMetaData.
    * @return an unmodifiable collection of the relationship roles defined
    *    for this entity
    */
   public Collection getRelationshipRoles() {
      return jdbcApplication.getRolesForEntity(entityName);
   }
      
   /**
    * Gets the primary key class for this entity
    * @return the primary key class for this entity
    */
   public Class getPrimaryKeyClass() {
      return primaryKeyClass;
   }
   
   /**
    * Is this entity read only? A readonly entity will never be stored into
    * the database.
    * @return true if this entity is read only
    */
   public boolean isReadOnly() {
      return readOnly;
   }
   
   /**
    * How long is a read of this entity valid. This property should only be
    * used on read only entities, and determines how long the data read from
    * the database is valid. When the read times out it should be reread from
    * the database. If the value is -1 and the entity is not using commit
    * option a, the read is only valid for the length of the transaction in
    * which it was loaded.
    * @return the length of time that a read is valid or -1 if the read is only
    *       valid for the length of the transaction
    */
   public int getReadTimeOut() {
      return readTimeOut;
   }

   /**
    * Gets the name of the primary key field of this entity or null if
    * the primary key is multivalued
    * @return the name of the primary key field of this entity or null 
    *    if the primary key is multivalued
    */
   public String getPrimaryKeyFieldName() {
      return primaryKeyFieldName;
   }


   /**
    * Gets the read ahead meta data for this entity.
    * @return the read ahead meta data for this entity.
    */
   public JDBCReadAheadMetaData getReadAhead() {
      return readAhead;
   }

   /**
    * Compares this JDBCEntityMetaData against the specified object. Returns
    * true if the objects are the same. Two JDBCEntityMetaData are the same
    * if they both have the same name and are defined in the same application.
    * @param o the reference object with which to compare
    * @return true if this object is the same as the object argument; 
    *    false otherwise
    */
   public boolean equals(Object o) {
      if(o instanceof JDBCEntityMetaData) {
         JDBCEntityMetaData entity = (JDBCEntityMetaData)o;
         return entityName.equals(entity.entityName) &&
               jdbcApplication.equals(entity.jdbcApplication);
      }
      return false;
   }
   
   /**
    * Returns a hashcode for this JDBCEntityMetaData. The hashcode is computed
    * based on the hashCode of the declaring application and the hashCode of
    * the entityName
    * @return a hash code value for this object
    */
   public int hashCode() {
      int result = 17;
      result = 37*result + jdbcApplication.hashCode();
      result = 37*result + entityName.hashCode();
      return result;
   }
   /**
    * Returns a string describing this JDBCEntityMetaData. The exact details
    * of the representation are unspecified and subject to change, but the
    * following may be regarded as typical:
    * 
    * "[JDBCEntityMetaData: entityName=UserEJB]"
    *
    * @return a string representation of the object
    */
   public String toString() {
      return "[JDBCEntityMetaData : entityName=" + entityName + "]";
   }
}
/*
vim:ts=3:sw=3:et
*/
