/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

// $Id: Line.java,v 1.1.1.1 2002/11/16 03:16:50 mikechen Exp $

package org.jboss.net.samples.store.server;

import org.jboss.net.samples.store.Unit;

import javax.ejb.EJBObject;
import javax.ejb.EJBHome;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.CreateException;

/**
 * Compact entity bean that is exposed as a value object
 * via Axis.
 * <br>
 * <h3>Change History</h3>
 * <ul>
 * </ul>
 * @created 21.03.2002
 * @author <a href="mailto:Christoph.Jung@infor.de">Christoph G. Jung</a>
 * @version $Revision: 1.1.1.1 $
 */

public interface Line extends EJBObject {
   public String getId();
   public void setId(String id);
   /**
    * @link association
    * @associates Item
    * @label refersTo
    * @supplierCardinality 1
    * @clientCardinality 0..*
    */
   public Item getItem();
   public void setItem(Item item);
   public void setQuantity(double quantity);
   public double getQuantity();
   /**
    * @link aggregation
    * @associates org.jboss.net.samples.store.Unit
    * @clientCardinality *
    * @supplierCardinality 1
    */
   public void setUnit(Unit unit);
   public Unit getUnit();

   public interface Home extends EJBHome {
      public Line create(String id) throws CreateException;
   }

   public static abstract class Bean implements EntityBean {
      transient private EntityContext ctx;

      public String ejbCreate(String id) {
         setId(id);
         return null;
      }

      public void ejbPostCreate(String id) {
      }

      public abstract String getId();
      public abstract void setId(String id);

      public abstract Item getItem();
      public abstract void setItem(Item item);

      public abstract double getQuantity();
      public abstract void setQuantity(double quant);

      public abstract Unit getUnit();
      public abstract void setUnit(Unit unit);

      public void setEntityContext(EntityContext ctx) {
         this.ctx = ctx;
      }

      public void unsetEntityContext() {
         this.ctx = null;
      }

      public void ejbActivate() {
      }

      public void ejbPassivate() {
      }

      public void ejbLoad() {
      }

      public void ejbStore() {
      }

      public void ejbRemove() {
      }
   }
}