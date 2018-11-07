package net.sourceforge.usbdm.deviceEditor.model;

public class Status {

   public enum Severity {
      OK, INFO, WARNING, ERROR;

      /**
       * Checks if the level is less than the given level
       * 
       * @param other
       * 
       * @return this&lt;other?
       */
      public boolean lessThan(Severity other) {
         return this.ordinal() < other.ordinal();
      }

      /**
       * Checks if the level is greater than the given level
       * 
       * @param other
       * 
       * @return this&gt;other?
       */
      public boolean greaterThan(Severity other) {
         return this.ordinal() > other.ordinal();
      }

   };

   private final Severity fSeverity;
   private final String   fText;
   
   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof Status)) {
         return false;
      }
      if (obj == this) {
         return true;
      }
      Status other = (Status) obj;
      return (fSeverity == other.fSeverity) && (fText.equalsIgnoreCase(other.fText));
   }

   public boolean equals(String msg) {
      return fText.equalsIgnoreCase(msg);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((fText == null) ? 0 : fText.hashCode());
      result = prime * result + ((fSeverity == null) ? 0 : fSeverity.hashCode());
      return result;
   }

   /**
    * Creates a message with default ERROR severity
    * 
    * @param message Message text
    */
   public Status(String message) {
      this.fText = message;
      this.fSeverity = Severity.ERROR;
   }
   
   /**
    * Create a message with given severity level
    * 
    * @param message    Message text
    * @param severity   Severity level
    */
   public Status(String message, Severity severity) {
      this.fText  = message;
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
    * Returns message text with severity prefix e.g. WARNING:....
    * @return
    */
   public String getText() {
      return fSeverity.name() + ": " + fText;
   }
   /**
    * Returns message text without severity prefix
    * @return
    */
   public String getSimpleText() {
      return fText;
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
      return getText();
   }
}