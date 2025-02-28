package polyglot.ext.param.types;

import polyglot.types.*;
import polyglot.util.Position;  
import java.util.List;  

/**
 * Parametric class.  This class is a wrapper around
 * a ClassType that associates formal parameters with the class.
 * formals can be any type object.
 */
public interface PClass extends Importable {
    /**
     * The formal type parameters associated with <code>this</code>.
     */
    List formals();
    
    /**
     * The class associated with <code>this</code>.  Note that
     * <code>this</code> should never be used as a first-class type.
     */
    ClassType clazz();
    
    /**
     * Instantiate <code>this</code>.
     * @param pos The position of the instantiation
     * @param actuals The actual type parameters for the instantiation
     */
    ClassType instantiate(Position pos, List actuals) throws SemanticException;
    
    /**
     * Null instantiate <code>this</code> at the position of
     * <code>clazz()</code>.
     */
    ClassType nullInstantiate();
    
    /**
     * Null instantiate <code>this</code>.  A null instantiation is an
     * instantiation where the actuals are identical to the formals.
     * @param pos The position of the instantiation
     */
    ClassType nullInstantiate(Position pos);
}
