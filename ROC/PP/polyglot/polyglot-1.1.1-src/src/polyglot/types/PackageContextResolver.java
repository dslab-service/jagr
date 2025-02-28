package polyglot.types;

import polyglot.ast.*;
import polyglot.util.*;
import polyglot.types.Package;

/**
 * A <code>PackageContextResolver</code> is responsible for looking up types
 * and packages in a packge by name.
 */
public class PackageContextResolver implements Resolver
{
    Package p;
    TypeSystem ts;
    Resolver cr;

    /**
     * Create a package context resolver.
     * @param ts The type system.
     * @param p The package in whose context to search.
     * @param cr The resolver to use for looking up types.
     */
    public PackageContextResolver(TypeSystem ts, Package p, Resolver cr) {
	this.ts = ts;
	this.p = p;
	this.cr = cr;
    }

    /**
     * The package in whose context to search.
     */
    public Package package_() {
        return p;
    }

    /**
     * Find a type object by name.
     */
    public Named find(String name) throws SemanticException {
	if (! StringUtil.isNameShort(name)) {
	    throw new InternalCompilerError(
		"Cannot lookup qualified name " + name);
	}

        if (cr == null) {
	    return ts.packageForName(p, name);
        }

	try {
	    return cr.find(p.fullName() + "." + name);
	}
	catch (NoClassException e) {
            if (!e.getClassName().equals(p.fullName() + "." + name)) {
                throw e;
            }
	    return ts.packageForName(p, name);
	}
    }

    public String toString() {
        return "(package-context " + p.toString() + ")";
    }
}
