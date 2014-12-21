package net.sourceforge.usbdm.cdt.ui.newProjectWizard;
/*
 Change History
+============================================================================================
| Revision History
+============================================================================================
| 16 Nov 13 | Fixed path lookup for resource files (e.g. header files) on linux   4.10.6.100
| 16 Nov 13 | Added default files header & vector files based upon subfamily      4.10.6.100
+============================================================================================
*/
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.deviceDatabase.Condition;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.deviceDatabase.ProjectAction;
import net.sourceforge.usbdm.deviceDatabase.ProjectActionList;
import net.sourceforge.usbdm.deviceDatabase.ProjectVariable;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 *  USBDM New Project Wizard page "USBDM Project"
 *
 */
public class UsbdmProjectOptionsPage extends WizardPage {

   // These constants are used both for the dialogue persistent storage AND the page data map keys
   private final static String PAGE_ID    = UsbdmConstants.PROJECT_OPTIONS_PAGE_ID;
   private final static String PAGE_NAME  = UsbdmConstants.PROJECT_OPTIONS_PAGE_NAME;

   private UsbdmProjectParametersPage   usbdmProjectPage;

   public UsbdmProjectOptionsPage() {
      super(PAGE_NAME);
      this.usbdmProjectPage = null;
      setTitle("USBDM Project Options");
      setDescription("Select project options");
      setPageComplete(true);
   }

   public UsbdmProjectOptionsPage(UsbdmProjectParametersPage usbdmProjectPage) {
      super(PAGE_NAME);
      this.usbdmProjectPage = usbdmProjectPage;
      setTitle("USBDM Project Options");
      setDescription("Select project options");
      setPageComplete(true);
   }

   public String getPageID() {
      return PAGE_ID;
   }

   /**
    *  Validates control & sets error message
    *  
    * @param message error message (null if none)
    */
   private void validate() {
      String message = null;
      setErrorMessage(message);
      setPageComplete(message == null);
   }

   ArrayList<ProjectVariable> variableList = new ArrayList<ProjectVariable>();

   private static final String OPTIONS_KEY = "usbdm.project.additionalOptions."; 
   
