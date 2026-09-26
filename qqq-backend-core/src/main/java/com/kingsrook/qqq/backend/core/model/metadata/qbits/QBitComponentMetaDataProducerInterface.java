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


import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerOutput;


/*******************************************************************************
 ** extension of MetaDataProducerInterface, designed for producing meta data
 ** within a (java-defined, at this time) QBit.
 **
 ** Specifically exists to accept the QBitConfig as a type parameter and a value,
 ** easily accessed in the producer's methods as getQBitConfig()
 *******************************************************************************/
public interface QBitComponentMetaDataProducerInterface<T extends MetaDataProducerOutput, C extends QBitConfig> extends MetaDataProducerInterface<T>
{

   /*******************************************************************************
    ** Getter for qBitConfig
    *******************************************************************************/
   C getQBitConfig();


   /*******************************************************************************
    ** Setter for qBitConfig
    *******************************************************************************/
   void setQBitConfig(C qBitConfig);

}
