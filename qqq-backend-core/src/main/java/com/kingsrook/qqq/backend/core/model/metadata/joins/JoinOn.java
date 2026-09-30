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

package com.kingsrook.qqq.backend.core.model.metadata.joins;


/*******************************************************************************
 ** Specification for (at least part of) how two tables join together - e.g.,
 ** leftField = rightField.  Used as part of a list in a QJoinMetaData.
 *******************************************************************************/
public class JoinOn implements Cloneable
{
   private String leftField;
   private String rightField;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public JoinOn()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public JoinOn(String leftField, String rightField)
   {
      this.leftField = leftField;
      this.rightField = rightField;
   }



   /*******************************************************************************
    ** Return a new JoinOn, with the fields of this one, but flipped (right ←→ left)
    *******************************************************************************/
   public JoinOn flip()
   {
      return new JoinOn(rightField, leftField);
   }



   /*******************************************************************************
    ** Getter for leftField
    **
    *******************************************************************************/
   public String getLeftField()
   {
      return leftField;
   }



   /*******************************************************************************
    ** Setter for leftField
    **
    *******************************************************************************/
   public void setLeftField(String leftField)
   {
      this.leftField = leftField;
   }



   /*******************************************************************************
    ** Fluent setter for leftField
    **
    *******************************************************************************/
   public JoinOn withLeftField(String leftField)
   {
      this.leftField = leftField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rightField
    **
    *******************************************************************************/
   public String getRightField()
   {
      return rightField;
   }



   /*******************************************************************************
    ** Setter for rightField
    **
    *******************************************************************************/
   public void setRightField(String rightField)
   {
      this.rightField = rightField;
   }



   /*******************************************************************************
    ** Fluent setter for rightField
    **
    *******************************************************************************/
   public JoinOn withRightField(String rightField)
   {
      this.rightField = rightField;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public JoinOn clone()
   {
      try
      {
         JoinOn clone = (JoinOn) super.clone();
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }
}
