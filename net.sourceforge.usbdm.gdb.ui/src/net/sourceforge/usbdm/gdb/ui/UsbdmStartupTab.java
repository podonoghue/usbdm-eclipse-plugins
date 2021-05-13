package net.sourceforge.usbdm.gdb.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;

/**
 * @since 4.12
 */
public class UsbdmStartupTab extends AbstractLaunchConfigurationTab {

   private static final String TAB_NAME = "Startup";
   private static final String TAB_ID   = "net.sourceforge.usbdm.gdb.ui.usbdmStartupTab";

   private static final String GDB_BREAK_PROMPT = 
         "Gdb break location - This may be:\n" +
               "  function_name                    \te.g. main\n"+
               "  file_name:line_number            \te.g. main.c:120\n"+
               "  file_name:function_name          \te.g. main.c:doit\n"+
               "  *address_expression              \te.g. *(0x1000+100)"
         ;
   
   private static final String GDB_EXPRESSION_PROMPT = 
         "Gdb Expression - This may use symbols or numbers recognised by GDB\n" +
         "e.g. 'main' or '0x1000+1023'";
   
   /*
    * *********************************************************
    * Dialogue controls
    * *********************************************************
    */
   private  Button  programTargetCheckButton;
   private  Button  doResetButton;
   private  Button  loadExternalFileButton;
   private  Text    externalFilePath;
   private  Button  browseImageWorkspaceButton;
   private  Button  browseImageExternalButton;
   private  Button  setInitialProgramCounterButton;
   private  Text    initialProgramCounterText;
   private  Button  setInitialBreakpointButton;
   private  Text    initialBreakpointText;
   private  Button  executeAfterLoadButton;
                    
   private  Button  doConnectTargetButton;
   private  Button  loadExternalSymbolButton;
   private  Text    externalSymbolPath;
   private  Button  browseSymbolProjectButton;
   private  Button  browseSymbolExternalButton;
   private  Button  resetAfterConnectButton;
   private  Button  resetAndContinueAfterConnectButton;
   private  Button  haltAfterConnectButton;
   private  Button  unchangedAfterConnectButton;
   private  Button  setBreakpointOnConnectButton;
   private  Text    breakpointOnConnectText;
   private  Label   synchronizeWarningLabel;

   private  Button  useStartupOptionsForRestartButton;
   private  Button  setRestartProgramCounterButton;
   private  Text    restartProgramCounterText;
   private  Button  setBreakpointOnRestartButton;
   private  Text    breakpointOnRestartText;
   private  Button  executeAfterRestartButton;
            
   private  Text    initCommands;
   private  Text    runCommands;

   private    Image fImage = null;
   
   public UsbdmStartupTab() {
      super();
   }

   @Override
   public void dispose() {
      if (fImage != null) {
         fImage.dispose();
      }
      super.dispose();
   }

   @Override
   public void createControl(Composite parent) {
      ScrolledComposite sc = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
      sc.setExpandHorizontal(true);
      sc.setExpandVertical(true);
      setControl(sc);

      Composite comp = new Composite(sc, SWT.FILL);
      sc.setContent(comp);
      GridLayout layout = new GridLayout();
      comp.setLayout(layout);

      Group group = new Group(comp, SWT.NONE);
      layout = new GridLayout();
      group.setLayout(layout);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      group.setLayoutData(gd);

      createStartupGroup(group);
      createConnectGroup(group);
      createRestartGroup(group);
      createInitGroup(group);
      createRunGroup(group);
      
      sc.setMinSize(group.computeSize(SWT.DEFAULT, SWT.DEFAULT));
   }

   void updateRequired() {
      try {
         scheduleUpdateJob();
      } catch (NoClassDefFoundError e) {
         // To allow testing
      }
   }

