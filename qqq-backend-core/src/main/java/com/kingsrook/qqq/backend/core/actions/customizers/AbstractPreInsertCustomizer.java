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
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Abstract class that a table can specify an implementation of, to provide
 ** custom actions before an insert takes place.
 **
 ** It's important for implementations to be aware of the isPreview field, which
 ** is set to true when the code is running to give users advice, e.g., on a review
 ** screen - vs. being false when the action is ACTUALLY happening.  So, if you're doing
 ** things like storing data, you don't want to do that if isPreview is true!!
 **
 ** General implementation would be, to iterate over the records (the inputs to
 ** the insert action), and look at their values:
 ** - possibly adding Errors (`addError`) or Warnings (`addWarning`) to the records
 ** - possibly manipulating values (`setValue`)
 ** - possibly throwing an exception - if you really don't want the insert operation to continue.
 ** - doing "whatever else" you may want to do.
 ** - returning the list of records (can be the input list) that you want to go on to the backend implementation class.
 **
 ** Note that the full insertInput is available as a field in this class.
 *******************************************************************************/
public abstract class AbstractPreInsertCustomizer implements TableCustomizerInterface
{
   protected InsertInput insertInput;

   protected boolean isPreview = false;



   /***************************************************************************
    ** allow the customizer to specify when it should be executed as part of the
    ** insert action.  default (per method in this class) is AFTER_ALL_VALIDATIONS
    ***************************************************************************/
   public enum WhenToRun
   {
      BEFORE_ALL_VALIDATIONS,
      BEFORE_UNIQUE_KEY_CHECKS,
      BEFORE_REQUIRED_FIELD_CHECKS,
      BEFORE_SECURITY_CHECKS,
      AFTER_ALL_VALIDATIONS
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
   {
      this.insertInput = insertInput;
      this.isPreview = isPreview;
      return (apply(records));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public WhenToRun whenToRunPreInsert(InsertInput insertInput, boolean isPreview)
   {
      return getWhenToRun();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public abstract List<QRecord> apply(List<QRecord> records) throws QException;



   /*******************************************************************************
    **
    *******************************************************************************/
   public WhenToRun getWhenToRun()
   {
      return (WhenToRun.AFTER_ALL_VALIDATIONS);
   }



   /*******************************************************************************
    ** Getter for insertInput
    **
    *******************************************************************************/
   public InsertInput getInsertInput()
   {
      return insertInput;
   }



   /*******************************************************************************
    ** Setter for insertInput
    **
    *******************************************************************************/
   public void setInsertInput(InsertInput insertInput)
   {
      this.insertInput = insertInput;
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
   public AbstractPreInsertCustomizer withIsPreview(boolean isPreview)
   {
      this.isPreview = isPreview;
      return (this);
   }

}
