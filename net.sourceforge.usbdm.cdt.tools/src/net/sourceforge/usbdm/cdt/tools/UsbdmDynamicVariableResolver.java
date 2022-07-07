package net.sourceforge.usbdm.cdt.tools;

import java.util.Hashtable;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.constants.UsbdmSharedSettings;
import net.sourceforge.usbdm.constants.VariableInformationData;
import net.sourceforge.usbdm.jni.Usbdm;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;

public class UsbdmDynamicVariableResolver implements IDynamicVariableResolver {

   static IPath usbdmApplicationPath  = null;
   static IPath usbdmResourcePath     = null;

   public UsbdmDynamicVariableResolver() {
      usbdmApplicationPath = Usbdm.getApplicationPath();
      if (usbdmApplicationPath == null) {
         usbdmApplicationPath = new Path("USBDM APPLICATION PATH NOT FOUND");
      }
      usbdmResourcePath = Usbdm.getResourcePath();
      if (usbdmResourcePath == null) {
    	  usbdmResourcePath = new Path("USBDM RESOURCE PATH NOT FOUND");
      }
   }
   
   @Override
   public String resolveValue(IDynamicVariable variable, String argument)
         throws CoreException {

      if (variable.getName().equals(UsbdmSharedConstants.USBDM_APPLICATION_PATH_VAR)) {
//	         System.err.println("UsbdmDynamicVariableResolver.resolveValue(usbdm_application_path) => " + usbdmApplicationPath.toOSString());
         return usbdmApplicationPath.toOSString();
      }
      if (variable.getName().equals(UsbdmSharedConstants.USBDM_RESOURCE_PATH_VAR)) {
//	         System.err.println("UsbdmDynamicVariableResolver.resolveValue(usbdm_resource_path) => " + usbdmApplicationPath.toOSString());
         return usbdmResourcePath.toOSString();
      }
      UsbdmSharedSettings usbdmSharedSettings = UsbdmSharedSettings.getSharedSettings();
      String variableValue = usbdmSharedSettings.get(variable.getName());
      if (variableValue == null) {
         Hashtable<String, VariableInformationData> variableInformationTable = VariableInformationData.getVariableInformationTable();
         VariableInformationData variableInfo = variableInformationTable.get(argument);
         if (variableInfo == null) {
            variableValue = "";
         }
         else {
            variableValue = variableInfo.getDefaultValue();
         }
      }
//      System.err.println("UsbdmDynamicVariableResolver.resolveValue("+variable.getName()+") => " + variableValue);
      return variableValue;
   }
}
