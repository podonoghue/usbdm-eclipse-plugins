package net.sourceforge.usbdm.cdt.ui;

import java.util.ArrayList;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.gdb.ui.UsbdmDebuggerPanel;

public abstract class WorkbenchGdbServerPreferencePage extends PreferencePage {

   private UsbdmDebuggerPanel usbdmDebuggerPanel;
   private InterfaceType      interfaceType;
   
   static public class WorkbenchPreferenceArmPage extends WorkbenchGdbServerPreferencePage 
   implements IWorkbenchPreferencePage {
      
      public WorkbenchPreferenceArmPage() {
         super(InterfaceType.T_ARM);
         super.setTitle("GDB Server settings for ARM");
      }

      @Override
      public void init(IWorkbench workbench) {
         super.init(workbench);
      }

      @Override
      public Control createContents(Composite parent) {
         return super.createContents(parent);
      }
   }

   static public class WorkbenchPreferenceCfv1Page extends WorkbenchGdbServerPreferencePage
   implements IWorkbenchPreferencePage {
      
      public WorkbenchPreferenceCfv1Page() {
         super(InterfaceType.T_CFV1);
         super.setTitle("GDB Server settings for Coldfire V1");
      }

      @Override
      public void init(IWorkbench workbench) {
         super.init(workbench);
      }

      @Override
      public Control createContents(Composite parent) {
         return super.createContents(parent);
      }
   }

   static public class WorkbenchPreferenceCfvxPage extends WorkbenchGdbServerPreferencePage 
   implements IWorkbenchPreferencePage {
      
      public WorkbenchPreferenceCfvxPage() {
         super(InterfaceType.T_CFVX);
         super.setTitle("GDB Server settings for Coldfire V2,3 & 4");
      }

      @Override
      public void init(IWorkbench workbench) {
         super.init(workbench);
      }

      @Override
      public Control createContents(Composite parent) {
         return super.createContents(parent);
      }
   }

   public WorkbenchGdbServerPreferencePage(String title) {
      super(title);
   }

   public WorkbenchGdbServerPreferencePage(String title, ImageDescriptor image) {
      super(title, image);
   }

   public WorkbenchGdbServerPreferencePage(InterfaceType interfaceType) {
      super();
      usbdmDebuggerPanel = new UsbdmDebuggerPanel();
      this.interfaceType = interfaceType;
      setDescription("Select default settings for GDB Socket Server");
   }

   public boolean isPageComplete() {
      // Allow early completion without displaying page (after display contents are checked)
      return !isControlCreated();
   }

   public void init(IWorkbench workbench) {
      noDefaultAndApplyButton();
   }

   @Override
   public Control createContents(Composite parent) {
      Control control = usbdmDebuggerPanel.createContents(parent, false);
      usbdmDebuggerPanel.setInterface(interfaceType, true);
      return control;         
   }

   /**
    *  Validates control & sets error message
    *  
    * @param message error message (null if none)
    * 
    * @return true => dialogue values are valid
    */
   public boolean validate() {
      String message = null;

      if (!isControlCreated()) {
         setErrorMessage(message);
         return true;
      }
      setErrorMessage(message);
      //      System.err.println("UsbdmConfigurationWizardPage.validate() => " + (message == null));
      return message == null;
   }

   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
    */
   @Override
   public void setVisible(boolean visible) {
      super.setVisible(visible);
      if (visible) {
         validate();
      }
   }

   protected void loadSettings() {
//      System.err.println("loadSettings() loading settings");
      try {
         usbdmDebuggerPanel.getGdbServerParameters().loadDefaultSettings();
      } catch (Exception e) {
         e.printStackTrace();
      }
      validate();
   }

   public boolean saveSettings() {
//      System.err.println("WorkbenchGdbServerPreferencePage.saveSetting() saving settings");
      if (!validate()) {
         return false;
      }
      if (!isControlCreated()) {
         return true;
      }
      boolean rv = false;
      try {
         rv = usbdmDebuggerPanel.saveGdbServerParametersAsDefault();
      } catch (Exception e) {
         e.printStackTrace();
      }
      return rv;
   }

   @Override
   public boolean performOk() {
      return super.performOk() && saveSettings();
   }

   /**
    * Get server command line
    * 
    * @return
    */
   public ArrayList<String> getServerCommandLine() {
       return usbdmDebuggerPanel.getGdbServerParameters().getServerCommandLine();
   }
}
