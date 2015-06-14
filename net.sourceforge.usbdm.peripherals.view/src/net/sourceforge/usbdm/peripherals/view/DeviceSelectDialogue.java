/*
===============================================================================================================
| History                                                                                                      
---------------------------------------------------------------------------------------------------------------
| 19 Jan 2015 | New format device selection                                                       | V4.10.6.250
===============================================================================================================
*/
package net.sourceforge.usbdm.peripherals.view;

import java.io.File;
import java.util.HashMap;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.ui.DeviceSelector;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripherals.model.UsbdmDevicePeripheralsModel;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DeviceSelectDialogue extends TitleAreaDialog  {
   
   DevicePeripherals     devicePeripherals       = null;
   
   private InterfaceType interfaceType           = null;
   private String        deviceOrFilename        = null;
   
   private Text          txtDeviceName           = null;
   private Button        btnDeviceSelect         = null;
   private Button        chkUseExternalSVDFile   = null;
   private Button        btnFileBrowse           = null;
   private Group         grpInternal;
   private Group         grpExternal;
   private Text          txtFilePath;

   public DeviceSelectDialogue(Shell parentShell, String deviceOrFilename, InterfaceType interfaceType) {
      super(parentShell);
      this.deviceOrFilename = deviceOrFilename;
      if (interfaceType == null) {
         this.interfaceType    = InterfaceType.T_ARM;
      }
      else {
         this.interfaceType    = interfaceType;
      }
   }
      
   private boolean fileExists(String filename) {
      if (filename == null) {
         return false;
      }
      IPath svdPath = new Path(filename);
      File svdFile = svdPath.toFile();
      return (svdFile.isFile());
   }
   
   private void validate() {
      boolean useExternalSVDFile = chkUseExternalSVDFile.getSelection();
      for (Control child : grpExternal.getChildren()) {
         child.setEnabled(useExternalSVDFile);
      }
      for (Control child : grpInternal.getChildren()) {
         child.setEnabled(!useExternalSVDFile);
      }
      
      String message = null;
      if (useExternalSVDFile) {
         // Check if valid external file and save
         deviceOrFilename = txtFilePath.getText();
         if (!fileExists(deviceOrFilename)) {
            message = "SVD file path is invalid";
            deviceOrFilename = null;
         }
         else {
            devicePeripherals = DevicePeripherals.createDatabase(deviceOrFilename);
            if (devicePeripherals == null) {
               message = "SVD file contents are invalid";
               deviceOrFilename = null;
            }
         }
      }
      else {
         deviceOrFilename = txtDeviceName.getText();
         devicePeripherals = DevicePeripherals.createDatabase(deviceOrFilename);
         if (devicePeripherals == null) {
            message = "SVD file not found or invalid";
            deviceOrFilename = null;
         }
      }
      getButton(IDialogConstants.OK_ID).setEnabled(message == null);
      setMessage(message, IMessageProvider.ERROR);
   }
   
   LocalResourceManager resManager = null;
   HashMap<String, Image> imageCache = new HashMap<String,Image>();

   public Image getMyImage(String imageId) {
      Image image = imageCache.get(imageId);
      if ((Activator.getDefault() != null) && (image == null)) {
         ImageDescriptor imageDescriptor  = Activator.getDefault().getImageDescriptor(imageId);
         image = resManager.createImage(imageDescriptor);
         imageCache.put(imageId, image);
      }
      return image;
   }
   
   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.Dialog#isResizable()
    */
   @Override
   protected boolean isResizable() {
      return true;
   }

   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.Dialog#create()
    */
   @Override
   public void create() {
      super.create();
      setTitle("Select internal or external SVD file describing the device");
      setTitleImage(getMyImage(Activator.ID_USBDM_IMAGE));
      setMessage("Please select SVD information");
      validate();
   }

   @Override
   protected Control createDialogArea(Composite parent) {
      
      // Create the manager and bind to main composite
      resManager = new LocalResourceManager(JFaceResources.getResources(), parent);

      Composite container = (Composite) super.createDialogArea(parent);

      GridLayout layout = new GridLayout(1, false);
      layout.marginRight = 5;
      layout.marginLeft = 5;
      container.setLayout(layout);

      /*
       * Create Internal group
       */
      grpInternal = new Group(container, SWT.NONE);     
      grpInternal.setText("USBDM Internal SVD File");
      grpInternal.setLayout(new GridLayout(3, false));
      grpInternal.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      
      Label label = new Label(grpInternal, SWT.NONE);
      label.setText("Target Device:"); //$NON-NLS-1$

      txtDeviceName = new Text(grpInternal, SWT.BORDER|SWT.READ_ONLY);
      txtDeviceName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      
      btnDeviceSelect = new Button(grpInternal, SWT.NONE);
      btnDeviceSelect.setText("Device...");
      btnDeviceSelect.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            DeviceSelector ds = new DeviceSelector(getShell(), interfaceType.targetType, txtDeviceName.getText());
            if (ds.open() == Window.OK) {
               txtDeviceName.setText(ds.getText());
            }
            validate();
         }
         @Override
         public void widgetDefaultSelected(SelectionEvent e) {
         }
      });
      chkUseExternalSVDFile = new Button(container, SWT.CHECK);
      chkUseExternalSVDFile.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      chkUseExternalSVDFile.setText("Use external SVD file");
      chkUseExternalSVDFile.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            validate();
         }
      });

      /*
       * Create External group
       */
      grpExternal = new Group(container, SWT.NONE);
      grpExternal.setText("External SVD File");
      grpExternal.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      grpExternal.setLayout(new GridLayout(3, false));

      label = new Label(grpExternal, SWT.NONE);
      label.setText("External File:"); //$NON-NLS-1$

      txtFilePath = new Text(grpExternal, SWT.BORDER|SWT.READ_ONLY);
      txtFilePath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

      btnFileBrowse = new Button(grpExternal, SWT.PUSH);
      btnFileBrowse.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      btnFileBrowse.setText("Browse...");
      btnFileBrowse.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            final String[] filterExts = {"*.svd;*.xml"}; 
            FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
            fd.setText("Locate SVD file describing device");
            fd.setFilterPath(txtFilePath.getText());
            fd.setFilterExtensions(filterExts);
            String directoryPath = fd.open();
            if (directoryPath != null) {
               txtFilePath.setText(directoryPath);
               validate();
            }
         }
      });
      if (deviceOrFilename == null) {
         deviceOrFilename = "";
      }
      // Try to pre-select previous target
      if (fileExists(deviceOrFilename)) {
         txtFilePath.setText(deviceOrFilename);
         chkUseExternalSVDFile.setSelection(true);
      }
      else {
         txtFilePath.setText("");
         txtDeviceName.setText(deviceOrFilename);
      }
      return container;
   }

   // overriding this methods allows you to set the
   // title of the custom dialog
   @Override
   protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText("Device Peripherals Selection");
   }

   DevicePeripherals getDevicePeripherals() {
      return devicePeripherals;
   }
   
   String getDeviceOrFilename() {
      return deviceOrFilename;
   }
   
   /**
    * Test main
    * 
    * @param args
    * @throws Exception 
    */
   public static void main(String[] args) throws Exception {
      
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setText("Selection");
      shell.setLayout(new FillLayout());

      DeviceSelectDialogue dialogue = new DeviceSelectDialogue(shell, "MKE06Z64M4", InterfaceType.T_ARM);
//      DeviceSelectDialogue dialogue = new DeviceSelectDialogue(shell, "C:/Users/podonoghue/Development/USBDM/ARM_Devices/Generated/STMicro/STM32F40x.svd.xml");
      int result = dialogue.open();
      if (result != Window.OK) {
         // Cancelled etc
         System.err.println("fileOrDevicename = Cancelled");
         return;
      }
      String deviceOrFilename = dialogue.getDeviceOrFilename(); 
      System.err.println("deviceOrFilename = " + deviceOrFilename);
      DevicePeripherals devicePeripherals = dialogue.getDevicePeripherals(); 
      System.err.println("devicePeripherals = " + devicePeripherals);
      UsbdmDevicePeripheralsModel peripheralModel = UsbdmDevicePeripheralsModel.createModel(null, devicePeripherals);
      System.err.println("peripheralModel = " + peripheralModel);
      
      display.dispose();
   }

} 