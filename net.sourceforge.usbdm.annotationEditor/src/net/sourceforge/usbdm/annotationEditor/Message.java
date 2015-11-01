package net.sourceforge.usbdm.annotationEditor;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.Severity;

public class Message {
   public final Severity severity;
   public final String message;
   
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
    * @return true if level is then that that given
    */
   public boolean lessThan(Severity level) {
      switch(level) {
      case OK:
         return true;
      case INFORMATION:
         return false;
      case WARNING:
         return severity==Severity.INFORMATION;
      case ERROR:
         return (severity==Severity.INFORMATION) || (severity==Severity.WARNING);
      default:
         break;
      }
      return false;
   }
}