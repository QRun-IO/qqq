/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.instances.QInstanceHelpContentManager;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpRole;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.utils.collections.MutableList;


/*******************************************************************************
 ** A section of fields - a logical grouping.
 ** TODO - this class should be named QTableSection!
 *******************************************************************************/
public class QFieldSection implements QMetaDataObject, Cloneable
{
   private String name;
   private String label;
   private Tier   tier;

   private List<String> fieldNames;
   private String       widgetName;
   private QIcon        icon;

   private boolean isHidden = false;
   private Integer gridColumns;

   private List<QHelpContent> helpContents;

   private Map<QFieldSectionAlternativeTypeInterface, QFieldSection> alternatives;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldSection()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldSection(String name, String label, QIcon icon, Tier tier, List<String> fieldNames)
   {
      this.name = name;
      this.label = label;
      this.icon = icon;
      this.tier = tier;
      this.fieldNames = fieldNames == null ? null : new MutableList<>(fieldNames);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldSection(String name, QIcon icon, Tier tier, List<String> fieldNames)
   {
      this.name = name;
      this.icon = icon;
      this.tier = tier;
      this.fieldNames = fieldNames == null ? null : new MutableList<>(fieldNames);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldSection(String name, QIcon icon, Tier tier)
   {
      this.name = name;
      this.icon = icon;
      this.tier = tier;
   }



   /*******************************************************************************
    ** Getter for name
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }



   /*******************************************************************************
    ** Setter for name
    **
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    **
    *******************************************************************************/
   public QFieldSection withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    **
    *******************************************************************************/
   public String getLabel()
   {
      return label;
   }



   /*******************************************************************************
    ** Setter for label
    **
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    **
    *******************************************************************************/
   public QFieldSection withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tier
    **
    *******************************************************************************/
   public Tier getTier()
   {
      return tier;
   }



   /*******************************************************************************
    ** Setter for tier
    **
    *******************************************************************************/
   public void setTier(Tier tier)
   {
      this.tier = tier;
   }



   /*******************************************************************************
    ** Fluent setter for tier
    **
    *******************************************************************************/
   public QFieldSection withTier(Tier tier)
   {
      this.tier = tier;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldNames
    **
    *******************************************************************************/
   public List<String> getFieldNames()
   {
      return fieldNames;
   }



   /*******************************************************************************
    ** Setter for fieldNames
    **
    *******************************************************************************/
   public void setFieldNames(List<String> fieldNames)
   {
      this.fieldNames = fieldNames == null ? null : new MutableList<>(fieldNames);
   }



   /*******************************************************************************
    ** Fluent setter for fieldNames
    **
    *******************************************************************************/
   public QFieldSection withFieldNames(List<String> fieldNames)
   {
      this.fieldNames = fieldNames;
      return (this);
   }



   /*******************************************************************************
    ** Getter for icon
    **
    *******************************************************************************/
   public QIcon getIcon()
   {
      return icon;
   }



   /*******************************************************************************
    ** Setter for icon
    **
    *******************************************************************************/
   public void setIcon(QIcon icon)
   {
      this.icon = icon;
   }



   /*******************************************************************************
    ** Fluent setter for icon
    **
    *******************************************************************************/
   public QFieldSection withIcon(QIcon icon)
   {
      this.icon = icon;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isHidden
    **
    *******************************************************************************/
   public boolean getIsHidden()
   {
      return (isHidden);
   }



   /*******************************************************************************
    ** Setter for isHidden
    **
    *******************************************************************************/
   public void setIsHidden(boolean isHidden)
   {
      this.isHidden = isHidden;
   }



   /*******************************************************************************
    ** Fluent Setter for isHidden
    **
    *******************************************************************************/
   public QFieldSection withIsHidden(boolean isHidden)
   {
      this.isHidden = isHidden;
      return (this);
   }



   /*******************************************************************************
    ** Getter for widgetName
    **
    *******************************************************************************/
   public String getWidgetName()
   {
      return widgetName;
   }



   /*******************************************************************************
    ** Setter for widgetName
    **
    *******************************************************************************/
   public void setWidgetName(String widgetName)
   {
      this.widgetName = widgetName;
   }



   /*******************************************************************************
    ** Fluent setter for widgetName
    **
    *******************************************************************************/
   public QFieldSection withWidgetName(String widgetName)
   {
      this.widgetName = widgetName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for gridColumns
    **
    *******************************************************************************/
   public Integer getGridColumns()
   {
      return gridColumns;
   }



   /*******************************************************************************
    ** Setter for gridColumns
    **
    *******************************************************************************/
   public void setGridColumns(Integer gridColumns)
   {
      this.gridColumns = gridColumns;
   }



   /*******************************************************************************
    ** Fluent setter for gridColumns
    **
    *******************************************************************************/
   public QFieldSection withGridColumns(Integer gridColumns)
   {
      this.gridColumns = gridColumns;
      return (this);
   }



   /*******************************************************************************
    ** Getter for helpContents
    *******************************************************************************/
   public List<QHelpContent> getHelpContents()
   {
      return (this.helpContents);
   }



   /*******************************************************************************
    ** Setter for helpContents
    *******************************************************************************/
   public void setHelpContents(List<QHelpContent> helpContents)
   {
      this.helpContents = helpContents;
   }



   /*******************************************************************************
    ** Fluent setter for helpContents
    *******************************************************************************/
   public QFieldSection withHelpContents(List<QHelpContent> helpContents)
   {
      this.helpContents = helpContents;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for adding 1 helpContent
    *******************************************************************************/
   public QFieldSection withHelpContent(QHelpContent helpContent)
   {
      if(this.helpContents == null)
      {
         this.helpContents = new ArrayList<>();
      }

      QInstanceHelpContentManager.putHelpContentInList(helpContent, this.helpContents);
      return (this);
   }



   /*******************************************************************************
    ** remove a single helpContent based on its set of roles
    *******************************************************************************/
   public void removeHelpContent(Set<HelpRole> roles)
   {
      QInstanceHelpContentManager.removeHelpContentByRoleSetFromList(roles, this.helpContents);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public QFieldSection clone()
   {
      try
      {
         QFieldSection clone = (QFieldSection) super.clone();
         if(fieldNames != null)
         {
            clone.fieldNames = new ArrayList<>(fieldNames);
         }
         if(helpContents != null)
         {
            clone.helpContents = new ArrayList<>(helpContents);
         }
         if(icon != null)
         {
            clone.icon = icon.clone();
         }

         if(alternatives != null)
         {
            clone.alternatives = new HashMap<>();
            for(Map.Entry<QFieldSectionAlternativeTypeInterface, QFieldSection> entry : alternatives.entrySet())
            {
               clone.alternatives.put(entry.getKey(), entry.getValue().clone());
            }
         }

         return (clone);
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }



   /*******************************************************************************
    * Getter for alternatives
    * @see #withAlternatives(Map)
    *******************************************************************************/
   public Map<QFieldSectionAlternativeTypeInterface, QFieldSection> getAlternatives()
   {
      return (this.alternatives);
   }



   /*******************************************************************************
    * Setter for alternatives
    * @see #withAlternatives(Map)
    *******************************************************************************/
   public void setAlternatives(Map<QFieldSectionAlternativeTypeInterface, QFieldSection> alternatives)
   {
      this.alternatives = alternatives;
   }



   /*******************************************************************************
    * Fluent setter for alternatives
    *
    * @param alternatives
    * Alternative versions of the section, to be used as needed by various frontends,
    * based on the type (keys in the map).
    * @return this
    *******************************************************************************/
   public QFieldSection withAlternatives(Map<QFieldSectionAlternativeTypeInterface, QFieldSection> alternatives)
   {
      this.alternatives = alternatives;
      return (this);
   }



   /*******************************************************************************
    * Fluent setter for adding a single alternative
    *
    * @param type the type of alternative being added
    * @param alternative the section that should be used for the specified type
    * @return this
    *******************************************************************************/
   public QFieldSection withAlternative(QFieldSectionAlternativeTypeInterface type, QFieldSection alternative)
   {
      if(this.alternatives == null)
      {
         this.alternatives = new HashMap<>();
      }
      this.alternatives.put(type, alternative);
      return (this);
   }



   /*******************************************************************************
    * Fluent setter for adding a single alternative - which is a clone of the base
    * section, with customizations performed by a lambda.
    *
    * <p>e.g., to add (say) a virtual field, or otherwise change the field list, etc.
    * So you don't have to fully re-create the section - you start with a clone.</p>
    *
    * @param type the type of alternative being added
    * @param alternativeMaker lambada to modify a clone of this section.
    * @return this
    *******************************************************************************/
   public QFieldSection withAlternative(QFieldSectionAlternativeTypeInterface type, Consumer<QFieldSection> alternativeMaker)
   {
      if(this.alternatives == null)
      {
         this.alternatives = new HashMap<>();
      }

      QFieldSection alternative = this.clone();
      alternativeMaker.accept(alternative);

      this.alternatives.put(type, alternative);
      return (this);
   }


}
