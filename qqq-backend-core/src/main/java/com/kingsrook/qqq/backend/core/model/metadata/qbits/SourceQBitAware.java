/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.qbits;


import com.kingsrook.qqq.backend.core.context.QContext;


/*******************************************************************************
 ** interface for meta data objects that may have come from a qbit, and where we
 ** might want to get data about that qbit (e.g., config or meta-data).
 *******************************************************************************/
public interface SourceQBitAware
{
   /*******************************************************************************
    ** Getter for sourceQBitName
    *******************************************************************************/
   String getSourceQBitName();


   /*******************************************************************************
    ** Setter for sourceQBitName
    *******************************************************************************/
   void setSourceQBitName(String sourceQBitName);


   /*******************************************************************************
    ** Fluent setter for sourceQBitName
    *******************************************************************************/
   Object withSourceQBitName(String sourceQBitName);


   /***************************************************************************
    **
    ***************************************************************************/
   default QBitMetaData getSourceQBit()
   {
      String qbitName = getSourceQBitName();
      return (QContext.getQInstance().getQBits().get(qbitName));
   }


   /***************************************************************************
    **
    ***************************************************************************/
   default QBitConfig getSourceQBitConfig()
   {
      QBitMetaData sourceQBit = getSourceQBit();
      if(sourceQBit == null)
      {
         return null;
      }
      else
      {
         return sourceQBit.getConfig();
      }
   }
}
