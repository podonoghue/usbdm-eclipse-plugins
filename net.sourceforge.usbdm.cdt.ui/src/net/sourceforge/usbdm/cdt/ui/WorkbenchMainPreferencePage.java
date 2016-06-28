package net.sourceforge.usbdm.cdt.ui;

import java.io.File;
import java.util.Hashtable;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.constants.ToolInformationData;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.constants.UsbdmSharedSettings;
import net.sourceforge.usbdm.constants.VariableInformationData;
import net.sourceforge.usbdm.constants.VariableInformationData.VariableType;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class WorkbenchMainPreferencePage extends PreferencePage implements
IWorkbenchPreferencePage {

   private String gccCommand = "";

   public WorkbenchMainPreferencePage() {
   }

   public WorkbenchMainPreferencePage(String title) {
      super(title);
   }

   public WorkbenchMainPreferencePage(String title, ImageDescriptor image) {
      super(title, image);
   }

   @Override
   public void init(IWorkbench workbench) {
      //System.err.println("WorkbenchPreferenceTopPage.init()\n");
      String os = System.getProperty("os.name");
      if ((os != null) && os.toUpperCase().contains("LINUX")) {
         gccCommand = UsbdmConstants.GCC_COMMAND_LINUX;
      }
      else {
         gccCommand = UsbdmConstants.GCC_COMMAND_WINDOWS;
      }
      noDefaultAndApplyButton();
   }

   @Override
   protected Control createContents(Composite parent) {

      Composite composite = new Composite(parent, SWT.NONE);      
      composite.setLayout(new GridLayout(1, false));

      createPathComposite(composite);
      createKDSComposite(composite);
      createVariableComposite(composite);
      // Load settings here so controls are valid
      loadSettings();

//      boolean t = 
      validate();
//      System.err.println("WorkbenchPreferenceTopPage.createContents() => validate = " + t);

      return composite;         
   }

   //===============================================

   static class ExtendedVariableInformationData extends VariableInformationData {
      protected Text   valueText = null;
      protected Label  descriptionLabel = null;

      private static Hashtable<String, ExtendedVariableInformationData> variableInformationTable = null;

      public ExtendedVariableInformationData(VariableInformationData other) {
         super(other);
      }
      public Text getValueText()                              { return valueText; }
      public void setValueText(Text idText)                   { this.valueText = idText; }

      public Label getDescriptionLabel()                      { return descriptionLabel; }
      public void setDescriptionLabel(Label descriptionLabel) { this.descriptionLabel = descriptionLabel; }

      public static Hashtable<String, ExtendedVariableInformationData> getExtendedVariableInformationTable() {
         if (variableInformationTable == null) {
            variableInformationTable = new Hashtable<String, ExtendedVariableInformationData>();
            for (VariableInformationData value : VariableInformationData.getVariableInformationTable().values()) {
               variableInformationTable.put(value.getVariableName(), new ExtendedVariableInformationData(value));
            }
         }
         return variableInformationTable;
      }
   }

   private void createVariableComposite(Composite parent) {

      Group group = new Group(parent, SWT.NONE);
      group.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false));
      group.setText("Variables");
      //
      group.setLayout(new GridLayout(3, false));

