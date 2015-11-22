/*
===============================================================================================================
| History                                                                                                      
---------------------------------------------------------------------------------------------------------------
| 19 Jan 2015 | New format device selection                                                       | V4.10.6.250
===============================================================================================================
*/
package net.sourceforge.usbdm.peripherals.view;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import net.sourceforge.usbdm.deviceDatabase.ui.DeviceSelector;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripheralsProviderInterface;
import net.sourceforge.usbdm.peripheralDatabase.SVDIdentifier;
import net.sourceforge.usbdm.peripheralDatabase.IPeripheralDescriptionProvider;
import net.sourceforge.usbdm.peripherals.model.UsbdmDevicePeripheralsModel;
import net.sourceforge.usbdm.peripherals.usbdm.UsbdmPeripheralDescriptionProvider;

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
import org.eclipse.swt.graphics.Point;
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
import org.eclipse.swt.widgets.Text;

/**
 * Dialogue to select device or SVD file
 * 
 * @author podonoghue
 *
 */
public class DevicePeripheralSelectionDialogue extends TitleAreaDialog  {

   private DevicePeripheralsProviderInterface devicePeriperalsProviderInterface = null;
   private DevicePeripherals                  devicePeripherals                 = null;
   private SVDIdentifier                      svdIdentifier                     = null;

   // Details of interfaces selected by comboManufacturerSelect
   private ArrayList<String> providerIds             = new ArrayList<String>();
   private Combo             comboManufacturerSelect = null;
   private Combo             comboDeviceName         = null;
   private Button            btnDeviceSelect         = null;
   private Button            chkUseExternalSVDFile   = null;
   private Button            btnFileBrowse           = null;
   private Group             grpInternal             = null;
   private Group             grpExternal             = null;
   private Text              txtFilePath             = null;

   //TODO - make variable
   private TargetType targetType = TargetType.T_ARM;
 
   /**
    * Dialogue to select device SVD file
    * 
    * @author podonoghue
    */
   public DevicePeripheralSelectionDialogue(Shell parentShell, SVDIdentifier svdIdentifier) {
      super(parentShell);
      this.devicePeripherals = null;
      this.svdIdentifier     = svdIdentifier;
      devicePeriperalsProviderInterface = new DevicePeripheralsProviderInterface();
   }
   
