package smacc.ast.smartalloc;

import smacc.Function;
import smacc.Variable;
import smacc.ast.ASTVisitorListener;
import smacc.ast.expr.ArrayElemNode;
import smacc.ast.expr.ArrayLiteralNode;
import smacc.ast.expr.BinaryOpNode;
import smacc.ast.expr.BoolLiteralNode;
import smacc.ast.expr.CallNode;
import smacc.ast.expr.CharLiteralNode;
import smacc.ast.expr.IdentNode;
import smacc.ast.expr.IntLiteralNode;
import smacc.ast.expr.NewPairNode;
import smacc.ast.expr.PairElemNode;
import smacc.ast.expr.UnaryOpNode;
import smacc.ast.stat.AssignmentNode;
import smacc.ast.stat.ExitNode;
import smacc.ast.stat.FreeNode;
import smacc.ast.stat.IfNode;
import smacc.ast.stat.PrintNode;
import smacc.ast.stat.ReadNode;
import smacc.ast.stat.ReturnNode;
import smacc.ast.stat.ScopeNode;
import smacc.ast.stat.SequenceNode;
import smacc.ast.stat.WhileNode;

public class LiveRangeAnalyser implements ASTVisitorListener {
  
  int topWhileIndex = 0;
  int whileCount = 0;
  
  public void inWhileLoop() {
    whileCount++;
  }
  
  public void outWhileLoop() {
    whileCount--;
  }
  
  private boolean isInLoop() {
    return whileCount > 0;
  }
  
  //TESTING PURPOSES
  @Override
  public void visitFunction(Function function) {}

  @Override
  public void visitArrayElemNode(ArrayElemNode node) {}

  @Override
  public void visitArrayLiteralNode(ArrayLiteralNode node) {}

  @Override
  public void visitBinaryOpNode(BinaryOpNode node) {}

  @Override
  public void visitBoolLiteralNode(BoolLiteralNode node) {}

  @Override
  public void visitCallNode(CallNode node) {}

  @Override
  public void visitCharLiteralNode(CharLiteralNode node) {}

  @Override
  public void visitIdentNode(IdentNode node) {
    Variable var = node.getVariable();
    if (var.getBirthIndex() == 0) {
      var.setBirthIndex(node.getIndex());
    }
    if (isInLoop()) {
      var.setDeathIndex(topWhileIndex);
    } else {
      var.setDeathIndex(node.getIndex());
    }
  }

  @Override
  public void visitIntLiteralNode(IntLiteralNode node) {}

  @Override
  public void visitNewPairNode(NewPairNode node) {}

  @Override
  public void visitPairElemNode(PairElemNode node) {}

  @Override
  public void visitUnaryOpNode(UnaryOpNode node) {}

  @Override
  public void visitAssignmentNode(AssignmentNode node) {}

  @Override
  public void visitIfNode(IfNode node) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void visitPrintNode(PrintNode node) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void visitReadNode(ReadNode node) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void visitFreeNode(FreeNode node) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void visitReturnNode(ReturnNode node) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void visitScopeNode(ScopeNode node) {
   // node.getSymbolTable().printLiveRangesOfLocals();
  }

  @Override
  public void visitSequenceNode(SequenceNode node) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visitWhileNode(WhileNode node) {
    topWhileIndex = Math.max(node.getIndex(), topWhileIndex);
  }

  @Override
  public void visitExitNode(ExitNode exitNode) {
    // TODO Auto-generated method stub
  }
}
