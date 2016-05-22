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
   
   /** Message (status) of variable */
   private         Message fMessage = null;
   
   /** Indicates that the variable is locked and cannot be edited by user */
   private boolean fLocked = false;

   /** Indicates the variable is disabled */
   private boolean fEnabled = true;

   /** Description of variable */
   private String fDescription = null;

   /** Tool tip for this variable */
   private String fToolTip = null;
   
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

   public void setMessage(String message) {
      if ((fMessage != null) && (message != null) && fMessage.equals(message)) {
         // No significant change
         return;
      }
      if (message == null) {
         setMessage((Message)null);
      }
      else {
         setMessage(new Message(message));
      }
   }

   public void setMessage(Message message) {
      if ((fMessage == null) && (message == null)) {
         // No change
         return;
      }
      if ((fMessage != null) && (message != null) && fMessage.equals(message)) {
         // No significant change
         return;
      }
//      System.err.println(this+"setMessage("+message+")");
      fMessage = message;
      notifyListeners();
   }
   
   public Message getMessage() {
      return fMessage;
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
    * Get tool tip
    * 
    * @return
    */
   public String getToolTip() {
      String tip = fToolTip;
      if (tip == null) {
         tip = "";
      }
      Message message = getMessage();
      if ((message != null) && (message.greaterThan(Message.Severity.WARNING))) {
         tip += (tip.isEmpty()?"":"\n")+message.getMessage();
      }
      else if (message != null) {
         tip += (tip.isEmpty()?"":"\n")+message.getRawMessage();
      }
      return (tip.isEmpty())?null:tip;
   }

   /**
    * Set tool tip
    * 
    * @param toolTip
    */
   public void setToolTip(String toolTip) {
      fToolTip = toolTip;
   }


}