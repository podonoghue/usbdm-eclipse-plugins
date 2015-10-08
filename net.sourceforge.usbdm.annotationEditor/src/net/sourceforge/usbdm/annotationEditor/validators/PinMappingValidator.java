package net.sourceforge.usbdm.annotationEditor.validators;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.viewers.TreeViewer;

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
      /** Value to force and/or farcing node */
      String                    forcingNodes;
      /** Count of forcing nodes.  If greater than 1 then the target needs to be set invalid*/
      int                       forcingNodeCount;
      
      /**
       * Create forcing node
       * 
       * @param target        Node being forced i.e. target
       * @param forcingNode   Name of node forcing
       */
      NodeToUpdate(EnumeratedOptionModelNode target) {
         this.target       = target;
         this.forcingNodes = null;
         forcingNodeCount  = 0;
      }

      /**
       * Add forcing node - this will mean that the target needs to be set invalid.
       * 
       * @param forcingNode Name of node forcing
       */
      void addForcingNode (String forcingNode) {
         if (forcingNodeCount++ == 0) {
            this.forcingNodes = forcingNode;
         }
         else {
            this.forcingNodes += ", "+forcingNode;
         }
      }
      
      public String toString() {
         return String.format("NodeToUpdate(t=%s, f=%s)", target.getName(), forcingNodes);
      }
   };
   
   @Override
   public void validate(TreeViewer viewer) throws Exception {
//      System.err.println("PinMappingValidator.validate()");
      super.validate(viewer);
      HashMap<EnumeratedOptionModelNode, NodeToUpdate> nodesToUpdate = new HashMap<EnumeratedOptionModelNode, NodeToUpdate>();
      
      ArrayList<SelectionTag> selectionNodes = getSelectionNodes();
      
      // Collect target nodes to update
      for (SelectionTag tag:selectionNodes) {
//         System.err.println("Processing selection node = " + tag.toString());
//         System.err.println(String.format("getValue() = %d, selectionValue= %d", tag.controllingNode.safeGetValueAsLong(), tag.selectionValue));

         NumericOptionModelNode targetNode = getNumericModelNode(tag.signalName);
         if (targetNode == null) {
            setValid(viewer, tag.controllingNode, "Can't find referenced selection node "+tag.signalName);
            System.err.println("PinMappingValidator.validate() Can't find referenced selection node "+tag.signalName);
            continue;
         }
         if (!(targetNode instanceof EnumeratedOptionModelNode)) {
            setValid(viewer, tag.controllingNode, "Referenced selection node "+tag.signalName+ " has wrong type"+ ", class = " + targetNode.getClass());
            System.err.println("PinMappingValidator.validate() Incorrect node class for node " + targetNode.getName() + ", class = " + targetNode.getClass());
            continue;
         }
         EnumeratedOptionModelNode target = (EnumeratedOptionModelNode) targetNode;
         NodeToUpdate nodeToUpdate = nodesToUpdate.get(target);
         if (nodeToUpdate == null) {
            nodeToUpdate = new NodeToUpdate(target);
            nodesToUpdate.put(target, nodeToUpdate);
         }
         if (tag.controllingNode.safeGetValueAsLong() != tag.selectionValue) {
//            System.err.println("Peripheral signal not mapped to this pin");
         }
         else {
            // Peripheral signal has been mapped to this pin
//            System.err.println("Peripheral signal has been mapped to this pin");
            nodeToUpdate.addForcingNode(tag.controllingNode.getName());
//            System.err.println("Tag = " + tag.toString() + " applied to " + targetNode.getName());
            }
      }
      // Update target nodes
      for (EnumeratedOptionModelNode targetNode:nodesToUpdate.keySet()) {
         NodeToUpdate nodeToUpdate = nodesToUpdate.get(targetNode);
//         System.err.println(String.format("Updating %s", nodeToUpdate.toString()));
//         System.err.println(String.format("Updating t=%s, v=%s", targetNode.getName(), nodeToUpdate.forcingNodes));
         if (nodeToUpdate.forcingNodeCount == 0) {
            update(viewer, targetNode, "Disabled");
            setValid(viewer, targetNode, "Signal has not been mapped to a pin");
         }
         else if (nodeToUpdate.forcingNodeCount == 1) {
            update(viewer, targetNode, nodeToUpdate.forcingNodes);
         }
         else {
            update(viewer, targetNode, "Disabled");
            setValid(viewer, targetNode, "Signal has been mapped to multiple pins [" + nodeToUpdate.forcingNodes + "]");
         }
      }
   }
   
}
