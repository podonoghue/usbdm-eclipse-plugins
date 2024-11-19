package net.sourceforge.usbdm.deviceEditor.graphicModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;

import net.sourceforge.usbdm.deviceEditor.graphicModel.Graphic.Type;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class ClockSelectionFigure {
   private Graphic[]                     fObjects            = null;
   private GraphicConnector[]            fConnectors         = null;
   private ArrayList<InitialInformation> initialInformation = new ArrayList<InitialInformation>();
   private final VariableProvider fProvider;

   /// Clock configuration index for this figure
   private final int fClockSelection;
   
   public ClockSelectionFigure(VariableProvider provider, int clockSelection) {
      fProvider = provider;
      fClockSelection = clockSelection;
   }
   
   private class InitialInformation {
      private String  fId;
      private String  fName;
      private String  fVarKey;
      private Type    fType;
      private String  fParams;
      private Boolean fEdit;
      private int     originX;
      private int     originY;
      
      @Override
      public
      String toString() {
         return "fId="+fId+", fVarKey="+fVarKey+", fType="+fType+", fParams="+fParams;
      }
      
      InitialInformation(int x, int y, String id, String name, String varKey, Type type, Boolean edit, String params) {
         fId     = (id!=null)?id:"";
         fName   = name;
         fVarKey = varKey;
         fType   = type;
         fParams = params;
         fEdit   = edit;
         originX = x;
         originY = y;
      }
      
      Graphic constructGraphic(VariableProvider variableProvider) throws Exception {
         Variable var = null;
         if ((fVarKey != null) && !fVarKey.isBlank()) {
            var = variableProvider.getVariable(fVarKey);
         }
         Boolean canEdit = fEdit || ((var != null) && (!var.isDerived()));
         switch(fType) {
         case reference:
            return GraphicReference.create(originX, originY, fId, fParams, canEdit, var);
            
         case annotation:
            return GraphicAnnotation.create(originX, originY, fId, fParams, canEdit, var);
            
         case label:
            return GraphicLabel.create(originX, originY, fId, fName, fParams, canEdit, var);
            
         case box:
            return GraphicBox.create(originX, originY, fId, fParams);
            
         case variableBox:
         case choice:
            return GraphicVariable.create(originX, originY, fId, fParams, canEdit, var);
            
         case mux:
            return GraphicMuxVariable.create(originX, originY, fId, fParams, canEdit, var);

         case node:
            return GraphicNode.create(originX, originY, fId, fParams, canEdit, var, variableProvider);
            
         case junction:
            return GraphicJunction.create(originX, originY, fId, fParams, canEdit, var);
            
         case polygon:
            return GraphicPolygon.create(originX, originY, fId, fParams, canEdit, var);
         case connector:
         case group:
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
   
   public void report() {
      StringBuilder sb = new StringBuilder();
      sb.append("<graphic>\n");
      for (Graphic info:getObjects()) {
         sb.append("   ");
         sb.append(info.report());
         sb.append("\n");
      };
      sb.append("</graphic>\n");
      System.err.println(sb.toString());
   }
   
   public void reportOld() {

      StringBuilder sb = new StringBuilder();
      sb.append("<graphic>\n");
      for (InitialInformation info:initialInformation) {
         sb.append("<graphicItem ");
         sb.append(String.format("id=\"%s\" ", info.fId));
         sb.append(String.format("var=\"%s\" ", info.fVarKey));
         sb.append(String.format("type=\"%s\" ", info.fType));
         sb.append(String.format("type=\"%s\" ", info.fParams));
         //               sb.append(String.format("type=\"%s\"", info.));
         sb.append("/>\n");
      };
      sb.append("</graphic>\n");
      System.err.println(sb.toString());
   }

   /**
    * Add information to construct a graphic
    * @param y
    * @param x
    * 
    * @param id      Unique ID to identify object in figure
    * @param varKey  Key for variable associated with graphic (this should be absolute key)
    * @param params  Parameters used to construct graphic
    * @param params2`
    */
   public void add(int x, int y, String id, String name, String varKey, String type, String edit, String params) {
      initialInformation.add(new InitialInformation(x, y, id, name, varKey, Graphic.Type.valueOf(type), Boolean.valueOf(edit), params));
   }
   
   /**
    * Get list of graphic objects
    * 
    * @param variableProvider  Provider to obtain needed variable from
    * 
    * @return  List of graphic objects
    * 
    * @throws Exception
    */
   public void instantiateGraphics(VariableProvider variableProvider) throws Exception {

      if (fObjects != null) {
         // Already done
         return;
      }
      
      // Used for connector end-point lookup
      Hashtable<String, Graphic> graphicTable =  new Hashtable<String, Graphic>();

      ArrayList<Graphic>          graphics   = new ArrayList<Graphic>();
      ArrayList<GraphicConnector> connectors = new ArrayList<GraphicConnector>();

      // Construct objects apart from connectors and non-graphic
      for (InitialInformation info:initialInformation) {
         try {
            Graphic graphic = info.constructGraphic(variableProvider);
            if (graphic != null) {
               graphics.add(graphic);
               String[] ids = info.fId.split(",");
               graphicTable.put(ids[0], graphic);
            }
         } catch (Exception e) {
            System.err.println("Failed to instantiate "+info);
            e.printStackTrace();
         }
      };
      // Construct connectors
      for (InitialInformation info:initialInformation) {
         try {
            if (info.fType == Type.connector) {
               Graphic graphic = info.constructGraphic(graphicTable, variableProvider);
               if (graphic == null) {
                  System.err.println("Opps");
               }
               connectors.add((GraphicConnector)graphic);
               String[] ids = info.fId.split(",");
               graphicTable.put(ids[0], graphic);
            }
            // Ignore non-graphic
         } catch (Exception e) {
            System.err.println("Failed to instantiate "+info);
            e.printStackTrace();
         }
      };
      // Discard original info
      initialInformation = null;

      fObjects    = graphics.toArray(new Graphic[graphics.size()]);
      fConnectors = connectors.toArray(new GraphicConnector[connectors.size()]);

      Comparator<Graphic> comp = new Comparator<Graphic>() {
         @Override
         public int compare(Graphic o1, Graphic o2) {

            int res = o1.getDrawPriority()-o2.getDrawPriority();
            if (res != 0) {
               return res;
            }
            // Order by x co-ord
            return (o1.x - o2.x);
         }
      };
      Arrays.sort(fObjects, comp);
      Arrays.sort(fConnectors, comp);
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
//   public Graphic[] getGraphics() throws Exception {
//      return getObjects();
//   }

   // For debug
   int size = 0;
   
   /**
    * For debug
    * 
    * @param variableGraphic
    * @return
    */
   public Graphic add(Graphic graphic) {
      if (fObjects == null) {
         fObjects = new Graphic[100];
      }
      fObjects[size++] = graphic;
      return graphic;
   }

   public Graphic[] getObjects() {
      return fObjects;
   }

   public GraphicConnector[] getConnectors() {
      return fConnectors;
   }

   /**
    * Returns the variable provider associated with this figure
    * 
    * @return Variable provider
    */
   public VariableProvider getProvider() {
      return fProvider;
   }

   /**
    *  Get clock configuration index for this figure
    * 
    * @return Clock configuration index for this figure
    */
   public int getClockSelection() {
      return fClockSelection;
   }
   
}