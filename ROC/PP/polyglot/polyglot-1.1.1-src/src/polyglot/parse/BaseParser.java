package polyglot.parse;

import polyglot.ast.*;
import polyglot.lex.*;
import polyglot.types.*;
import polyglot.util.*;
import java_cup.runtime.Symbol;

import java.util.*;

public abstract class BaseParser extends java_cup.runtime.lr_parser
{
  public final Lexer lexer;
  public final ErrorQueue eq;
  public final TypeSystem ts;
  public final NodeFactory nf;
  protected Position prev_pos;
  protected Position position;

  public BaseParser (Lexer l, TypeSystem t, NodeFactory n, ErrorQueue q) {
    lexer = l;
    eq = q;
    ts = t;
    nf = n;
    prev_pos = Position.COMPILER_GENERATED;
    position = Position.COMPILER_GENERATED;
  }

  /**
   * The standard scanning routine for use in the CUP "scan with"
   * declaration. Should read:
   *   scan with {: return nextSymbol(); :}
   */
  public Symbol nextSymbol() throws java.io.IOException {
    Token t = lexer.nextToken();
    // use two positions, since the parser does one token lookahead
    position = prev_pos;
    prev_pos = t.getPosition();
    return t.symbol();
  }

  public Position position() {
    return position;
  }

 /**
  * Override the default CUP routine.
  */
  public void report_fatal_error (String message, Object info)
    throws Exception
  {
    report_error (message, info);
    eq.enqueue (ErrorInfo.SYNTAX_ERROR,
		"Unable to recover from previous errors.");
    die ();
  }

  /**
   * Report a fatal error then abort parsing.
   */
  public void die (String msg, Position pos) throws Exception
  {
    report_fatal_error (msg, pos);
  }

  /**
   * Report a fatal error then abort parsing.
   */
  public void die (Position pos) throws Exception
  {
    report_fatal_error ("Syntax error.", pos);
  }

	/**
	 * Report a fatal error then abort parsing.
	 */
  public void die () throws Exception
  {
    throw new Exception ("Couldn't repair and continue parse");
  }

	/**
	 * Return the position of the Token.
	 */
  public static Position pos (Token t)
  {
    if (t == null) return null;
    return t.getPosition ();
  }

	/**
	 * Return the source position of the Type.
	 */
  public static Position pos (Type n)
  {
    if (n == null) return null;
    return n.position ();
  }

	/**
	 * Return the source position of the first element in the list.
	 */
  public static Position pos (List l)
  {
    if (l == null || l.isEmpty ())
      {
	return null;
      }

    Object n = l.get (0);

    if (n instanceof Node) return pos ((Node) n);
    if (n instanceof Token) return pos ((Token) n);
    if (n instanceof Type) return pos ((Type) n);
    if (n instanceof List) return pos ((List) n);

    return null;
  }

  /**
   * Return the source position of the declaration.
   */
  public static Position pos (VarDeclarator n)
  {
    if (n == null) return null;
    return n.pos;
  }

	/**
	 * Return the source position of the Node.
	 */
  public static Position pos (Node n)
  {
    if (n == null)
      {
	return null;
      }
    return n.position ();
  }

	/**
	 * Return a TypeNode representing a <code>dims</code>-dimensional
	 * array of <code>n</code>.
	 */
  public TypeNode array (TypeNode n, int dims) throws Exception
  {
    if (dims > 0)
      {
	if (n instanceof CanonicalTypeNode)
	  {
	    Type t = ((CanonicalTypeNode) n).type ();
	      return nf.CanonicalTypeNode (pos (n), ts.arrayOf (t, dims));
	  }
	return nf.ArrayTypeNode (pos (n), array (n, dims - 1));
      }
    else
      {
	return n;
      }
  }

	/**
	 * Helper for exprToType.
	 */
  protected QualifierNode prefixToQualifier (Prefix p) throws Exception
  {
    if (p instanceof TypeNode)
      {
	return typeToQualifier ((TypeNode) p);
      }

    if (p instanceof Expr)
      {
	return exprToQualifier ((Expr) p);
      }

    if (p instanceof AmbReceiver)
      {
	AmbReceiver a = (AmbReceiver) p;

	if (a.prefix () != null)
	  {
	    return nf.AmbQualifierNode (pos (p),
					prefixToQualifier (a.prefix ()),
					a.name ());
	  }
	else
	  {
	    return nf.AmbQualifierNode (pos (p), a.name ());
	  }
      }

    if (p instanceof AmbPrefix)
      {
	AmbPrefix a = (AmbPrefix) p;

	if (a.prefix () != null)
	  {
	    return nf.AmbQualifierNode (pos (p),
					prefixToQualifier (a.prefix ()),
					a.name ());
	  }
	else
	  {
	    return nf.AmbQualifierNode (pos (p), a.name ());
	  }
      }

    die (pos (p));
    return null;
  }

	/**
	 * Helper for exprToType.
	 */
  protected QualifierNode typeToQualifier (TypeNode t) throws Exception
  {
    if (t instanceof AmbTypeNode)
      {
	AmbTypeNode a = (AmbTypeNode) t;

	if (a.qualifier () != null)
	  {
	    return nf.AmbQualifierNode (pos (t), a.qual (), a.name ());
	  }
	else
	  {
	    return nf.AmbQualifierNode (pos (t), a.name ());
	  }
      }

    die (pos (t));
    return null;
  }

	/**
	 * Helper for exprToType.
	 */
  protected QualifierNode exprToQualifier (Expr e) throws Exception
  {
    if (e instanceof AmbExpr)
      {
	AmbExpr a = (AmbExpr) e;
	  return nf.AmbQualifierNode (pos (e), a.name ());
      }

    if (e instanceof Field)
      {
	Field f = (Field) e;
	Receiver r = f.target ();
	return nf.AmbQualifierNode (pos (e), prefixToQualifier (r),
				    f.name ());
      }

    die (pos (e));
    return null;
  }

	/**
	 * Convert <code>e</code> into a type, yielding a <code>TypeNode</code>.
	 * This is used by the cast_expression production.
	 */
  public TypeNode exprToType (Expr e) throws Exception
  {
    if (e instanceof AmbExpr)
      {
	AmbExpr a = (AmbExpr) e;
	  return nf.AmbTypeNode (pos (e), a.name ());
      }

    if (e instanceof Field)
      {
	Field f = (Field) e;
	Receiver r = f.target ();
	return nf.AmbTypeNode (pos (e), prefixToQualifier (r), f.name ());
      }

    die (pos (e));
    return null;
  }
}
