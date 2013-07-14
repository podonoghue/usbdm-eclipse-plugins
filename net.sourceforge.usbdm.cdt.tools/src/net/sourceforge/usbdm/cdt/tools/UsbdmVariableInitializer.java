package net.sourceforge.usbdm.cdt.tools;

import java.util.Hashtable;

import net.sourceforge.usbdm.constants.VariableInformationData;

import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.IValueVariableInitializer;

public class UsbdmVariableInitializer implements
      IValueVariableInitializer {

   @Override
   public void initialize(IValueVariable variable) {
      Hashtable<String,VariableInformationData> variableInformation = VariableInformationData.getVariableInformationTable();
      VariableInformationData variableData = variableInformation.get(variable.getName());
      if (variableData != null) {
         variable.setValue(variableData.getDefaultValue());
         variable.setDescription(variableData.getDescription());
      }
   }
}
