package polyglot.ext.jl.ast;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.visit.*;
import polyglot.util.*;

/**
 * An integer literal: longs, ints, shorts, bytes, and chars.
 */
public abstract class NumLit_c extends Lit_c implements NumLit
{
    protected long value;

    public NumLit_c(Position pos, long value) {
	super(pos);
	this.value = value;
    }

    /** Get the value of the expression. */
    public long longValue() {
	return this.value;
    }

    /** Get the value of the expression, as an object. */
    public Object objValue() {
        return new Long(this.value);
    }
}
