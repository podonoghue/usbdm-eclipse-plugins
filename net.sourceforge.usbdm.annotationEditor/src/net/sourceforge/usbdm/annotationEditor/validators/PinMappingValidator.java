package net.sourceforge.usbdm.annotationEditor.validators;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.viewers.TreeViewer;

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
//      ArrayList<SelectionTag> inactiveSelectionNodes = null;
      if (mapByPin) {
         activeSelectionNodes   = pinSelectionNodes;
//         inactiveSelectionNodes = functionSelectionNodes;
      }
      else {
         activeSelectionNodes   = functionSelectionNodes;
//         inactiveSelectionNodes = pinSelectionNodes;
      }

//      // Clear messages inactive nodes
//      for (SelectionTag tag:activeSelectionNodes) {
//         setValid(viewer, tag.controllingNode);
//      }
      
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
         //            else {
         //             System.err.println("Peripheral signal not mapped to this pin");
         //            }
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
               update(viewer, targetNode, "Disabled");
               setValid(viewer, targetNode, new Message("Signal has been mapped to multiple pins: \n" + nodeToUpdate.getForcingNodeNames()));
            }
         }
      }
      else {
         for (EnumeratedOptionModelNode targetNode:nodesToUpdate.keySet()) {
            NodeToUpdate nodeToUpdate = nodesToUpdate.get(targetNode);
            //         System.err.println(String.format("Updating %s", nodeToUpdate.toString()));
            //         System.err.println(String.format("Updating t=%s, v=%s", targetNode.getName(), nodeToUpdate.forcingNodes));
            if (nodeToUpdate.forcingNodeCount == 0) {
               update(viewer, targetNode, "Default");
               setValid(viewer, targetNode, new Message("pin has not been mapped to a signal", AnnotationModel.Severity.WARNING));
            }
            else if (nodeToUpdate.forcingNodeCount == 1) {
               update(viewer, targetNode, nodeToUpdate.forcingNodes.get(0).signalValue);
            }
            else {
               update(viewer, targetNode, "Default");
               setValid(viewer, targetNode, new Message("pin has been mapped to multiple signals: \n" + nodeToUpdate.getForcingNodeNames()));
            }
         }
      }
   }

   ArrayList<SelectionTag> findActiveSelectionNodes(AnnotationModelNode node, ArrayList<SelectionTag> foundSelectionTags) {
      //      System.err.println("findActiveSelectionNodes() - node = " + node.getName());
      if (!node.isEnabled()) {
         //         System.err.println("findActiveSelectionNodes() - node = " + node.getName() + " not enabled");
         return foundSelectionTags;
      }
      if (((node instanceof OptionHeadingModelNode) && ((OptionHeadingModelNode)node).safeGetValue()) ||
            (node instanceof HeadingModelNode) ){
         //         System.err.println("findActiveSelectionNodes() - node = " + node.getName() + " searching children");
         // Search children if enabled and selected
         for (AnnotationModelNode child:node.getChildren()) {
            findActiveSelectionNodes(child, foundSelectionTags);
         }
         return foundSelectionTags;
      }
      if (node instanceof EnumeratedOptionModelNode) {
         //         System.err.println("findActiveSelectionNodes() - EnumeratedOptionModelNode node = " + node.getName());
         EnumeratedOptionModelNode enNode = (EnumeratedOptionModelNode) node;
         ArrayList<SelectionTag> selectionTags = enNode.getSelectionTags();
         if (selectionTags != null) {
            for (SelectionTag selectionTag:selectionTags) {
//               if (selectionTag.selectionValue == selectionTag.controllingNode.safeGetValueAsLong()) {
                  //                  System.err.println("findActiveSelectionNodes() - selectionTag = " + node.getName() + " added");
                  foundSelectionTags.add(selectionTag);
//               }
//               else {
                  //                  System.err.println("findActiveSelectionNodes() - selectionTag = " + node.getName() + " skipped");
//               }
            }
         }
         //         else {
         //            System.err.println("findActiveSelectionNodes() - EnumeratedOptionModelNode node = " + node.getName() +" No selectionTags");
         //         }
         return foundSelectionTags;
      }
      //      System.err.println("findActiveSelectionNodes() - not processing = " + node.getName());
      return foundSelectionTags;
   }

}