   void optionsChanged(boolean doUpdate) {
      boolean enabled;
      
      enabled = programTargetCheckButton.getSelection();
      doResetButton.setEnabled(enabled);
      loadExternalFileButton.setEnabled(enabled);
      externalFilePath.setEnabled(enabled && loadExternalFileButton.getSelection());
      browseImageWorkspaceButton.setEnabled(enabled && loadExternalFileButton.getSelection());
      browseImageExternalButton.setEnabled(enabled && loadExternalFileButton.getSelection());

      setInitialProgramCounterButton.setEnabled(enabled);
      initialProgramCounterText.setEnabled(enabled&&setInitialProgramCounterButton.getSelection());
      setInitialBreakpointButton.setEnabled(enabled);
      initialBreakpointText.setEnabled(enabled&&setInitialBreakpointButton.getSelection());
      executeAfterLoadButton.setEnabled(enabled);

      enabled = doConnectTargetButton.getSelection();
      loadExternalSymbolButton.setEnabled(enabled);
      externalSymbolPath.setEnabled(enabled && loadExternalSymbolButton.getSelection());
      browseSymbolExternalButton.setEnabled(enabled && loadExternalSymbolButton.getSelection());
      browseSymbolProjectButton.setEnabled(enabled && loadExternalSymbolButton.getSelection());

      resetAfterConnectButton.setEnabled(enabled);
      resetAndContinueAfterConnectButton.setEnabled(enabled);
      haltAfterConnectButton.setEnabled(enabled);
      unchangedAfterConnectButton.setEnabled(enabled);
      setBreakpointOnConnectButton.setEnabled(enabled);
      breakpointOnConnectText.setEnabled(enabled && setBreakpointOnConnectButton.getSelection());
      synchronizeWarningLabel.setEnabled(enabled && unchangedAfterConnectButton.getSelection());
      
      enabled = !useStartupOptionsForRestartButton.getSelection();
      setRestartProgramCounterButton.setEnabled(enabled);
      restartProgramCounterText.setEnabled(enabled && setRestartProgramCounterButton.getSelection());
      setBreakpointOnRestartButton.setEnabled(enabled);
      breakpointOnRestartText.setEnabled(enabled && setBreakpointOnRestartButton.getSelection());
      executeAfterRestartButton.setEnabled(enabled);

      if (doUpdate) {
         updateRequired();
      }
   }