//      final Collection<ExtendedVariableInformationData> variables = ExtendedVariableInformationData.getExtendedVariableInformationTable().values();
      
      for (final ExtendedVariableInformationData toolInformation : ExtendedVariableInformationData.getExtendedVariableInformationTable().values()) {
         if (toolInformation.getVariableName() ==  UsbdmSharedConstants.USBDM_KSDK_PATH) {
            // Special case - done separately
            continue;
         }
         toolInformation.setDescriptionLabel(new Label(group, SWT.NONE));
         toolInformation.getDescriptionLabel().setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
         toolInformation.getDescriptionLabel().setToolTipText(toolInformation.getVariableName());
         toolInformation.getDescriptionLabel().setText(toolInformation.getDescription() + ": ");

         toolInformation.setValueText(new Text(group, SWT.BORDER));
         if (toolInformation.getHint() != null) {
            toolInformation.getValueText().setToolTipText(toolInformation.getHint());
         }
         GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
         if (toolInformation.getType() == VariableType.SIMPLE) {
            new Label(group, SWT.NONE);
            gd.widthHint = 450;
            gd.horizontalAlignment = SWT.LEFT;
         }
         else if (toolInformation.getType() == VariableType.FILE_PATH) {
            Button pathBrowseButton = new Button(group, SWT.NONE);
            pathBrowseButton.setText("Browse...");
            pathBrowseButton.setToolTipText("Browse to " + toolInformation.getDescription());
            pathBrowseButton.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(SelectionEvent e) {
                  DirectoryDialog fd = new DirectoryDialog(getShell(), SWT.OPEN);
                  fd.setText(toolInformation.getDescription());
                  fd.setFilterPath(toolInformation.getValueText().getText());
                  String directoryPath = fd.open();
                  if (directoryPath != null) {
                     Path path = new Path(directoryPath);
                     toolInformation.getValueText().setText(path.toPortableString());
                     validate();
                  }
               }
            });
         }
         toolInformation.getValueText().setLayoutData(gd);
      }
   }

   private String validateVariables() {

      // variables are always considered valid
      return null;
   }

   private void loadVariables(UsbdmSharedSettings settings) {
      if (settings == null) {
         return;
      }
      for (final ExtendedVariableInformationData variableInformation : ExtendedVariableInformationData.getExtendedVariableInformationTable().values()) {
         String variable = settings.get(variableInformation.getVariableName(), variableInformation.getDefaultValue());
         variableInformation.getValueText().setText(variable);
//         System.err.println("WorkbenchMainPreferencePage.loadVariables() Found variable = " + variable);
      }
   }

   private void saveVariables(UsbdmSharedSettings settings) {
      for (final ExtendedVariableInformationData toolInformation : ExtendedVariableInformationData.getExtendedVariableInformationTable().values()) {
         settings.put(toolInformation.getVariableName(), toolInformation.getValueText().getText());
      }
   }

   static class ExtendedToolInformationData extends ToolInformationData {

      private Label pathText    = null;
      private Label prefixText  = null;
      private Label pathLabel   = null;
      private Label prefixLabel = null;

      public Label getPathText()                    { return pathText; }
      public void setPathText(Label pathText)       { this.pathText = pathText; }
      public Label getPrefixText()                  { return prefixText; }
      public void setPrefixText(Label prefixText)   { this.prefixText = prefixText; }
      public Label getPathLabel()                   { return pathLabel; }
      public void setPathLabel(Label pathLabel)     { this.pathLabel = pathLabel; }
      public Label getPrefixLabel()                 { return prefixLabel; }
      public void setPrefixLabel(Label prefixLabel) { this.prefixLabel = prefixLabel; }

      ExtendedToolInformationData(ToolInformationData other) {
         super(other);
      }

      private static Hashtable<String, ExtendedToolInformationData> toolInformationTable = null;

      public static Hashtable<String, ExtendedToolInformationData> getExtendedToolInformationTable() {
         if (toolInformationTable == null) {
            toolInformationTable = new Hashtable<String, ExtendedToolInformationData>();
            for (ToolInformationData value : ExtendedToolInformationData.getToolInformationTable().values()) {
               toolInformationTable.put(value.getBuildToolId(), new ExtendedToolInformationData(value));
            }
         }
         return toolInformationTable;
      }
   }

   private void createPathComposite (Composite parent) {

      for (final ExtendedToolInformationData toolInformation : ExtendedToolInformationData.getExtendedToolInformationTable().values()) {
         Group group = new Group(parent, SWT.NONE);
         group.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false));
         group.setText(toolInformation.getDescription());
         //
         group.setLayout(new GridLayout(3, false));

         //
         // Cross compiler prefix
         //
         toolInformation.setPrefixLabel(new Label(group, SWT.NONE));
         toolInformation.getPrefixLabel().setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
         toolInformation.getPrefixLabel().setToolTipText("${" + toolInformation.getPrefixVariableName() + "}");

         toolInformation.setPrefixText(new Label(group, SWT.BORDER));
         GridData gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
         gd.widthHint = 150;
         toolInformation.getPrefixText().setLayoutData(gd);
         toolInformation.getPrefixText().setToolTipText("Prefix for Cross Compiler commands e.g. arm-none-eabi-");

         Button clearButton = new Button(group, SWT.NONE);
         clearButton.setText("Clear");
         clearButton.setToolTipText("Clear && disable this entry");
         clearButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
         clearButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               toolInformation.getPathText().setText(UsbdmConstants.PATH_NOT_SET);
               toolInformation.getPrefixText().setText(UsbdmConstants.PREFIX_NOT_SET);
               validate();
            }
         });

         //
         // Cross compiler path
         //
         //==================
         toolInformation.setPathLabel(new Label(group, SWT.NONE));
         toolInformation.getPathLabel().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
         toolInformation.getPathLabel().setText("Path: ");
         toolInformation.getPathLabel().setToolTipText("${" + toolInformation.getPrefixVariableName() + "}");

         toolInformation.setPathText(new Label(group, SWT.SINGLE | SWT.BORDER));
         GridData gd1 = new GridData(SWT.FILL, SWT.CENTER, false, false);
         gd1.widthHint = 300;
         toolInformation.getPathText().setLayoutData(gd1);
         toolInformation.getPathText().setToolTipText("Path to Cross Compiler bin directory");

         Button pathBrowseButton = new Button(group, SWT.NONE);
         pathBrowseButton.setText("Browse...");
         pathBrowseButton.setToolTipText("Browse to gcc executable");
         pathBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
               fd.setText(toolInformation.getDescription()+" - Select GCC command");
               fd.setFilterPath(toolInformation.getPathText().getText());
               String directoryPath = fd.open();
               if (directoryPath != null) {
                  IPath gccPath = new Path(directoryPath);
                  String prefix = gccPath.lastSegment();
                  if (prefix.endsWith(gccCommand))  {
                     int index = prefix.lastIndexOf(gccCommand);
                     prefix = prefix.substring(0,  index);
                  }
                  int trailingSegmentsToRemove = 1;
                  String path = gccPath.removeLastSegments(trailingSegmentsToRemove).toPortableString();
                  toolInformation.getPathText().setText(path);
                  toolInformation.getPrefixText().setText(prefix);
                  validate();
               }
            }
         });

      }
      updatePathCompositeLabels(false);
   }

   private void updatePathCompositeLabels(boolean showVariableNames) {
      for (final ExtendedToolInformationData toolInformation : ExtendedToolInformationData.getExtendedToolInformationTable().values()) {
         if (showVariableNames) {
            toolInformation.getPrefixLabel().setText("Prefix ${" + toolInformation.getPrefixVariableName() + "}:");
            toolInformation.getPathLabel().setText("Path ${" + toolInformation.getPathVariableName() + "}:");
         }
         else {
            toolInformation.getPrefixLabel().setText("Prefix:");
            toolInformation.getPathLabel().setText("Path:");
         }
      }
   }

   private String validatePaths() {
      String selectedBuildTool = "";
      //      if (usbdmPage != null) {
      //         selectedBuildTool = usbdmPage.getSelectedBuildToolId();
      //      }
      String message = null;
      for (final ExtendedToolInformationData toolInformation : ExtendedToolInformationData.getExtendedToolInformationTable().values()) {
         boolean toolMustBeValid = toolInformation.getBuildToolId().equals(selectedBuildTool);
         String pathText   = toolInformation.getPathText().getText().trim();
         if (pathText.equals(UsbdmConstants.PATH_NOT_SET)) {
            if (toolMustBeValid) {
               // Used entry MUST be set
               message = toolInformation.getDescription() + " - Values must be set";
               break;
            }
            else {
               // Dummied value & not one actually being used
               continue;
            }
         }
         String prefixText = toolInformation.getPrefixText().getText().trim();
         IPath gccPath = new Path(pathText).append(prefixText+gccCommand);
         File gccFile = gccPath.toFile();
         if (!gccFile.isFile() || !gccFile.canExecute()) {
            message = toolInformation.getDescription() + " - Prefix or path invalid";
            break;
         }
      }
      return message;
   }

   private void loadPaths(UsbdmSharedSettings settings) {

      if (settings == null) {
         return;
      }
      for (final ExtendedToolInformationData toolInformation : ExtendedToolInformationData.getExtendedToolInformationTable().values()) {
         String toolPath = settings.get(toolInformation.getPathVariableName(), UsbdmConstants.PATH_NOT_SET);
//         System.err.println("WorkbenchMainPreferencePage.loadPaths() Found path variable   = " + toolInformation.getPathVariableName());
//         System.err.println("WorkbenchMainPreferencePage.loadPaths() Found prefix variable = " + toolInformation.getPrefixVariableName());
         toolInformation.getPathText().setText(toolPath);
         String toolPrefix = settings.get(toolInformation.getPrefixVariableName(), "");
         toolInformation.getPrefixText().setText(toolPrefix);
      }
   }

   private void savePaths(UsbdmSharedSettings settings) {

      for (final ExtendedToolInformationData toolInformation : ExtendedToolInformationData.getExtendedToolInformationTable().values()) {
         settings.put(toolInformation.getPrefixVariableName(), toolInformation.getPrefixText().getText());
         settings.put(toolInformation.getPathVariableName(),   toolInformation.getPathText().getText());
      }
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
         return true;
      }
      message = validatePaths();
      if (message == null) {
         message = validateVariables();
      }
      setErrorMessage(message);
