package net.sourceforge.usbdm.deviceEditor.model;

public class Message {

   public enum Severity {
      OK, INFORMATION, WARNING, ERROR;

      /**
       * Checks if the level is less than the given level
       * 
       * @param other
       * 
       * @return this&lt;other?
       */
      boolean lessThan(Severity other) {
         return this.ordinal() < other.ordinal();
      }

      /**
       * Checks if the level is greater than the given level
       * 
       * @param other
       * 
       * @return this&gt;other?
       */
      boolean greaterThan(Severity other) {
         return this.ordinal() > other.ordinal();
      }

   };

   private final Severity fSeverity;
   private final String   fMessage;
   
   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof Message)) {
         return false;
      }
      if (obj == this) {
         return true;
      }
      Message other = (Message) obj;
      return (fSeverity == other.fSeverity) && (fMessage.equalsIgnoreCase(other.fMessage));
   }

   public boolean equals(String msg) {
      return fMessage.equalsIgnoreCase(msg);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((fMessage == null) ? 0 : fMessage.hashCode());
      result = prime * result + ((fSeverity == null) ? 0 : fSeverity.hashCode());
      return result;
   }

   /**
    * Creates a message with default ERROR severity
    * 
    * @param message Message text
    */
   public Message(String message) {
      this.fMessage = message;
      this.fSeverity = Severity.ERROR;
   }
   
   /**
    * Create a message with given severity level
    * 
    * @param message    Message text
    * @param severity   Severity level
    */
   public Message(String message, Severity severity) {
      this.fMessage  = message;
      this.fSeverity = severity;
   }
   
   /**
    * Indicates if the severity if less than the given level
    * 
    * @param level   Severity level to compare to
    * 
    * @return true if level is less than level given
    */
   public boolean lessThan(Severity level) {
      return fSeverity.lessThan(level);
   }
   
   /**
    * Indicates if the severity if greater than the given level
    * 
    * @param level   Severity level to compare to
    * 
    * @return true if level is greater than level given
    */
   public boolean greaterThan(Severity level) {
      return fSeverity.greaterThan(level);
   }

   /**
    * Returns message text
    * @return
    */
   public String getMessage() {
      return fSeverity.name() + ": " + fMessage;
   }
   /**
    * Returns severity level of message
    * 
    * @return Level of message
    */
   public Severity getSeverity() {
      return fSeverity;
   }

   @Override
   public String toString() {
      return fMessage;
   }
   
}