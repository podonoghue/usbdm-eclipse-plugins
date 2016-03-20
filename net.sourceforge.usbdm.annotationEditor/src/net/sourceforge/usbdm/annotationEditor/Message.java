package net.sourceforge.usbdm.annotationEditor;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.Severity;

public class Message {
   
   private final Severity severity;
   private final String   message;
   
   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof Message)) {
         return false;
      }
      if (obj == this) {
         return true;
      }
      Message other = (Message) obj;
      return (severity == other.severity) && (message.compareTo(other.message) == 0);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((message == null) ? 0 : message.hashCode());
      result = prime * result + ((severity == null) ? 0 : severity.hashCode());
      return result;
   }

   /**
    * Creates a message with default ERROR severity
    * 
    * @param message Message text
    */
   public Message(String message) {
      this.message = message;
      this.severity = Severity.ERROR;
   }
   
   /**
    * Create a message with given severity level
    * 
    * @param message    Message text
    * @param severity   Severity level
    */
   public Message(String message, Severity severity) {
      this.message  = message;
      this.severity = severity;
   }
   
   /**
    * Indicates if the severity if less than the given level
    * 
    * @param level   Severity level to compare to
    * 
    * @return true if level is less than level given
    */
   public boolean lessThan(Severity level) {
      return severity.lessThan(level);
   }
   
   /**
    * Indicates if the severity if greater than the given level
    * 
    * @param level   Severity level to compare to
    * 
    * @return true if level is greater than level given
    */
   public boolean greaterThan(Severity level) {
      return severity.greaterThan(level);
   }

   /**
    * Returns message text
    * @return
    */
   public String getMessage() {
      return message;
   }
   /**
    * Returns severity level of message
    * 
    * @return Level of message
    */
   public Severity getSeverity() {
      return severity;
   }
}