   private void createStartupGroup(Composite parent) {
      programTargetCheckButton = new Button(parent, SWT.RADIO);
      programTargetCheckButton.setText("Program target before debugging");
      programTargetCheckButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            optionsChanged(true);
         }
      });
      StartupGroup(parent);
   }

   private void StartupGroup(Composite parent) {
      Group startUpGroup = new Group(parent, SWT.NONE);
      GridLayout layout = new GridLayout(4, false);
      startUpGroup.setLayout(layout);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      startUpGroup.setLayoutData(gd);
      startUpGroup.setText("Startup options");
      startUpGroup.setToolTipText("These options are applied when starting a target that will be programmed\n");
      /*
       * ===================================================================
       */
      doResetButton = new Button(startUpGroup, SWT.CHECK);
      gd = new GridData();
//      gd.horizontalAlignment = SWT.FILL;
      gd.horizontalSpan = 4;
      gd.grabExcessHorizontalSpace = true;
      doResetButton.setLayoutData(gd);
      doResetButton.setText("Reset target");
      doResetButton.setToolTipText("Reset target when doing initial connection");
      doResetButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            optionsChanged(true);
         }
      });
      /*
       * ===================================================================
       */
      loadExternalFileButton = new Button(startUpGroup, SWT.CHECK);
      loadExternalFileButton.setText("Load external file");
      loadExternalFileButton.setToolTipText("Load binary image and symbols from external file rather than project file");
      loadExternalFileButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            optionsChanged(true);
         }
      });

      externalFilePath = new Text(startUpGroup, SWT.BORDER|SWT.FILL|SWT.READ_ONLY);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.widthHint = 100;
      gd.grabExcessHorizontalSpace = true;
      externalFilePath.setLayoutData(gd);
      externalFilePath.setToolTipText("Path to external image and symbols files");

      browseImageWorkspaceButton = new Button(startUpGroup, SWT.NONE);
      browseImageWorkspaceButton.setText("Workspace...");
      browseImageWorkspaceButton.setToolTipText("Browse workspace for file");
      browseImageWorkspaceButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            if (Utility.browseWorkspaceButtonSelected(getShell(), 
                  "Select file for binary image and symbols", externalFilePath) != null) {
               updateRequired();
            }
         }
      });
      browseImageExternalButton = new Button(startUpGroup, SWT.NONE);
      browseImageExternalButton.setText("File System...");
      browseImageExternalButton.setToolTipText("Browse filesystem for file");
      browseImageExternalButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            if (Utility.browseButtonSelected(getShell(), 
                  "Select file for binary image and symbols", externalFilePath) != null) {
               updateRequired();
            }
         }
      });
      /*
       * ===================================================================
       */
      setInitialProgramCounterButton = new Button(startUpGroup, SWT.CHECK);
      setInitialProgramCounterButton.setText("Set initial program counter to ");
      setInitialProgramCounterButton.setToolTipText("Overide initial program counter set after load");
      setInitialProgramCounterButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            optionsChanged(true);
         }
      });
      initialProgramCounterText = new Text(startUpGroup, SWT.BORDER|SWT.FILL);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.widthHint = 150;
      gd.horizontalSpan = 3;
      initialProgramCounterText.setLayoutData(gd);
      initialProgramCounterText.setToolTipText(GDB_EXPRESSION_PROMPT);
      initialProgramCounterText.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            updateRequired();
         }
      });
      /*
       * ===================================================================
       */
      setInitialBreakpointButton = new Button(startUpGroup, SWT.CHECK);
      setInitialBreakpointButton.setText("Set temporary breakpoint at ");
      setInitialBreakpointButton.setToolTipText("A temporary breakpoint set after program load before initial execution");
      setInitialBreakpointButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            optionsChanged(true);
         }
      });
      initialBreakpointText = new Text(startUpGroup, SWT.BORDER);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.widthHint = 150;
      gd.horizontalSpan = 3;
      initialBreakpointText.setLayoutData(gd);
      initialBreakpointText.setToolTipText(GDB_BREAK_PROMPT);
      initialBreakpointText.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            updateRequired();
         }
      });
      /*
       * ===================================================================
       */
      executeAfterLoadButton = new Button(startUpGroup, SWT.CHECK);
      gd = new GridData();
      gd.horizontalSpan = 2;
      executeAfterLoadButton.setLayoutData(gd);
      executeAfterLoadButton.setText("Start execution after load");
      executeAfterLoadButton.setToolTipText(
            "Start execution after load\n"+
            "Disable to debug startup code");
      executeAfterLoadButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            updateRequired();
         }
      });
   }

   private void createConnectGroup(Composite parent) {
      doConnectTargetButton = new Button(parent, SWT.RADIO);
      doConnectTargetButton.setText("Connect to running target");
      doConnectTargetButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            optionsChanged(true);
         }
      });
      ConnectGroup(parent);
   }

   private void ConnectGroup(Composite parent) {
      Group connectGroup = new Group(parent, SWT.NONE);
      GridLayout layout = new GridLayout(4, false);
      connectGroup.setLayout(layout);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      connectGroup.setLayoutData(gd);
      connectGroup.setText("Connection options");
      connectGroup.setToolTipText("These options are applied when connecting to a running target\n");
      /*
       * ===================================================================
       */
      loadExternalSymbolButton = new Button(connectGroup, SWT.CHECK);
      loadExternalSymbolButton.setText("Load external symbol file");
      loadExternalSymbolButton.setToolTipText("Load symbols from external file rather than project file");
      loadExternalSymbolButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            optionsChanged(true);
         }
      });
      externalSymbolPath = new Text(connectGroup, SWT.BORDER|SWT.FILL|SWT.READ_ONLY);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.widthHint = 150;
      gd.grabExcessHorizontalSpace = true;
      externalSymbolPath.setLayoutData(gd);
      externalSymbolPath.setToolTipText("Path to external image");

      browseSymbolProjectButton = new Button(connectGroup, SWT.NONE);
      browseSymbolProjectButton.setText("Workspace...");
      browseSymbolProjectButton.setToolTipText("Browse workspace for file");
      browseSymbolProjectButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            if (Utility.browseWorkspaceButtonSelected(getShell(), 
                  "Select binary image file", externalSymbolPath) != null) {
               updateRequired();
            }
         }
      });
      browseSymbolExternalButton = new Button(connectGroup, SWT.NONE);
      browseSymbolExternalButton.setText("File System...");
      browseSymbolExternalButton.setToolTipText("Browse filesystem for file");
      browseSymbolExternalButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            if (Utility.browseButtonSelected(getShell(), 
                  "Select binary symbol file", externalSymbolPath) != null) {
               updateRequired();
            }
         }
      });
      /*
       * ===================================================================
       */
      resetAfterConnectButton = new Button(connectGroup, SWT.RADIO);
      gd = new GridData();
      gd.horizontalSpan = 1;
      resetAfterConnectButton.setLayoutData(gd);
      resetAfterConnectButton.setText("Reset Target");
      resetAfterConnectButton.setToolTipText("Reset target after connection - target will be halted");
      resetAfterConnectButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            optionsChanged(true);
         }
      });
      /*
       * ===================================================================
       */
      resetAndContinueAfterConnectButton = new Button(connectGroup, SWT.RADIO);
      gd = new GridData();
      gd.horizontalSpan = 3;
      resetAndContinueAfterConnectButton.setLayoutData(gd);
      resetAndContinueAfterConnectButton.setText("Reset Target and Continue ");
      resetAndContinueAfterConnectButton.setToolTipText("Reset target after connection - target will then continue execution");
      resetAndContinueAfterConnectButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            optionsChanged(true);
         }
      });
      /*
       * ===================================================================
       */
      haltAfterConnectButton = new Button(connectGroup, SWT.RADIO);
      gd = new GridData();
      gd.horizontalSpan = 1;
      haltAfterConnectButton.setLayoutData(gd);
      haltAfterConnectButton.setText("Halt Target");
      haltAfterConnectButton.setToolTipText("Halt target after connection");
      haltAfterConnectButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            optionsChanged(true);
         }
      });
      /*
       * ===================================================================
       */
      unchangedAfterConnectButton = new Button(connectGroup, SWT.RADIO);
      gd = new GridData();
      gd.horizontalSpan = 3;
      unchangedAfterConnectButton.setLayoutData(gd);
      unchangedAfterConnectButton.setText("Target execution is unaffected");
      unchangedAfterConnectButton.setToolTipText("No changes are made to target execution state");
      unchangedAfterConnectButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            updateRequired();
         }
      });
      /*
       * ===================================================================
       */
      setBreakpointOnConnectButton = new Button(connectGroup, SWT.CHECK);
      setBreakpointOnConnectButton.setText("Set temporary breakpoint at ");
      setBreakpointOnConnectButton.setToolTipText(
            "A temporary breakpoint set after connection\n"+
            "Note: this does not require the target to be halted");
      setBreakpointOnConnectButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            optionsChanged(true);
         }
      });
      breakpointOnConnectText = new Text(connectGroup, SWT.BORDER);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.widthHint = 150;
      gd.horizontalSpan = 3;
      breakpointOnConnectText.setLayoutData(gd);
      breakpointOnConnectText.setToolTipText(GDB_BREAK_PROMPT);
      breakpointOnConnectText.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            updateRequired();
         }
      });
      /*
       * ===================================================================
       */
      synchronizeWarningLabel = new Label(connectGroup, SWT.WRAP);
      gd = new GridData();
      gd.horizontalSpan = 2;
      synchronizeWarningLabel.setLayoutData(gd);
      synchronizeWarningLabel.setText("Note: GDB may not be synchronized with target until single-stepped or halted");
      synchronizeWarningLabel.setToolTipText("GDB needs to read the target registers which cannot be done while executing");
   }

   private void createRestartGroup(Composite parent) {
      Group restartGroup = new Group(parent, SWT.NONE);
      GridLayout layout = new GridLayout(4, false);
      restartGroup.setLayout(layout);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      restartGroup.setLayoutData(gd);
      restartGroup.setText("Restart options");
      restartGroup.setToolTipText("These options are applied when restarting the target\n");
      /*
       * ===================================================================
       */
      useStartupOptionsForRestartButton = new Button(restartGroup, SWT.CHECK);
      gd = new GridData();
      gd.horizontalSpan = 4;
      useStartupOptionsForRestartButton.setLayoutData(gd);
      useStartupOptionsForRestartButton.setText("Use startup options");
      useStartupOptionsForRestartButton.setToolTipText("Use the options from the Startup group");
      useStartupOptionsForRestartButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            optionsChanged(true);
         }
      });
      /*
       * ===================================================================
       */
      setRestartProgramCounterButton = new Button(restartGroup, SWT.CHECK);
      setRestartProgramCounterButton.setText("Set initial program counter to ");
      setRestartProgramCounterButton.setToolTipText("Overide initial program counter set after load");
      setRestartProgramCounterButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            optionsChanged(true);
         }
      });
      restartProgramCounterText = new Text(restartGroup, SWT.BORDER|SWT.FILL);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.widthHint = 150;
      gd.horizontalSpan = 3;
      restartProgramCounterText.setLayoutData(gd);
      restartProgramCounterText.setToolTipText(GDB_EXPRESSION_PROMPT);
      restartProgramCounterText.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            updateRequired();
         }
      });
      /*
       * ===================================================================
       */
      setBreakpointOnRestartButton = new Button(restartGroup, SWT.CHECK);
      setBreakpointOnRestartButton.setText("Set temporary breakpoint at ");
      setBreakpointOnRestartButton.setToolTipText("A temporary breakpoint active after reset");
      setBreakpointOnRestartButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            optionsChanged(true);
         }
      });
      breakpointOnRestartText = new Text(restartGroup, SWT.BORDER);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.widthHint = 150;
      gd.horizontalSpan = 3;
      gd.grabExcessHorizontalSpace = true;
      breakpointOnRestartText.setLayoutData(gd);
      breakpointOnRestartText.setToolTipText(GDB_BREAK_PROMPT);
      breakpointOnRestartText.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            updateRequired();
         }
      });
      /*
       * ===================================================================
       */
      executeAfterRestartButton = new Button(restartGroup, SWT.CHECK);
      gd = new GridData();
      gd.horizontalSpan = 2;
      executeAfterRestartButton.setLayoutData(gd);
      executeAfterRestartButton.setText("Start execution after restart");
      executeAfterRestartButton.setToolTipText(
            "Start execution after reset\n"+
            "Disable to debug startup code");
      executeAfterRestartButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            updateRequired();
         }
      });
   }
   
   public void createInitGroup(Composite parent) {
      Group group = new Group(parent, SWT.NONE);
      GridLayout layout = new GridLayout();
      group.setLayout(layout);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      group.setLayoutData(gd);
      group.setText("Initialisation Commands - done before Target Execution");
      initCommands = new Text(group, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
      gd = new GridData(GridData.FILL_BOTH);
      gd.heightHint = 45;
      initCommands.setLayoutData(gd);
      initCommands.addModifyListener(new ModifyListener() {
         public void modifyText(ModifyEvent evt) {
            updateRequired();
         }
      });
   }
   
   public void createRunGroup(Composite parent) {
      Group group = new Group(parent, SWT.NONE);
      GridLayout layout = new GridLayout();
      group.setLayout(layout);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      group.setLayoutData(gd);
      group.setText("Run Commands - done after Target Execution");
      runCommands = new Text(group, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
      gd = new GridData(GridData.FILL_BOTH);
      gd.heightHint = 45;
      runCommands.setLayoutData(gd);
      runCommands.addModifyListener(new ModifyListener() {
         public void modifyText(ModifyEvent evt) {
            scheduleUpdateJob();
         }
      });
   }

   /* (non-Javadoc)
    * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
    */
   @Override
   public Image getImage() {
      if (fImage == null) {
         fImage = Activator.getImageDescriptor(Activator.ID_ARROW_IMAGE).createImage();
      }
      return fImage;
   }

   @Override
   public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
      configuration.setAttribute(UsbdmSharedConstants.ATTR_PROGRAM_TARGET,                UsbdmSharedConstants.DEFAULT_PROGRAM_TARGET                ); 
      configuration.setAttribute(UsbdmSharedConstants.ATTR_DO_RESET,                      UsbdmSharedConstants.DEFAULT_DO_RESET                      );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_USE_EXTERNAL_FILE,             UsbdmSharedConstants.DEFAULT_USE_EXTERNAL_FILE             );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_EXTERNAL_FILE_NAME,            UsbdmSharedConstants.DEFAULT_EXTERNAL_FILE_NAME            );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_SET_PC_REGISTER,               UsbdmSharedConstants.DEFAULT_SET_PC_REGISTER               );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_PC_REGISTER_VALUE,             UsbdmSharedConstants.DEFAULT_PC_REGISTER_VALUE             );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_DO_STOP_AT_MAIN,               UsbdmSharedConstants.DEFAULT_DO_STOP_AT_MAIN               );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_STOP_AT_MAIN_ADDRESS,          UsbdmSharedConstants.DEFAULT_STOP_AT_MAIN_ADDRESS          );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_DO_RESUME,                     UsbdmSharedConstants.DEFAULT_DO_RESUME                     );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_USE_EXTERNAL_SYMBOL_FILE,      UsbdmSharedConstants.DEFAULT_USE_EXTERNAL_SYMBOL_FILE      );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_EXTERNAL_FILE_NAME,            UsbdmSharedConstants.DEFAULT_EXTERNAL_FILE_NAME            );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_DO_HALT,                       UsbdmSharedConstants.DEFAULT_DO_HALT                       );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_STOP_AT_MAIN_ADDRESS,          UsbdmSharedConstants.DEFAULT_STOP_AT_MAIN_ADDRESS          );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_RESTART_USES_STARTUP,          UsbdmSharedConstants.DEFAULT_RESTART_USES_STARTUP          );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_RESTART_SET_PC_REGISTER,       UsbdmSharedConstants.DEFAULT_RESTART_SET_PC_REGISTER       );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_RESTART_PC_REGISTER_VALUE,     UsbdmSharedConstants.DEFAULT_RESTART_PC_REGISTER_VALUE     );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_RESTART_DO_STOP_AT_MAIN,       UsbdmSharedConstants.DEFAULT_RESTART_DO_STOP_AT_MAIN       );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_RESTART_STOP_AT_MAIN_ADDRESS,  UsbdmSharedConstants.DEFAULT_RESTART_STOP_AT_MAIN_ADDRESS  );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_RESTART_DO_RESUME,             UsbdmSharedConstants.DEFAULT_RESTART_DO_RESUME             );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_INIT_COMMANDS,                 UsbdmSharedConstants.DEFAULT_INIT_COMMANDS                 );
      configuration.setAttribute(UsbdmSharedConstants.ATTR_RUN_COMMANDS,                  UsbdmSharedConstants.DEFAULT_RUN_COMMANDS                  );
   }

   @Override
   public void initializeFrom(ILaunchConfiguration configuration) {
      
      try {
         boolean doReset    = configuration.getAttribute(UsbdmSharedConstants.ATTR_DO_RESET,    UsbdmSharedConstants.DEFAULT_DO_RESET);
         boolean doHalt     = configuration.getAttribute(UsbdmSharedConstants.ATTR_DO_HALT,     UsbdmSharedConstants.DEFAULT_DO_HALT);
         boolean doContinue = configuration.getAttribute(UsbdmSharedConstants.ATTR_DO_RESUME,   UsbdmSharedConstants.DEFAULT_DO_RESUME);

         programTargetCheckButton.setSelection(   configuration.getAttribute(UsbdmSharedConstants.ATTR_PROGRAM_TARGET,                  UsbdmSharedConstants.DEFAULT_PROGRAM_TARGET));
         doConnectTargetButton.setSelection(!programTargetCheckButton.getSelection());                                               

         //===================
         doResetButton.setSelection(doReset);
         
         loadExternalFileButton.setSelection(        configuration.getAttribute(UsbdmSharedConstants.ATTR_USE_EXTERNAL_FILE,            UsbdmSharedConstants.DEFAULT_USE_EXTERNAL_FILE));
         externalFilePath.setText(                   configuration.getAttribute(UsbdmSharedConstants.ATTR_EXTERNAL_FILE_NAME,           UsbdmSharedConstants.DEFAULT_EXTERNAL_FILE_NAME));
         
         setInitialProgramCounterButton.setSelection( configuration.getAttribute(UsbdmSharedConstants.ATTR_SET_PC_REGISTER,             UsbdmSharedConstants.DEFAULT_SET_PC_REGISTER));
         initialProgramCounterText.setText(           configuration.getAttribute(UsbdmSharedConstants.ATTR_PC_REGISTER_VALUE,           UsbdmSharedConstants.DEFAULT_PC_REGISTER_VALUE));
         
         setInitialBreakpointButton.setSelection(     configuration.getAttribute(UsbdmSharedConstants.ATTR_DO_STOP_AT_MAIN,             UsbdmSharedConstants.DEFAULT_DO_STOP_AT_MAIN));
         initialBreakpointText.setText(               configuration.getAttribute(UsbdmSharedConstants.ATTR_STOP_AT_MAIN_ADDRESS,        UsbdmSharedConstants.DEFAULT_STOP_AT_MAIN_ADDRESS));
         
         executeAfterLoadButton.setSelection(       doContinue);

         //===================
         loadExternalSymbolButton.setSelection(       configuration.getAttribute(UsbdmSharedConstants.ATTR_USE_EXTERNAL_SYMBOL_FILE,    UsbdmSharedConstants.DEFAULT_USE_EXTERNAL_SYMBOL_FILE));
         externalSymbolPath.setText(                  configuration.getAttribute(UsbdmSharedConstants.ATTR_EXTERNAL_SYMBOL_FILE_NAME,   UsbdmSharedConstants.DEFAULT_EXTERNAL_SYMBOL_FILE_NAME));

         // Radio buttons                                                                                           
         resetAfterConnectButton.setSelection(            doReset &&   doHalt); // RESET (results in halt)
         resetAndContinueAfterConnectButton.setSelection( doReset &&  !doHalt); // RESET & RESUME
         haltAfterConnectButton.setSelection(            !doReset &&   doHalt); // HALT
         unchangedAfterConnectButton.setSelection(       !doReset &&  !doHalt); //
         
         // Fix radio buttons as options may overlap in configuration
         if (resetAfterConnectButton.getSelection()) {
            resetAndContinueAfterConnectButton.setSelection(false);
            haltAfterConnectButton.setSelection(false);
            unchangedAfterConnectButton.setSelection(false);
         }
         else if (resetAndContinueAfterConnectButton.getSelection()) {
            haltAfterConnectButton.setSelection(false);
            unchangedAfterConnectButton.setSelection(false);
         }
         else if (haltAfterConnectButton.getSelection()) {
            unchangedAfterConnectButton.setSelection(false);               
         }
         else {
            // Not reset or halt => resume
            unchangedAfterConnectButton.setSelection(true);               
         }

         setBreakpointOnConnectButton.setSelection(   setInitialBreakpointButton.getSelection());
         breakpointOnConnectText.setText(             initialBreakpointText.getText());
         
         //===========================
         // Restart
         useStartupOptionsForRestartButton.setSelection( configuration.getAttribute(UsbdmSharedConstants.ATTR_RESTART_USES_STARTUP,          UsbdmSharedConstants.DEFAULT_RESTART_USES_STARTUP));
         setRestartProgramCounterButton.setSelection(    configuration.getAttribute(UsbdmSharedConstants.ATTR_RESTART_SET_PC_REGISTER,       UsbdmSharedConstants.DEFAULT_RESTART_SET_PC_REGISTER));
         restartProgramCounterText.setText(              configuration.getAttribute(UsbdmSharedConstants.ATTR_RESTART_PC_REGISTER_VALUE,     UsbdmSharedConstants.DEFAULT_RESTART_PC_REGISTER_VALUE));
         setBreakpointOnRestartButton.setSelection(      configuration.getAttribute(UsbdmSharedConstants.ATTR_RESTART_DO_STOP_AT_MAIN,       UsbdmSharedConstants.DEFAULT_RESTART_DO_STOP_AT_MAIN));
         breakpointOnRestartText.setText(                configuration.getAttribute(UsbdmSharedConstants.ATTR_RESTART_STOP_AT_MAIN_ADDRESS,  UsbdmSharedConstants.DEFAULT_RESTART_STOP_AT_MAIN_ADDRESS));
         executeAfterRestartButton.setSelection(         configuration.getAttribute(UsbdmSharedConstants.ATTR_RESTART_DO_RESUME,             UsbdmSharedConstants.DEFAULT_RESTART_DO_RESUME));

         // Initialisation commands
         initCommands.setText(                           configuration.getAttribute(UsbdmSharedConstants.ATTR_INIT_COMMANDS,                 UsbdmSharedConstants.DEFAULT_INIT_COMMANDS));

         // Run Commands
         runCommands.setText(                            configuration.getAttribute(UsbdmSharedConstants.ATTR_RUN_COMMANDS,                  UsbdmSharedConstants.DEFAULT_RUN_COMMANDS));

      } catch (CoreException e) {
         Activator.getDefault().getLog().log(e.getStatus());
      }
      optionsChanged(true);
   }

   @Override
   public void performApply(ILaunchConfigurationWorkingCopy configuration) {

      // Load image                                                                         
      configuration.setAttribute(UsbdmSharedConstants.ATTR_PROGRAM_TARGET,                programTargetCheckButton.getSelection());

      // Note: The following options appear in two different locations in the TAB but only one is active (preserved)
      if (programTargetCheckButton.getSelection()) {                                    
         // Reset options                                                     
         configuration.setAttribute(UsbdmSharedConstants.ATTR_DO_RESET,                   doResetButton.getSelection());

         // Load external file
         configuration.setAttribute(UsbdmSharedConstants.ATTR_USE_EXTERNAL_FILE,          loadExternalFileButton.getSelection());
         configuration.setAttribute(UsbdmSharedConstants.ATTR_EXTERNAL_FILE_NAME,         externalFilePath.getText());
                                                                                               
         // Optionally set initial PC                                                                  
         configuration.setAttribute(UsbdmSharedConstants.ATTR_SET_PC_REGISTER,            setInitialProgramCounterButton.getSelection());
         configuration.setAttribute(UsbdmSharedConstants.ATTR_PC_REGISTER_VALUE,          initialProgramCounterText.getText());
                                                                                               
         // Optionally set temporary breakpoint at main                                            
         configuration.setAttribute(UsbdmSharedConstants.ATTR_DO_STOP_AT_MAIN,            setInitialBreakpointButton.getSelection());
         configuration.setAttribute(UsbdmSharedConstants.ATTR_STOP_AT_MAIN_ADDRESS,       initialBreakpointText.getText());

         // Optionally execute after programming
         configuration.setAttribute(UsbdmSharedConstants.ATTR_DO_RESUME,                  executeAfterLoadButton.getSelection());
      }                                                                                     
      else {                                                                                
         // Load symbols from image or external symbol file                                 
         configuration.setAttribute(UsbdmSharedConstants.ATTR_USE_EXTERNAL_SYMBOL_FILE,   loadExternalSymbolButton.getSelection());
         configuration.setAttribute(UsbdmSharedConstants.ATTR_EXTERNAL_FILE_NAME,         externalSymbolPath.getText());
                                                                                            
         // Radio buttons encode Reset, halt and resume options                                                     
         configuration.setAttribute(UsbdmSharedConstants.ATTR_DO_RESET,                   resetAfterConnectButton.getSelection() ||
                                                                                          resetAndContinueAfterConnectButton.getSelection());
         configuration.setAttribute(UsbdmSharedConstants.ATTR_DO_HALT,                    resetAfterConnectButton.getSelection() ||
                                                                                          haltAfterConnectButton.getSelection());
         // Optionally set temporary breakpoint                                             
         configuration.setAttribute(UsbdmSharedConstants.ATTR_DO_STOP_AT_MAIN,            setBreakpointOnConnectButton.getSelection());
         configuration.setAttribute(UsbdmSharedConstants.ATTR_STOP_AT_MAIN_ADDRESS,       breakpointOnConnectText.getText());
      }                                                                                   
      configuration.setAttribute(UsbdmSharedConstants.ATTR_RESTART_USES_STARTUP,          useStartupOptionsForRestartButton.getSelection());

      configuration.setAttribute(UsbdmSharedConstants.ATTR_RESTART_SET_PC_REGISTER,       setRestartProgramCounterButton.getSelection());
      configuration.setAttribute(UsbdmSharedConstants.ATTR_RESTART_PC_REGISTER_VALUE,     restartProgramCounterText.getText());
      configuration.setAttribute(UsbdmSharedConstants.ATTR_RESTART_DO_STOP_AT_MAIN,       setBreakpointOnRestartButton.getSelection());
      configuration.setAttribute(UsbdmSharedConstants.ATTR_RESTART_STOP_AT_MAIN_ADDRESS,  breakpointOnRestartText.getText());
      configuration.setAttribute(UsbdmSharedConstants.ATTR_RESTART_DO_RESUME,             executeAfterRestartButton.getSelection());
                                                                                            
      // Initialisation commands                                                            
      configuration.setAttribute(UsbdmSharedConstants.ATTR_INIT_COMMANDS,                 initCommands.getText());
                                                                                            
      // Run Commands                                                                       
      configuration.setAttribute(UsbdmSharedConstants.ATTR_RUN_COMMANDS,                  runCommands.getText());
   }

   @Override
   public String getName() {
      return TAB_NAME;
   }

   @Override
   public String getId() {
      return TAB_ID;
   }

   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setText("Launch Config");
      shell.setLayout(new FillLayout());

      Composite composite = new Composite(shell, SWT.NONE);
      composite.setLayout(new FillLayout());

      UsbdmStartupTab view = new UsbdmStartupTab();

      view.createControl(composite);

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }
}
