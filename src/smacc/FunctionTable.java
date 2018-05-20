package smacc;

import java.util.*;

import smacc.ast.stat.StatNode;
import smacc.exceptions.IdentifierDeclaredPreviouslyInCurrentScopeException;
import smacc.exceptions.IdentifierUndeclaredException;
import smacc.arm.ARMNode;
import smacc.arm.Translator;
import smacc.types.WACCType;

/*
 * FunctionTable
 * Holds a map from String identifiers to WACCFunction types
 * Can be considered to be in global scope
 */
public class FunctionTable {

  private Hashtable<String, HashSet<Function>> funcTable;
  private Function main;
  private Translator translator = new Translator();

  public FunctionTable() {
    funcTable = new Hashtable<>();
    // main = new Function(new IntType(), new ArrayList<Variable> (), new
    // SymbolTable());
  }

  public Function getMain() {
    return main;
  }

  public void setMain(Function main) {
    this.main = main;
  }

  public StatNode getMainBody() {
    return main.getBody();
  }

  public Iterator<Map.Entry<String, HashSet<Function>>> iterator() {
    return funcTable.entrySet().iterator();
  }

  // Check if params is in funcTable - If not, throw exception.
  //
  // Get list of functions that params maps to. Look in this list for any
  // function
  public Function lookupFunction(String baseId, List<WACCType> params)
      throws IdentifierUndeclaredException {
    if (!funcTable.containsKey(baseId)) {
      throw new IdentifierUndeclaredException(baseId);
    }
    HashSet<Function> functions = funcTable.get(baseId);
    for (Function function : functions) {
      if (function.getArgumentCount() == params.size()) {
        boolean matches = true;
        for (int i = 0; i < function.getArgumentCount(); i++) {
          matches &= function.getArgumentType(i).equals(params.get(i));
        }
        if (matches) {
          return function;
        }
      }
    }
    throw new IdentifierUndeclaredException(baseId);
  }

  //  Assumes function was constructed with base id.
  //  base_id kept constant but id set to have f_ prefix and suffix
  //  based on overload count (number of functions with the same name)
  public void declare(Function func, String baseId)
      throws IdentifierDeclaredPreviouslyInCurrentScopeException {

    if (!funcTable.containsKey(baseId)) {
      HashSet<Function> functionSet = new HashSet<>();
      func.setId("f_" + baseId);
      functionSet.add(func);
      funcTable.put(baseId, functionSet);
    } else {
      HashSet<Function> functions = funcTable.get(baseId);
      int overloadCount = functions.size();
      for (Function function : functions) {
        if (function.equals(func)) {
          throw new IdentifierDeclaredPreviouslyInCurrentScopeException();
        }
      }
      func.setId("f_" + overloadCount + "_" + baseId);
      functions.add(func);
      funcTable.put(baseId, functions);
    }
  }

  public List<ARMNode> translate() {
    for (String id : funcTable.keySet()) {
      for (Function function : funcTable.get(id)) {
        function.translate(translator);
      }
    }
    main.translate(translator);
    return translator.getARM();
  }

  public String toString() {
    StringBuilder s = new StringBuilder("");
    for (String id : funcTable.keySet()) {
      for (Function func : funcTable.get(id)) {
        s.append(func.getId() + ": ");
        s.append(func.getBody().toString());
      }
    }
    s.append(getMain() + ": ");
    s.append(getMainBody().toString());
    return s.toString();
  }
}
