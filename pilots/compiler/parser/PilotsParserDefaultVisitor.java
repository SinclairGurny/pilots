/* Generated By:JavaCC: Do not edit this line. PilotsParserDefaultVisitor.java Version 6.0_1 */
package pilots.compiler.parser;

public class PilotsParserDefaultVisitor implements PilotsParserVisitor{
  public Object defaultVisit(SimpleNode node, Object data){
    node.childrenAccept(this, data);
    return data;
  }
  public Object visit(SimpleNode node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTPilots node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTInput node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTConstant node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTOutput node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTError node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTSignature node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTMode node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTEstimate node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTCorrect node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTVars node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTConstInSignature node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTDim node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTMethod node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTMethods node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTTime node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTExps node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTExp node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTExp2 node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTFunc node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTNumber node, Object data){
    return defaultVisit(node, data);
  }
  public Object visit(ASTValue node, Object data){
    return defaultVisit(node, data);
  }
}
/* JavaCC - OriginalChecksum=cb64391011569efd08108adde3beb4e5 (do not edit this line) */
