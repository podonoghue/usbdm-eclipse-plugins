package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IrqVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

public class IrqVariable extends Variable {
   
   public static enum Mode {
      NotInstalled,
      ClassMethod,
      UserMethod
   };
   
   public final static String NOT_INSTALLED_VALUE = "$"+Mode.NotInstalled.name();
   public final static String CLASS_VALUE         = "$"+Mode.ClassMethod.name();
   
   protected Mode fMode = Mode.NotInstalled;
   
   protected String fHandlerName = "";
   
   /** Default value of variable */
   protected String fDefault;
   
   /** Set pattern to match IRQ handler name */
   private String fPattern = null;
   
   /** Class handler name */
   private String fClassHandler = null;

   public IrqVariable(String name, String key) {
      super(name, key);
   }

   /**
    * Checks if identifier is a valid C name after % substitution
    * 
    * @param id
    * 
    * @return Valid => null<br>
    *         Invalid => Error string
    */
   public static String isValidCIdentifier(String id) {
      // Allow regex group substitutions 

      if (id != null) {
         id = id.replaceAll("%", "");
         if (id.matches("[_a-zA-Z][_a-zA-z0-9]*")) {
            return null;
         }
      }
      return "Illegal name for C identifier";
   }

   @Override
   public String isValid() {
      switch (fMode) {
      case ClassMethod:
      case NotInstalled:
         return null;
      case UserMethod:
         return isValidCIdentifier(fHandlerName);
      }
      return null;
   }

   @Override
   public String getSubstitutionValue() {
      return Integer.valueOf(fMode.ordinal()).toString();
   }

   /**
    * Extract mode from encoded string
    * 
    * @param value String encoding mode e.g. $ClassMethod or MyHandler
    * @return
    */
   public static Mode getMode(String value) {
      if (value.startsWith("$")) {
         try {
            return Mode.valueOf(value.substring(1));
         }
         catch (Exception e) {
            return Mode.NotInstalled;
         }
      }
      else {
         return Mode.UserMethod;
      }
   }
   
   /**
    * Get handler name from encoded string
    * 
    * @param value
    * @return
    */
   public static String getHandlerName(String value) {
      if (value.startsWith("$")) {
         return "";
      }
      else {
         return value;
      }
   }
   
   /**
    * Get handler name as specified by user
    * 
    * @return
    */
   public String getHandlerName() {
      return fHandlerName;
   }
   
   /**
    * Get mode indicating how the vector is handled
    * 
    * @return
    */
   public Mode getMode() {
      return fMode;
   }
   
   @Override
   public String getValueAsString() {
      switch (fMode) {
      case NotInstalled:
         return "No handler installed";
      case ClassMethod:
         return "Software (Use setCallback() or override class method)";
      case UserMethod:
         return "User method: "+fHandlerName;
      }
      return null;
   }

   @Override
   public long getValueAsLong() {
      return fMode.ordinal();
   }
   
   @Override
   public boolean setValue(Object value) {
      if (getPersistentValue().equals(value.toString())) {
         return false;
      }
      super.debugPrint("IrqVariable["+this+"].setValue("+value+"), old "+value);
      setPersistentValue(value.toString());
      notifyListeners();
      return true;
   }

   @Override
   public void setValueQuietly(Object value) {
      setPersistentValue(value.toString());
   }

   @Override
   public String getPersistentValue() {
      switch (fMode) {
      case ClassMethod:
      case NotInstalled:
         return "$"+fMode.name();
      case UserMethod:
         return fHandlerName;
      }
      return null;
   }

   @Override
   public void setPersistentValue(String value) {
      if (value.startsWith("$")) {
         try {
            fMode = Mode.valueOf(value.substring(1));
         }
         catch (Exception e) {
            fMode = Mode.NotInstalled;
         }
      }
      else if (isValidCIdentifier(value) != null) {
         fMode = Mode.NotInstalled;
         }
      else {
         fMode = Mode.UserMethod;
         fHandlerName = value;
      }
   }

   @Override
   public void setDefault(Object value) {
      // Ignored
   }

   @Override
   public Object getDefault() {
      return getPersistentValue();
   }

   @Override
   public VariableModel createModel(BaseModel parent) {
      return new IrqVariableModel(parent, this);
   }

   /**
    * Set pattern to match IRQ handler name
    * 
    * @param pattern
    */
   public void setPattern(String pattern) {
      fPattern = pattern;
   }

   /**
    * Get pattern to match IRQ handler name
    * 
    * @return pattern
    */
   public String getPattern() {
      return fPattern;
   }

   /**
    * Set name of class method that handles this interrupt
    * 
    * @param handler Name of handler
    */
   public void setClassHandler(String handler) {
      fClassHandler = handler;
   }

   /**
    * Get name of class method that handles this interrupt
    * 
    * @return Name of handler
    */
   public String getClassHandler() {
      return fClassHandler;
   }

   @Override
   public boolean isDefault() {
      return getPersistentValue().equals("$"+Mode.NotInstalled.name());
   }

   
}
