/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.menus.items;


import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 ** Menu item that triggers the execution of a QQQ process.
 **
 ** <p>This menu item type is used to provide menu-driven access to processes
 ** defined in the QQQ application. When selected, it will execute the specified
 ** process.</p>
 **
 ** <p>The label and icon for this menu item can be automatically derived from
 ** the process's metadata if not explicitly set.</p>
 **
 ** <p>The process must be associated with the same table that contains this
 ** menu item.</p>
 *******************************************************************************/
public class QMenuItemRunProcess extends QMenuItemBase
{
   private String processName;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public QMenuItemRunProcess()
   {
   }



   /*******************************************************************************
    ** Constructor that initializes the run process item with a process name.
    **
    ** @param processName the name of the process to execute
    *******************************************************************************/
   public QMenuItemRunProcess(String processName)
   {
      this.processName = processName;
   }



   /***************************************************************************
    * Returns the label for this menu item, automatically deriving it from
    * the process's metadata if not explicitly set.
    *
    * <p>If no label has been set on this menu item, this method attempts
    * to retrieve the label from the process's metadata and sets it automatically.</p>
    *
    * @return the label for this menu item
    ***************************************************************************/
   @Override
   public String getLabel()
   {
      //////////////////////////////////////////////////////////////////////////////////////////
      // if a label hasn't been set here, try to get the process's label, and set it in here. //
      //////////////////////////////////////////////////////////////////////////////////////////
      if(super.getLabel() == null)
      {
         QInstance qInstance = QContext.getQInstance();
         if(qInstance != null)
         {
            QProcessMetaData process = qInstance.getProcess(processName);
            if(process != null)
            {
               setLabel(process.getLabel());
            }
         }
      }

      return super.getLabel();
   }



   /***************************************************************************
    * Returns the icon for this menu item, automatically deriving it from
    * the process's metadata if not explicitly set.
    *
    * <p>If no icon has been set on this menu item, this method attempts
    * to retrieve the icon from the process's metadata and sets it automatically.</p>
    *
    * @return the icon for this menu item, or null if none is available
    ***************************************************************************/
   @Override
   public QIcon getIcon()
   {
      /////////////////////////////////////////////////////////////////////////////////////////
      // if an icon hasn't been set here, try to get the process's icon, and set it in here. //
      /////////////////////////////////////////////////////////////////////////////////////////
      if(super.getIcon() == null)
      {
         QInstance qInstance = QContext.getQInstance();
         if(qInstance != null)
         {
            QProcessMetaData process = qInstance.getProcess(processName);
            if(process != null)
            {
               setIcon(process.getIcon());
            }
         }
      }

      return super.getIcon();
   }



   /***************************************************************************
    * Returns the item type identifier for run process menu items.
    *
    * @return always returns "RUN_PROCESS"
    ***************************************************************************/
   @Override
   public String getItemType()
   {
      return ("RUN_PROCESS");
   }



   /***************************************************************************
    * Returns the values map containing the process name.
    *
    * @return a map containing the process name under the key "processName"
    ***************************************************************************/
   @Override
   public Map<String, Serializable> getValues()
   {
      return MapBuilder.of("processName", getProcessName());
   }



   /*******************************************************************************
    * Getter for processName
    * @see #withProcessName(String)
    *******************************************************************************/
   public String getProcessName()
   {
      return (this.processName);
   }



   /*******************************************************************************
    * Setter for processName
    * @see #withProcessName(String)
    *******************************************************************************/
   public void setProcessName(String processName)
   {
      this.processName = processName;
   }



   /*******************************************************************************
    * Fluent setter for processName
    *
    * @param processName
    * The name of the process to run.  Required to be a valid process in the instance,
    * and associated with the table in which this menu item is defined (via process.tableName).
    * @return this
    *******************************************************************************/
   public QMenuItemRunProcess withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /***************************************************************************
    * Validates that the run process menu item has a valid process name and
    * that the process exists and is associated with the correct table.
    *
    * <p>Ensures that:</p>
    * <ul>
    *   <li>A process name is specified</li>
    *   <li>The process exists in the QQQ instance</li>
    *   <li>The process is associated with the same table as the menu item
    *       (if parent is a QTableMetaData)</li>
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

      if(validator.assertCondition(StringUtils.hasContent(getProcessName()), "Missing a processName for menu item [" + getLabel() + "] in " + parentObject))
      {
         QProcessMetaData process = qInstance.getProcess(getProcessName());
         if(validator.assertCondition(process != null, "Could not find process [" + getProcessName() + "], referenced in menu item [" + getLabel() + "] in " + parentObject))
         {
            if(parentObject instanceof QTableMetaData table)
            {
               validator.assertCondition(Objects.equals(table.getName(), process.getTableName()), "Process [" + getProcessName() + "] is not associated with table [" + table.getName() + "] in menu item [" + getLabel() + "] in " + parentObject);
            }
         }
      }
   }



   /***************************************************************************
    * Creates and returns a shallow copy of this run process menu item.
    *
    * @return a clone of this menu item
    ***************************************************************************/
   @Override
   public QMenuItemRunProcess clone()
   {
      QMenuItemRunProcess clone = (QMenuItemRunProcess) super.clone();

      return clone;
   }
}
