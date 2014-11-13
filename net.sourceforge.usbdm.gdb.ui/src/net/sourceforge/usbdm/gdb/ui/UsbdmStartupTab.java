package net.sourceforge.usbdm.gdb.ui;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.gdb.UsbdmGdbServer;

import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.resource.ImageDescriptor;
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
   private  Button  doInitialDownloadCheckButton;
   private  Button  loadExternalImageButton;
   private  Text    externalImagePath;
   private  Button  browseImageWorkspaceButton;
   private  Button  browseImageExternalButton;
   private  Button  setInitialProgramCounterButton;
   private  Text    initialProgramCounterText;
   private  Button  setInitialBreakpointButton;
   private  Text    initialBreakpointText;
   private  Button  executeAfterLaunchButton;
                    
   private  Button  doConnectTarget;
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

   public UsbdmStartupTab() {
      super();
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
      
      enabled = doInitialDownloadCheckButton.getSelection();
      loadExternalImageButton.setEnabled(enabled);
      externalImagePath.setEnabled(enabled && loadExternalImageButton.getSelection());
      browseImageWorkspaceButton.setEnabled(enabled && loadExternalImageButton.getSelection());
      browseImageExternalButton.setEnabled(enabled && loadExternalImageButton.getSelection());

      setInitialProgramCounterButton.setEnabled(enabled);
      initialProgramCounterText.setEnabled(enabled&&setInitialProgramCounterButton.getSelection());
      setInitialBreakpointButton.setEnabled(enabled);
      initialBreakpointText.setEnabled(enabled&&setInitialBreakpointButton.getSelection());
      executeAfterLaunchButton.setEnabled(enabled);

      enabled = doConnectTarget.getSelection();
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
      doInitialDownloadCheckButton = new Button(parent, SWT.RADIO);
      doInitialDownloadCheckButton.setText("Program target before debugging");
      doInitialDownloadCheckButton.addSelectionListener(new SelectionAdapter() {
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
      loadExternalImageButton = new Button(startUpGroup, SWT.CHECK);
      loadExternalImageButton.setText("Load external file");
      loadExternalImageButton.setToolTipText("Load binary image and symbols from external file rather than project file");
      loadExternalImageButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            optionsChanged(true);
         }
      });

      externalImagePath = new Text(startUpGroup, SWT.BORDER|SWT.FILL|SWT.READ_ONLY);
      gd = new GridData();
      gd.horizontalAlignment = SWT.FILL;
      gd.widthHint = 100;
      gd.grabExcessHorizontalSpace = true;
      externalImagePath.setLayoutData(gd);
      externalImagePath.setToolTipText("Path to external image and symbols files");

      browseImageWorkspaceButton = new Button(startUpGroup, SWT.NONE);
      browseImageWorkspaceButton.setText("Workspace...");
      browseImageWorkspaceButton.setToolTipText("Browse workspace for file");
      browseImageWorkspaceButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            if (Utility.browseWorkspaceButtonSelected(getShell(), 
                  "Select file for binary image and symbols", externalImagePath) != null) {
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
                  "Select file for binary image and symbols", externalImagePath) != null) {
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
      executeAfterLaunchButton = new Button(startUpGroup, SWT.CHECK);
      gd = new GridData();
      gd.horizontalSpan = 2;
      executeAfterLaunchButton.setLayoutData(gd);
      executeAfterLaunchButton.setText("Start execution after load");
      executeAfterLaunchButton.setToolTipText(
            "Start execution after load\n"+
            "Disable to debug startup code");
      executeAfterLaunchButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            updateRequired();
         }
      });
   }

   private void createConnectGroup(Composite parent) {
      doConnectTarget = new Button(parent, SWT.RADIO);
      doConnectTarget.setText("Connect to running target");
      doConnectTarget.addSelectionListener(new SelectionAdapter() {
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
      ImageDescriptor imageDescriptor = UsbdmGdbServer.getDefault().getImageDescriptor(UsbdmGdbServer.ID_ARROW_IMAGE);
      return imageDescriptor.createImage();
   }

   @Override
   public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {

      // Load image
      configuration.setAttribute(IGDBJtagConstants.ATTR_LOAD_IMAGE,                        UsbdmSharedConstants.LAUNCH_DEFAULT_LOAD_IMAGE);
      configuration.setAttribute(IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_IMAGE,         UsbdmSharedConstants.LAUNCH_DEFAULT_USE_PROJ_BINARY_FOR_IMAGE);
      configuration.setAttribute(IGDBJtagConstants.ATTR_USE_FILE_FOR_IMAGE,                UsbdmSharedConstants.LAUNCH_DEFAULT_USE_FILE_FOR_IMAGE);
      configuration.setAttribute(IGDBJtagConstants.ATTR_IMAGE_FILE_NAME,                   UsbdmSharedConstants.LAUNCH_DEFAULT_IMAGE_FILE_NAME);
      configuration.setAttribute(IGDBJtagConstants.ATTR_IMAGE_OFFSET,                      UsbdmSharedConstants.LAUNCH_DEFAULT_IMAGE_OFFSET);
                                                                                           
      // Always load symbols from same file as image                                       
      configuration.setAttribute(IGDBJtagConstants.ATTR_LOAD_SYMBOLS,                      UsbdmSharedConstants.LAUNCH_DEFAULT_LOAD_SYMBOLS);
      configuration.setAttribute(IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_SYMBOLS,       UsbdmSharedConstants.LAUNCH_DEFAULT_USE_PROJ_BINARY_FOR_SYMBOLS);
      configuration.setAttribute(IGDBJtagConstants.ATTR_USE_FILE_FOR_SYMBOLS,              UsbdmSharedConstants.LAUNCH_DEFAULT_USE_FILE_FOR_SYMBOLS);
      configuration.setAttribute(IGDBJtagConstants.ATTR_SYMBOLS_FILE_NAME,                 UsbdmSharedConstants.LAUNCH_DEFAULT_SYMBOLS_FILE_NAME);
      configuration.setAttribute(IGDBJtagConstants.ATTR_SYMBOLS_OFFSET,                    UsbdmSharedConstants.LAUNCH_DEFAULT_SYMBOLS_OFFSET);
                                                                                           
      // Reset, halt and resume options                                                    
      configuration.setAttribute(IGDBJtagConstants.ATTR_DO_RESET,                          UsbdmSharedConstants.LAUNCH_DEFAULT_DO_RESET);
      configuration.setAttribute(IGDBJtagConstants.ATTR_DELAY,                             0);
      configuration.setAttribute(IGDBJtagConstants.ATTR_DO_HALT,                           UsbdmSharedConstants.LAUNCH_DEFAULT_DO_HALT);
      configuration.setAttribute(IGDBJtagConstants.ATTR_SET_RESUME,                        UsbdmSharedConstants.LAUNCH_DEFAULT_SET_RESUME);
                                                                                           
      // Don't set PC                                                                      
      configuration.setAttribute(IGDBJtagConstants.ATTR_SET_PC_REGISTER,                   UsbdmSharedConstants.LAUNCH_DEFAULT_SET_PC_REGISTER);
      configuration.setAttribute(IGDBJtagConstants.ATTR_PC_REGISTER,                       UsbdmSharedConstants.LAUNCH_DEFAULT_PC_REGISTER);
                                                                                           
      // Set temporary breakpoint                                                          
      configuration.setAttribute(IGDBJtagConstants.ATTR_SET_STOP_AT,                       UsbdmSharedConstants.LAUNCH_DEFAULT_SET_STOP_AT);
      configuration.setAttribute(IGDBJtagConstants.ATTR_STOP_AT,                           UsbdmSharedConstants.LAUNCH_DEFAULT_STOP_AT);

      configuration.setAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_USES_STARTUP,    UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_USES_STARTUP);
      configuration.setAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_SET_PC_REGISTER, UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_SET_PC_REGISTER);
      configuration.setAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_PC_REGISTER,     UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_PC_REGISTER);
      configuration.setAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_SET_STOP_AT,     UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_SET_STOP_AT);
      configuration.setAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_STOP_AT,         UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_STOP_AT);
      configuration.setAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_SET_RESUME,      UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_SET_RESUME);
      
      // Initialisation commands
      configuration.setAttribute(IGDBJtagConstants.ATTR_INIT_COMMANDS,                     UsbdmSharedConstants.LAUNCH_DEFAULT_INIT_COMMANDS);

      // Run Commands
      configuration.setAttribute(IGDBJtagConstants.ATTR_RUN_COMMANDS,                      UsbdmSharedConstants.LAUNCH_DEFAULT_RUN_COMMANDS); 
   }

   @Override
   public void initializeFrom(ILaunchConfiguration configuration) {
      
      try {
         boolean doReset    = configuration.getAttribute(IGDBJtagConstants.ATTR_DO_RESET,                                            UsbdmSharedConstants.LAUNCH_DEFAULT_DO_RESET);
         boolean doHalt     = configuration.getAttribute(IGDBJtagConstants.ATTR_DO_HALT,                                             UsbdmSharedConstants.LAUNCH_DEFAULT_DO_HALT);
         boolean doContinue = configuration.getAttribute(IGDBJtagConstants.ATTR_SET_RESUME,                                          UsbdmSharedConstants.LAUNCH_DEFAULT_SET_RESUME);

         // Program target
         doInitialDownloadCheckButton.setSelection(   configuration.getAttribute(IGDBJtagConstants.ATTR_LOAD_IMAGE,                  UsbdmSharedConstants.LAUNCH_DEFAULT_LOAD_IMAGE));
         doConnectTarget.setSelection(!doInitialDownloadCheckButton.getSelection());                                               
         executeAfterLaunchButton.setSelection(       doContinue);

         // File loading.  Ignores ATTR_USE_FILE_FOR_IMAGE, ATTR_USE_FILE_FOR_SYMBOLS, ATTR_IMAGE_OFFSET, ATTR_SYMBOLS_OFFSET
         loadExternalImageButton.setSelection(       !configuration.getAttribute(IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_IMAGE,   UsbdmSharedConstants.LAUNCH_DEFAULT_USE_PROJ_BINARY_FOR_IMAGE));
         externalImagePath.setText(                   configuration.getAttribute(IGDBJtagConstants.ATTR_IMAGE_FILE_NAME,             UsbdmSharedConstants.LAUNCH_DEFAULT_IMAGE_FILE_NAME));
         loadExternalSymbolButton.setSelection(      !configuration.getAttribute(IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_SYMBOLS, UsbdmSharedConstants.LAUNCH_DEFAULT_USE_PROJ_BINARY_FOR_SYMBOLS));
         externalSymbolPath.setText(                  configuration.getAttribute(IGDBJtagConstants.ATTR_SYMBOLS_FILE_NAME,           UsbdmSharedConstants.LAUNCH_DEFAULT_SYMBOLS_FILE_NAME));

         // Connect target options.                                                                                           
         resetAfterConnectButton.setSelection(            doReset &&   doHalt); // RESET (results in halt)
         resetAndContinueAfterConnectButton.setSelection( doReset &&  !doHalt); // RESET & RESUME
         haltAfterConnectButton.setSelection(            !doReset &&   doHalt); // HALT
         unchangedAfterConnectButton.setSelection(       !doReset &&  !doHalt); //
         
         // Program target options                                                                                                 
         setInitialProgramCounterButton.setSelection( configuration.getAttribute(IGDBJtagConstants.ATTR_SET_PC_REGISTER,             UsbdmSharedConstants.LAUNCH_DEFAULT_SET_PC_REGISTER));
         initialProgramCounterText.setText(           configuration.getAttribute(IGDBJtagConstants.ATTR_PC_REGISTER,                 UsbdmSharedConstants.LAUNCH_DEFAULT_PC_REGISTER));
         
         // This information is used twice
         setInitialBreakpointButton.setSelection(     configuration.getAttribute(IGDBJtagConstants.ATTR_SET_STOP_AT,                 UsbdmSharedConstants.LAUNCH_DEFAULT_SET_STOP_AT));
         setBreakpointOnConnectButton.setSelection(   setInitialBreakpointButton.getSelection());
         initialBreakpointText.setText(               configuration.getAttribute(IGDBJtagConstants.ATTR_STOP_AT,                     UsbdmSharedConstants.LAUNCH_DEFAULT_STOP_AT));
         breakpointOnConnectText.setText(             initialBreakpointText.getText());

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
         useStartupOptionsForRestartButton.setSelection( configuration.getAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_USES_STARTUP,    UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_USES_STARTUP));
         setRestartProgramCounterButton.setSelection(    configuration.getAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_SET_PC_REGISTER, UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_SET_PC_REGISTER));
         restartProgramCounterText.setText(              configuration.getAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_PC_REGISTER,     UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_PC_REGISTER));
         setBreakpointOnRestartButton.setSelection(      configuration.getAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_SET_STOP_AT,     UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_SET_STOP_AT));
         breakpointOnRestartText.setText(                configuration.getAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_STOP_AT,         UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_STOP_AT));
         executeAfterRestartButton.setSelection(         configuration.getAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_SET_RESUME,      UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_SET_RESUME));

         // Initialisation commands
         initCommands.setText(                           configuration.getAttribute(IGDBJtagConstants.ATTR_INIT_COMMANDS,                     UsbdmSharedConstants.LAUNCH_DEFAULT_INIT_COMMANDS));

         // Run Commands
         runCommands.setText(                            configuration.getAttribute(IGDBJtagConstants.ATTR_RUN_COMMANDS,                      UsbdmSharedConstants.LAUNCH_DEFAULT_RUN_COMMANDS));

      } catch (CoreException e) {
         UsbdmGdbServer.getDefault().getLog().log(e.getStatus());
      }
      optionsChanged(true);
   }

   @Override
   public void performApply(ILaunchConfigurationWorkingCopy configuration) {

      // No user commands
      configuration.setAttribute(IGDBJtagConstants.ATTR_INIT_COMMANDS,                      "");
                                                                                            
      // Load image                                                                         
      configuration.setAttribute(IGDBJtagConstants.ATTR_LOAD_IMAGE,                         doInitialDownloadCheckButton.getSelection());
      configuration.setAttribute(IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_IMAGE,          !loadExternalImageButton.getSelection());
      configuration.setAttribute(IGDBJtagConstants.ATTR_USE_FILE_FOR_IMAGE,                 loadExternalImageButton.getSelection());
      configuration.setAttribute(IGDBJtagConstants.ATTR_IMAGE_FILE_NAME,                    externalImagePath.getText());
      configuration.setAttribute(IGDBJtagConstants.ATTR_IMAGE_OFFSET,                       "");
                                                                                            
      // Reset, halt and resume options                                                     
      configuration.setAttribute(IGDBJtagConstants.ATTR_DO_RESET,                           resetAfterConnectButton.getSelection() ||
                                                                                            resetAndContinueAfterConnectButton.getSelection());
                                                                                            
      configuration.setAttribute(IGDBJtagConstants.ATTR_DO_HALT,                            resetAfterConnectButton.getSelection() ||
                                                                                            haltAfterConnectButton.getSelection());
      // Reset recovery delay is a USBDM option                                             
      configuration.setAttribute(IGDBJtagConstants.ATTR_DELAY,                              0);
                                                                                            
      configuration.setAttribute(IGDBJtagConstants.ATTR_SET_RESUME,                         executeAfterLaunchButton.getSelection());
                                                                                            
      // Optionally set PC                                                                  
      configuration.setAttribute(IGDBJtagConstants.ATTR_SET_PC_REGISTER,                    setInitialProgramCounterButton.getSelection());
      configuration.setAttribute(IGDBJtagConstants.ATTR_PC_REGISTER,                        initialProgramCounterText.getText());
                                                                                            
      // Note: The following options appear in two different locations in the TAB but only one is active (preserved)
      if (doInitialDownloadCheckButton.getSelection()) {                                    
         // Load symbols from same file as image                                            
         configuration.setAttribute(IGDBJtagConstants.ATTR_LOAD_SYMBOLS,                    true);
         configuration.setAttribute(IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_SYMBOLS,     !loadExternalImageButton.getSelection());
         configuration.setAttribute(IGDBJtagConstants.ATTR_USE_FILE_FOR_SYMBOLS,            loadExternalImageButton.getSelection());
         configuration.setAttribute(IGDBJtagConstants.ATTR_SYMBOLS_FILE_NAME,               externalImagePath.getText());
         configuration.setAttribute(IGDBJtagConstants.ATTR_SYMBOLS_OFFSET,                  "");
                                                                                            
         // Optionally set temporary breakpoint                                             
         configuration.setAttribute(IGDBJtagConstants.ATTR_SET_STOP_AT,                     setInitialBreakpointButton.getSelection());
         configuration.setAttribute(IGDBJtagConstants.ATTR_STOP_AT,                         initialBreakpointText.getText());
      }                                                                                     
      else {                                                                                
         // Load symbols from image or external symbol file                                 
         configuration.setAttribute(IGDBJtagConstants.ATTR_LOAD_SYMBOLS,                    true);
         configuration.setAttribute(IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_SYMBOLS,     !loadExternalSymbolButton.getSelection());
         configuration.setAttribute(IGDBJtagConstants.ATTR_USE_FILE_FOR_SYMBOLS,            loadExternalSymbolButton.getSelection());
         configuration.setAttribute(IGDBJtagConstants.ATTR_SYMBOLS_FILE_NAME,               externalSymbolPath.getText());
         configuration.setAttribute(IGDBJtagConstants.ATTR_SYMBOLS_OFFSET,                  "");
                                                                                            
         // Optionally set temporary breakpoint                                             
         configuration.setAttribute(IGDBJtagConstants.ATTR_SET_STOP_AT,                     setBreakpointOnConnectButton.getSelection());
         configuration.setAttribute(IGDBJtagConstants.ATTR_STOP_AT,                         breakpointOnConnectText.getText());
      }                                                                                     
      // No run Commands                                                                    
      configuration.setAttribute(IGDBJtagConstants.ATTR_RUN_COMMANDS,                       "");
                                                                                            
      configuration.setAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_USES_STARTUP,     useStartupOptionsForRestartButton.getSelection());
      configuration.setAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_SET_PC_REGISTER,  setRestartProgramCounterButton.getSelection());
      configuration.setAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_PC_REGISTER,      restartProgramCounterText.getText());
      configuration.setAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_SET_STOP_AT,      setBreakpointOnRestartButton.getSelection());
      configuration.setAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_STOP_AT,          breakpointOnRestartText.getText());
      configuration.setAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_SET_RESUME,       executeAfterRestartButton.getSelection());
                                                                                            
      // Initialisation commands                                                            
      configuration.setAttribute(IGDBJtagConstants.ATTR_INIT_COMMANDS,                      initCommands.getText());
                                                                                            
      // Run Commands                                                                       
      configuration.setAttribute(IGDBJtagConstants.ATTR_RUN_COMMANDS,                       runCommands.getText());
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
