package smacc;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.ast.Translatable;
import smacc.ast.stat.ScopeNode;
import smacc.ast.stat.StatNode;
import smacc.types.WACCType;

import java.util.ArrayList;
import java.util.List;

/*
 * Function class used to hold the id, parameter and return types and
 * eventually function body as AST Nodes
 */
public class Function implements Translatable {

  private ScopeNode scopeNode;
  private WACCType returnType;
  private List<Variable> parameters;

  //  Identifier as the user defined it
  private String baseId;

  //  Unique identifier used in ARM output
  private String id;

  public Function(String baseId, WACCType returnType,
      ArrayList<Variable> arrayList, SymbolTable funcScope) {
    this.returnType = returnType;
    this.parameters = arrayList;
    this.baseId = baseId;
    this.scopeNode = new ScopeNode(null, funcScope); // null while we prototype
    if (baseId.equals("main")) {
      this.id = baseId;
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getBaseId() {
    return baseId;
  }

  public void visit(ASTVisitorListener listener) {
    listener.visitFunction(this);
    scopeNode.visit(listener);
  }

  public boolean isMain() {
    return baseId.equals("main");
  }

  public WACCType getReturnType() {
    return returnType;
  }

  public int getArgumentCount() {
    return parameters.size();
  }

  // May return null or throw exception
  public WACCType getArgumentType(int i) {
    return parameters.get(i).getType();
  }

  public int totalParameterSizeInBytes() {
    int size = 0;
    for (Variable parameter : parameters) {
      size += parameter.getSizeInBytes();
    }
    return size;
  }

  @Override
  public boolean equals(Object o) {

    if (!(o instanceof Function)) {
      return false;
    }

    Function other = (Function) o;

    if (parameters.size() != other.parameters.size()) {
      return false;
    }

    boolean parametersHaveSameType = true;
    for (int i = 0; i < parameters.size(); i++) {
      parametersHaveSameType &= parameters.get(i).getType().equals(
          other.parameters.get(i).getType());
    }

    return (other.returnType.equals(returnType)) && parametersHaveSameType
        && (baseId.equals(other.baseId));
  }

  public SymbolTable getFuncScope() {
    return scopeNode.getSymbolTable();
  }

  public StatNode getBody() {
    return scopeNode;
  }

  public void setBody(StatNode body) {
    scopeNode.setStat(body);
  }

  public boolean isParam(String varName, SymbolTable scope) {
    SymbolTable paramTable = scope;
    while (paramTable.getPreviousTable() != null) {
      if (paramTable.containsKeyInThisTable(varName)) {
        return false;
      }
      paramTable = paramTable.getPreviousTable();
    }
    return paramTable.containsKeyInThisTable(varName);

  }

  @Override
  public int hashCode() {
    return returnType.hashCode() * parameters.hashCode() + scopeNode.hashCode()
        + scopeNode.getSymbolTable().hashCode();
  }

  @Override
  public String toString() {
    String s = "(";
    for (Variable arg : parameters) {
      s += arg.getType().toString();
    }
    s += ")->" + returnType.toString();
    return s;
  }

  @Override
  public void translate(Translator translator) {
    translator.translateFunction(this);
  }

}
