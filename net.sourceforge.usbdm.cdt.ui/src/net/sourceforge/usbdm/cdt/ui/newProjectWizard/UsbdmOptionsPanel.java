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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.packageParser.ApplyWhenCondition;
import net.sourceforge.usbdm.packageParser.ProjectAction;
import net.sourceforge.usbdm.packageParser.ProjectActionList;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Value;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Visitor;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Visitor.Result;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Visitor.Result.Status;
import net.sourceforge.usbdm.packageParser.ProjectConstant;
import net.sourceforge.usbdm.packageParser.ProjectVariable;
import net.sourceforge.usbdm.packageParser.WizardGroup;
import net.sourceforge.usbdm.packageParser.WizardPageInformation;

import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

/**
 *  USBDM New Project Wizard page "USBDM Project"
 *
 */
public class UsbdmOptionsPanel  extends Composite {

   private static final String OPTIONS_KEY = "usbdm.project.additionalOptions."; 

   protected ProjectActionList               fProjectActionList  = null;
   /**
    * Map of buttons on the dialogue
    * Maps variable name -> button
    */
   protected HashMap<String,Button>          fButtonMap          = null;
   protected HashMap<String,ProjectVariable> fVariableMap        = null;
   protected IDialogSettings                 fDialogSettings     = null;
   protected Device                          fDevice             = null;
   protected Composite                       fControl            = null;
   protected boolean                         fHasChanged         = true;
   /**
    * Only constructs controls that below to this page
    */
   protected WizardPageInformation           fWizardPageInfo     = null;

   /**
    * Map of options active when dialogue displayed
    */
   protected Map<String, String>             fOptionMap          = null;
   /**
    * Groups in the dialogue
    */
   protected List<Group>                     fGroupList          = null;


   public UsbdmOptionsPanel(
         Composite               parent, 
         int                     style, 
         IDialogSettings         dialogSettings,
         Device                  device, 
         ProjectActionList       projectActionList,
         Map<String, String>     optionMap,
         WizardPageInformation   wizardPageInfo) {

      super(parent, style);

      fDialogSettings     = dialogSettings;
      fDevice             = device;
      fProjectActionList  = projectActionList;
      fOptionMap          = optionMap;
      fWizardPageInfo     = wizardPageInfo;
   }

   public UsbdmOptionsPanel(
         Composite                  parent, 
         int                        style, 
         ProjectActionList          projectActionList, 
         IDialogSettings            dialogSettings) {

      super(parent, style);

      fProjectActionList  = projectActionList;
      fDialogSettings     = dialogSettings;
   }

   /**
    * Get Project action lists
    * 
    * @return
    */
   public ProjectActionList getProjectActionList() {
      return fProjectActionList;
   }

   public void getButtonData(final Map<String, String> paramMap) {
      if (fButtonMap == null) {
         // Button not yet created
         return;
      }
      // Check all buttons
      for (Entry<String, Button> entry : fButtonMap.entrySet()) {
         Button                button          = entry.getValue();
         ProjectVariable       projectVariable = fVariableMap.get(entry.getKey());
//         System.err.println(String.format("getButtonData() %s => %s", projectVariable.getId(), button.isEnabled() && button.getSelection()));
         paramMap.put(projectVariable.getId(), (button.isEnabled() && button.getSelection())?Boolean.TRUE.toString():Boolean.FALSE.toString());
      }
   }
   
   /**
    *    Validates control & returns error message
    *    
    *    @return 
    *  
    *    @return Error message (null if none)
    *    
    *    @throws Exception 
    */
   public String validate() {
      if (fButtonMap == null) {
         // Button not yet created
         return null;
      }
      // Propagate dependencies between buttons
      // Bit of a hack - do the page multiple times to propagate dependency changes
      Set<Entry<String, Button>> buttonSet = fButtonMap.entrySet();
      for (int i=0; i<5; i++) {
         // Check and update all buttons and record if any changed
         boolean noChanges = true;
         for (Entry<String, Button> entry : buttonSet) {
            Button                button          = entry.getValue();
            ProjectVariable       projectVariable = fVariableMap.get(entry.getKey());
            boolean               enabled         = true;
            try {
               ApplyWhenCondition applyWhenCondition = projectVariable.getRequirement();
//               if (projectVariable.getId().equals("projectOptionValue.KSDK-usb-audio-generator")) {
//                  applyWhenCondition.setVerbose(true);
//               }
               enabled = applyWhenCondition.enabled(fDevice, fOptionMap, fButtonMap);
            } catch (Exception e) {
               e.printStackTrace();
               return e.getMessage();
            }
            if (button.isEnabled() != enabled) {
               button.setEnabled(enabled);
               noChanges = false;
            }
            if (!button.isEnabled()) {
               button.setSelection(false);
            }
         }
         if (noChanges) {
            break;
         }
      }
      // Check for at least one radio button selected in each group
      for (Group group: fGroupList) {
         boolean radioButtonSelected = false;
         boolean radioButtonPresent = false;
         for (Control child : group.getChildren()) {
            if (child instanceof Button) {
               Button button = (Button) child;
               if (button.isEnabled() && (button.getStyle() & SWT.RADIO) != 0) {
                  radioButtonPresent = true;
                  if (radioButtonSelected) {
                     // Clear multiple button selection
                     button.setSelection(false);
                  }
                  radioButtonSelected = radioButtonSelected || button.getSelection();
               }
            }
         }
         if (radioButtonPresent && !radioButtonSelected) {
            // Prompt to select a radio button
            return "Select " + group.getText();
         }
      }
      return null;
   }
   
