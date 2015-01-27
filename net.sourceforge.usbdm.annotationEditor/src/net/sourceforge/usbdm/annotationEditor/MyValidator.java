package net.sourceforge.usbdm.annotationEditor;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.AnnotationModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BinaryOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;

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
    * Sets the node value & refreshes the viewer
    * 
    * @param viewer        Viewer containing the node
    * @param node          Node to set error message foe
    * @param value         Node value
    * 
    * @note The node is set valid
    * @note Done delayed on the display thread
    */
   protected void update(final TreeViewer viewer, final NumericOptionModelNode node, final long value) {
      if (node.getValueAsLong() == node.limitedValue(value)) {
         // No update needed (after constricting to target range)
         return;
      }
      Display.getDefault().asyncExec(new Runnable () {
         @Override
         public void run () {
            try {
//               System.err.println(String.format("MyValidator.Update(%s,%d)", node.getName(), value));
               node.setErrorMessage(null);
               node.setValue(value);
               refresh(viewer);
            } catch (Exception e) {
            }
         }
      });
   }

   protected void update(final TreeViewer viewer, final BinaryOptionModelNode node, final boolean value) {
      if (node.safeGetValue() == value) {
         // No update needed
         return;
      }
      Display.getDefault().asyncExec(new Runnable () {
         @Override
         public void run () {
            try {
               System.err.println(String.format("Update(%s,%d)", node.getName(), value));
               node.setErrorMessage(null);
               node.setValue(value);
               refresh(viewer);
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
   protected void setValid(final TreeViewer viewer, final NumericOptionModelNode node, final String errorMessage ) {
      Display.getDefault().asyncExec(new Runnable () {
         @Override
         public void run () {
            try {
               node.setErrorMessage(errorMessage);
               refresh(viewer);
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

