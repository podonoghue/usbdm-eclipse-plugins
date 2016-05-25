package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;

public abstract class Variable extends ObservableModel {
   
   /**
    * Class to hold the Name/Value pair
    */
   public static class Pair {
      
      /** Name used by GUI/model */
      public final String name;
      
      /** Value used by substitution */
      public final String value;
      
      /**
       * 
       * @param name  Name used by GUI/model
       * @param value Value used by data
       */
      public Pair(String name, String value) {
         this.name  = name;
         this.value = value;
      }
   }

   /** Name of variable visible to user */
   private final   String  fName;
   
   /** Name of variable visible to user */
   private         String  fKey;
   
   /** Indicates that the variable is locked and cannot be edited by user */
   private boolean fLocked = false;

   /** Indicates the variable is disabled */
   private boolean fEnabled = true;

   /** Description of variable */
   private String fDescription = null;

   /** Tool tip for this variable */
   private String fToolTip = null;
   
   /** Status of variable */
   private Message fStatus = null;
   
   /** Origin of variable value */
   private String fOrigin = null;
   
   /**
    * Constructor
    * 
    * @param name Name to display to user. Also used as default key.
    */
   public Variable(String name) {
      fName = name;
      fKey  = name;
   }

   /**
    * @return the Name
    */
   public String getName() {
      return fName;
   }

   /**
    * @return the key
    */
   public String getKey() {
      return fKey;
   }

   /**
    * @param key The key to set
    */
   public void setKey(String key) {
      this.fKey = key;
   }

   /**
    * Get the variable value as a string for use in substitutions
    * 
    * @return the Value
    */
   public abstract String getSubstitutionValue();

   /**
    * Get variable value as a string suitable for user display
    * @return
    */
   public abstract String getValueAsString();
   
   /**
    * Sets variable value
    * 
    * @param value The value to set
    * 
    * @return True if variable actually changed value
    */
   public abstract boolean setValue(Object value);

   private String getSimpleClassName() {
      String s = getClass().toString();
      int index = s.lastIndexOf(".");
      return s.substring(index+1, s.length());
   }
   
   @Override
   public String toString() {
      return String.format(getSimpleClassName()+"(Name=%s, value=%s (%s)", getName(), getSubstitutionValue(), getValueAsString());
   }

   public void setStatus(String message) {
      if ((fStatus != null) && (message != null) && fStatus.equals(message)) {
         // No significant change
         return;
      }
      if (message == null) {
         setStatus((Message)null);
      }
      else {
         setStatus(new Message(message));
      }
   }

   public void setStatus(Message message) {
      if ((fStatus == null) && (message == null)) {
         // No change
         return;
      }
      if ((fStatus != null) && (message != null) && fStatus.equals(message)) {
         // No significant change
         return;
      }
//      System.err.println(this+"setMessage("+message+")");
      fStatus = message;
      notifyListeners();
   }
   
   public Message getStatus() {
      return fStatus;
   }

   /**
    * Get the origin of signal value
    * 
    * @return The origin
    */
   public String getOrigin() {
      return fOrigin;
   }

   /**
    * Set the origin of signal value
    * 
    * @param origin The origin to set
    */
   public void setOrigin(String origin) {
      if ((fOrigin == null) && (origin == null)) {
         // No change
         return;
      }
      if ((fOrigin != null) && (fOrigin.equalsIgnoreCase(origin))) {
         // No significant change
         return;
      }
      fOrigin = origin;
      notifyListeners();
   }

   /** Set if the variable is locked and cannot be edited by user
    * 
    * @return the locked
    */
   public boolean isLocked() {
      return fLocked;
   }

   /** Indicates if the variable is locked and cannot be edited by user
    * 
    * @param locked The locked state to set
    * 
    * @return True if variable actually changed lock state
    */
   public boolean setLocked(boolean locked) {
      if (fLocked == locked) {
         return false;
      }
      fLocked = locked;
      notifyListeners();
      return true;
   }

   /**
    * Get value as a boolean
    * 
    * @return Value as boolean
    */
   public boolean getValueAsBoolean() {
      throw new RuntimeException(this+"("+getClass()+") is not compatible with boolean" );
   }

   /**
    * Get the variable value as a long
    * 
    * @return Value in user format as long
    */
   public long getValueAsLong() {
      throw new RuntimeException(this+"("+getClass()+") is not compatible with long" );
      }

   /**
    * Checks if the value is valid for assignment to this variable
    * 
    * @param value
    * 
    * @return Error message or null if valid
    */
   public String isValid(String value) {
      return null;
   }

   /**
    * Set the enabled state of variable
    * 
    * @param enabled State to set
    * 
    * @return true if the enabled state changed
    */
   public boolean enable(boolean enabled) {
      if (fEnabled == enabled) {
         return false;
      }
      fEnabled = enabled;
      notifyListeners();
      return true;
   }

   /**
    * @return The enabled state of variable
    */
   public boolean isEnabled() {
      return fEnabled;
   }

   /**
    * Gets description of variable
    * 
    * @return string
    */
   public String getDescription() {
      return fDescription;
   }

   /**
    * Set description of variable
    * 
    * @param description
    */
   public void setDescription(String description) {
      fDescription = description;
   }

   /**
    * Set tool tip
    * 
    * @param toolTip
    */
   public void setToolTip(String toolTip) {
      fToolTip = toolTip;
   }

   /**
    * Get tool tip
    * 
    * @return
    */
   public String getToolTip() {
      StringBuilder sb = new StringBuilder();
      if (fStatus != null) {
         if (fStatus.greaterThan(Message.Severity.WARNING)) {
            sb.append(fStatus.getMessage());
         }
         else if (fStatus != null) {
            sb.append(fStatus.getRawMessage());
         }
      }
      if (fToolTip != null) {
         if (sb.length() != 0) {
            sb.append('\n');
         }
         sb.append(fToolTip);
      }
      if (fOrigin != null) {
         if (sb.length() != 0) {
            sb.append('\n');
         }
         sb.append("Origin: ");
         sb.append(fOrigin);
      }
      return (sb.length()==0)?null:sb.toString();
   }
   

}