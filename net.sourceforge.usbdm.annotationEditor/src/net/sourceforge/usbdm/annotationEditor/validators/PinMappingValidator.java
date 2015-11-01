package net.sourceforge.usbdm.annotationEditor.validators;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BinaryOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.EnumeratedOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
//import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BinaryOptionModelNode;
//import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.SelectionTag;
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

      boolean mapByPin      = map_by_pinNode.safeGetValue();
      
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
      
      ArrayList<SelectionTag> selectionNodes = getSelectionNodes();
      
      // Collect target nodes to update
//      System.err.println("Collecting Target Nodes");
      for (SelectionTag tag:selectionNodes) {
//         System.err.println("Processing selection node = " + tag.toString());
//         System.err.println(String.format("getValue() = %d, selectionValue= %d", tag.controllingNode.safeGetValueAsLong(), tag.selectionValue));

         if (tag.controllingNode.isEnabled()) {
            NumericOptionModelNode targetSignalNode = getNumericModelNode(tag.signalName);
            if (targetSignalNode == null) {
               setValid(viewer, tag.controllingNode, "Can't find referenced selection node "+tag.signalName);
               System.err.println("PinMappingValidator.validate() Can't find referenced selection node "+tag.signalName);
               continue;
            }
            if (!(targetSignalNode instanceof EnumeratedOptionModelNode)) {
               setValid(viewer, tag.controllingNode, "Referenced selection node "+tag.signalName+ " has wrong type"+ ", class = " + targetSignalNode.getClass());
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
      }
      
      // Update target nodes
//      System.err.println("Updating Target Nodes");
      for (EnumeratedOptionModelNode targetNode:nodesToUpdate.keySet()) {
         NodeToUpdate nodeToUpdate = nodesToUpdate.get(targetNode);
//         System.err.println(String.format("Updating %s", nodeToUpdate.toString()));
//         System.err.println(String.format("Updating t=%s, v=%s", targetNode.getName(), nodeToUpdate.forcingNodes));
         if (nodeToUpdate.forcingNodeCount == 0) {
            update(viewer, targetNode, "Disabled");
            setValid(viewer, targetNode, "Signal has not been mapped to a pin");
         }
         else if (nodeToUpdate.forcingNodeCount == 1) {
            update(viewer, targetNode, nodeToUpdate.forcingNodes.get(0).signalValue);
         }
         else {
            update(viewer, targetNode, "Disabled");
            setValid(viewer, targetNode, "Signal has been mapped to multiple pins: \n" + nodeToUpdate.getForcingNodeNames());
         }
      }
      
   }
   
}