   /**
    *    Populate the Options controls
    * 
    *    @param parent
    * 
    *    @return Composite created
    *    @throws Exception 
    */
   private Composite createOptionsControl(Composite parent) {

      final Composite comp = new Composite(parent, SWT.NONE);
      GridLayout layout    = new GridLayout(2, true);
      //      GridBagLayout layout = new GridBagLayout();
      comp.setLayout(layout);

      if (fProjectActionList == null) {
         // Should be impossible
         new Label(comp, SWT.NONE).setText("No device options");
         return comp;
      }
      /*
       * Collect variables for dialogue
       */
      fVariableMap = new HashMap<String, ProjectVariable>();
      final ArrayList<ProjectVariable> variableList = new ArrayList<ProjectVariable>();
      final HashMap<String, WizardGroup> wizardGroups = new HashMap<String, WizardGroup>();
      Visitor visitor = new ProjectActionList.Visitor() {

         @Override
         public Result applyTo(ProjectAction action, ProjectActionList.Value result, IProgressMonitor monitor) {
            if (action instanceof ProjectActionList) {
               ProjectActionList pal = (ProjectActionList)action;
               if (!pal.appliesTo(fDevice, fOptionMap)) {
                  return PRUNE;
               }
            }
            else if (action instanceof ProjectVariable) {
               ProjectVariable projectVariable = (ProjectVariable) action;
               fVariableMap.put(projectVariable.getId(), projectVariable);
               variableList.add(projectVariable);
//               System.err.println(String.format("Adding %s", projectVariable));
            }
            else if (action instanceof WizardPageInformation) {
               WizardPageInformation wizardPageInfo = (WizardPageInformation) action;
//               System.err.println("WizardPage found : " + wizardPageInfo);
               if (fWizardPageInfo.getId().equals(wizardPageInfo.getId())) {
                  ArrayList<WizardGroup> groups = wizardPageInfo.getGroups();
//                  System.err.println("Processing WizardPage : " + wizardPageInfo);
                  for (WizardGroup group:groups) {
                     wizardGroups.put(group.getId(), group);
//                     System.err.println("   WizardGroup found : " + group);
                  }
               }
            }
            return CONTINUE;
         }
      };
      fProjectActionList.visit(visitor, null);

      if (fVariableMap.size() == 0) {
         Group defaultGroup = new Group(comp, SWT.NONE);
         defaultGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
         defaultGroup.setLayout(new GridLayout(2, false));
         Label label = new Label(defaultGroup, SWT.NONE);
         label.setText("No Options"); //$NON-NLS-1$
      }
      else {
         SelectionListener listener = new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               fHasChanged = true;
               notifyListeners(SWT.CHANGED, null);
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
               notifyListeners(SWT.CHANGED, null);
            }
         };
         
         // Group created dynamically from XML
         HashMap<String, Group> groups = new HashMap<String, Group>();
         fGroupList = new ArrayList<Group>();

