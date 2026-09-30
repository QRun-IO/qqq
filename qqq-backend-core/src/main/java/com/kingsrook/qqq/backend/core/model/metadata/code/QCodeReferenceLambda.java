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

package com.kingsrook.qqq.backend.core.model.metadata.code;


import java.util.Objects;


/*******************************************************************************
 ** Specialized type of QCodeReference that takes a lambda function object.
 **
 ** Originally intended for more concise setup of backend steps in tests - but,
 ** may be generally useful.
 *******************************************************************************/
public class QCodeReferenceLambda<T> extends QCodeReference
{
   private final T lambda;



   /***************************************************************************
    **
    ***************************************************************************/
   public QCodeReferenceLambda(T lambda)
   {
      this.lambda = lambda;
      this.setCodeType(QCodeType.JAVA);
      this.setName("[Lambda:" + lambda.toString() + "]");
   }



   /*******************************************************************************
    ** Getter for lambda
    **
    *******************************************************************************/
   public T getLambda()
   {
      return lambda;
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      if(o == null || getClass() != o.getClass())
      {
         return false;
      }
      if(!super.equals(o))
      {
         return false;
      }
      QCodeReferenceLambda<?> that = (QCodeReferenceLambda<?>) o;
      return Objects.equals(lambda, that.lambda);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public int hashCode()
   {
      return Objects.hash(super.hashCode(), lambda);
   }
}
