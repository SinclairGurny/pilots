/* Generated By:JJTree: Do not edit this line. ASTAlgorithm.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package pilots.compiler.trainer.parser;

public
class ASTAlgorithm extends SimpleNode {
  public ASTAlgorithm(int id) {
    super(id);
  }

  public ASTAlgorithm(TrainerParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(TrainerParserVisitor visitor, Object data) {

    return
    visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=db7c0892fc9afca7cfc6a070bd43f10c (do not edit this line) */
