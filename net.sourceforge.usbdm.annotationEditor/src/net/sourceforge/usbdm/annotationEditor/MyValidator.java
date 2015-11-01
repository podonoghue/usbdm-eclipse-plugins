package net.sourceforge.usbdm.annotationEditor;

import java.util.ArrayList;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.AnnotationModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BinaryOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.EnumeratedOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.SelectionTag;

public abstract class MyValidator {
   protected      String[]               arguments;
   protected      AnnotationModelNode[]  nodes;
   private        AnnotationModel        model;
   private        Boolean                updatePending = false;

   /**
    * Called when the model has changed
    * 
    * @param viewer Viewer associated with the model
    * 
    * @throws Exception
    */
   public void validate(final TreeViewer viewer) throws Exception {
   }
   
   /**
    * Sets the model 
    * 
    * @param model
    */
   public void setModel(AnnotationModel model) {
      this.model = model;
   }

   /**
    * Locates a numeric node
    * 
    * @param name  Name of node to locate
    * @return      Node found or null
    * 
    * @throws Exception if the node has the wrong type
    */
   protected NumericOptionModelNode safeGetNumericModelNode(String name) throws Exception {
      AnnotationModelNode node = model.getModelNode(name);
      if ((node != null) && !(node instanceof NumericOptionModelNode)) {
         throw new Exception("Variable has unexpected type "+name);
      }
      return (NumericOptionModelNode)node;
   }
   
   /**
    * Locates a numeric node
    * 
    * @param name  Name of node to locate
    * @return      Node found or null
    * 
    * @throws Exception if the node is not found or has the wrong type
    */
   protected NumericOptionModelNode getNumericModelNode(String name) throws Exception {
      NumericOptionModelNode node = safeGetNumericModelNode(name);
      if (node == null) {
         throw new Exception("Unable to find variable "+name);
      }
      return node;
   }
   
   /**
    * Locates a binary node
    * 
    * @param name  Name of node to locate
    * @return      Node found or null
    * 
    * @throws Exception if the node has the wrong type
    */
   protected BinaryOptionModelNode safeGetBinaryModelNode(String name) throws Exception {
      AnnotationModelNode node = model.getModelNode(name);
      if ((node != null) && !(node instanceof BinaryOptionModelNode)) {
         throw new Exception("Variable has unexpected type "+name);
      }
      return (BinaryOptionModelNode)node;
   }
   
   /**
    * Locates a binary node
    * 
    * @param name  Name of node to locate
    * @return      Node found or null
    * 
    * @throws Exception if the node is not found or has the wrong type
    */
   protected BinaryOptionModelNode getBinaryModelNode(String name) throws Exception {
      BinaryOptionModelNode node = safeGetBinaryModelNode(name);
      if (node == null) {
         throw new Exception("Unable to find variable "+name);
      }
      return node;
   }
   
   /**
    * This returns a list of nodes in the associated model which have selection tags
    * 
    * @return List
    */
   protected ArrayList<SelectionTag> getSelectionNodes() {
      return model.getSelections();
   }
   
   /**
    * Sets the node value & refreshes the viewer
    * 
    * @param viewer        Viewer containing the node
    * @param node          Node to set error message foe
    * @param value         Node value
    * 
    * @note The node is set valid
    * @note Done delayed on the display thread
    */
   protected void update(final TreeViewer viewer, final NumericOptionModelNode node, long value) {
      final long limitedValue = node.limitedValue(value);
      if ((node.safeGetValueAsLong() == limitedValue) && 
          (node.getErrorMessage() == null)) {
         // No update needed (after constricting to target range)
         return;
      }
      Display.getDefault().asyncExec(new Runnable () {
         @Override
         public void run () {
            try {
//               System.err.println(String.format("MyValidator.Update(%s,%d)", node.getName(), value));
               node.setErrorMessage(null);
               if (node.getValueAsLong() != limitedValue) {
                  node.setValue(limitedValue);
               }
               refresh(viewer);
            } catch (Exception e) {
            }
         }
      });
   }

