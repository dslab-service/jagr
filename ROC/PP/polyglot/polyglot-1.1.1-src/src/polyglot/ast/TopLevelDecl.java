package polyglot.ast;

import polyglot.types.Flags;

/**
 * A top-level declaration.  This is any declaration that can appear in the
 * outermost scope of a source file.
 */
public interface TopLevelDecl extends Node
{
    /** The declaration's flags. */
    Flags flags();

    /** The declaration's name. */
    String name();
}
