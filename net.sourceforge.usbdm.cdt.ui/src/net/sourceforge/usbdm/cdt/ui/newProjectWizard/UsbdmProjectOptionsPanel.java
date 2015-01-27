package net.sourceforge.usbdm.cdt.ui.newProjectWizard;
/*
 Change History
+============================================================================================
| Revision History
+============================================================================================
| 28 Dec 14 | Added requirements                                                  4.10.6.250
| 16 Nov 13 | Fixed path lookup for resource files (e.g. header files) on linux   4.10.6.100
| 16 Nov 13 | Added default files header & vector files based upon subfamily      4.10.6.100
+============================================================================================
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.deviceDatabase.Block;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.deviceDatabase.ProjectAction;
import net.sourceforge.usbdm.deviceDatabase.ProjectActionList;
import net.sourceforge.usbdm.deviceDatabase.ProjectVariable;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;

import org.eclipse.jface.dialogs.IDialogSettings;
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
public class UsbdmProjectOptionsPanel  extends Composite{

   private static final String OPTIONS_KEY = "usbdm.project.additionalOptions."; 

   private HashMap<ProjectVariable,Button>   fButtonList               = null;
   private ArrayList<ProjectVariable>        fVariableList             = null;
   private IDialogSettings                   fDialogSettings           = null;
   private Map<String,String>                fOptionMap                = null;
   private Control                           fOptionsControl           = null;
   private Device                            fDevice                   = null;

   public UsbdmProjectOptionsPanel(
         Composite          parent, 
         int                style, 
         Device             device, 
         IDialogSettings    dialogSettings, 
         Map<String,String> optionMap) throws Exception {
      super(parent, style);
      fButtonList       = new HashMap<ProjectVariable,Button>();
      fDevice           = device;
      fDialogSettings   = dialogSettings;
      fOptionMap        = optionMap;
      
      createControl(parent);
   }

   /**
    *    Validates control & returns error message
    *    
    *    @return 
    *  
    *    @return Error message (null if none)
    */
   public String validate() {
      String message = null;
      
      Iterator<Entry<ProjectVariable, Button>> it = fButtonList.entrySet().iterator();
      while (it.hasNext()) {
        Entry<ProjectVariable, Button> entry = it.next();
        if (entry.getValue().getSelection()) {
           ProjectVariable variable = (ProjectVariable)entry.getValue().getData();
           for (ProjectVariable requirement:variable.getRequirements()) {
              Button requiredButton = fButtonList.get(requirement);
              if (requiredButton == null) {
                 message = "Internal error: '" + variable.getName() + "' requires non-existent option '" + requirement + "'";
                 break;
              }
              if (!requiredButton.getSelection()) {
                 ProjectVariable reqVariable = (ProjectVariable)requiredButton.getData();
                 message = "'" + variable.getName() + "' requires '" + reqVariable.getName() + "' option";
                 break;
              }
           }
           for (ProjectVariable preclusion:variable.getPreclusion()) {
              Button requiredButton = fButtonList.get(preclusion);
              if (requiredButton == null) {
                 break;
              }
              if (requiredButton.getSelection()) {
                 ProjectVariable reqVariable = (ProjectVariable)requiredButton.getData();
                 message = "'" + variable.getName() + "' is incompatible with  '" + reqVariable.getName() + "' option";
                 break;
              }
           }
        }
      }
//      System.err.println("UsbdmProjectOptionsPanel.validate() - " + message);
      return message;
   }

   /**
    *    Populate the Options controls
    * 
    *    @param device
    *    @param parent
    * 
    *    @return control created
    * @throws Exception 
    */
   private Control createOptionsControl(Composite parent) throws Exception {

      ScrolledComposite sc = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
      sc.setExpandHorizontal(true);
      sc.setExpandVertical(true);
      
      Composite comp = new Composite(sc, SWT.FILL);
      sc.setContent(comp);
      GridLayout layout = new GridLayout();
      comp.setLayout(layout);

      Group group = new Group(comp, SWT.NONE);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      group.setLayoutData(gd);
      group.setLayout(new GridLayout(2, false));

      if (fDevice == null) {
         // Should be impossible
         group.setText("No device selected!!");
         return group;
      }
      group.setText("Additional Project Options ("+fDevice.getName()+")");
      
      // Create list of variables used by all conditions
      fVariableList = new ArrayList<ProjectVariable>();
      
      HashSet<ProjectVariable>     conditionMap = new HashSet<ProjectVariable>();
      ArrayList<ProjectActionList> projectActionLists;
      try {
         projectActionLists = fDevice.getProjectActionLists(fOptionMap);
      } catch (Exception e1) {
         // Should be impossible
         group.setText("Error locating Project Action Lists");
         return group;
      }
      /*
       * Create list of variables for dialogue
       */
      for (ProjectActionList actionList:projectActionLists) {
         for (ProjectAction action : actionList) {
            Block condition = action.getCondition();
            if (condition != null) {
               ArrayList<ProjectVariable> varList = condition.getVariables();
               if (varList != null) {
                  for (ProjectVariable projectVariable:varList) {
                     if (!conditionMap.contains(projectVariable) && projectVariable.getId().startsWith(UsbdmConstants.CONDITION_PREFIX_KEY+".")) {
                        fVariableList.add(projectVariable);
                        conditionMap.add(projectVariable);
                     }
                  }
               }
            }
         }
      }
      // Create buttons
      if (fVariableList.size() == 0) {
         Label label = new Label(group, SWT.NONE);
         label.setText("No Options"); //$NON-NLS-1$
      }
      else {
         SelectionListener listener = new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               Button button = (Button) e.getSource();
               ProjectVariable variable = (ProjectVariable)button.getData();
               variable.setValue(button.getSelection()?"true":"false");
               notifyListeners(SWT.CHANGED, null);
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
               Button button = (Button) e.getSource();
               ProjectVariable variable = (ProjectVariable)button.getData();
               variable.setValue(variable.getDefaultValue());
            }
         };
         for (ProjectVariable variable : fVariableList) {
            if ((variable.getName() == null) || (variable.getName().length() == 0)) {
               throw new Exception("Variable without name " + variable.getId());
            }
            Label label = new Label(group, SWT.NONE);
            label.setText(variable.getName()+"  ");         //$NON-NLS-1$
            Button button = new Button(group, SWT.CHECK);
            fButtonList.put(variable, button);
            button.setText("  enable");
            boolean value = getSetting(OPTIONS_KEY+variable.getId(), Boolean.valueOf(variable.getDefaultValue()));
            variable.setValue(value?"true":"false");  //$NON-NLS-1$  //$NON-NLS-2$
            button.setSelection(value);
            button.setToolTipText(variable.getDescription());
            button.setData(variable);
            button.addSelectionListener(listener);
         }
      }
      return group;
   }
   
   /**
    *    Creates main control
    *  
    *    @param parent
    *    @param device
    * @throws Exception 
    */
   public void createControl(Composite parent) throws Exception {
      setLayout(new GridLayout());
      fOptionsControl = createOptionsControl(this);
      fOptionsControl.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
   }
   
   /**
    *    Gets a setting
    * 
    *    @param key  Key to look for
    *    @param string 
    *    @return Value (null if none)
    */
   private boolean getSetting(String key, boolean value) {
      String sValue = null;
      if (fDialogSettings != null) {
         sValue = fDialogSettings.get(key);
      }
      if (sValue != null) {
         return Boolean.valueOf(sValue);
      }
      return value;
   }
   
   /**
    *   Add settings to paramMap
    *   
    *   @param paramMap 
    */
   public void getPageData(Map<String, String> paramMap) {
      for (ProjectVariable variable : fVariableList) {
         paramMap.put(variable.getId(),  variable.getValue());
      }
   }

   void initData(IDialogSettings dialogSettings) {
      
   }
   
   /**
    *    Load settings from dialogSettings
    *   
    *    @param dialogSettings 
    */
   public void loadSettings(IDialogSettings dialogSettings) {
      for (ProjectVariable variable : fVariableList) {
         dialogSettings.get(OPTIONS_KEY+variable.getId());
      }
   }
   
   /**
    *    Add settings to dialogSettings
    *   
    *    @param dialogSettings 
    */
   public void saveSettings(IDialogSettings dialogSettings) {
      for (ProjectVariable variable : fVariableList) {
         dialogSettings.put(OPTIONS_KEY+variable.getId(), variable.getValue());
      }
   }
   
   /**
    *    For debug
    * 
    *    @param currentDevice
    *    @return
    */
   private static Device getDevice(String currentDevice) {
      DeviceDatabase deviceDatabase = new DeviceDatabase(TargetType.T_ARM);
      if (!deviceDatabase.isValid()) {
         return null;
      }
      return deviceDatabase.getDevice(currentDevice);
   }

   /**
    *    Test main
    * 
    *    @param args
    */
   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setText("Packages Available");
      shell.setLayout(new FillLayout());
      shell.setSize(400, 300);
      Map<String,String> optionMap = new HashMap<String, String>();
      optionMap.put("linkerRamSize", "0x2000");
      UsbdmProjectOptionsPanel page = null;
      try {
         page = new UsbdmProjectOptionsPanel(shell, SWT.NONE, getDevice("FRDM-K20D50M"), null, optionMap);
      } catch (Exception e) {
         e.printStackTrace();
      }
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      Map<String, String> map = new HashMap<String, String>();
      page.getPageData(map);
      display.dispose();
   }

}
