package org.jboss.test.cmp2.relationship.manyToManyUnidirectional;

import java.util.Collection;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;

public abstract class ABean implements EntityBean {
	transient private EntityContext ctx;

	public Integer ejbCreate(Integer id) {
		setId(id);
		return null;
	}

	public void ejbPostCreate(Integer id) {
	}

	public abstract Integer getId();
	public abstract void setId(Integer id);

	public abstract Collection getB();
	public abstract void setB(Collection b);

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