//      System.err.println("WorkbenchPreferenceTopPage.validate() => " + (message == null));
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
      UsbdmSharedSettings settings = UsbdmSharedSettings.getSharedSettings();
      loadPaths(settings);
      loadVariables(settings);
      validate();
   }

   protected boolean saveSettings() {
//    System.err.println("saveSetting() saving settings");
      if (!validate()) {
         return false;
      }
      if (!isControlCreated()) {
         return true;
      }

      UsbdmSharedSettings settings = UsbdmSharedSettings.getSharedSettings();

      savePaths(settings);
      saveVariables(settings);

      settings.flush();
      return true;
   }

   @Override
   public boolean performOk() {
      return super.performOk() && saveSettings();
   }

   private void createKDSComposite(final Composite parent) {

      Group mainGroup = new Group(parent, SWT.NONE);
      mainGroup.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false));
      mainGroup.setText("Kinetis Software Development Kit");
      mainGroup.setLayout(new GridLayout(1, false));

      Composite pathGroup = new Composite(mainGroup, SWT.NONE);
      pathGroup.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false));
      pathGroup.setLayout(new GridLayout(3, false));

      final ExtendedVariableInformationData toolInformation = ExtendedVariableInformationData.getExtendedVariableInformationTable().get(UsbdmSharedConstants.USBDM_KSDK_PATH);
      toolInformation.setDescriptionLabel(new Label(pathGroup, SWT.NONE));
      toolInformation.getDescriptionLabel().setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
      toolInformation.getDescriptionLabel().setToolTipText(toolInformation.getVariableName());
      toolInformation.getDescriptionLabel().setText("Path: ");

      toolInformation.setValueText(new Text(pathGroup, SWT.BORDER|SWT.READ_ONLY|SWT.NO_FOCUS));
      if (toolInformation.getHint() != null) {
         toolInformation.getValueText().setToolTipText(toolInformation.getHint());
      }
      GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
      Button pathBrowseButton = new Button(pathGroup, SWT.NONE);
      pathBrowseButton.setText("Browse...");
      pathBrowseButton.setToolTipText("Browse to " + toolInformation.getDescription());
      pathBrowseButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            DirectoryDialog fd = new DirectoryDialog(getShell(), SWT.OPEN);
            fd.setText(toolInformation.getDescription());
            fd.setFilterPath(toolInformation.getValueText().getText());
            String directoryPath = fd.open();
            if (directoryPath != null) {
               Path path = new Path(directoryPath);
               toolInformation.getValueText().setText(path.toPortableString());
               validate();
            }
         }
      });
      toolInformation.getValueText().setLayoutData(gd);

