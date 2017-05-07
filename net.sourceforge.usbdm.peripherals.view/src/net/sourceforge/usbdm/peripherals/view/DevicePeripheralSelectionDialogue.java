/*
===============================================================================================================
| History                                                                                                      
---------------------------------------------------------------------------------------------------------------
| 19 Jan 2015 | New format device selection                                                       | V4.10.6.250
===============================================================================================================
 */
package net.sourceforge.usbdm.peripherals.view;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.sourceforge.usbdm.deviceDatabase.ui.DeviceSelector;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripheralsProviderInterface;
import net.sourceforge.usbdm.peripheralDatabase.IPeripheralDescriptionProvider;
import net.sourceforge.usbdm.peripheralDatabase.SVDIdentifier;
import net.sourceforge.usbdm.peripherals.usbdm.UsbdmPeripheralDescriptionProvider;

/**
 * Dialogue to select device or SVD file
 * 
 * @author podonoghue
 *
 */
public class DevicePeripheralSelectionDialogue extends TitleAreaDialog  {

   private DevicePeripheralsProviderInterface devicePeriperalsProviderInterface = null;
   private SVDIdentifier                      fSvdIdentifier                    = null;

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
   private String fDescription;

   /**
    * Dialogue to select device SVD file
    * 
    * @author podonoghue
    */
   public DevicePeripheralSelectionDialogue(Shell parentShell, SVDIdentifier svdIdentifier) {
      super(parentShell);
      //      this.devicePeripherals = null;
      fSvdIdentifier  = svdIdentifier;
      devicePeriperalsProviderInterface = new DevicePeripheralsProviderInterface();
   }

   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.TitleAreaDialog#setMessage(java.lang.String, int)
    */
   @Override
   public void setMessage(String newMessage, int newType) {
      super.setMessage(newMessage, newType);
      Button okButton = getButton(IDialogConstants.OK_ID);
      if (okButton != null) {
         okButton.setEnabled(newType <= IMessageProvider.INFORMATION);
      }
   }


   LocalResourceManager   resManager = null;
   HashMap<String, Image> imageCache = new HashMap<String,Image>();

