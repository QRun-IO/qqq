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

package com.kingsrook.qqq.backend.core.model.actions.tables.aggregate;


import java.util.Objects;


/*******************************************************************************
 ** Aggregate analysis metadata.
 *******************************************************************************/
public final class DateBucket implements GroupByBucket
{
   private DateBucketUnit unit;



   /*******************************************************************************
    ** Constructor.
    *******************************************************************************/
   public DateBucket(DateBucketUnit unit)
   {
      this.unit = unit;
   }



   /*******************************************************************************
    ** Getter for unit
    *******************************************************************************/
   public DateBucketUnit getUnit()
   {
      return (this.unit);
   }



   /*******************************************************************************
    ** Setter for unit
    *******************************************************************************/
   public void setUnit(DateBucketUnit unit)
   {
      this.unit = unit;
   }



   /*******************************************************************************
    ** Fluent setter for unit
    *******************************************************************************/
   public DateBucket withUnit(DateBucketUnit unit)
   {
      setUnit(unit);
      return (this);
   }



   /*******************************************************************************
    ** Value equality for result keys.
    *******************************************************************************/
   @Override
   public boolean equals(Object other)
   {
      return (other instanceof DateBucket that && Objects.equals(unit, that.unit));
   }



   /*******************************************************************************
    ** Hash for result keys.
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return (Objects.hash(unit));
   }
}
