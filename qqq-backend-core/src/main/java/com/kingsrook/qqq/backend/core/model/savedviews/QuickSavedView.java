/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.model.savedviews;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;


/*******************************************************************************
 * Entity bean for the quick saved view table - a many to one join
 * with SavedView, for display in as a "quick" view.
 *
 * <p>This is a many-to-one join with saved view, because there's some data in
 * this table that might be adjusted per-user (sortOrder, doCount).  So in addition
 * to sharing a SavedView to a user (or, in an application via additional fields
 * like a groupId or userTypeId), a QuickSavedView against one SavedView can be
 * set up for multiple users (groups, etc in application-layer) with different values
 * for those settings.</p>
 *******************************************************************************/
public class QuickSavedView extends QRecordEntity
{
   public static final String TABLE_NAME = "quickSavedView";

   @QField(isEditable = false)
   private Integer id;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;

   @QField(isRequired = true, possibleValueSourceName = SavedView.TABLE_NAME)
   private Integer savedViewId;

   @QField(label = "User")
   private String userId;

   @QField(defaultValue = "1")
   private Integer sortOrder;

   @QField(defaultValue = "false")
   private Boolean doCount;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QuickSavedView()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QuickSavedView(QRecord qRecord) throws QException
   {
      populateFromQRecord(qRecord);
   }



   /*******************************************************************************
    * Getter for id
    * @see #withId(Integer)
    *******************************************************************************/
   public Integer getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    * Setter for id
    * @see #withId(Integer)
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /*******************************************************************************
    * Fluent setter for id
    *
    * @param id
    * primary key of the record
    * @return this
    *******************************************************************************/
   public QuickSavedView withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    * Getter for createDate
    * @see #withCreateDate(Instant)
    *******************************************************************************/
   public Instant getCreateDate()
   {
      return (this.createDate);
   }



   /*******************************************************************************
    * Setter for createDate
    * @see #withCreateDate(Instant)
    *******************************************************************************/
   public void setCreateDate(Instant createDate)
   {
      this.createDate = createDate;
   }



   /*******************************************************************************
    * Fluent setter for createDate
    *
    * @param createDate
    * create date of the record
    * @return this
    *******************************************************************************/
   public QuickSavedView withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }



   /*******************************************************************************
    * Getter for modifyDate
    * @see #withModifyDate(Instant)
    *******************************************************************************/
   public Instant getModifyDate()
   {
      return (this.modifyDate);
   }



   /*******************************************************************************
    * Setter for modifyDate
    * @see #withModifyDate(Instant)
    *******************************************************************************/
   public void setModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
   }



   /*******************************************************************************
    * Fluent setter for modifyDate
    *
    * @param modifyDate
    * modify date of the record
    * @return this
    *******************************************************************************/
   public QuickSavedView withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }



   /*******************************************************************************
    * Getter for savedViewId
    * @see #withSavedViewId(Integer)
    *******************************************************************************/
   public Integer getSavedViewId()
   {
      return (this.savedViewId);
   }



   /*******************************************************************************
    * Setter for savedViewId
    * @see #withSavedViewId(Integer)
    *******************************************************************************/
   public void setSavedViewId(Integer savedViewId)
   {
      this.savedViewId = savedViewId;
   }



   /*******************************************************************************
    * Fluent setter for savedViewId
    *
    * @param savedViewId
    * id of the saved view that this quick saved view refers to
    * @return this
    *******************************************************************************/
   public QuickSavedView withSavedViewId(Integer savedViewId)
   {
      this.savedViewId = savedViewId;
      return (this);
   }



   /*******************************************************************************
    * Getter for sortOrder
    * @see #withSortOrder(Integer)
    *******************************************************************************/
   public Integer getSortOrder()
   {
      return (this.sortOrder);
   }



   /*******************************************************************************
    * Setter for sortOrder
    * @see #withSortOrder(Integer)
    *******************************************************************************/
   public void setSortOrder(Integer sortOrder)
   {
      this.sortOrder = sortOrder;
   }



   /*******************************************************************************
    * Fluent setter for sortOrder
    *
    * @param sortOrder
    * Integer to control the sort-order for the user's quick saved views.
    * @return this
    *******************************************************************************/
   public QuickSavedView withSortOrder(Integer sortOrder)
   {
      this.sortOrder = sortOrder;
      return (this);
   }



   /*******************************************************************************
    * Getter for doCount
    * @see #withDoCount(Boolean)
    *******************************************************************************/
   public Boolean getDoCount()
   {
      return (this.doCount);
   }



   /*******************************************************************************
    * Setter for doCount
    * @see #withDoCount(Boolean)
    *******************************************************************************/
   public void setDoCount(Boolean doCount)
   {
      this.doCount = doCount;
   }



   /*******************************************************************************
    * Fluent setter for doCount
    *
    * @param doCount
    * boolean to specify whether or not the frontend should execute a count whenever
    * it displays a button for this quick saved view.
    * @return this
    *******************************************************************************/
   public QuickSavedView withDoCount(Boolean doCount)
   {
      this.doCount = doCount;
      return (this);
   }


   /*******************************************************************************
    * Getter for userId
    * @see #withUserId(String)
    *******************************************************************************/
   public String getUserId()
   {
      return (this.userId);
   }



   /*******************************************************************************
    * Setter for userId
    * @see #withUserId(String)
    *******************************************************************************/
   public void setUserId(String userId)
   {
      this.userId = userId;
   }



   /*******************************************************************************
    * Fluent setter for userId
    *
    * @param userId
    * TODO document this property
    * @return this
    *******************************************************************************/
   public QuickSavedView withUserId(String userId)
   {
      this.userId = userId;
      return (this);
   }


}
