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

package com.kingsrook.qqq.backend.core.exceptions;


import java.util.List;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Exception thrown during qqq-starup, if a QInstance is found to have validation
 ** issues.  Contains a list of reasons (to avoid spoon-feeding as much as possible).
 **
 *******************************************************************************/
public class QInstanceValidationException extends QException
{
   private List<String> reasons;



   /*******************************************************************************
    ** Constructor of message - does not populate reasons!
    **
    *******************************************************************************/
   public QInstanceValidationException(String message)
   {
      super(message);
   }



   /*******************************************************************************
    ** Constructor of a list of reasons.  They feed into the core exception message.
    **
    *******************************************************************************/
   public QInstanceValidationException(List<String> reasons)
   {
      super((CollectionUtils.nullSafeHasContents(reasons))
         ? "Instance validation failed for the following reasons:\n - " + StringUtils.join("\n - ", reasons) + "\n(" + reasons.size() + " Total reason" + StringUtils.plural(reasons) + ")"
         : "Validation failed, but no reasons were provided");

      if(CollectionUtils.nullSafeHasContents(reasons))
      {
         this.reasons = reasons;
      }
   }



   /*******************************************************************************
    ** Constructor of message & cause - does not populate reasons!
    **
    *******************************************************************************/
   public QInstanceValidationException(String message, Throwable cause)
   {
      super(message, cause);
   }



   /*******************************************************************************
    ** Getter for reasons
    **
    *******************************************************************************/
   public List<String> getReasons()
   {
      return reasons;
   }
}
