/* Generated By:JJTree: Do not edit this line. ASTData.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package pilots.compiler.trainer.parser;

public
class ASTData extends SimpleNode {
  public ASTData(int id) {
    super(id);
  }

  public ASTData(TrainerParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(TrainerParserVisitor visitor, Object data) {

    return
    visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=719739a11ff0948f4272acda40179e64 (do not edit this line) */