   /**
    * Sets the node value & refreshes the viewer
    * 
    * @param viewer        Viewer containing the node
    * @param node          Node to set error message foe
    * @param value         Node value
    * 
    * @note The node is set valid
    * @note Done delayed on the display thread
    */
   protected void update(final TreeViewer viewer, final BinaryOptionModelNode node, final boolean value) {
      if (node.safeGetValue() == value) {
         // No update needed
         return;
      }
      Display.getDefault().asyncExec(new Runnable () {
         @Override
         public void run () {
            try {
//               System.err.println(String.format("Update(%s,%d)", node.getName(), value));
               node.setErrorMessage(null);
               node.setValue(value);
               refresh(viewer);
            } catch (Exception e) {
            }
         }
      });
   }

   /**
    * Sets the node value & refreshes the viewer
    * 
    * @param viewer        Viewer containing the node
    * @param node          Node to set value for
    * @param value         Node value
    * 
    * @note The node is set valid
    * @note Done delayed on the display thread
    */
   protected void update(final TreeViewer viewer, final EnumeratedOptionModelNode node, final String value) {
//      System.err.println(String.format("update(%s,%s)", node.getName(), value));
      int index = node.getEnumIndex(value);
      if (index>=0) {
         update(viewer, node, node.getEnumerationValues().get(index).getValue());
      }
   }

   /**
    * Clears the node error message & refreshes the viewer
    * 
    * @param viewer        Viewer containing the node
    * @param node          Node to set error message for
    * @param errorMessage  Error message.  A null value indicates no error
    * 
    * @note Done delayed on the display thread
    */
   protected void setValid(final TreeViewer viewer, final NumericOptionModelNode node) {
      Display.getDefault().asyncExec(new Runnable () {
         @Override
         public void run () {
            try {
               if (node.getErrorMessage() != null) {
                  node.setErrorMessage(null);
                  refresh(viewer);
               }
            } catch (Exception e) {
            }
         }
      });
   }

   /**
    * Sets the node error message & refreshes the viewer
    * 
    * @param viewer        Viewer containing the node
    * @param node          Node to set error message for
    * @param errorMessage  Error message.  A null value indicates no error
    * 
    * @note Done delayed on the display thread
    */
   protected void setValid(final TreeViewer viewer, final NumericOptionModelNode node, String errorMessage ) {
      
      final Message message = (errorMessage==null)?null:new Message(errorMessage);
      
      Display.getDefault().asyncExec(new Runnable () {
         @Override
         public void run () {
            try {
               if ((node.getErrorMessage() != null) || (message != null)) {
                  node.setErrorMessage(message);
                  refresh(viewer);
               }
            } catch (Exception e) {
            }
         }
      });
   }

   /**
    * Sets the node error message & refreshes the viewer
    * 
    * @param viewer        Viewer containing the node
    * @param node          Node to set error message for
    * @param errorMessage  Error message.  A null value indicates no error
    * 
    * @note Done delayed on the display thread
    */
   protected void setValid(final TreeViewer viewer, final NumericOptionModelNode node, final Message errorMessage ) {
      Display.getDefault().asyncExec(new Runnable () {
         @Override
         public void run () {
            try {
               if ((node.getErrorMessage() != null) || (errorMessage != null)) {
                  node.setErrorMessage(errorMessage);
                  refresh(viewer);
               }
            } catch (Exception e) {
            }
         }
      });
   }

   /**
    * Refreshes the viewer
    * 
    * @param viewer Viewer to refresh
    * 
    * @note Done delayed on the display thread
    */
   private synchronized void refresh(final TreeViewer viewer) {
      synchronized (updatePending) {
         if (updatePending) {
            return;
         }
         updatePending = true;
      }
      Display.getDefault().asyncExec(new Runnable () {
         @Override
         public void run () {
            if (!viewer.getControl().isDisposed()) {
               viewer.refresh();
               synchronized (updatePending) {
                  updatePending = false;
               }
            }
         }
      });
   }


}

