package net.sourceforge.usbdm.annotationEditor.validators;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.AnnotationModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BinaryOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.EnumeratedOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.HeadingModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.OptionHeadingModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.SelectionTag;
import net.sourceforge.usbdm.annotationEditor.Message;
import net.sourceforge.usbdm.annotationEditor.MyValidator;

public class PinMappingValidator extends MyValidator {

   // Bit of a hack - Magic number used to indicate reset value 
   private final long RESET_MUX_VALUE = -2;
   
   /**
    * Represents a node that is forcing
    */
   static class NodeToUpdate {
      /** Target i.e. node being forced */
      EnumeratedOptionModelNode target;

      /** Nodes that are forcing */
      ArrayList<SelectionTag> forcingNodes;

      /** Count of forcing nodes.  If greater than 1 then the target will be set invalid */
      int forcingNodeCount;

      /**
       * Create forcing node
       * 
       * @param target        Node being forced i.e. target
       * @param forcingNode   Name of node forcing
       */
      NodeToUpdate(EnumeratedOptionModelNode target) {
         this.target       = target;
         this.forcingNodes = new ArrayList<SelectionTag>();
         forcingNodeCount  = 0;
      }

      /**
       * Add forcing node - this will mean that the target needs to be set invalid.
       * 
       * @param tag Name of node forcing
       */
      void addForcingNode (SelectionTag tag) {
         forcingNodeCount++;
         this.forcingNodes.add(tag);
      }

      /** 
       * Gets a formated list of the forcing nodes
       * 
       * @return
       */
      String getForcingNodeNames() {
         StringBuffer sb = new StringBuffer();
         boolean doneFirst = false;
         for (SelectionTag tag:forcingNodes) {
            if (doneFirst) {
               sb.append(", \n");
            }
            sb.append("- ");
            sb.append(tag.controllingNode.getDescription());
            doneFirst = true;
         }
         return sb.toString();
      }

      public String toString() {
         return String.format("NodeToUpdate(t=%s, f=%s)", target.getName(), forcingNodes.toString());
      }
   };