         // Create buttons
         fButtonMap = new HashMap<String,Button>();
         for (ProjectVariable variable:variableList) {
            if ((variable.getName() == null) || (variable.getName().length() == 0)) {
               throw new RuntimeException("Variable without name " + variable.getId());
            }
            String groupId     = variable.getButtonGroupId();
            int    buttonStyle = variable.getButtonStyle();
            Group  group = groups.get(groupId);
            if (group == null) {
               WizardGroup wizardGroup = wizardGroups.get(groupId);
               if (wizardGroup == null) {
                  continue;
               }
               group = new Group(comp, SWT.NONE);
               fGroupList.add(group);
               String groupName = MacroSubstitute.substitute(wizardGroup.getName(), fOptionMap);
               group.setText(groupName);
               GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
               gd.horizontalSpan = wizardGroup.getSpan();
               group.setLayoutData(gd);
               group.setLayout(new GridLayout(wizardGroup.getWidth(), false));
               groups.put(groupId, group);
            }
            Button button = new Button(group, buttonStyle);
            fButtonMap.put(variable.getId(), button);
            button.setText("  " + variable.getName());
            boolean value = getSetting(variable.getId(), Boolean.valueOf(variable.getDefaultValue()));
            button.setSelection(value);
            button.setToolTipText(variable.getDescription().replaceAll("\\\\n", "\n"));
            button.setData(variable);
            button.addSelectionListener(listener);
         }
      }
      return comp;
   }

   /**
    *    Creates main control
    *  
    *    @param parent
    *    @param device
    *    @throws Exception 
    */
   protected void createControl() {

      setLayout(new FillLayout());

      ScrolledComposite sc = new ScrolledComposite(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
      sc.setLayout(new FillLayout());
      Composite fOptionsControl = createOptionsControl(sc);
      fOptionsControl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
      sc.setContent(fOptionsControl);
      sc.setExpandHorizontal(true);
      sc.setExpandVertical(true);
      sc.setMinSize(200, 200);

      fControl = sc;
   }

   /**
    *    Gets a setting
    * 
    *    @param  key      Key to look for
    *    @param  value    Value to return if key not present 
    *    
    *    @return setting value
    */
   private boolean getSetting(String key, boolean value) {

      String sValue = null;
      if (fDialogSettings != null) {
         sValue = fDialogSettings.get(OPTIONS_KEY+key);
      }
      if (sValue != null) {
         return Boolean.valueOf(sValue);
      }
      return value;
   }

   public void getPageData(final Map<String, String> paramMap) {
      getPageData(paramMap, fProjectActionList);
   }
   /**
    *   Add settings to paramMap
    *   
    *   @param paramMap 
    *   @throws Exception 
    */
   public void getPageData(final Map<String, String> paramMap, ProjectActionList projectActionLists) {
      Visitor visitor = new Visitor() {
         @Override
         public Result applyTo(ProjectAction action, Value result, IProgressMonitor monitor) {
            try {
               if (action instanceof ProjectActionList) {
                  ProjectActionList projectActionList = (ProjectActionList) action;
                  return projectActionList.appliesTo(fDevice, paramMap)?CONTINUE:PRUNE;
               }
               else if (action instanceof ProjectVariable) {
                  ProjectVariable projectVariable = (ProjectVariable) action;
                  Button button = fButtonMap.get(projectVariable.getId());
                  if (button == null) {
                     return new Result(new Exception("Can't find button for var : " + projectVariable + " from " + action.getId()));
                  }
                  Boolean value = button.getSelection();
//                  System.err.println("UsbdmOptionsPanel.getPageData() projectVariable = " + projectVariable.toString() + ", value = " + value);
                  paramMap.put(projectVariable.getId(), value.toString());
               }
               else if (action instanceof ProjectConstant) {
                  ProjectConstant projectConstant = (ProjectConstant) action;
//                System.err.println(String.format("UsbdmOptionsPanel.getPageData(): Adding constant %s => %s",  projectConstant.getId(), projectConstant.getValue()));
                String value = paramMap.get(projectConstant.getId());
                if (value != null) {
                   if (projectConstant.isWeak()) {
                      // Ignore - assume constant is a default that has been overwritten
                      return CONTINUE;
                   }
                   if (!projectConstant.doReplace() && !value.equals(projectConstant.getValue())) {
                      return new Result(new Exception("paramMap already contains constant " + projectConstant.getId()));
                   }
                }
                paramMap.put(projectConstant.getId(), projectConstant.getValue());
             }
               return CONTINUE;
            } catch (Exception e) {
               return new Result(e);
            }
         }
      };
      // Visit all enabled actions and collect variables and constants
      Result result = fProjectActionList.visit(visitor, null);
      if (result.getStatus() == Status.EXCEPTION) {
         result.getException().printStackTrace();
      }
   }

   public boolean hasChanged() {
      boolean changed = fHasChanged;
      fHasChanged = false;
      return changed;
   }

   /**
    *    Save dialog settings
    */
   public void saveSettings() {
      if (fDialogSettings == null) {
         return;
      }
      Iterator<Entry<String, Button>> it = fButtonMap.entrySet().iterator();
      while (it.hasNext()) {
         Entry<String, Button> entry               = it.next();
         String                projectVariableName = entry.getKey();
         Button                button              = entry.getValue();
         Boolean               value               = button.isEnabled() && button.getSelection();
         fDialogSettings.put(OPTIONS_KEY+projectVariableName, value.toString());
      }
   }
}
