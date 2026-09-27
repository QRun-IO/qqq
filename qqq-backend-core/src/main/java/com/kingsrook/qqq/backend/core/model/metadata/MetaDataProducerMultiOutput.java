/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.SourceQBitAware;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Output object for a MetaDataProducer, which contains multiple meta-data
 ** objects.
 *******************************************************************************/
public class MetaDataProducerMultiOutput implements MetaDataProducerOutput, SourceQBitAware
{
   private List<MetaDataProducerOutput> contents;

   private String sourceQBitName;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance instance)
   {
      for(MetaDataProducerOutput metaDataProducerOutput : CollectionUtils.nonNullList(contents))
      {
         metaDataProducerOutput.addSelfToInstance(instance);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void add(MetaDataProducerOutput metaDataProducerOutput)
   {
      if(contents == null)
      {
         contents = new ArrayList<>();
      }
      contents.add(metaDataProducerOutput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public MetaDataProducerMultiOutput with(MetaDataProducerOutput metaDataProducerOutput)
   {
      add(metaDataProducerOutput);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public <T extends MetaDataProducerOutput> List<T> getEach(Class<T> c)
   {
      List<T> rs = new ArrayList<>();

      for(MetaDataProducerOutput content : CollectionUtils.nonNullList(contents))
      {
         if(content instanceof MetaDataProducerMultiOutput multiOutput)
         {
            rs.addAll(multiOutput.getEach(c));
         }
         else if(c.isInstance(content))
         {
            rs.add(c.cast(content));
         }
      }

      return (rs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String getSourceQBitName()
   {
      return (this.sourceQBitName);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setSourceQBitName(String sourceQBitName)
   {
      this.sourceQBitName = sourceQBitName;

      /////////////////////////////////////////////
      // propagate the name down to the children //
      /////////////////////////////////////////////
      for(MetaDataProducerOutput content : contents)
      {
         if(content instanceof SourceQBitAware aware)
         {
            aware.setSourceQBitName(sourceQBitName);
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public MetaDataProducerMultiOutput withSourceQBitName(String sourceQBitName)
   {
      setSourceQBitName(sourceQBitName);
      return this;
   }



   /***************************************************************************
    * get a typed and named meta-data object out of this output container.
    *
    * @param <C> the type of the object to return, e.g., QTableMetaData
    * @param outputClass the class for the type to return
    * @param name the name of the object, e.g., a table or process name.
    * @return the requested TopLevelMetaDataInterface object (in the requested
    * type), or null if not found.
    ***************************************************************************/
   public <C extends TopLevelMetaDataInterface> C get(Class<C> outputClass, String name)
   {
      for(MetaDataProducerOutput content : CollectionUtils.nonNullList(contents))
      {
         if(content instanceof MetaDataProducerMultiOutput multiOutput)
         {
            C c = multiOutput.get(outputClass, name);
            if(c != null)
            {
               return (c);
            }
         }
         else if(outputClass.isInstance(content) && name.equals(((TopLevelMetaDataInterface)content).getName()))
         {
            return (C) content;
         }
      }

      return null;
   }
}
