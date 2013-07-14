package net.sourceforge.usbdm.cdt.tools;

import java.util.Hashtable;

import net.sourceforge.usbdm.constants.ToolInformationData;

import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.IValueVariableInitializer;

public class UsbdmPathVariableInitialiser implements IValueVariableInitializer {

   @Override
   public void initialize(IValueVariable variable) {
      Hashtable<String, ToolInformationData> pathInformation = ToolInformationData.getToolInformationTable();
      ToolInformationData variableData = pathInformation.get(variable.getName());
      if (variableData != null) {
         variable.setDescription(variableData.getDescription());
      }
      variable.setValue(UsbdmConstants.PATH_NOT_SET);
   }
}
