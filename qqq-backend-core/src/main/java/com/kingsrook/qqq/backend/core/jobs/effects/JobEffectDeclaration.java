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


import java.io.Serializable;


/*******************************************************************************
 ** A side effect a durable process's step causes, declared in its
 ** DurableProcessMetaData (spec section 2.13): its name (unique in the process),
 ** the step that emits it, the target that delivers it (external effects), and
 ** its type.
 *******************************************************************************/
public class JobEffectDeclaration implements Serializable
{
   private String        name;
   private String        stepName;
   private String        targetName;
   private JobEffectType effectType;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public JobEffectDeclaration()
   {
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public JobEffectDeclaration(String name, String stepName, String targetName, JobEffectType effectType)
   {
      this.name = name;
      this.stepName = stepName;
      this.targetName = targetName;
      this.effectType = effectType;
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public JobEffectDeclaration withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for stepName
    *******************************************************************************/
   public String getStepName()
   {
      return (this.stepName);
   }



   /*******************************************************************************
    ** Fluent setter for stepName
    *******************************************************************************/
   public JobEffectDeclaration withStepName(String stepName)
   {
      this.stepName = stepName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for targetName
    *******************************************************************************/
   public String getTargetName()
   {
      return (this.targetName);
   }



   /*******************************************************************************
    ** Fluent setter for targetName
    *******************************************************************************/
   public JobEffectDeclaration withTargetName(String targetName)
   {
      this.targetName = targetName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for effectType
    *******************************************************************************/
   public JobEffectType getEffectType()
   {
      return (this.effectType);
   }



   /*******************************************************************************
    ** Fluent setter for effectType
    *******************************************************************************/
   public JobEffectDeclaration withEffectType(JobEffectType effectType)
   {
      this.effectType = effectType;
      return (this);
   }
}
