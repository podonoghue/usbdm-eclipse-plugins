package net.sourceforge.usbdm.deviceDatabase.ui;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;

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

   private DeviceDatabase  deviceDatabase = null;
   private TargetType      targetType     = TargetType.T_ARM;
   private Tree            tree           = null;
   private Text            text           = null;
   
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
            new Pair("^(SKEA).*$",        "Kinetis E (MKE/SKEA)"),
            new Pair("^(MKE).*$",         "Kinetis E (MKE/SKEA)"),
            new Pair("^(MKL).*$",         "Kinetis L (MKL)"),
            new Pair("^(MKM).*$",         "Kinetis M (MKM)"),
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
      switch (targetType) {
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
            new Pair("(FRDM-[a-zA-Z]*).*$",                             "$1"),
            new Pair("(TWR-.*)$",                                       "$1"),
            new Pair("^(STM32F[0-9]*).*$",                              "$1"),
            new Pair("^(PK[0-9]*).*$",                                  "$1"),
            new Pair("^(SKEA)[a-zA-Z]*[0-9]*(M[0-9]+)$",                "$1xxx$2"),
            new Pair("^(MKE02)Z.*(M2)$",                                "$1xxx$2 (20MHz)"),
            new Pair("^(MKE02)Z.*(M4)$",                                "$1xxx$2 (40MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z).*(M4)$",      "$1xxx$2 (48MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z).*(M5)$",      "$1xxx$2 (50MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z).*(M7)$",      "$1xxx$2 (70MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z).*(M10)$",     "$1xxx$2 (100MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z).*(M12)$",     "$1xxx$2 (120MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z).*(M15)$",     "$1xxx$2 (150MHz)"),
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
      switch (targetType) {
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
      this.targetType = targetType;
      if ((deviceDatabase == null) || (deviceDatabase.getTargetType() != targetType)) {
         deviceDatabase = new DeviceDatabase(targetType);
      }
      if (!deviceDatabase.isValid()) {
         text.setText("Device database invalid");
      }
      else {
         String currentFamily    = null;
         TreeItem familyTree     = null;
         String currentSubFamily = null;
         TreeItem subFamilyTree  = null;
         for (Device device : deviceDatabase.getDeviceList()) {
            if (device.isHidden()) {
               continue;
            }
            String family = getFamilyNamePrefix(device.getName());
            if ((familyTree == null) || !currentFamily.equalsIgnoreCase(family)) {
               familyTree = findCatergoryNode(tree, family);
            }
            if (familyTree == null) {
               currentFamily = family;
               familyTree = new TreeItem(tree, 0);
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
    * Used to determine initially selected devices
    *  
    * @param targetName
    */
   public void setDevice(String name) {
      if (name == null) {
         return;
      }
      TreeItem item = findLeafNode(tree, name);
      if (item != null) {
         text.setText(item.getText());
      }
   }

   /**
    * Create control composite
    * 
    * @param parent
    */
   private void createControl(Composite parent) {
      setLayout(new GridLayout(2, false));

      Label label =new Label(this, SWT.NONE);
      label.setText("Device: ");
      text = new Text(this, SWT.BORDER|SWT.READ_ONLY|SWT.NO_FOCUS);
      text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      text.addFocusListener(new FocusListener() {
         @Override
         public void focusLost(FocusEvent e) {
         }
         
         @Override
         public void focusGained(FocusEvent e) {
            tree.setFocus();
         }
      });
      tree = new Tree(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
      tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
      tree.addListener(SWT.Selection, new Listener() {
         public void handleEvent(Event event) {

            if (event.detail != SWT.CHECK) {
               if (event.item instanceof TreeItem) {
                  TreeItem treeItem = (TreeItem) event.item;
                  if (treeItem.getItemCount() == 0) {
                     text.setText(treeItem.getText());
                     text.notifyListeners(SWT.CHANGED, new Event());
                  }
               }
            }
         }
      });

      tree.addListener(SWT.Expand, new Listener() {
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
      if ((deviceDatabase == null) || (!deviceDatabase.isValid())) {
         return "Device database is invalid";
      }
      if (text.getText().length() == 0) {
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
      if ((deviceDatabase == null) || (deviceDatabase.getTargetType() != targetType)) {
         deviceDatabase = new DeviceDatabase(targetType);
      }
      Device device = null;
      if (deviceDatabase.isValid()) {
         String deviceName = text.getText();
         device = deviceDatabase.getDevice(deviceName);
         if (device == null) {
            device = deviceDatabase.getDefaultDevice();
         }
      }
      return device;
   }

   /* (non-Javadoc)
    * @see org.eclipse.swt.widgets.Widget#addListener(int, org.eclipse.swt.widgets.Listener)
    */
   @Override
   public void addListener(int eventType, Listener listener) {
      if (eventType == SWT.CHANGED) {
         text.addListener(eventType, listener);
      }
      else {
         super.addListener(eventType, listener);
      }
   }

   /**
    * Get name of selected device
    * 
    * @return Name of device
    */
   public String getText() {
      return text.getText();
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