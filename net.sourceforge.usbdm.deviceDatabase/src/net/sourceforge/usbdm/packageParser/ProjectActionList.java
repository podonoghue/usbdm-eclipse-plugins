package net.sourceforge.usbdm.packageParser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Visitor.Result;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Visitor.Result.Status;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

public class ProjectActionList extends ProjectAction {

   /** Indicates if the action list should be done once only if encountered multiple times */
   private boolean fDoOnceOnly = false;

   public static class Value {
      private Object returnValue;

      public Value(Object initialValue) {
         returnValue = initialValue;
      }
      /**
       * @return the returnValue
       */
      public Object getReturnValue() {
         return returnValue;
      }
      /**
       * @param returnValue the returnValue to set
       */
      public void setReturnValue(Object returnValue) {
         this.returnValue = returnValue;
      }
   }
   
   public static interface Visitor {
      /**
       * Indicates to continue processing
       */
      Result CONTINUE = new Result(Status.CONTINUE);
      /**
       * Indicates to stop processing this node <br>
       * Used to 'prune' subtrees
       */
      Result PRUNE    = new Result(Status.PRUNE);

      /**
       * Class used to control visitation
       * 
       * @author podonoghue
       *
       */
      public static class Result {
         public enum Status {
            /**
             * Indicates to continue processing
             */
            CONTINUE, 
            /**
             * Indicates to stop processing this node <br>
             * Used to 'prune' subtrees
             */
            PRUNE, 
            /**
             * Indicates processing should stop and throw an exception <br>
             * Used for unexpected happenings
             */
            EXCEPTION};
         
         private final Status fStatus;
         private final Exception fException;
         
         private Result(final Status status, final Exception e) {
            fStatus    = status;
            fException = e;
         }
         
         public Result(final Status status) {
            this(status, null);
         }
         
         public Result(final Exception e) {
            this(Status.EXCEPTION, e);
         }
         
         public Result(final Result other) {
            this(other.fStatus, other.fException);
         }
         
         public String getMessage() { 
            return fException.getMessage();
         }
         public Status getStatus() { 
            return fStatus;
         }

         public Exception getException() {
            if (fException == null) {
               return new Exception("Unexpected Result.getException()");
            }
            return fException;
         }
         public String toString() {
            switch (fStatus) {
            case CONTINUE:  return "CONTINUE";
            case EXCEPTION: return "EXCEPTION";
            case PRUNE:     return "PRUNE";
            }
            return "ILLEGAL";
         }
      };
      /**
       * Method used to Visit the action nodes
       * 
       * @param action The ProjectAction to visit
       * @param result Object available to hold result , status etc.
       * 
       * @return Result object controlling how the visitation proceeds
       */
      Result applyTo(ProjectAction action, Value result, IProgressMonitor monitor);
   } 

   private ApplyWhenCondition               fApplyWhenCondition   = ApplyWhenCondition.trueCondition;
   private ArrayList<ProjectAction>         fProjectActionList    = new ArrayList<ProjectAction>();

   public ProjectActionList(String id) {
      super(id);
   }

   /**
    * @param condition the condition to set
    * @throws Exception 
    */
   public void setApplyWhenCondition(ApplyWhenCondition condition) throws Exception {
      if (fApplyWhenCondition != ApplyWhenCondition.trueCondition) {
         throw new Exception("ProjectActionList already has ApplyWhenCondition");
      }
      this.fApplyWhenCondition = condition;
   }

   /**
    * @return the condition
    */
   public ApplyWhenCondition getApplyWhenCondition() {
      return fApplyWhenCondition;
   }

   public String toString(String root) {
      StringBuffer buffer = new StringBuffer();
      Iterator<ProjectAction> it = fProjectActionList.iterator();
      while(it.hasNext()) {
         ProjectAction action = it.next();
         buffer.append(action.toString());
      }
      return buffer.toString();     
   }
   
   public boolean appliesTo(Device device, Map<String, String> variableMap) {
      return fApplyWhenCondition.appliesTo(device, variableMap);
   }
   
   public boolean applies(Map<String, String> variableMap) {
      return fApplyWhenCondition.applies(variableMap);
   }
   
   public void add(int arg0, ProjectAction projectAction) {
      projectAction.setOwner(this);
      fProjectActionList.add(arg0, projectAction);
   }
   
   public boolean add(ProjectAction projectAction) {
      projectAction.setOwner(this);
      return fProjectActionList.add(projectAction);
   }

   /**
    * @return the projectActionList
    */
   public ArrayList<ProjectAction> getProjectActionList() {
      return fProjectActionList;
   }

   /**
    * Add action to list
    * 
    * @param action
    */
   public void addProjectAction(ProjectAction action) {
     fProjectActionList.add(action);
   }
   
   /**
    * Visit each action recursively
    * Uses NullProgressMonitor
    * 
    * @param  visitor  Method to apply
    * @param  value    Object to hold progress/return value
    * 
    * @throws Exception 
    */
   public Result visit(Visitor visitor, Value value) {
      return visit(visitor, value, new NullProgressMonitor());
   }
   
   /**
    * Visit each action recursively
    * 
    * @param  visitor  Method to apply
    * @param  value    Object to hold progress/return value
    * @param  monitor  Monitor to indicate progress
    * 
    * @throws Exception 
    */
   public Result visit(Visitor visitor, Value value, IProgressMonitor progressMonitor) {
      SubMonitor monitor = SubMonitor.convert(progressMonitor);
      monitor.beginTask("Visiting", (1+fProjectActionList.size()));

      Result control = visitor.applyTo(this, value, monitor.newChild(1));
      switch (control.getStatus()) {
      case EXCEPTION     : 
         // No more actions applied
         return control;
      case PRUNE     : 
         // No children visited
         return new Result(Status.CONTINUE);
      case CONTINUE  : 
         // Keep going
         break;
      }
      for (ProjectAction action:fProjectActionList) {
         if (action instanceof ProjectActionList) {
            control = ((ProjectActionList)action).visit(visitor, value, monitor.newChild(1));
         }
         else {
            control = visitor.applyTo(action, value, monitor.newChild(1));
         }
         switch (control.getStatus()) {
         case EXCEPTION     : 
            // No more actions applied
            return control;
         case PRUNE     : 
         case CONTINUE  : 
            // Keep going
            break;
         }
      }
      return new Result(Status.CONTINUE);
   }

   @Override
   public String toString() {
      return String.format("ProjectActionList[ID=%s, #children=%d]", getId(), fProjectActionList.size());
   }

   /**
    * Indicates if the action list should be done once only if encountered multiple times
    *
    * @param doOnceOnly
    */
   public void setDoOnceOnly(boolean doOnceOnly) {
      fDoOnceOnly = doOnceOnly;
   }

   /**
    * Indicates if the action list should be done once only if encountered multiple times
    */
   public boolean isDoOnceOnly() {
      return fDoOnceOnly;
   }

}