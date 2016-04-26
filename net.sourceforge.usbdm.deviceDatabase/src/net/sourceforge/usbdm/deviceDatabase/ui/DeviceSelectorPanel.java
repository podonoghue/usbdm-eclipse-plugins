package net.sourceforge.usbdm.deviceDatabase.ui;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;

public class DeviceSelectorPanel extends Composite {

   /**
    * Construct a device selection panel
    * 
    * @param parent
    * @param style
    */
   public DeviceSelectorPanel(Composite parent, int style) {
      super(parent, style);
      createControl(parent);
//      addDisposeListener(new DisposeListener() {
//         @Override
//         public void widgetDisposed(DisposeEvent e) {
//         }
//      });
   }

   // Controls
   private Tree            fTree           = null;
   private Text            fDeviceText     = null;
   
   // Internal state
   private TargetType      fTargetType     = TargetType.T_ARM;
   private DeviceDatabase  fDeviceDatabase = null;
   private String          fDeviceName     = null;

   private static final String NO_DEVICE_STRING = "<No device selected>";

   private class Pair {
      String pattern;
      String substitution;
      Pair(String pattern, String substitution) {
         this.pattern      = pattern;
         this.substitution = substitution;
      }
   }

   private String getFamilyNamePrefix(String name) {
      Pair[] armPatterns = {
            new Pair("^(S9KEA).*$",       "Kinetis E (MKE/S9KEA)"),
            new Pair("^(MKE).*$",         "Kinetis E (MKE/S9KEA)"),
            new Pair("^(MKL).*$",         "Kinetis L (MKL)"),
            new Pair("^(MKM).*$",         "Kinetis M (MKM)"),
            new Pair("^(MKV).*$",         "Kinetis V (MKV)"),
            new Pair("^(MK)[0-9].*$",     "Kinetis K (MK)"),
            new Pair("^(FRDM).*$",        "Freedom boards (FRDM)"),
            new Pair("^(TWR).*$",         "Tower boards (TWR)"),
            new Pair("^(STM32).*$",       "ST Micro ($1)"),
            new Pair("^([a-zA-Z]+).*$",   "Other ($1)"),
      };
      Pair[] cfv1Patterns = {
            new Pair("^(MCF[0-9]*).*$",   "$1"),
            new Pair("^([a-zA-Z]+).*$",   "$1"),
      };
      Pair[] cfvxPatterns = {
            new Pair("^(MCF[0-9]{4}).*$", "$1"),
      };
      Pair[] patterns = null;
      switch (fTargetType) {
      case T_ARM:
         patterns = armPatterns;
         break;
      case T_CFV1:
         patterns = cfv1Patterns;
         break;
      case T_CFVx:
         patterns = cfvxPatterns;
         break;
      default:
         break;
      }
      for (Pair pattern:patterns) {
         Pattern p = Pattern.compile(pattern.pattern);
         Matcher m = p.matcher(name);
         if (m.matches()) {
            return m.replaceAll(pattern.substitution);
         }
      }
      return name;
   }

   private String getSubFamilyNamePrefix(String name) {
      Pair[] armPatterns = {
            new Pair("(FRDM[-_][a-zA-Z]*).*$",                          "$1"),
            new Pair("(TWR[-_].*)$",                                    "$1"),
            new Pair("^(STM32F[0-9]*).*$",                              "$1"),
            new Pair("^(PK[0-9]*).*$",                                  "$1"),
            new Pair("^(S9KEA)[a-zA-Z]*[0-9]*(M[0-9]+)$",               "$1xxx$2"),
            new Pair("^(MKE02)Z.*(M2)$",                                "$1xxx$2 (20MHz)"),
            new Pair("^(MKE02)Z.*(M4)$",                                "$1xxx$2 (40MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z|F).*(M4)$",      "$1xxx$2 (48MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z|F).*(M5)$",      "$1xxx$2 (50MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z|F).*(M7)$",      "$1xxx$2 (70MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z|F).*(M10)$",     "$1xxx$2 (100MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z|F).*(M12)$",     "$1xxx$2 (120MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z|F).*(M15)$",     "$1xxx$2 (150MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z|F).*(M18)$",     "$1xxx$2 (180MHz)"),
            //            new Pair("^([a-zA-Z]+[0-9]*).*$",
      };
      Pair[] cfv1Patterns = {
            new Pair("^(MCF[0-9]*[a-zA-z]*).*$", "$1"),
            new Pair("^uuu(.*)$",                "$1"),
            new Pair("^(TWR.*)$",                "$1"),
            new Pair("^([a-zA-Z]+).*$",          "$1"),
      };
      Pair[] cfvxPatterns = {
            new Pair("^(.*)$",      "$1"),
      };
      Pair[] patterns = null;
      switch (fTargetType) {
      case T_ARM:
         patterns = armPatterns;
         break;
      case T_CFV1:
         patterns = cfv1Patterns;
         break;
      case T_CFVx:
         patterns = cfvxPatterns;
         break;
      default:
         break;
      }
      for (Pair pattern:patterns) {
         Pattern p = Pattern.compile(pattern.pattern);
         Matcher m = p.matcher(name);
         if (m.matches()) {
            return m.replaceAll(pattern.substitution);
         }
      }
      return name;
   }