   /**
    * Populate the Parameters controls
    * 
    * @param parent
    * @return
    */
   private Control createOptionsControl(Composite parent) {

      IDialogSettings dialogSettings = getDialogSettings();
      
      ScrolledComposite sc = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
      sc.setExpandHorizontal(true);
      sc.setExpandVertical(true);
      setControl(sc);

      Composite comp = new Composite(sc, SWT.FILL);
      sc.setContent(comp);
      GridLayout layout = new GridLayout();
      comp.setLayout(layout);

      Device device = getDevice();
//      Device device = getDevice("MKE06Z64M4");
      
      Group group = new Group(comp, SWT.NONE);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      group.setLayoutData(gd);
      group.setLayout(new GridLayout(2, false));

      if (device == null) {
         // Should be impossible
         group.setText("No device selected!!");
         return group;
      }
      group.setText("Additional Project Options ("+device.getName()+")");
      
      HashSet<ProjectVariable> conditionMap = new HashSet<ProjectVariable>();
      variableList = new ArrayList<ProjectVariable>();

      if (device != null) {
         ArrayList<ProjectActionList> projectActionLists = device.getProjectActionLists();
         for (ProjectActionList actionList:projectActionLists) {
            for (ProjectAction action : actionList) {
               Condition condition = action.getCondition();
//               System.err.println("createOptionsControl() - " + action.toString());
//               if (action instanceof ProjectCustomAction) {
//                  System.err.println("createOptionsControl() - " + action.toString());
//                  System.err.println("                       - " + ((condition==null)?"<null>":condition.getVariable().getName()));
//               }
               if (condition != null) {
                  if (!conditionMap.contains(condition.getVariable())) {
                     conditionMap.add(condition.getVariable());
                     variableList.add(condition.getVariable());
                  }
               }
            }
         }
      }
      if (variableList.size() == 0) {
         Label label = new Label(group, SWT.NONE);
         label.setText("No Options"); //$NON-NLS-1$
      }
      else {
         for (ProjectVariable variable : variableList) {
            Label label = new Label(group, SWT.NONE);
            label.setText(variable.getName()+"  "); //$NON-NLS-1$
            Button button = new Button(group, SWT.CHECK);
            button.setText("  enable");
            boolean value = Boolean.valueOf(variable.getDefaultValue());
            if (dialogSettings != null) {
               String sValue = dialogSettings.get(OPTIONS_KEY+variable.getId());
               if (sValue != null) {
                  value = Boolean.valueOf(sValue);
               }
            }
            variable.setValue(value?"true":"false");
            button.setSelection(value);
            button.setToolTipText(variable.getDescription());
            button.setData(variable);
            button.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetSelected(SelectionEvent e) {
                  Button button = (Button) e.getSource();
                  ProjectVariable variable = (ProjectVariable)button.getData();
                  variable.setValue(button.getSelection()?"true":"false");
               }

               @Override
               public void widgetDefaultSelected(SelectionEvent e) {
                  Button button = (Button) e.getSource();
                  ProjectVariable variable = (ProjectVariable)button.getData();
                  variable.setValue(variable.getDefaultValue());
               }
            });
         }
      }
      return group;
   }
   
   Control optionsControl = null;
   
   private void createHolderControl(Composite parent) {
      GridData gd;
      parent.setLayout(new GridLayout());

      if (optionsControl != null) {
         optionsControl.dispose();
         optionsControl = null;
      }
      optionsControl = createOptionsControl(parent);
      gd = new GridData(SWT.FILL, SWT.NONE, true, false);
      optionsControl.setLayoutData(gd);
   }

   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
    */
   @Override
   public void createControl(Composite parent) {
      
      Composite control = new Composite(parent, SWT.NONE);
      control.setLayout(new GridLayout());

      createHolderControl(control);
      
      setControl(control);
      
      validate();
   }
   
   public void refresh() {
      Composite control = (Composite) getControl();
      if (control != null) {
         createHolderControl(control);
         control.layout(true);
      }
   }
   
   private Device getDevice() {
      if (usbdmProjectPage == null) {
         usbdmProjectPage = (UsbdmProjectParametersPage) getWizard().getPage(UsbdmConstants.PROJECT_PAGE_ID);
      }
      if (usbdmProjectPage == null) {
         return null;
      }
      return usbdmProjectPage.getDevice();
   }
   /**
    * For debug
    * 
    * @param currentDevice
    * @return
    */
   @SuppressWarnings("unused")
   private Device getDevice(String currentDevice) {
      DeviceDatabase deviceDatabase = new DeviceDatabase(TargetType.T_ARM);
      if (!deviceDatabase.isValid()) {
         return null;
      }
      return deviceDatabase.getDevice(currentDevice);
   }
   
   /*
    Names available in template:
    */
   public void getPageData(Map<String, String> paramMap) {
      for (ProjectVariable variable : variableList) {
         paramMap.put(UsbdmConstants.CONDITION_PREFIX_KEY+"."+variable.getId(),  variable.getValue());
      }
//      System.err.println("UsbdmProjectOptionsPage.getPageData()");
   }

   public void saveSettings() {
      IDialogSettings dialogSettings = getDialogSettings();
      if (dialogSettings != null) {
         for (ProjectVariable variable : variableList) {
            dialogSettings.put(OPTIONS_KEY+variable.getId(), variable.getValue());
         }
      }
   }
   
   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setText("Packages Available");
      shell.setLayout(new FillLayout());

      Composite composite = new Composite(shell, SWT.NONE);
      composite.setLayout(new FillLayout());

      UsbdmProjectOptionsPage page = new UsbdmProjectOptionsPage();
      page.createControl(composite);

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }

}
