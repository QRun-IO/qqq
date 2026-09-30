/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.sampleapp.fixturemetadata;


import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 ** Possible values discovered from an annotated sample enum.
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum
public enum LabStatus implements PossibleValueEnum<Integer>
{
   OPEN(1, "Open"),
   CLOSED(2, "Closed");

   private final Integer id;
   private final String label;



   /*******************************************************************************
    ** 
    *******************************************************************************/
   LabStatus(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @Override
   public Integer getPossibleValueId()
   {
      return id;
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @Override
   public String getPossibleValueLabel()
   {
      return label;
   }
}