   private TreeItem findLeafNode(Object node, String name) {
      TreeItem[] items = null;
      if (node instanceof Tree) {
         items = ((Tree)node).getItems();
      }
      else if (node instanceof TreeItem) {
         items = ((TreeItem)node).getItems();
      }
      if (items != null) {
         for (TreeItem item:items) {
//            System.err.println("setDevice() Checking " + item.getText());
            if (item.getItemCount() != 0) {
               TreeItem searchItem = findLeafNode(item, name);
               if (searchItem != null) {
                  return searchItem;
               }
            }
            else if (item.getText().equalsIgnoreCase(name)) {
               return item;
            }
         }
      }
      return null;
   }
   
   private TreeItem findCatergoryNode(Object node, String name) {
      TreeItem[] items = null;
      if (node instanceof Tree) {
         items = ((Tree)node).getItems();
      }
      else if (node instanceof TreeItem) {
         items = ((TreeItem)node).getItems();
      }
      if (items != null) {
         for (TreeItem item:items) {
//            System.err.println("setDevice() Checking " + item.getText());
            if (item.getItemCount() != 0) {
               if (item.getText().equalsIgnoreCase(name)) {
                  return item;
               }
               TreeItem searchItem = findLeafNode(item, name);
               if (searchItem != null) {
                  return searchItem;
               }
            }
         }
      }
      return null;
   }
   
   /**
    * Set target type<br>
    * Used to determine which device list to display
    * 
    * @param targetType
    */
   public void setTargetType(TargetType targetType) {
      fTargetType = targetType;
      fDeviceName = null;
      
      if ((fDeviceDatabase == null) || (fDeviceDatabase.getTargetType() != targetType)) {
         fDeviceDatabase = new DeviceDatabase(targetType);
      }
      if (!fDeviceDatabase.isValid()) {
         fDeviceText.setText("<Device database invalid>");
      }
      else {
         String currentFamily    = null;
         TreeItem familyTree     = null;
         String currentSubFamily = null;
         TreeItem subFamilyTree  = null;
         for (Device device : fDeviceDatabase.getDeviceList()) {
            if (device.isHidden()) {
               continue;
            }
            String family = getFamilyNamePrefix(device.getName());
            if ((familyTree == null) || !currentFamily.equalsIgnoreCase(family)) {
               familyTree = findCatergoryNode(fTree, family);
            }
            if (familyTree == null) {
               currentFamily = family;
               familyTree = new TreeItem(fTree, 0);
               familyTree.setText(family);
               currentSubFamily = null;
               subFamilyTree = null;
            }
            String subFamily = getSubFamilyNamePrefix(device.getName());
            if ((subFamilyTree == null) || (!currentSubFamily.equalsIgnoreCase(subFamily))) {
               subFamilyTree = findCatergoryNode(familyTree, subFamily);
            }
            if (subFamilyTree == null) {
               currentSubFamily = subFamily;
               subFamilyTree = new TreeItem(familyTree, 0);
               subFamilyTree.setText(currentSubFamily);
            }
            if (device.getName().equalsIgnoreCase(currentSubFamily)) {
               continue;
            }
            TreeItem treeItem = new TreeItem(subFamilyTree, 0);
            treeItem.setText(device.getName());
         }
      }
   }

   /**
    * Set device name<br>
    * Used to determine initially selected device
    *  
    * @param targetName
    */
   public void setDevice(String targetName) {
      fDeviceName = null;
      if (targetName == null) {
         fTree.deselectAll();
         return;
      }
      TreeItem item = findLeafNode(fTree, targetName);
      if (item != null) {
         fDeviceName = item.getText();
         fDeviceText.setText(fDeviceName);
      }
   }

