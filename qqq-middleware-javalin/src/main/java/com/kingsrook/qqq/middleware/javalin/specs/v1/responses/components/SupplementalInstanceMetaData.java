/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/***************************************************************************
 ** Instance-level settings from supplemental modules, for a frontend.
 **
 ** This is an explicit allow-list:  supplemental instance meta-data objects
 ** can carry server configuration (route providers, file-system paths,
 ** authenticator classes, etc), so they are never serialized as a whole.
 ** Each published setting is copied here, by name, from the module that owns
 ** it (read generically, as those modules are not dependencies of this one).
 ***************************************************************************/
public class SupplementalInstanceMetaData implements ToSchema
{
   @OpenAPIExclude()
   private static final QLogger LOG = QLogger.getLogger(SupplementalInstanceMetaData.class);

   @OpenAPIExclude()
   public static final String MATERIAL_DASHBOARD_NAME = "materialDashboard";

   @OpenAPIExclude()
   private static final String PROCESS_NAMES_GETTER = "getProcessNamesToAddToAllQueryAndViewScreens";

   @OpenAPIExclude()
   private static final String WEEKDAY_CRITERIA_SETTINGS_GETTER = "getWeekdayCriteriaSettings";

   @OpenAPIDescription("Settings from the instance's `materialDashboard` supplemental meta-data, when the instance defines it.")
   private MaterialDashboardInstanceSettings materialDashboard;



   /*******************************************************************************
    ** Build the published supplemental meta-data from a meta-data action's output,
    ** or null when the instance has none of the supplemental meta-data that is
    ** published (so the property is omitted).
    *******************************************************************************/
   public static SupplementalInstanceMetaData of(MetaDataOutput metaDataOutput)
   {
      QSupplementalInstanceMetaData materialDashboard = CollectionUtils.nonNullMap(metaDataOutput.getSupplementalInstanceMetaData()).get(MATERIAL_DASHBOARD_NAME);
      if(materialDashboard == null)
      {
         return (null);
      }

      ///////////////////////////////////////////////////////////////////////////////
      // keep the configured order, drop duplicates, and list only processes that //
      // the meta-data action let this user see                                   //
      ///////////////////////////////////////////////////////////////////////////////
      Set<String> visibleProcessNames = CollectionUtils.nonNullMap(metaDataOutput.getProcesses()).keySet();
      Set<String> processNames        = new LinkedHashSet<>();
      for(String processName : getConfiguredProcessNames(materialDashboard))
      {
         if(visibleProcessNames.contains(processName))
         {
            processNames.add(processName);
         }
      }

      return (new SupplementalInstanceMetaData().withMaterialDashboard(new MaterialDashboardInstanceSettings()
         .withProcessNamesToAddToAllQueryAndViewScreens(new ArrayList<>(processNames))
         .withWeekdayCriteriaSettings(getWeekdayCriteriaSettings(materialDashboard))));
   }



   /*******************************************************************************
    ** Read the weekday criteria settings (enabled, dateTimeFieldFunctionArguments)
    ** from the material dashboard's supplemental meta-data, or null when it has
    ** none (by getters, as those classes live in the material dashboard module).
    *******************************************************************************/
   private static MaterialDashboardWeekdayCriteriaSettings getWeekdayCriteriaSettings(QSupplementalInstanceMetaData materialDashboard)
   {
      Object settings = invokeGetter(materialDashboard, WEEKDAY_CRITERIA_SETTINGS_GETTER);
      if(settings == null)
      {
         return (null);
      }

      MaterialDashboardWeekdayCriteriaSettings published = new MaterialDashboardWeekdayCriteriaSettings()
         .withEnabled(!Boolean.FALSE.equals(invokeGetter(settings, "getEnabled")));

      if(invokeGetter(settings, "getDateTimeFieldFunctionArguments") instanceof Map<?, ?> arguments && !arguments.isEmpty())
      {
         Map<String, Serializable> dateTimeFieldFunctionArguments = new LinkedHashMap<>();
         for(Map.Entry<?, ?> entry : arguments.entrySet())
         {
            if(entry.getKey() instanceof String name && entry.getValue() instanceof Serializable value)
            {
               dateTimeFieldFunctionArguments.put(name, value);
            }
         }
         published.setDateTimeFieldFunctionArguments(dateTimeFieldFunctionArguments);
      }

      return (published);
   }



   /*******************************************************************************
    ** Call a no-argument getter by name, returning null when the object does not
    ** have it (e.g., an older material dashboard module) or it fails.
    *******************************************************************************/
   private static Object invokeGetter(Object target, String getterName)
   {
      try
      {
         Method getter = target.getClass().getMethod(getterName);
         return (getter.invoke(target));
      }
      catch(NoSuchMethodException e)
      {
         return (null);
      }
      catch(Exception e)
      {
         LOG.warn("Error reading a setting from supplemental instance meta-data", e, logPair("name", MATERIAL_DASHBOARD_NAME), logPair("getter", getterName));
         return (null);
      }
   }



   /*******************************************************************************
    ** Read the process names from the material dashboard's supplemental meta-data
    ** (by its getter, as that class lives in the material dashboard module).
    *******************************************************************************/
   private static List<String> getConfiguredProcessNames(QSupplementalInstanceMetaData materialDashboard)
   {
      Object value;
      try
      {
         Method getter = materialDashboard.getClass().getMethod(PROCESS_NAMES_GETTER);
         value = getter.invoke(materialDashboard);
      }
      catch(NoSuchMethodException e)
      {
         return (Collections.emptyList());
      }
      catch(Exception e)
      {
         LOG.warn("Error reading process names from supplemental instance meta-data", e, logPair("name", MATERIAL_DASHBOARD_NAME));
         return (Collections.emptyList());
      }

      List<String> processNames = new ArrayList<>();
      if(value instanceof Collection<?> collection)
      {
         for(Object element : collection)
         {
            if(element instanceof String processName)
            {
               processNames.add(processName);
            }
         }
      }
      return (processNames);
   }



   /*******************************************************************************
    ** Getter for materialDashboard
    *******************************************************************************/
   public MaterialDashboardInstanceSettings getMaterialDashboard()
   {
      return (this.materialDashboard);
   }



   /*******************************************************************************
    ** Setter for materialDashboard
    *******************************************************************************/
   public void setMaterialDashboard(MaterialDashboardInstanceSettings materialDashboard)
   {
      this.materialDashboard = materialDashboard;
   }



   /*******************************************************************************
    ** Fluent setter for materialDashboard
    *******************************************************************************/
   public SupplementalInstanceMetaData withMaterialDashboard(MaterialDashboardInstanceSettings materialDashboard)
   {
      this.materialDashboard = materialDashboard;
      return (this);
   }

}