   @Override
   public void validate(TreeViewer viewer) throws Exception {
      BinaryOptionModelNode  map_by_functionNode = getBinaryModelNode("MAP_BY_FUNCTION");
      BinaryOptionModelNode  map_by_pinNode      = getBinaryModelNode("MAP_BY_PIN");

      boolean mapByPin = map_by_pinNode.safeGetValue();

      // Make selection consistent
      if (mapByPin == map_by_functionNode.safeGetValue()) {
         if (mapByPin) {
            update(viewer, map_by_functionNode, false);
         }
         else {
            update(viewer, map_by_functionNode, true);
         }
      }
//      System.err.println("PinMappingValidator.validate()");
      super.validate(viewer);
      HashMap<EnumeratedOptionModelNode, NodeToUpdate> nodesToUpdate = new HashMap<EnumeratedOptionModelNode, NodeToUpdate>();

      ArrayList<SelectionTag> pinSelectionNodes = new ArrayList<SelectionTag>();
      findActiveSelectionNodes(map_by_pinNode,      pinSelectionNodes);
      
      ArrayList<SelectionTag> functionSelectionNodes = new ArrayList<SelectionTag>();
      findActiveSelectionNodes(map_by_functionNode, functionSelectionNodes);

      System.err.println("Collecting Selection Nodes");
      ArrayList<SelectionTag> activeSelectionNodes   = null;
      if (mapByPin) {
         activeSelectionNodes   = pinSelectionNodes;
      }
      else {
         activeSelectionNodes   = functionSelectionNodes;
      }

      // Collect target nodes to update
      for (SelectionTag tag:activeSelectionNodes) {
//         System.err.println("Processing active selection node = " + tag.toString());
//         System.err.println(String.format("getValue() = %d, selectionValue= %d", tag.controllingNode.safeGetValueAsLong(), tag.selectionValue));

         // Clear messages inactive nodes
         setValid(viewer, tag.controllingNode);

         NumericOptionModelNode targetSignalNode = getNumericModelNode(tag.signalName);
         if (targetSignalNode == null) {
            setValid(viewer, tag.controllingNode, new Message("Can't find referenced selection node "+tag.signalName));
            System.err.println("PinMappingValidator.validate() Can't find referenced selection node "+tag.signalName);
            continue;
         }
         if (!(targetSignalNode instanceof EnumeratedOptionModelNode)) {
            setValid(viewer, tag.controllingNode, new Message("Referenced selection node "+tag.signalName+ " has wrong type"+ ", class = " + targetSignalNode.getClass()));
            System.err.println("PinMappingValidator.validate() Incorrect node class for node " + targetSignalNode.getName() + ", class = " + targetSignalNode.getClass());
            continue;
         }
         EnumeratedOptionModelNode target = (EnumeratedOptionModelNode) targetSignalNode;
         NodeToUpdate nodeToUpdate = nodesToUpdate.get(target);
         if (nodeToUpdate == null) {
            nodeToUpdate = new NodeToUpdate(target);
            nodesToUpdate.put(target, nodeToUpdate);
         }
         if (tag.controllingNode.safeGetValueAsLong() == tag.selectionValue) {
            // Peripheral signal has been mapped to this pin
//             System.err.println("Peripheral signal has been mapped to this pin");
//             System.err.println("Tag = " + tag.toString() + " applied to " + targetNode.getName());
            nodeToUpdate.addForcingNode(tag);
         }
      }

      // Update target nodes
//      System.err.println("Updating Target Nodes");
      if (mapByPin) {
         for (EnumeratedOptionModelNode targetNode:nodesToUpdate.keySet()) {
            NodeToUpdate nodeToUpdate = nodesToUpdate.get(targetNode);
//         System.err.println(String.format("Updating %s", nodeToUpdate.toString()));
//         System.err.println(String.format("Updating t=%s, v=%s", targetNode.getName(), nodeToUpdate.forcingNodes));
            if (nodeToUpdate.forcingNodeCount == 0) {
               update(viewer, targetNode, "Disabled");
               setValid(viewer, targetNode, new Message("Signal has not been mapped to a pin", AnnotationModel.Severity.WARNING));
            }
            else if (nodeToUpdate.forcingNodeCount == 1) {
               update(viewer, targetNode, nodeToUpdate.forcingNodes.get(0).signalValue);
            }
            else {
               boolean clash = false;
               for (SelectionTag tag:nodeToUpdate.forcingNodes) {
                  if (!nodeToUpdate.forcingNodes.get(0).signalValue.equals(tag.signalValue)) {
                     System.err.println(nodeToUpdate.forcingNodes.get(0).signalValue + "!=" + tag.signalValue);
                     clash = true;
                  }
               }
               if (clash) {
                  update(viewer, targetNode, "Disabled");
                  setValid(viewer, targetNode, new Message("Signal has been mapped to multiple pins: \n" + nodeToUpdate.getForcingNodeNames()));
                  for (SelectionTag forcingNode:nodeToUpdate.forcingNodes) {
                     setValid(viewer, forcingNode.controllingNode, new Message("Selection conflicts between: \n" + nodeToUpdate.getForcingNodeNames()));
                  }
               }
               else {
                  update(viewer, targetNode, nodeToUpdate.forcingNodes.get(0).signalValue);
               }
            }
         }
      }
      else {
         // Map by function
         for (EnumeratedOptionModelNode targetNode:nodesToUpdate.keySet()) {
            NodeToUpdate nodeToUpdate = nodesToUpdate.get(targetNode);
//         System.err.println(String.format("Updating %s", nodeToUpdate.toString()));
//         System.err.println(String.format("Updating t=%s, v=%s", targetNode.getName(), nodeToUpdate.forcingNodes));
            if (nodeToUpdate.forcingNodeCount == 0) {
//               update(viewer, targetNode, "Default");
               update(viewer, targetNode, RESET_MUX_VALUE);
               setValid(viewer, targetNode, new Message("Pin has not been mapped to a signal\nSet to default", AnnotationModel.Severity.WARNING));
            }
            else if (nodeToUpdate.forcingNodeCount == 1) {
               update(viewer, targetNode, nodeToUpdate.forcingNodes.get(0).signalValue);
            }
            else {
               boolean      clash;
//               SelectionTag gpioTag;
//               do {
                  clash   = false;
//                  gpioTag = null;
                  for (SelectionTag tag:nodeToUpdate.forcingNodes) {
                     if (!nodeToUpdate.forcingNodes.get(0).signalValue.equals(tag.signalValue)) {
                        System.err.println(String.format("Conflict f=%s => t=%s", tag, targetNode.getName()));
                        System.err.println(nodeToUpdate.forcingNodes.get(0).signalValue + "!=" + tag.signalValue);
                        clash = true;
                     }
//                     if (tag.controllingNode.getName().matches("GPIO[A-Z]+_[0-9]+.*")) {
//                        gpioTag = tag;
//                     }
                  }
                  // Problem with changing a GPIO - it gets reset if there is a conflict!
//                  if (clash && (gpioTag != null)) {
//                     final String name = nodeToUpdate.getForcingNodeNames(); 
//                     //gpioTag.controllingNode.getDescription();
//                     PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
//                        public void run() {
//                           Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
//                           MessageBox messageBox = new MessageBox(activeShell, SWT.ICON_WARNING | SWT.OK);
//                           messageBox.setText("Warning");
//                           messageBox.setMessage("Conflict with GPIO\n" + name + "\n GPIO will be unmapped");
//                           messageBox.open();
//                        }
//                     });
//                     // Remove GPIO as forcing node
//                     System.err.println(String.format("Removing %s", gpioTag.controllingNode.getName()));
//                     update(viewer, gpioTag.controllingNode, "Disabled");
//                     nodeToUpdate.forcingNodes.remove(gpioTag);
//                  }
//               } while (clash && (gpioTag != null));
               if (clash) {
                  update(viewer, targetNode, "Default");
                  setValid(viewer, targetNode, new Message("Pin has been mapped to multiple signals: \n" + nodeToUpdate.getForcingNodeNames()));
                  for (SelectionTag forcingNode:nodeToUpdate.forcingNodes) {
                     setValid(viewer, forcingNode.controllingNode, new Message("Selection conflicts between: \n" + nodeToUpdate.getForcingNodeNames()));
                  }
               }
               else {
                  update(viewer, targetNode, nodeToUpdate.forcingNodes.get(0).signalValue);
               }
            }
         }
      }
   }

   ArrayList<SelectionTag> findActiveSelectionNodes(AnnotationModelNode node, ArrayList<SelectionTag> foundSelectionTags) {
      if (!node.isEnabled()) {
         return foundSelectionTags;
      }
      if (((node instanceof OptionHeadingModelNode) && ((OptionHeadingModelNode)node).safeGetValue()) ||
            (node instanceof HeadingModelNode) ){
         for (AnnotationModelNode child:node.getChildren()) {
            findActiveSelectionNodes(child, foundSelectionTags);
         }
         return foundSelectionTags;
      }
      if (node instanceof EnumeratedOptionModelNode) {
         EnumeratedOptionModelNode enNode = (EnumeratedOptionModelNode) node;
         ArrayList<SelectionTag> selectionTags = enNode.getSelectionTags();
         if (selectionTags != null) {
            for (SelectionTag selectionTag:selectionTags) {
                  foundSelectionTags.add(selectionTag);
            }
         }
         return foundSelectionTags;
      }
      return foundSelectionTags;
   }

}
