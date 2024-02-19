package net.sourceforge.usbdm.deviceEditor.model;

public class Status {

   public enum Severity {
      OK, INFO, WARNING, ERROR;
      
      /**
       * Checks if the level is less than the given level <br>
       * Null is treated as greater than ERROR i.e. always false
       * 
       * @param other
       * 
       * @return this&lt;other?
       */
      public boolean lessThan(Severity other) {
         return (other != null) && (this.ordinal() < other.ordinal());
      }

      /**
       * Checks if the level is less than the given level <br>
       * Null is treated as greater than ERROR i.e. always false
       * 
       * @param other
       * 
       * @return this&lt;=other?
       */
      public boolean lessThanOrEqual(Severity other) {
         return (other != null) && (this.ordinal() <= other.ordinal());
      }

      /**
       * Checks if the level is greater than the given level
       * Null is treated as less than OK i.e. result is true
       * 
       * @param other
       * 
       * @return this&gt;other?
       */
      public boolean greaterThan(Severity other) {
         return (other == null) || (this.ordinal() > other.ordinal());
      }

      /**
       * Checks if the level is greater than the given level
       * 
       * @param other
       * 
       * @return this&gt;other?
       */
      public boolean greaterThanOrEqual(Severity other) {
         return this.ordinal() >= other.ordinal();
      }

   };

   private final Severity fSeverity;
   private final String   fText;
   private final String   fHint;
   
   static public boolean equals(Object op1, Object op2) {
      if (op1 == op2) {
         return true;
      }
      if (!(op1 instanceof Status)) {
         return false;
      }
      return op1.equals(op2);
   }
   
   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (!(obj instanceof Status)) {
         return false;
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
      this.fText     = message;
      this.fSeverity = Severity.ERROR;
      this.fHint     = null;
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
      this.fHint     = null;
   }
   
   /**
    * Creates a message with default ERROR severity
    * 
    * @param message Message text
    */
   public Status(String message, String hint) {
      this.fText     = message;
      this.fSeverity = Severity.ERROR;
      this.fHint     = hint;
   }
   
   /**
    * Create a message with given severity level
    * 
    * @param message    Message text
    * @param severity   Severity level
    */
   public Status(String message, Severity severity, String hint) {
      this.fText  = message;
      this.fSeverity = severity;
      this.fHint     = hint;
   }
   
   /**
    * Indicates if the severity if less than the given level
    * 
    * @param level   Severity level to compare to
    * 
    * @return true if level is &lt;= level given
    */
   public boolean lessThanOrEqual(Severity level) {
      return fSeverity.lessThanOrEqual(level);
   }
   
   /**
    * Indicates if the severity if less than the given level
    * 
    * @param level   Severity level to compare to
    * 
    * @return true if level is &lt; level given
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
      StringBuilder sb = new StringBuilder();
      if (fSeverity.greaterThan(Severity.OK)) {
         sb.append(fSeverity.name());
         sb.append(" : ");
      }
      return sb.append(fText).toString();
   }
   /**
    * Returns message text without severity prefix
    * @return
    */
   public String getSimpleText() {
      return fText;
   }
   /**
    * Get hint text
    * 
    * @return Hint text or null if no hint set
    */
   public String getHint() {
      return fHint;
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