   /**
    * Create control composite
    * 
    * @param parent
    */
   private void createControl(Composite parent) {
      setLayout(new GridLayout(2, false));

      Label label = new Label(this, SWT.NONE);
      label.setText("Device: ");
      fDeviceText = new Text(this, SWT.BORDER|SWT.READ_ONLY|SWT.NO_FOCUS);
      fDeviceText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      fDeviceText.addFocusListener(new FocusListener() {
         @Override
         public void focusLost(FocusEvent e) {
         }
         
         @Override
         public void focusGained(FocusEvent e) {
            fTree.setFocus();
         }
      });
      fDeviceText.setText(NO_DEVICE_STRING);
      
      fTree = new Tree(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
      fTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
      
      fTree.addListener(SWT.Selection, new Listener() {
         public void handleEvent(Event event) {
            if (event.detail != SWT.CHECK) {
               if (event.item instanceof TreeItem) {
                  TreeItem treeItem = (TreeItem) event.item;
                  if (treeItem.getItemCount() == 0) {
                     fDeviceName = treeItem.getText();
                     fDeviceText.setText(fDeviceName);
                     fDeviceText.notifyListeners(SWT.CHANGED, new Event());
                  }
               }
            }
         }
      });

      fTree.addListener(SWT.Expand, new Listener() {
         public void handleEvent(Event event) {

            if (event.detail != SWT.CHECK) {
               if (event.item instanceof TreeItem) {
                  TreeItem treeItem = (TreeItem) event.item;
                  TreeItem parent = treeItem.getParentItem();
                  if (parent != null) {
                     for (TreeItem sibling:parent.getItems()) {
                        if (sibling != event.item) {
                           sibling.setExpanded(false);
                        }
                     }
                  }
                  else {
                     Tree tree = treeItem.getParent();
                     if (tree != null) {
                        for (TreeItem sibling:tree.getItems()) {
                           if (sibling != event.item) {
                              sibling.setExpanded(false);
                           }
                        }
                     }
                  }
               }
            }
         }
      });
   }
   
   /**
    * Validates control
    * 
    * @return Error message or null if no problems
    */
   public String validate() {
      if ((fDeviceDatabase == null) || (!fDeviceDatabase.isValid())) {
         return "Device database is invalid";
      }
      if (fDeviceText.getText().equals(NO_DEVICE_STRING) || (fDeviceText.getText().length() == 0)) {
         return "Select device";
      }
      return null;
   }
   
   /**
    * Get selected device
    * 
    * @return Selected device
    */
   public Device getDevice() {
      if ((fDeviceDatabase == null) || 
          (!fDeviceDatabase.isValid()) || 
          (fDeviceDatabase.getTargetType() != fTargetType)) {
         return null;
      }
//      if ((fDeviceDatabase == null) || (fDeviceDatabase.getTargetType() != fTargetType)) {
//         fDeviceDatabase = new DeviceDatabase(fTargetType);
//      }
      Device device = null;
      if ((fDeviceName != null) && fDeviceDatabase.isValid()) {
         device = fDeviceDatabase.getDevice(fDeviceName);
      }
      return device;
   }

   /**
    * Get name of selected device (or null if none)
    * 
    * @return Selected device
    */
   public String getDeviceName() {
      return fDeviceName;
   }
   
   /* (non-Javadoc)
    * @see org.eclipse.swt.widgets.Widget#addListener(int, org.eclipse.swt.widgets.Listener)
    */
   @Override
   public void addListener(int eventType, Listener listener) {
      if (eventType == SWT.CHANGED) {
         fDeviceText.addListener(eventType, listener);
      }
      else {
         super.addListener(eventType, listener);
      }
   }

   public static void main(String[] args) {
      Display display = new Display();
      Shell shell = new Shell(display);
      shell.setText("Device Selector");
      shell.setSize(300, 330);
      shell.setBackground(new Color(display, 255,0,0));
      shell.setLayout(new FillLayout());
      
      DeviceSelectorPanel deviceSelectorPanel = new DeviceSelectorPanel(shell, SWT.NONE);
      deviceSelectorPanel.setTargetType(TargetType.T_ARM);
//      deviceSelector.setDevice("MK20DX128M5");
      
      shell.layout();
      
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }
}