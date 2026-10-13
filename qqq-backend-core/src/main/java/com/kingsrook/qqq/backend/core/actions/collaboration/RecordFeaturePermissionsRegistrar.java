/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qqq.backend.core.actions.collaboration;


import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.FeaturePermissionMode;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.TableFeatureImplication;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.TableFeaturePermission;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.TableFeaturePermissionsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.TableFeaturePredicate;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Adapt collaboration declarations to the generic table feature permission registry.
 *******************************************************************************/
public class RecordFeaturePermissionsRegistrar
{
   /*******************************************************************************
    ** Safe to repeat; resolutions use the supplied instance, including after cloning.
    *******************************************************************************/
   public static void register(QInstance instance)
   {
      if(!RecordFeaturesResolver.hasAnyMetaData(instance))
      {
         return;
      }
      TableFeaturePermissionsMetaData metadata = TableFeaturePermissionsMetaData.ofOrWithNew(instance);
      for(RecordFeature feature : RecordFeature.values())
      {
         if(feature.getPermissionSuffix() == null)
         {
            continue;
         }
         String label = switch(feature)
         {
            case COMMENTS -> "Comment";
            case ATTACHMENTS -> "Attach files";
            case TAGS -> "Tag";
            case SHARING -> "Share";
            case TASKS -> "Tasks";
            case ASSIGNMENT -> "Assign";
            default -> throw new IllegalStateException("Missing permission label for " + feature);
         };
         FeatureRule rule = new FeatureRule(feature);
         metadata.withPermission(new TableFeaturePermission().withSuffix(feature.getPermissionSuffix()).withLabel(label)
            .withAppliesTo(rule).withImpliedBy(rule));
      }
   }



   /*******************************************************************************
    ** Capture only the feature enum, never the registering QInstance or table.
    *******************************************************************************/
   private static class FeatureRule implements TableFeaturePredicate, TableFeatureImplication
   {
      private final RecordFeature feature;



      /*******************************************************************************
       **
       *******************************************************************************/
      private FeatureRule(RecordFeature feature)
      {
         this.feature = feature;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public boolean test(QInstance instance, QTableMetaData table)
      {
         return (RecordFeaturesResolver.resolve(instance, table).isEnabled(feature));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public TablePermissionSubType impliedBy(QInstance instance, QTableMetaData table)
      {
         return (RecordFeaturesResolver.resolve(instance, table).getFeatures().get(feature).getPermissionMode() == FeaturePermissionMode.EDIT_OR_GRANTED
            ? TablePermissionSubType.EDIT : null);
      }
   }

}
