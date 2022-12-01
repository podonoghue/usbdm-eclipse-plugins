package net.sourceforge.usbdm.deviceEditor.graphicModel;

import java.util.ArrayList;
import java.util.Hashtable;

import net.sourceforge.usbdm.deviceEditor.graphicModel.Graphic.Type;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class ClockSelectionFigure {
   Graphic[]                     objects            = null;
   ArrayList<InitialInformation> initialInformation = new ArrayList<InitialInformation>();

   private class InitialInformation {
      private String  fId;
      private String  fVarKey;
      private Type    fType;
      private String  fParams;
      private Boolean fEdit;
      
      InitialInformation(String id, String varKey, Type type, Boolean edit, String params) {
         fId     = id;
         fVarKey = varKey;
         fType   = type;
         fParams = params;
         fEdit   = edit;
      }
      
      Graphic constructGraphic(VariableProvider variableProvider) throws Exception {
         Variable var = null;
         if ((fVarKey != null) && !fVarKey.isBlank()) {
            var = variableProvider.getVariable(fVarKey);
         }
         Boolean canEdit = fEdit || ((var != null) && (!var.isDerived()));
         switch(fType) {
         case reference:
            return GraphicReference.create(fId, fParams, canEdit, var);
            
         case annotation:
            return GraphicAnnotation.create(fId, fParams, canEdit, var);
            
         case label:
            return GraphicLabel.create(fId, fParams, canEdit, var);
            
         case box:
            return GraphicBox.create(fId, fParams);
            
         case variableBox:
            return GraphicVariable.create(fId, fParams, canEdit, var);
            
         case choice:
            return GraphicVariable.create(fId, fParams, canEdit, var);
            
         case mux:
            return GraphicMuxVariable.create(fId, fParams, canEdit, var);

         case node:
            return GraphicNode.create(fId, fParams, canEdit, var);
            
         default:
            return null;
         }
      }

      public Graphic constructGraphic(Hashtable<String, Graphic> graphicTable, VariableProvider variableProvider) throws Exception {
         Variable var = null;
         if ((fVarKey != null) && !fVarKey.isBlank()) {
            var = variableProvider.getVariable(fVarKey);
         }
         return GraphicConnector.create(graphicTable, fId, fParams, var);
      }
   }
   
   /**
    * Add information to construct a graphic
    * 
    * @param id      Unique ID to identify object in figure
    * @param varKey  Key for variable associated with graphic (this should be absolute key)
    * @param params  Parameters used to construct graphic
    * @param params2
    */
   public void add(String id, String varKey, String type, String edit, String params) {
      initialInformation.add(new InitialInformation(id, varKey, Graphic.Type.valueOf(type), Boolean.valueOf(edit), params));
   }
   
   /**
    * Get list if graphic objects
    * 
    * @param variableProvider  Provider to obtain needed variable from
    * 
    * @return  List of graphic objects
    * 
    * @throws Exception
    */
   public void instantiateGraphics(VariableProvider variableProvider) throws Exception {

      Hashtable<String, Graphic> graphicTable =  new Hashtable<String, Graphic>();

      if ((objects == null)) {
         objects = new Graphic[initialInformation.size()];
         int index = 0;
         // Construct objects apart from connectors
         for (InitialInformation info:initialInformation) {
            try {
               Graphic graphic = info.constructGraphic(variableProvider);
               if (graphic != null) {
                  objects[index++] = graphic;
                  String[] ids = info.fId.split(",");
                  graphicTable.put(ids[0], graphic);
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
         };
         // Construct connectors
         for (InitialInformation info:initialInformation) {
            try {
               if (info.fType == Type.connector) {
                  Graphic graphic = info.constructGraphic(graphicTable, variableProvider);
                  objects[index++] = graphic;
                  String[] ids = info.fId.split(",");
                  graphicTable.put(ids[0], graphic);
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
         };
         initialInformation = null;
      }
   }

   /**
    * Get list if graphic objects
    * 
    * @param variableProvider  Provider to obtain needed variable from
    * 
    * @return  List of graphic objects
    * 
    * @throws Exception
    */
   public Graphic[] getGraphics() throws Exception {
      return objects;
   }

   int size = 0;
   
   /**
    * For debug
    * 
    * @param variableGraphic
    * @return
    */
   public Graphic add(Graphic graphic) {
      if (objects == null) {
         objects = new Graphic[100];
      }
      objects[size++] = graphic;
      return graphic;
   }
   
}