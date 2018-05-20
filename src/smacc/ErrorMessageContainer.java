package smacc;

import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;

/*
 * ErrorMessageContainer
 * 
 * Stores warnings and errors generated by the compiler
 * Uses a reference to the source code to print the line associated with the error
 */

public class ErrorMessageContainer {

  private StringBuilder messages;
  private int errorCount;
  private CharStream source;
  private List<Integer> newlinePositions;
  private boolean syntacticErr = false;

  public enum ErrorCode {
    SYNTACTIC_ERR, SEMANTIC_ERR, SUCCESS, RUNTIME_ERR;
    public int getValue() {
      switch (this) {
        case SYNTACTIC_ERR:
          return exit_syntactic_err;
        case RUNTIME_ERR:
          return exit_runtime_err;
        case SEMANTIC_ERR:
          return exit_semantic_err;
        case SUCCESS:
          return exit_success;
        default:
          return 0;
      }

    }
  }

  private ErrorCode errorCode = ErrorCode.SUCCESS;

  private static int exit_syntactic_err = 0;
  private static int exit_semantic_err = 0;
  private static int exit_success = 0;
  private static int exit_runtime_err = 0;

  public ErrorMessageContainer(CharStream source, List<Integer> newlinePositions) {
    messages = new StringBuilder();
    errorCount = 0;
    this.source = source;
    this.newlinePositions = newlinePositions;
  }

  public String toString() {
    return messages.toString();
  }

  public int getErrorCount() {
    return errorCount;
  }

  // Avoid overcoupling ErrorMessageContainer with SMACC / WACC language
  public void setupErrorCodes(int exit_success, int exit_syntactic_err,
      int exit_semantic_err, int exit_runtime_err) {
    ErrorMessageContainer.exit_syntactic_err = exit_syntactic_err;
    ErrorMessageContainer.exit_semantic_err = exit_semantic_err;
    ErrorMessageContainer.exit_success = exit_success;
    ErrorMessageContainer.exit_runtime_err = exit_runtime_err;
  }

  public int getErrorCode() {
    return errorCode.getValue();
  }

  /*
   * We check for some syntactic errors in SemanticVisitor and add them to the
   * error container This should have a prioritised error code
   */
  public void setErrorType(ErrorCode e) {
    this.errorCode = e;
  }

  void add(String message) {
    messages.append(message);
    messages.append('\n');
    errorCount++;
  }

  /*
   * Takes a string with % characters that are replaced, in order, by the
   * toString methods of the following objects ParserRuleContext is passed to
   * allow linking to the source code
   */
  public void add(ParserRuleContext ctx, String message, Object... types) {

    if (errorCode.equals(ErrorCode.SUCCESS))
      errorCode = ErrorCode.SEMANTIC_ERR;

    int line = ctx.start.getLine();
    // Error built in its own StringBuilder to be appended at the end of the
    // message
    StringBuilder error = new StringBuilder();

    String[] parts = message.split("%");

    // Replace instances of % with corresponding object
    //
    // Is not strict with the counts of either so if there are insufficient %
    // characters
    // we ignore the remaining objects and insufficient objects will just result
    // in no
    // replacement
    for (int i = 0; i < parts.length; i++) {
      error.append(parts[i]);
      if (types.length <= i)
        continue;
      error.append(String.format("'%s'", (types[i].toString())));
    }
    error.append('\n');

    String sourceLine = source.getText(new Interval(newlinePositions
        .get(line - 1), newlinePositions.get(line) - 2));
    // Strip leading whitespace
    sourceLine = sourceLine.replaceAll("^\\s*", "");
    messages.append(String.format("Error on line %d:\n>>  %s\n%s\n", line,
        sourceLine, error.toString()));
    errorCount++;

  }
}