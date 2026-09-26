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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CompositeWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.AbstractBlockWidgetData;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIEnumSubSet;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIOneOf;


/*******************************************************************************
 **
 *******************************************************************************/
public class WidgetBlock implements Serializable, ToSchema
{
   @OpenAPIExclude()
   private static final QLogger LOG = QLogger.getLogger(WidgetBlock.class);

   @OpenAPIExclude()
   private final AbstractBlockWidgetData<?, ?, ?, ?> wrapped;



   /***************************************************************************
    **
    ***************************************************************************/
   public WidgetBlock(AbstractBlockWidgetData<?, ?, ?, ?> abstractBlockWidgetData)
   {
      this.wrapped = abstractBlockWidgetData;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Unique identifier for this block within it widget.  Used as a key for helpContents.")
   public String getBlockId()
   {
      return (this.wrapped.getBlockId());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public enum BlockType
   {
      BUTTON,
      AUDIO,
      BIG_NUMBER,
      COMPOSITE,
      DIVIDER,
      IMAGE,
      INPUT_FIELD,
      NUMBER_ICON_BADGE,
      PROGRESS_BAR,
      // todo? REVEAL,
      TABLE_SUB_ROW_DETAIL_ROW,
      TEXT,
      UP_OR_DOWN_NUMBER
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("What type of block to render.")
   public BlockType getBlockType()
   {
      /////////////////////////////////////////////////////////////////////////
      // a block type this version does not know is left out (blockTypeName //
      // still names it) rather than failing the whole response            //
      /////////////////////////////////////////////////////////////////////////
      try
      {
         return (BlockType.valueOf(this.wrapped.getBlockTypeName()));
      }
      catch(IllegalArgumentException | NullPointerException e)
      {
         return (null);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Values to show in the block, or otherwise control its behavior.  Different fields based on blockType.")
   @OpenAPIOneOf()
   public WidgetBlockValues getValues()
   {
      return (WidgetBlockValues.of(this.wrapped.getValues()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Styles to apply to the block.  Different fields based on blockType.")
   @OpenAPIOneOf()
   public WidgetBlockStyles getStyles()
   {
      return (WidgetBlockStyles.of(this.wrapped.getStyles()));
   }

   // todo link, links

   // todo tooltip, tooltips



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Optional field name (e.g,. from a process's set of fields) to act as a 'guard' for the block - e.g., only include it in the UI if the value for this field is true")
   public String getConditional()
   {
      return (this.wrapped.getConditional());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("For COMPOSITE type blocks, a list of sub-blocks.")
   @OpenAPIListItems(value = WidgetBlock.class, useRef = true)
   public List<WidgetBlock> getSubBlocks()
   {
      if(this.wrapped instanceof CompositeWidgetData compositeWidgetData)
      {
         return (compositeWidgetData.getBlocks() == null ? null : compositeWidgetData.getBlocks().stream().map(b -> new WidgetBlock(b)).toList());
      }

      return (null);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("For COMPOSITE type blocks, optional control to make the widget appear modally")
   public CompositeWidgetData.ModalMode getModalMode()
   {
      if(this.wrapped instanceof CompositeWidgetData compositeWidgetData)
      {
         return (compositeWidgetData.getModalMode());
      }

      return (null);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class LayoutSubSet implements OpenAPIEnumSubSet.EnumSubSet<CompositeWidgetData.Layout>
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public EnumSet<CompositeWidgetData.Layout> getSubSet()
      {
         return EnumSet.of(
            CompositeWidgetData.Layout.FLEX_COLUMN,
            CompositeWidgetData.Layout.FLEX_ROW_WRAPPED,
            CompositeWidgetData.Layout.FLEX_ROW_SPACE_BETWEEN,
            CompositeWidgetData.Layout.FLEX_ROW_CENTER,
            CompositeWidgetData.Layout.TABLE_SUB_ROW_DETAILS,
            CompositeWidgetData.Layout.BADGES_WRAPPER
         );
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("For COMPOSITE type blocks, an indicator of how the sub-blocks should be laid out")
   @OpenAPIEnumSubSet(LayoutSubSet.class)
   public CompositeWidgetData.Layout getLayout()
   {
      if(this.wrapped instanceof CompositeWidgetData compositeWidgetData && compositeWidgetData.getLayout() != null)
      {
         CompositeWidgetData.Layout layout = compositeWidgetData.getLayout();
         if(new LayoutSubSet().getSubSet().contains(layout))
         {
            return (layout);
         }
         else
         {
            LOG.info("Layout [" + layout + "] is not in the subset used by this version.  It will not be returned.");
         }
      }

      return (null);
   }


   /* todo
   private Map<String, Serializable> styleOverrides = new HashMap<>();
   private String                    overlayHtml;
   private Map<String, Serializable> overlayStyleOverrides = new HashMap<>();
   */



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Name of the block's type, as the backend declares it (including types this API version does not enumerate in blockType).")
   public String getBlockTypeName()
   {
      return (this.wrapped.getBlockTypeName());
   }

}
