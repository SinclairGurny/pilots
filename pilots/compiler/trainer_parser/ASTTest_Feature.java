/* Generated By:JJTree: Do not edit this line. ASTTest_Feature.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package pilots.compiler.trainer.parser;

public
class ASTTest_Feature extends SimpleNode {
  public ASTTest_Feature(int id) {
    super(id);
  }

  public ASTTest_Feature(TrainerParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(TrainerParserVisitor visitor, Object data) {

    return
    visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=87b6d8c86ce9bc48e933cdc0be1cb1bf (do not edit this line) */