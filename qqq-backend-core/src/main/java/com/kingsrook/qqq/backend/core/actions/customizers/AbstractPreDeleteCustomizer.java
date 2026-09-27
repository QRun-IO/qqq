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

package com.kingsrook.qqq.backend.core.actions.customizers;


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Abstract class that a table can specify an implementation of, to provide
 ** custom actions before a delete takes place.
 **
 ** It's important for implementations to be aware of the isPreview field, which
 ** is set to true when the code is running to give users advice, e.g., on a review
 ** screen - vs. being false when the action is ACTUALLY happening.  So, if you're doing
 ** things like storing data, you don't want to do that if isPreview is true!!
 **
 ** General implementation would be, to iterate over the records (which the DeleteAction
 ** would look up based on the inputs to the delete action), and look at their values:
 ** - possibly adding Errors (`addError`) or Warnings (`addWarning`) to the records
 ** - possibly throwing an exception - if you really don't want the delete operation to continue.
 ** - doing "whatever else" you may want to do.
 ** - returning the list of records (can be the input list) - this is how errors
 **   and warnings are propagated to the DeleteAction.  Note that any records with
 **   an error will NOT proceed to the backend's delete interface - but those with
 **   warnings will.
 **
 ** Note that the full deleteInput is available as a field in this class.
 **
 *******************************************************************************/
public abstract class AbstractPreDeleteCustomizer implements TableCustomizerInterface
{
   protected DeleteInput deleteInput;

   protected boolean isPreview = false;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preDelete(DeleteInput deleteInput, List<QRecord> records, boolean isPreview) throws QException
   {
      this.deleteInput = deleteInput;
      this.isPreview = isPreview;
      return apply(records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public abstract List<QRecord> apply(List<QRecord> records) throws QException;



   /*******************************************************************************
    ** Getter for deleteInput
    **
    *******************************************************************************/
   public DeleteInput getDeleteInput()
   {
      return deleteInput;
   }



   /*******************************************************************************
    ** Setter for deleteInput
    **
    *******************************************************************************/
   public void setDeleteInput(DeleteInput deleteInput)
   {
      this.deleteInput = deleteInput;
   }



   /*******************************************************************************
    ** Getter for isPreview
    *******************************************************************************/
   public boolean getIsPreview()
   {
      return (this.isPreview);
   }



   /*******************************************************************************
    ** Setter for isPreview
    *******************************************************************************/
   public void setIsPreview(boolean isPreview)
   {
      this.isPreview = isPreview;
   }



   /*******************************************************************************
    ** Fluent setter for isPreview
    *******************************************************************************/
   public AbstractPreDeleteCustomizer withIsPreview(boolean isPreview)
   {
      this.isPreview = isPreview;
      return (this);
   }

}
