package net.sourceforge.usbdm.constants;

import java.util.Hashtable;

/**
 *   Used to hold information about Eclipse Dynamic Variables
 *   
 *   @author Peter
 *
 */
public class VariableInformationData {
   protected String description;
   protected String variableName;
   protected String linuxDefault;
   protected String winDefault;

   static Hashtable<String, VariableInformationData> variableInformationTable = null;

   public VariableInformationData(String description, String linuxDefault, String winDefault, String variableName) {
      this.description     = description;
      this.variableName    = variableName;
      this.linuxDefault    = linuxDefault;
      this.winDefault      = winDefault;
   }
   public VariableInformationData(VariableInformationData other) {
      this.description     = other.description;
      this.variableName    = other.variableName;
      this.linuxDefault    = other.linuxDefault;
      this.winDefault      = other.winDefault;
   }
   /**
    *   Provides a description of variable
    * 
    *   @return the description as a string
    */
   public String getDescription()        { return description; }
   /**
    *   Provides the name of variable
    * 
    *   @return the name as a string
    */
   public String getVariableName()       { return variableName; }
   /**
    *   Provides a hint for the variable
    * 
    *   @return the hint as a string
    */
   public String getHint()               { 
      return description + " to use, e.g. " + getDefaultValue(); 
   }
   /**
    *   Provides a default value for Linux platforms
    * 
    *   @return the default value as a string
    */
   public String getLinuxDefaultValue()  { return linuxDefault; }
   /**
    *   Provides a default value for Win platforms
    * 
    *   @return the default value as a string
    */
   public String getWinDefaultValue()    { return winDefault; }
   /**
    *   Provides a default value
    * 
    *   @return the default value as a string
    */
   public String getDefaultValue()     { 
      String os = System.getProperty("os.name");
      if ((os != null) && os.toUpperCase().contains("LINUX")) {
         return getLinuxDefaultValue();
      }
      else {
         return getWinDefaultValue();
      }
   }
   /**
    *   Table of variables
    * 
    *   @return the table
    */
   public static Hashtable<String, VariableInformationData> getVariableInformationTable() {
      if (variableInformationTable == null) {
         variableInformationTable = new Hashtable<String, VariableInformationData>();
         variableInformationTable.put(UsbdmSharedConstants.USBDM_MAKE_COMMAND_VAR, 
               new VariableInformationData("Make command",        "make", "usbdm-make", UsbdmSharedConstants.USBDM_MAKE_COMMAND_VAR));
         variableInformationTable.put(UsbdmSharedConstants.USBDM_RM_COMMAND_VAR,
               new VariableInformationData("Rm (delete) command", "rm",   "usbdm-rm",   UsbdmSharedConstants.USBDM_RM_COMMAND_VAR));
      }
      return variableInformationTable;
   }
}

