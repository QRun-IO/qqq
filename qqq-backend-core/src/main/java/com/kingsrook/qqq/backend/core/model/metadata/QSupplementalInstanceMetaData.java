/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata;


import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;


/*******************************************************************************
 ** Base-class for instance-level meta-data defined by some supplemental module, etc,
 ** outside of qqq core
 *******************************************************************************/
public interface QSupplementalInstanceMetaData extends TopLevelMetaDataInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   default void enrich(QInstance qInstance)
   {
      ////////////////////////
      // noop in base class //
      ////////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   default void validate(QInstance qInstance, QInstanceValidator validator)
   {
      ////////////////////////
      // noop in base class //
      ////////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   default void addSelfToInstance(QInstance qInstance)
   {
      qInstance.withSupplementalMetaData(this);
   }


   /***************************************************************************
    **
    ***************************************************************************/
   static <S extends QSupplementalInstanceMetaData> S of(QInstance qInstance, String name)
   {
      return ((S) qInstance.getSupplementalMetaData(name));
   }


   /***************************************************************************
    **
    ***************************************************************************/
   static <S extends QSupplementalInstanceMetaData> S ofOrWithNew(QInstance qInstance, String name, Supplier<S> supplier)
   {
      S s = (S) qInstance.getSupplementalMetaData(name);
      if(s == null)
      {
         s = supplier.get();
         s.addSelfToInstance(qInstance);
      }
      return (s);
   }

}
