/* Generated By:JJTree: Do not edit this line. ASTSignature2.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package pilots.compiler.parser;

public
class ASTSignature2 extends SimpleNode {
  public ASTSignature2(int id) {
    super(id);
  }

  public ASTSignature2(PilotsParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(PilotsParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=4491767e1f9c10fb2088fb31b593e23f (do not edit this line) */