   public Image getMyImage(String imageId) {
      Image image = imageCache.get(imageId);
      if ((Activator.getDefault() != null) && (image == null)) {
         ImageDescriptor imageDescriptor  = Activator.getImageDescriptor(imageId);
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
//      setMessage("Please select SVD information");
   }

   /**
    * Gets the provider ID for the currently selected provider (comboManufacturerSelect)
    * @return
    */
   String getProviderId() {
      int index = comboManufacturerSelect.getSelectionIndex();
      if ((index<0) || (index>=providerIds.size())) {
         return null;
      }
      return providerIds.get(index);
   }

   /**
    * Populate device list based on currently selected provider
    */
   void populateDeviceList() {
      // Clear existing device details
      comboManufacturerSelect.setToolTipText("No description");
      comboDeviceName.removeAll();

      String providerId = getProviderId();
      if (providerId == null) {
         return;
      }
      try {
         IPeripheralDescriptionProvider peripheralDescriptionProvider = devicePeriperalsProviderInterface.getProvider(providerId);
         comboManufacturerSelect.setToolTipText(peripheralDescriptionProvider.getDescription());
         Vector<String> deviceNames = peripheralDescriptionProvider.getDeviceNames();
         for (String s:deviceNames) {
            comboDeviceName.add(s);
         }
         comboDeviceName.select(0);
      } catch (Exception e) {
         System.err.println("DevicePeripheralSelectionDialogue.populateDeviceList()"+e.getMessage());
      }
   }

   /**
    * Populate device list based on currently selected provider and <br>
    * update SVD information
    */
   void manufacturerChanged() {
      populateDeviceList();
      updateSvdId();
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
      populateDeviceList();
      comboDeviceName.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent arg0) {
            // Selected device has changed
            updateSvdId();
         }
         @Override
         public void widgetDefaultSelected(SelectionEvent arg0) {
         }
      });
      btnDeviceSelect = new Button(grpInternal, SWT.NONE);
      btnDeviceSelect.setText("Device...");
      btnDeviceSelect.setToolTipText("Select internal USBDM\ndevice by category");
      btnDeviceSelect.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent event) {
            // Open dialogue to select device
            DeviceSelector ds = new DeviceSelector(getShell(), targetType , comboDeviceName.getText());
            if (ds.open() == Window.OK) {
               // Map device name to SVD file name
               String message = null;
               try {
                  IPeripheralDescriptionProvider provider = devicePeriperalsProviderInterface.getProvider(UsbdmPeripheralDescriptionProvider.ID);
                  String mappedName = provider.getMappedDeviceName(ds.getText());
                  if (mappedName == null) {
                     message = "Cannot locate SVD file for device";
                  }
                  else {
//                     System.err.println("Mapped name = " + mappedName);
                     int providerIndex = providerIds.indexOf(UsbdmPeripheralDescriptionProvider.ID);
                     if (providerIndex<0) {
                        providerIndex = 0;
                     }
                     comboManufacturerSelect.select(providerIndex);
                     populateDeviceList();
                     comboDeviceName.setText(mappedName);
                     updateSvdId();
                  }
               } catch (Exception e) {
                  message = "Cannot locate SVD for device\nReason " + e.getMessage();
               }
               if (message != null) {
                  MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.CANCEL);
                  messageBox.setText("Error");
                  messageBox.setMessage(message);
                  messageBox.open();
               }
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
            updateSvdId();
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
      btnFileBrowse.setToolTipText("Select external file...");
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
               updateSvdId();
            }
         }
      });
      setSvdIdentifier(fSvdIdentifier);
      return container;
   }

   /**
    * Set SVD Identifier and update dialogue to reflect change.
    * 
    * @param svdId
    */
   void setSvdIdentifier(SVDIdentifier svdId) {
//      System.err.println("setSvdIdentifier("+svdId+")");
      fSvdIdentifier = svdId;

      if (fSvdIdentifier == null) {
         // Choose default device
         chkUseExternalSVDFile.setSelection(false);
         comboManufacturerSelect.select(providerIds.indexOf(UsbdmPeripheralDescriptionProvider.ID));
         manufacturerChanged();
      }
      else if (fSvdIdentifier.getPath() != null) {
         // Set up external device
         chkUseExternalSVDFile.setSelection(true);
         txtFilePath.setText(fSvdIdentifier.getPath().toString());
         updateSvdId();
      }
      else {
         // Set up internal device
         String providerName = devicePeriperalsProviderInterface.getProviderName(fSvdIdentifier.getproviderId());
         comboManufacturerSelect.setText(providerName);
         populateDeviceList();
         try {
            comboDeviceName.setText(fSvdIdentifier.getDeviceName());
         } catch (Exception e) {
            System.err.println("DevicePeripheralSelectionDialogue.setSvdIdentifier()"+e.getMessage());
         }
         updateSvdId();
      }
   }

   /**
    * Update SVD from dialogue information
    */
   void updateSvdId() {
//      System.err.println("updateSvdId()");
      SVDIdentifier svdId = null;
      try {
         if (chkUseExternalSVDFile.getSelection()) {
            // External file
            svdId = new SVDIdentifier(Paths.get(txtFilePath.getText()));
         }
         else {
            // Internal file
            svdId = new SVDIdentifier(getProviderId(), comboDeviceName.getText());
         }
      } catch (Exception e) {
         System.err.println("DevicePeripheralSelectionDialogue.updateSvdId() "+e.getMessage());
      }
      if (svdId != null) {
         fSvdIdentifier = svdId;
      }
      validate();
   }

   /**
    * Validate dialogue contents and update status area etc.
    */
   private void validate() {
//      System.err.println("validate()");
      final boolean useExternalSVDFile = chkUseExternalSVDFile.getSelection();
      for (Control child : grpExternal.getChildren()) {
         child.setEnabled(useExternalSVDFile);
      }
      for (Control child : grpInternal.getChildren()) {
         child.setEnabled(!useExternalSVDFile);
      }

      String message = null;
      fDescription = "";
      if ((fSvdIdentifier == null) || !fSvdIdentifier.isValid()) {
         // Update message
         if (useExternalSVDFile) {
            message = "SVD file or path is invalid";
         }
         else {
            message = "Can't find SVD file for selected device";
         }
      }
      else {
         // Update description
         try {
            fDescription = fSvdIdentifier.getDevicePeripherals().getDescription();
         } catch (Exception e) {
            System.err.println("DevicePeripheralSelectionDialogue.validate()"+e.getMessage());
         }
      }
      if (message != null) {
         setMessage(message, IMessageProvider.ERROR);
      }
      else {
         setMessage(fDescription, IMessageProvider.INFORMATION);
      }
   }

   @Override
   protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText("Device Peripherals Selection");
   }

   /**
    * Get selected device
    * 
    * @return identifier for selected device
    */
   public SVDIdentifier getSVDId() {
      return fSvdIdentifier;
   }
} 