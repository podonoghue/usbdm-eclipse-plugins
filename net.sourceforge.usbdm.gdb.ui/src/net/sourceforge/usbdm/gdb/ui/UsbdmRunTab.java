package net.sourceforge.usbdm.gdb.ui;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.gdb.UsbdmGdbServer;

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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class UsbdmRunTab extends AbstractLaunchConfigurationTab {

   private static final String TAB_NAME = "Run";
   private static final String TAB_ID   = "net.sourceforge.usbdm.gdb.ui.usbdmRunTab";

   private static final String GDB_EXPRESSION_PROMPT = 
         "Gdb Expression - This may use symbols or numbers recognised by GDB\n" +
         "e.g. 'main' or '0x1000+1023'";
   
   /*
    * *********************************************************
    * Dialogue controls
    * *********************************************************
    */
   private  Button  loadExternalImageButton;
   private  Text    externalImagePath;
   private  Button  browseImageWorkspaceButton;
   private  Button  browseImageExternalButton;
   private  Button  setInitialProgramCounterButton;
   private  Text    initialProgramCounterText;

   private  Text    initCommands;
   private  Text    runCommands;

   public UsbdmRunTab() {
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
      browseImageWorkspaceButton.setEnabled(loadExternalImageButton.getSelection());
      browseImageExternalButton.setEnabled(loadExternalImageButton.getSelection());

      initialProgramCounterText.setEnabled(setInitialProgramCounterButton.getSelection());
      if (doUpdate) {
         updateRequired();
      }
   }

   private void createStartupGroup(Composite parent) {
      StartupGroup(parent);
   }

   private void StartupGroup(Composite parent) {
      Group startUpGroup = new Group(parent, SWT.NONE);
      GridLayout layout = new GridLayout(4, false);
      startUpGroup.setLayout(layout);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      startUpGroup.setLayoutData(gd);
      startUpGroup.setText("Startup options");
      startUpGroup.setToolTipText("These options are applied when running a target\n");
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
   }
   
   @Override
   public void initializeFrom(ILaunchConfiguration configuration) {

      try {
         // Program target

         loadExternalImageButton.setSelection(       configuration.getAttribute(UsbdmSharedConstants.ATTR_USE_EXTERNAL_FILE,   UsbdmSharedConstants.DEFAULT_USE_EXTERNAL_FILE));
         externalImagePath.setText(                  configuration.getAttribute(UsbdmSharedConstants.ATTR_EXTERNAL_FILE_NAME,  UsbdmSharedConstants.DEFAULT_EXTERNAL_FILE_NAME));

         // Program target options                                                                                                 
         setInitialProgramCounterButton.setSelection( configuration.getAttribute(UsbdmSharedConstants.ATTR_SET_PC_REGISTER,    UsbdmSharedConstants.DEFAULT_SET_PC_REGISTER));
         initialProgramCounterText.setText(           configuration.getAttribute(UsbdmSharedConstants.ATTR_PC_REGISTER_VALUE,  UsbdmSharedConstants.DEFAULT_PC_REGISTER_VALUE));

         // Initialisation commands
         initCommands.setText(                        configuration.getAttribute(UsbdmSharedConstants.ATTR_INIT_COMMANDS,      UsbdmSharedConstants.DEFAULT_INIT_COMMANDS));

         // Run Commands
         runCommands.setText(                         configuration.getAttribute(UsbdmSharedConstants.ATTR_RUN_COMMANDS,       UsbdmSharedConstants.DEFAULT_RUN_COMMANDS));

      } catch (CoreException e) {
         UsbdmGdbServer.getDefault().getLog().log(e.getStatus());
      }
      optionsChanged(true);
   }

   @Override
   public void performApply(ILaunchConfigurationWorkingCopy configuration) {

      // Load image                                                                         
      configuration.setAttribute(UsbdmSharedConstants.ATTR_USE_EXTERNAL_FILE,          loadExternalImageButton.getSelection());
      configuration.setAttribute(UsbdmSharedConstants.ATTR_EXTERNAL_FILE_NAME,         externalImagePath.getText());
                                                                                            
      // Optionally set PC                                                                  
      configuration.setAttribute(UsbdmSharedConstants.ATTR_SET_PC_REGISTER,            setInitialProgramCounterButton.getSelection());
      configuration.setAttribute(UsbdmSharedConstants.ATTR_PC_REGISTER_VALUE,          initialProgramCounterText.getText());
                                                                                            
      // Initialisation commands                                                            
      configuration.setAttribute(UsbdmSharedConstants.ATTR_INIT_COMMANDS,              initCommands.getText());
                                                                                            
      // Run Commands                                                                       
      configuration.setAttribute(UsbdmSharedConstants.ATTR_RUN_COMMANDS,               runCommands.getText());
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

      UsbdmRunTab view = new UsbdmRunTab();

      view.createControl(composite);

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }
}
