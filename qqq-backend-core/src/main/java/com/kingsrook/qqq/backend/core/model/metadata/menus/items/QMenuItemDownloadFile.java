/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.menus.items;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 ** Menu item that triggers a file download from a field in the current record.
 **
 ** <p>This menu item type is used to provide download functionality for
 ** file fields (BLOB fields) or URL fields in a table. When selected, it
 ** will download the file content from the specified field.</p>
 **
 ** <p>The label for this menu item can be automatically derived from the
 ** field's metadata if not explicitly set.</p>
 *******************************************************************************/
public class QMenuItemDownloadFile extends QMenuItemBase
{
   private String fieldName;
   private String tableName;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public QMenuItemDownloadFile()
   {
   }



   /*******************************************************************************
    ** Constructor that initializes the download file item with a field name.
    **
    ** <p>This constructor assumes the field belongs to the table that contains
    ** this menu item.</p>
    **
    ** @param fieldName the name of the field containing the file to download
    *******************************************************************************/
   public QMenuItemDownloadFile(String fieldName)
   {
      this(null, fieldName);
   }



   /*******************************************************************************
    ** Constructor that initializes the download file item with a table and field name.
    **
    ** @param table the table metadata containing the field (can be null if field
    **              is in the same table as the menu)
    ** @param fieldName the name of the field containing the file to download
    *******************************************************************************/
   public QMenuItemDownloadFile(QTableMetaData table, String fieldName)
   {
      this.fieldName = fieldName;
      this.tableName = table == null ? null : table.getName();
   }



   /***************************************************************************
    * Returns the item type identifier for download file menu items.
    *
    * @return always returns "DOWNLOAD_FILE"
    ***************************************************************************/
   @Override
   public String getItemType()
   {
      return ("DOWNLOAD_FILE");
   }



   /***************************************************************************
    * Returns the values map containing the field name for the download.
    *
    * @return a map containing the field name under the key "fieldName"
    ***************************************************************************/
   @Override
   public Map<String, Serializable> getValues()
   {
      return MapBuilder.of("fieldName", getFieldName());
   }



   /***************************************************************************
    * Returns the label for this menu item, automatically deriving it from
    * the field's metadata if not explicitly set.
    *
    * <p>If no label has been set on this menu item, this method attempts
    * to retrieve the label from the field's metadata and sets it automatically.</p>
    *
    * @return the label for this menu item
    ***************************************************************************/
   @Override
   public String getLabel()
   {
      ///////////////////////////////////////////////////////////////////////////////
      // if a label hasn't been set here, try to get the field's label, and set it //
      ///////////////////////////////////////////////////////////////////////////////
      if(super.getLabel() == null && tableName != null && fieldName != null)
      {
         QInstance qInstance = QContext.getQInstance();
         if(qInstance != null)
         {
            QTableMetaData table = qInstance.getTable(tableName);
            if(table != null)
            {
               QFieldMetaData field = table.getField(fieldName);
               if(field != null)
               {
                  setLabel(field.getLabel());
               }
            }
         }
      }

      return super.getLabel();
   }



   /***************************************************************************
    * Validates that the download file menu item has a valid field name and
    * that the field exists in the table.
    *
    * <p>Ensures that:</p>
    * <ul>
    *   <li>A label is set (either explicitly or derived from the field)</li>
    *   <li>A field name is specified</li>
    *   <li>The field exists in the table (if parent is a QTableMetaData)</li>
    * </ul>
    *
    * @param validator the validator instance to use for reporting errors
    * @param qInstance the QQQ instance being validated
    * @param parentObject the parent metadata object containing this menu item
    ***************************************************************************/
   @Override
   public void validate(QInstanceValidator validator, QInstance qInstance, QMetaDataObject parentObject)
   {
      super.validate(validator, qInstance, parentObject);

      validator.assertCondition(StringUtils.hasContent(getLabel()), "Missing a label for a menu item of type [" + getClass().getSimpleName() + "] for field [" + getFieldName() + "] in " + parentObject);

      if(validator.assertCondition(StringUtils.hasContent(getFieldName()), "Missing a fieldName for menu item [" + getLabel() + "] in " + parentObject))
      {
         if(parentObject instanceof QTableMetaData table)
         {
            QFieldMetaData field = table.getFields().get(getFieldName());
            validator.assertCondition(field != null, "Could not find field [" + getFieldName() + "], referenced in menu item [" + getLabel() + "] in " + parentObject);
         }
      }
   }



   /*******************************************************************************
    * Getter for fieldName
    * @see #withFieldName(String)
    *******************************************************************************/
   public String getFieldName()
   {
      return (this.fieldName);
   }



   /*******************************************************************************
    * Setter for fieldName
    * @see #withFieldName(String)
    *******************************************************************************/
   public void setFieldName(String fieldName)
   {
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    * Fluent setter for fieldName
    *
    * @param fieldName
    * The field on the table holding this menu, that is either a BLOB, or a URL,
    * which is what should be downloaded.
    * @return this
    *******************************************************************************/
   public QMenuItemDownloadFile withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /***************************************************************************
    * Creates and returns a shallow copy of this download file menu item.
    *
    * @return a clone of this menu item
    ***************************************************************************/
   @Override
   public QMenuItemDownloadFile clone()
   {
      QMenuItemDownloadFile clone = (QMenuItemDownloadFile) super.clone();

      return clone;
   }

}
