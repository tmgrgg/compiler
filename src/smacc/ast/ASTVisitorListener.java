package smacc.ast;

import smacc.Function;
import smacc.ast.expr.*;
import smacc.ast.stat.*;

public interface ASTVisitorListener {

  void visitFunction(Function function);

  void visitArrayElemNode(ArrayElemNode node);

  void visitArrayLiteralNode(ArrayLiteralNode node);

  void visitBinaryOpNode(BinaryOpNode node);

  void visitBoolLiteralNode(BoolLiteralNode node);

  void visitCallNode(CallNode node);

  void visitCharLiteralNode(CharLiteralNode node);

  void visitIdentNode(IdentNode node);

  void visitIntLiteralNode(IntLiteralNode node);

  void visitNewPairNode(NewPairNode node);

  void visitPairElemNode(PairElemNode node);

  void visitUnaryOpNode(UnaryOpNode node);

  void visitAssignmentNode(AssignmentNode node);

  void visitIfNode(IfNode node);

  void visitPrintNode(PrintNode node);

  void visitReadNode(ReadNode node);

  void visitFreeNode(FreeNode node);

  void visitReturnNode(ReturnNode node);

  void visitScopeNode(ScopeNode node);

  void visitSequenceNode(SequenceNode node);

  void visitWhileNode(WhileNode node);

  void visitExitNode(ExitNode exitNode);

}
