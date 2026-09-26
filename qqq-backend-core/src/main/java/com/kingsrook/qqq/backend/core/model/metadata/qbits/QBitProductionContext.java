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


import java.util.Collections;
import java.util.List;
import java.util.Stack;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;


/*******************************************************************************
 ** While a qbit is being produced, track the context of the current config
 ** and metaDataProducerMultiOutput that is being used.  also, in case one
 ** qbit produces another, push these contextual objects on a stack.
 *******************************************************************************/
public class QBitProductionContext
{
   private static final QLogger LOG = QLogger.getLogger(QBitProductionContext.class);

   private static Stack<QBitConfig>                  qbitConfigStack                  = new Stack<>();
   private static Stack<MetaDataProducerMultiOutput> metaDataProducerMultiOutputStack = new Stack<>();



   /***************************************************************************
    **
    ***************************************************************************/
   public static void pushQBitConfig(QBitConfig qBitConfig)
   {
      qbitConfigStack.push(qBitConfig);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static QBitConfig peekQBitConfig()
   {
      if(qbitConfigStack.isEmpty())
      {
         LOG.warn("Request to peek at empty QBitProductionContext configStack - returning null");
         return (null);
      }
      return qbitConfigStack.peek();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void popQBitConfig()
   {
      if(qbitConfigStack.isEmpty())
      {
         LOG.warn("Request to pop empty QBitProductionContext configStack - returning with noop");
         return;
      }

      qbitConfigStack.pop();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void pushMetaDataProducerMultiOutput(MetaDataProducerMultiOutput metaDataProducerMultiOutput)
   {
      metaDataProducerMultiOutputStack.push(metaDataProducerMultiOutput);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static MetaDataProducerMultiOutput peekMetaDataProducerMultiOutput()
   {
      if(metaDataProducerMultiOutputStack.isEmpty())
      {
         LOG.warn("Request to peek at empty QBitProductionContext configStack - returning null");
         return (null);
      }
      return metaDataProducerMultiOutputStack.peek();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static List<MetaDataProducerMultiOutput> getReadOnlyViewOfMetaDataProducerMultiOutputStack()
   {
      return Collections.unmodifiableList(metaDataProducerMultiOutputStack);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void popMetaDataProducerMultiOutput()
   {
      if(metaDataProducerMultiOutputStack.isEmpty())
      {
         LOG.warn("Request to pop empty QBitProductionContext metaDataProducerMultiOutput - returning with noop");
         return;
      }

      metaDataProducerMultiOutputStack.pop();
   }

}
