package net.sourceforge.usbdm.peripherals.view;

import java.io.File;
import java.util.HashMap;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.peripherals.model.UsbdmDevicePeripheralsModel;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class DeviceSelectDialogue extends TitleAreaDialog  {

   private Combo              targetDeviceNameCombo   = null;
   private DeviceDatabase     deviceDatabase          = null;
   private InterfaceType      deviceType              = InterfaceType.T_ARM;

   private String deviceOrFilename = null;
   
   private Button useExternalSVDFileCheckbox;
   private Button fileBrowseButton;
   private Group  usbdmGroup;
   private Group  externalGroup;
   private Label  filePathLabel;
   private Label  targetDeviceLabel;

   public DeviceSelectDialogue(Shell parentShell, String deviceOrFilename) {
      super(parentShell);
      this.deviceOrFilename = deviceOrFilename;
   }
   
   private void populateTargets() {
      targetDeviceNameCombo.removeAll();
      if ((deviceDatabase == null) || (deviceDatabase.getTargetType() != deviceType.targetType)) {
         deviceDatabase = new DeviceDatabase(deviceType.targetType);
      }
      if (!deviceDatabase.isValid()) {
         targetDeviceNameCombo.add("Device database not found");
         targetDeviceNameCombo.setEnabled(false);
      }
      else {
         for (Device device : deviceDatabase.getDeviceList()) {
            if (!device.isHidden()) {
               targetDeviceNameCombo.add(device.getName());
            }
         }
      }
      targetDeviceNameCombo.select(0);
   }
   
   private void setTargetDevice(String target) {
      int targetDeviceIndex = -1;
      if (target != null) {
         // Try to set target device
         targetDeviceNameCombo.setText(target.toUpperCase());
         targetDeviceIndex = targetDeviceNameCombo.getSelectionIndex();
      }
      if (targetDeviceIndex<0) {
         targetDeviceNameCombo.select(0);
      }
   }
   
//   private void updateTargets() {
//      // For future if target type is allowed to change
//      String currentDevice = targetDeviceNameCombo.getText();
//      populateTargets();
//      setTarget(currentDevice);
//   }
   
   
   private boolean fileExists(String filename) {
      if (filename == null) {
         return false;
      }
      IPath svdPath = new Path(filename);
      File svdFile = svdPath.toFile();
      return (svdFile.isFile());
   }
   
   private void update() {
//      System.out.println("update()");

      boolean useExternalSVDFile = useExternalSVDFileCheckbox.getSelection();
      for (Control child : externalGroup.getChildren()) {
         child.setEnabled(useExternalSVDFile);
      }
      for (Control child : usbdmGroup.getChildren()) {
         child.setEnabled(!useExternalSVDFile);
      }
      String message = null;

      deviceOrFilename = null;
      if (useExternalSVDFile) {
         // Check if valid external file and save
         String filename = filePathLabel.getText();
         if (!fileExists(filename)) {
            message = "SVD file path is invalid";
         }
         else {
            deviceOrFilename = filename;
         }
      }
      else {
         // Internal file - assume valid
         deviceOrFilename = targetDeviceNameCombo.getText();
      }
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
      
      GridData gd;
      
      usbdmGroup = new Group(container, SWT.NONE);     
      usbdmGroup.setText("USBDM Internal SVD File");
      usbdmGroup.setLayout(new GridLayout(2, false));
      usbdmGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      
      targetDeviceLabel = new Label(usbdmGroup, SWT.NONE);
      targetDeviceLabel.setText("Target Device:"); //$NON-NLS-1$

      //
      // Create & Populate Combo for USBDM devices
      //
      targetDeviceNameCombo = new Combo(usbdmGroup, SWT.BORDER|SWT.READ_ONLY);
      gd = new GridData();
      gd.widthHint = 200;
      targetDeviceNameCombo.setLayoutData(gd);
      targetDeviceNameCombo.select(0);
      targetDeviceNameCombo.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            update();
         }
      });

      useExternalSVDFileCheckbox = new Button(container, SWT.CHECK);
      useExternalSVDFileCheckbox.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      useExternalSVDFileCheckbox.setText("Use external SVD file");
      useExternalSVDFileCheckbox.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            update();
         }
      });

      externalGroup = new Group(container, SWT.NONE);
      externalGroup.setText("External SVD File");
      externalGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      externalGroup.setLayout(new GridLayout(2, false));

      fileBrowseButton = new Button(externalGroup, SWT.PUSH);
      fileBrowseButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      fileBrowseButton.setText("Browse");
      fileBrowseButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
            fd.setText("Locate SVD file describing device");
            fd.setFilterPath(filePathLabel.getText());
            String directoryPath = fd.open();
            if (directoryPath != null) {
               filePathLabel.setText(directoryPath);
               update();
            }
         }
      });

      filePathLabel = new Label(externalGroup, SWT.NONE);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      gd.grabExcessHorizontalSpace = true;
      filePathLabel.setLayoutData(gd);
      
      populateTargets();
      
      // Try to pre-select previous target
      if (fileExists(deviceOrFilename)) {
         filePathLabel.setText(deviceOrFilename);
         useExternalSVDFileCheckbox.setSelection(true);
      }
      else {
         filePathLabel.setText("");
         setTargetDevice(deviceOrFilename);
      }
      update();
      
      return container;
   }

   // overriding this methods allows you to set the
   // title of the custom dialog
   @Override
   protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText("Device Peripherals Selection");
   }

   String getDeviceOrFilename() {
      return deviceOrFilename;
   }
   
   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
      
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setText("Selection");
      shell.setLayout(new FillLayout());

//      DeviceSelectDialogue dialogue = new DeviceSelectDialogue(shell, "mke06z64M4");
      DeviceSelectDialogue dialogue = new DeviceSelectDialogue(shell, "C:/Users/podonoghue/Development/USBDM/ARM_Devices/Generated/STMicro/STM32F40x.svd.xml");
      int result = dialogue.open();
      String deviceOrSvdFilename = dialogue.getDeviceOrFilename(); 
      if ((result != Window.OK) || (deviceOrSvdFilename == null)) {
         // Cancelled etc
         System.err.println("fileOrDevicename = Cancelled");
         return;
      }
      System.err.println("fileOrDevicename = " + deviceOrSvdFilename);
      UsbdmDevicePeripheralsModel peripheralModel = new UsbdmDevicePeripheralsModel(deviceOrSvdFilename, null);
      System.err.println("fileOrDevicename = " + peripheralModel);
      
      display.dispose();
   }

} 