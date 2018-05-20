package smacc.arm;

/*
 * For constant messages placed at the beginning of the arm output
 * holds boolean isUserDefinedMessage
 * if the message is user defined we allow duplicates otherwise we
 * ensure messages are unique
 */
public class ArmMessage {

  private String message;
  private boolean isUserDefinedMessage;

  public ArmMessage(String message, boolean isUserDefinedMessage) {
    this.message = message;
    this.isUserDefinedMessage = isUserDefinedMessage;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!o.getClass().equals(getClass())) {
      return false;
    }

    ArmMessage other = (ArmMessage) o;

    return isUserDefinedMessage ? super.equals(other) : message
        .equals(other.message);
  }

  @Override
  public int hashCode() {
    return isUserDefinedMessage ? super.hashCode() : message.hashCode();
  }

  public String toString() {
    return message;
  }
}
