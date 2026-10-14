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


package com.kingsrook.qqq.backend.core.jobs.effects;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** Resolve registered effect targets (DurableJobsMetaData.effectTargets) to
 ** instances, one per code reference.
 *******************************************************************************/
public final class EffectTargets
{
   private static final Map<String, EffectTarget> INSTANCES = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** Static helpers only.
    *******************************************************************************/
   private EffectTargets()
   {
   }



   /*******************************************************************************
    ** The target registered under the name in the current instance.
    *******************************************************************************/
   public static EffectTarget get(String targetName) throws QException
   {
      DurableJobsMetaData settings = DurableJobsMetaData.of(QContext.getQInstance());
      QCodeReference      code     = settings == null ? null : settings.getEffectTargets().get(targetName);
      if(code == null)
      {
         throw (new QException("No effect target is registered as " + targetName + "."));
      }
      return (INSTANCES.computeIfAbsent(code.getName() + "#" + targetName, key -> QCodeLoader.getAdHoc(EffectTarget.class, code)));
   }



   /*******************************************************************************
    ** Forget loaded instances (tests).
    *******************************************************************************/
   public static void clear()
   {
      INSTANCES.clear();
   }
}