//      Composite importGroup = new Composite(mainGroup, SWT.NONE);
//      importGroup.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false));
//      importGroup.setLayout(new GridLayout(2, false));
//
//      Label  lblKDS       = new Label(importGroup, SWT.NONE);
//      lblKDS.setText("KSDK Libraries");
//      Button btnImportKDS = new Button(importGroup, SWT.PUSH);
//      btnImportKDS.setText("Import Library...");
//      btnImportKDS.setToolTipText("Import KSDK libraries into the workspace.");
//      btnImportKDS.addSelectionListener(new SelectionAdapter() {
//         @Override
//         public void widgetSelected(SelectionEvent e) {
//            openWizard(getShell());
//         }
//      });
   }
   
//   public  void openWizard(Shell shell) {
//      IWizard wizard    = new KSDKLibraryImportWizard();
//       WizardDialog wd  = new  WizardDialog(shell, wizard);
//       wd.setTitle(wizard.getWindowTitle());
//       wd.open();
//     }

   /**
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();
      Shell shell = new Shell(display);
      shell.setSize(600,450);
      WorkbenchMainPreferencePage topPage = new WorkbenchMainPreferencePage("Hello there");
      shell.setLayout(new FillLayout());
      Control control = topPage.createContents(shell);
      topPage.setControl(control);
      topPage.init(null);
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      topPage.performOk();

      display.dispose();
   }

}
