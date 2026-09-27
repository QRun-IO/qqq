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

package com.kingsrook.qqq.middleware.javalin.specs;


import com.kingsrook.qqq.openapi.model.Method;


/*******************************************************************************
 ** Extension of a BasicOperation that adds the full openAPI Method object.
 *******************************************************************************/
public class CompleteOperation extends BasicOperation
{
   private Method method;



   /***************************************************************************
    **
    ***************************************************************************/
   public CompleteOperation(BasicOperation basicOperation)
   {
      setPath(basicOperation.getPath());
      setHttpMethod(basicOperation.getHttpMethod());
      setTag(basicOperation.getTag());
      setLongDescription(basicOperation.getLongDescription());
      setShortSummary(basicOperation.getShortSummary());
   }



   /*******************************************************************************
    ** Getter for method
    *******************************************************************************/
   public Method getMethod()
   {
      return (this.method);
   }



   /*******************************************************************************
    ** Setter for method
    *******************************************************************************/
   public void setMethod(Method method)
   {
      this.method = method;
   }



   /*******************************************************************************
    ** Fluent setter for method
    *******************************************************************************/
   public CompleteOperation withMethod(Method method)
   {
      this.method = method;
      return (this);
   }

}