   /**
    * Validate dialogue contents and update status area etc.
    *  
    */
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
         // External file
         Path filepath = Paths.get(txtFilePath.getText());
         svdIdentifier = new SVDIdentifier(filepath);
         devicePeripherals = devicePeriperalsProviderInterface.getDevice(svdIdentifier);
         if (devicePeripherals == null) {
            message = "SVD file path is invalid";
            txtFilePath.setToolTipText("No description");
         }
         else {
            txtFilePath.setToolTipText(devicePeripherals.getDescription());
            svdIdentifier.setDeviceName(devicePeripherals.getName());
         }
      }
      else {
         // Internal provider
         svdIdentifier = new SVDIdentifier(getProviderId(), comboDeviceName.getText());
         devicePeripherals = devicePeriperalsProviderInterface.getDevice(svdIdentifier);
         if (devicePeripherals == null) {
            message = "Can't find SVD file for selected device";
            comboDeviceName.setToolTipText("No description");
         }
         else {
            comboDeviceName.setToolTipText(devicePeripherals.getDescription());
         }
      }
      Button b = getButton(IDialogConstants.OK_ID);
      if (b != null) {
         b.setEnabled(message == null);
      }
      if (message != null) {
         setMessage(message, IMessageProvider.ERROR);
      }
      else {
         setMessage(devicePeripherals.getDescription(), IMessageProvider.INFORMATION);
      }
   }
   
   LocalResourceManager   resManager = null;
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

   String getProviderId() {
      int index = comboManufacturerSelect.getSelectionIndex();
      if ((index<0) || (index>=providerIds.size())) {
         return null;
      }
      return providerIds.get(index);
   }
   
   void manufacturerChanged() {
      // Clear existing device details
      comboManufacturerSelect.setToolTipText("No description");
      comboDeviceName.removeAll();

      String providerId = getProviderId();
      if (providerId == null) {
         return;
      }
      IPeripheralDescriptionProvider peripheralDescriptionProvider = devicePeriperalsProviderInterface.getProvider(providerId);
      if (peripheralDescriptionProvider == null) {
         return;
      }
      comboManufacturerSelect.setToolTipText(peripheralDescriptionProvider.getDescription());
      Vector<String> deviceNames = peripheralDescriptionProvider.getDeviceNames();
      for (String s:deviceNames) {
         comboDeviceName.add(s);
      }
      comboDeviceName.select(0);
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
      grpInternal.setText("Internal SVD Files");
      grpInternal.setLayout(new GridLayout(3, false));
      grpInternal.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      
      Label label = new Label(grpInternal, SWT.NONE);
      label.setText("Manufacturer:"); //$NON-NLS-1$

      comboManufacturerSelect = new Combo(grpInternal, SWT.BORDER|SWT.READ_ONLY);
      comboManufacturerSelect.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      new Label(grpInternal, SWT.NONE);
      providerIds = devicePeriperalsProviderInterface.getProviderIDs();
      for(String pd:providerIds) {
         comboManufacturerSelect.add(devicePeriperalsProviderInterface.getProviderName(pd));
      }
      comboManufacturerSelect.select(0);
      comboManufacturerSelect.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent arg0) {
            manufacturerChanged();
         }
         @Override
         public void widgetDefaultSelected(SelectionEvent arg0) {
         }
      });
      label = new Label(grpInternal, SWT.NONE);
      label.setText("Target Device:"); //$NON-NLS-1$

      comboDeviceName = new Combo(grpInternal, SWT.BORDER|SWT.READ_ONLY);
      comboDeviceName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      comboDeviceName.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent arg0) {
            validate();
         }
         @Override
         public void widgetDefaultSelected(SelectionEvent arg0) {
         }
      });
      btnDeviceSelect = new Button(grpInternal, SWT.NONE);
      btnDeviceSelect.setText("Device...");
      btnDeviceSelect.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            DeviceSelector ds = new DeviceSelector(getShell(), targetType , comboDeviceName.getText());
            if (ds.open() == Window.OK) {
               try {
                  svdIdentifier = new SVDIdentifier(UsbdmPeripheralDescriptionProvider.ID, ds.getText());
               } catch (Exception e1) {
                  e1.printStackTrace();
               }
            }
            svdToInternal(svdIdentifier);
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
      svdToInternal(svdIdentifier);
      return container;
   }

   void svdToInternal(SVDIdentifier svdId) {
//      System.err.println("svdToInternal()" + svdId);
      if (svdId != null) {
         // Load previous device file
         devicePeripherals = devicePeriperalsProviderInterface.getDevice(svdId);
         if (svdId.getPath() != null) {
            chkUseExternalSVDFile.setSelection(true);
            txtFilePath.setText(svdId.getPath().toString());
         }
         else {
            String providerName = devicePeriperalsProviderInterface.getProviderName(svdId.getproviderId());
            if (providerName != null) {
               comboManufacturerSelect.setText(providerName);
               manufacturerChanged();
               comboDeviceName.setText(svdId.getDeviceName());
            }
         }
      }
   }
   
   @Override
   protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText("Device Peripherals Selection");
   }

   public DevicePeripherals getDevicePeripherals() {
      return devicePeripherals;
   }
   
   /*
    * (non-Javadoc)
    * @see org.eclipse.jface.dialogs.Dialog#getInitialSize()
    */
   @Override
   protected Point getInitialSize() {
     return new Point(800, 400);
   }
   
   /**
    * Get selected device
    * 
    * @return identifier for selected device
    */
   public SVDIdentifier getSVDId() {
      return svdIdentifier;
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

//      SVDIdentifier svdIdentifier = new SVDIdentifier(Paths.get("C:/Program Files (x86)/pgo/USBDM 4.11.1.70/DeviceData/Device.SVD/Freescale/MK10D7.svd.xml"));
      SVDIdentifier svdIdentifier = new SVDIdentifier("[SVDIdentifier:usbdm.arm.devices:FRDM_K64F]");
      svdIdentifier = new SVDIdentifier(svdIdentifier.toString());
      DevicePeripheralSelectionDialogue dialogue = new DevicePeripheralSelectionDialogue(shell, svdIdentifier);
//      DeviceSelectDialogue dialogue = new DeviceSelectDialogue(shell, "C:/Users/podonoghue/Development/USBDM/ARM_Devices/Generated/STMicro/STM32F40x.svd.xml");
      int result = dialogue.open();
      if (result != Window.OK) {
         // Cancelled etc
         System.err.println("fileOrDevicename = Cancelled");
         return;
      }
      SVDIdentifier svdID = dialogue.getSVDId();
      System.err.println("svdID = " + svdID);
      System.err.println("svdID.getDeviceName = " + svdID.getDeviceName());
      DevicePeripheralsProviderInterface pif = new DevicePeripheralsProviderInterface();
      DevicePeripherals devicePeripherals = pif.getDevice(svdID);
      System.err.println("devicePeripherals = " + devicePeripherals);
      
      devicePeripherals = dialogue.getDevicePeripherals(); 
      System.err.println("deviceOrFilename = " + devicePeripherals.getName());
      System.err.println("devicePeripherals = " + devicePeripherals);
      UsbdmDevicePeripheralsModel peripheralModel = UsbdmDevicePeripheralsModel.createModel(null, devicePeripherals);
      System.err.println("peripheralModel = " + peripheralModel);
      
      display.dispose();
   }
} 