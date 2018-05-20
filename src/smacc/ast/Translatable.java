package smacc.ast;

import smacc.arm.Translator;

public interface Translatable {
  void translate(Translator translator);
}
