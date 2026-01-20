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
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 ** Menu item that represents a built-in action provided by the QQQ framework.
 **
 ** <p>Built-in menu items are standard actions that are implemented by the
 ** framework itself, such as CRUD operations (New, Copy, Edit, Delete),
 ** bulk operations, system functions (Developer Mode, Audit), and process lists.</p>
 **
 ** <p>These items are distinguished from custom menu items (like running
 ** processes or downloading files) in that their behavior is predefined by
 ** the framework and does not require additional configuration.</p>
 **
 ** @see DefaultOptions
 *******************************************************************************/
public class QMenuItemBuiltIn extends QMenuItemBase
{

   /***************************************************************************
    * Interface marker for built-in option types.
    *
    * <p>This interface is implemented by enums that define available built-in
    * options, such as {@link DefaultOptions}.</p>
    ***************************************************************************/
   public interface BuiltInOptionInterface extends Serializable
   {
   }



   /***************************************************************************
    * Enumeration of default built-in menu item options provided by QQQ.
    *
    * <p>These options represent standard actions that can be used in menus
    * throughout a QQQ application. The options are grouped by category:</p>
    * <ul>
    *   <li>Single-record CRUD: NEW, COPY, EDIT, DELETE</li>
    *   <li>Bulk operations: BULK_LOAD, BULK_EDIT, BULK_EDIT_WITH_FILE, BULK_DELETE</li>
    *   <li>System functions: RUN_SCRIPT, DEVELOPER_MODE, AUDIT</li>
    *   <li>Lists of QQQ Processes: THIS_TABLE_PROCESS_LIST, ALL_TABLES_PROCESS_LIST</li>
    * </ul>
    ***************************************************************************/
   public enum DefaultOptions implements BuiltInOptionInterface
   {
      /** Create a new record. */
      NEW,

      /** Copy an existing record. */
      COPY,

      /** Edit the current record. */
      EDIT,

      /** Delete the current record. */
      DELETE,

      /** Bulk load records from a file. */
      BULK_LOAD,

      /** Bulk edit multiple records. */
      BULK_EDIT,

      /** Bulk edit multiple records using a file. */
      BULK_EDIT_WITH_FILE,

      /** Bulk delete multiple records. */
      BULK_DELETE,

      /** Run a script. */
      RUN_SCRIPT,

      /** Toggle developer mode. */
      DEVELOPER_MODE,

      /** View audit trail. */
      AUDIT,

      /** Display process list for the current table.  e.g.,
       * {@link com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData}
       * objects whose tableName is the table being viewed. */
      THIS_TABLE_PROCESS_LIST,

      /** Display process list for generic processes which are enabled for all
       *  tables, such as ScriptsMetaDataProvider.RUN_RECORD_SCRIPT_PROCESS_NAME */
      ALL_TABLES_PROCESS_LIST
   }



   private BuiltInOptionInterface option;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public QMenuItemBuiltIn()
   {
   }



   /*******************************************************************************
    ** Constructor that initializes the built-in option.
    **
    ** @param option the built-in option to use (e.g., DefaultOptions.NEW)
    *******************************************************************************/
   public QMenuItemBuiltIn(BuiltInOptionInterface option)
   {
      this.option = option;
   }



   /***************************************************************************
    * Returns the item type identifier for built-in menu items.
    *
    * @return always returns "BUILT_IN"
    ***************************************************************************/
   @Override
   public String getItemType()
   {
      return "BUILT_IN";
   }



   /***************************************************************************
    * Returns the values map containing the built-in option.
    *
    * <p>The map contains a single entry with key "option" and the option value.</p>
    *
    * @return a map containing the built-in option
    ***************************************************************************/
   @Override
   public Map<String, Serializable> getValues()
   {
      return MapBuilder.of("option", option);
   }



   /***************************************************************************
    * Validates that the built-in menu item has a valid option configured.
    *
    * <p>Ensures that the option field is not null before the menu item can
    * be used in a menu.</p>
    *
    * @param validator the validator instance to use for reporting errors
    * @param qInstance the QQQ instance being validated
    * @param parentObject the parent metadata object containing this menu item
    ***************************************************************************/
   @Override
   public void validate(QInstanceValidator validator, QInstance qInstance, QMetaDataObject parentObject)
   {
      super.validate(validator, qInstance, parentObject);

      validator.assertCondition(option != null, "Missing an option for menu item [" + getLabel() + "] in " + parentObject);
   }



   /*******************************************************************************
    * Getter for option
    * @see #withOption(BuiltInOptionInterface)
    *******************************************************************************/
   public BuiltInOptionInterface getOption()
   {
      return (this.option);
   }



   /*******************************************************************************
    * Setter for option
    * @see #withOption(BuiltInOptionInterface)
    *******************************************************************************/
   public void setOption(BuiltInOptionInterface option)
   {
      this.option = option;
   }



   /*******************************************************************************
    * Fluent setter for option
    *
    * @param option
    * Which built-in item to use
    * @return this
    *******************************************************************************/
   public QMenuItemBuiltIn withOption(BuiltInOptionInterface option)
   {
      this.option = option;
      return (this);
   }



   /***************************************************************************
    * Creates and returns a shallow copy of this built-in menu item.
    *
    * @return a clone of this menu item
    ***************************************************************************/
   @Override
   public QMenuItemBuiltIn clone()
   {
      QMenuItemBuiltIn clone = (QMenuItemBuiltIn) super.clone();

      return clone;
   }

}
