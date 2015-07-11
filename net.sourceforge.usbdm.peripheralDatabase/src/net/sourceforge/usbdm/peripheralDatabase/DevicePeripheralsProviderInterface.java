package net.sourceforge.usbdm.peripheralDatabase;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;

public class DevicePeripheralsProviderInterface {
   
   private static final String PeripheralDescriptionProvider_ID = 
         "net.sourceforge.usbdm.peripherals.DevicePeripheralsProvider";
   
   // List of IDs of providers found
   private ArrayList<String> providerIDs                = new ArrayList<String>();
   // Maps providerID to provider name
   private HashMap<String, String> providerNames        = new HashMap<String, String>();
   // Maps providerID to provider description
   private HashMap<String, String> providerDescriptions = new HashMap<String, String>();

   /**
    * Constructor
    * 
    * Creates list of providers on instantiation
    */
   public DevicePeripheralsProviderInterface() {
      IExtensionRegistry registry = Platform.getExtensionRegistry();
      if (registry == null) {
         return;
      }
      IConfigurationElement[] config = registry.getConfigurationElementsFor(PeripheralDescriptionProvider_ID);

      IApplyTo listProviderIds = new IApplyTo(config) {
         @Override
         public void run(IPeripheralDescriptionProvider provider) {
            providerIDs.add(provider.getId());
            providerNames.put(provider.getId(), provider.getName());
            providerDescriptions.put(provider.getId(), provider.getDescription());
         }
      };
      listProviderIds.foreach();
   }
   
   /**
    * Class to iterate IPeripheralDescriptionProvider plug-ins
    * 
    * @author podonoghue
    */
   static abstract class IApplyTo {
      protected IConfigurationElement[] config = null;
      
      /**
       * Constructor
       * 
       * @param config Configurations to iterate
       */
      protected IApplyTo(IConfigurationElement[] config) {
         this.config = config;
      }
      /**
       * Applies run to each provider found
       */
      public void foreach() {
         try {
            for (IConfigurationElement e : config) {
//               System.err.println("Evaluating extension");
               final Object o = e.createExecutableExtension("class");
               if (o instanceof IPeripheralDescriptionProvider) {
                  ISafeRunnable runnable = new ISafeRunnable() {
                     @Override
                     public void handleException(Throwable e) {
                        System.err.println("Exception in client");
                     }

                     @Override
                     public void run() throws Exception {
                        IApplyTo.this.run((IPeripheralDescriptionProvider) o);
                     }
                  };
                  SafeRunner.run(runnable);
               }
            }
         } catch (CoreException ex) {
            System.err.println(ex.getMessage());
         }
      }
      /**
       * Function to apply to each instance found
       * 
       * @param provider Provider found
       */
      public abstract void run(IPeripheralDescriptionProvider provider);
   }
   /**
    * Used to obtain names of providers
    * 
    * @author podonoghue
    */
   class ListProviderNames extends IApplyTo {
      ListProviderNames(IConfigurationElement[] config) {
         super(config);
      }
      @Override
      public void run(IPeripheralDescriptionProvider provider) {
         System.err.println("Found plugin for " + provider.getName());
         System.err.println("Found plugin for " + provider.getDeviceNames());
      }
   }
 
   /**
    * Get list of provider IDs
    * 
    * @return List of IDs
    */
   public ArrayList<String> getProviderIDs() {
      return providerIDs;
   }
   
   /**
    * Get device peripheral description
    * 
    * @param svdIdentifier
    * @return
    */
   public DevicePeripherals getDevice(SVDIdentifier svdIdentifier) {
      System.err.println("DevicePeripherals.getDevice() looking for device = " + svdIdentifier);
      if (svdIdentifier == null) {
         return null;
      }
      if (svdIdentifier.getPath() != null) {
         // External file - Create default factory
         DevicePeripheralsFactory devicePeripheralsFactory = new DevicePeripheralsFactory();
         return devicePeripheralsFactory.getDevicePeripherals(svdIdentifier.getPath());
      }
      else {
         IPeripheralDescriptionProvider provider = getProvider(svdIdentifier.getproviderId());
         if (provider == null) {
            return null;
         }
         return provider.getDevicePeripherals(svdIdentifier.getDeviceName());
      }
   }
   /**
    * Get provider from ID
    * 
    * @param providerId
    * @return
    */
   public IPeripheralDescriptionProvider getProvider(final String providerId) {
      System.err.println("IPeripheralDescriptionProvider.getProvider() looking for providerId = " + providerId);

      IExtensionRegistry registry = Platform.getExtensionRegistry();
      if (registry == null) {
         return null;
      }
      IConfigurationElement[] config = registry.getConfigurationElementsFor(PeripheralDescriptionProvider_ID);

      // Get provider
      final ArrayList<IPeripheralDescriptionProvider> peripheralDescriptionProvider = 
            new ArrayList<IPeripheralDescriptionProvider>();
      IApplyTo listProviderNames = new IApplyTo(config) {
         @Override
         public void run(IPeripheralDescriptionProvider provider) {
            if (provider.getId().equals(providerId)) {
               peripheralDescriptionProvider.add(provider);
            }
         }
      };
      listProviderNames.foreach();
      if (peripheralDescriptionProvider.size() > 0) {
         return peripheralDescriptionProvider.get(0);
      }
      return null;
   }
   /**
    * Get provider namer from ID
    * 
    * @param providerId
    * @return
    */
   public String getProviderName(String providerId) {
      return providerNames.get(providerId);
   }
   /**
    * Get provider description from ID
    * 
    * @param providerId
    * @return
    */
   public String getProviderDescription(String providerId) {
      return providerDescriptions.get(providerId);   
   }

} 