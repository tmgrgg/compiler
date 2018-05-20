package smacc.arm;

import java.util.HashMap;
import java.util.LinkedHashMap;

/*
 * Declares static messages and strings at the beginning of the output
 */
public class ARMFileStart extends ARMNode {

  private HashMap<ArmMessage, ARMLabel> messages = new LinkedHashMap<>();

  public ARMFileStart() {

  }

  // Avoid adding the same message twice
  public void addMessage(ArmMessage message) {
    if (!messages.containsKey(message)) {
      messages.put(message, new ARMLabel(true));
    }
  }

  public ARMLabel getMessageLabel(ArmMessage message) {
    return messages.get(message);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (ArmMessage message : messages.keySet()) {
      String messageString = message.toString();
      int length = messageString.length();
      for (int i = 0; i < (messageString.length()); i++) {
        if ((messageString.charAt(i) == '\\')
            && (messageString.charAt(i + 1) != '\\')) {
          length--;
        }
      }
      sb.append(String.format("%s:\n\t.word %s\n\t.ascii \"%s\"\n", messages
          .get(message), length, message));
    }

    return String.format("%s.text\n\n.global main\n", (sb.length() == 0) ? ""
        : ".data\n\n" + sb + "\n");
  }

}
