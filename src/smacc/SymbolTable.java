package smacc;

import java.util.LinkedHashMap;

import smacc.exceptions.IdentifierDeclaredPreviouslyInCurrentScopeException;
import smacc.exceptions.IdentifierUndeclaredException;

/*
 * SymbolTable class
 * Each instance is effectively a scope with a reference to its parent
 * 
 */
public class SymbolTable {
  private LinkedHashMap<String, Variable> entries;
  private SymbolTable previousTable;
  private int offset = 0;
  private int spOffset = 0;

  //  This constructor only used to create a global symbol table
  public SymbolTable() {
    previousTable = null;
    entries = new LinkedHashMap<>();
  }

  public SymbolTable(SymbolTable previousTable) {
    this.previousTable = previousTable;
    entries = new LinkedHashMap<>();
  }

  //  Returns with the size of this scope summed with all of its ancestors
  public int getSizeOfAllInBytes() {
    int size = getSizeOfTableInBytes();
    try {
      size += getPreviousTable().getSizeOfAllInBytes();
    } catch (NullPointerException e) {

    }
    return size;
  }

  public int getSizeOfTableInBytes() {
    int size = 0;
    for (String entry : entries.keySet()) {
      size += entries.get(entry).getSizeInBytes();
    }
    return size;
  }

  public SymbolTable getPreviousTable() {
    return previousTable;
  }

  //  Look for a variable through all ancestors
  public Variable lookupAll(String id) throws IdentifierUndeclaredException {
    Variable symbol = entries.get(id);
    if (symbol == null) {
      if (previousTable != null) {
        return previousTable.lookupAll(id);
      }
      throw new IdentifierUndeclaredException(id); // it's at the global
                                                   // symboltable
    }
    return symbol;
  }

  //  Change Stack Pointer Offset
  public void changeSPOffset(int x) {
    spOffset += x;
  }

  public void setSPOffset(int x) {
    spOffset = x;
  }

  public void declare(Variable entry, String id)
      throws IdentifierDeclaredPreviouslyInCurrentScopeException {
    if (entries.containsKey(id)) {
      throw new IdentifierDeclaredPreviouslyInCurrentScopeException();
    }
    offset += entry.getSizeInBytes();
    entry.setOffset(offset);
    entries.put(id, entry);
  }

  //  Get the stack offset of a variable
  public int getOffset(Variable entry) {
    int entryOffset = 0;
    SymbolTable currentTable = this;
    int currentTableSize = currentTable.getSizeOfTableInBytes();
    while ((currentTable != null) && !currentTable.entries.containsValue(entry)) {
      entryOffset += currentTable.getSizeOfTableInBytes();
      currentTableSize = currentTable.getSizeOfTableInBytes();
      currentTable = currentTable.getPreviousTable();
    }
    if (currentTable != null) {
      currentTableSize = currentTable.getSizeOfTableInBytes();
    }
    entryOffset += currentTableSize - entry.getOffset();
    return entryOffset + spOffset;
  }

  public boolean containsKeyInThisTable(String id) {
    return entries.containsKey(id);
  }

  public String toString() {
    String s = "";
    for (String key : entries.keySet()) {
      s += key + ", ";
    }
    return s;
  }

  //  In order to test live range calculation
  public String printLiveRangesOfLocals() {

    String s = "";
    for (String id : entries.keySet()) {
      s += id + entries.get(id).printLiveRange();
    }
    System.out.println(s);
    return s;
  }
}