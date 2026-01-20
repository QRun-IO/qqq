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

package com.kingsrook.qqq.backend.core.model.metadata.menus.adjusters;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.metadata.menus.defaults.QMenuDefaultViewScreenActionsMenu;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemBase;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemBuiltIn;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemDivider;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemInterface;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemSubList;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemSubMenu;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QMenuAdjuster 
 *******************************************************************************/
class QMenuAdjusterTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRemovingBuiltInOptions()
   {
      QMenuDefaultViewScreenActionsMenu menu = new QMenuDefaultViewScreenActionsMenu();

      //////////////////////////////////////////
      // this option is in the top-level list //
      //////////////////////////////////////////
      assertTrue(doesMenuHaveBuiltInOption(menu.getItems(), QMenuItemBuiltIn.DefaultOptions.THIS_TABLE_PROCESS_LIST));
      assertTrue(QMenuAdjuster.removeFirst(menu, new QMenuItemMatcher(QMenuItemBuiltIn.DefaultOptions.THIS_TABLE_PROCESS_LIST)));
      assertFalse(doesMenuHaveBuiltInOption(menu.getItems(), QMenuItemBuiltIn.DefaultOptions.THIS_TABLE_PROCESS_LIST));

      //////////////////////////////////
      // this option is in a sub list //
      //////////////////////////////////
      assertTrue(doesMenuHaveBuiltInOption(menu.getItems(), QMenuItemBuiltIn.DefaultOptions.NEW));
      assertTrue(QMenuAdjuster.removeFirst(menu, new QMenuItemMatcher(QMenuItemBuiltIn.DefaultOptions.NEW)));
      assertFalse(doesMenuHaveBuiltInOption(menu.getItems(), QMenuItemBuiltIn.DefaultOptions.NEW));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoFailureIfRemoveDoesntMatch()
   {
      QMenuDefaultViewScreenActionsMenu menu = new QMenuDefaultViewScreenActionsMenu();

      //////////////////////////////////////////////////////////////
      // a built-in that isn't in the menu shouldn't be a problem //
      //////////////////////////////////////////////////////////////
      assertFalse(doesMenuHaveBuiltInOption(menu.getItems(), CustomBuiltInOptions.RED));
      assertFalse(QMenuAdjuster.removeFirst(menu, new QMenuItemMatcher(CustomBuiltInOptions.RED)));
      assertFalse(doesMenuHaveBuiltInOption(menu.getItems(), CustomBuiltInOptions.RED));

      ///////////////////////////////////////////////////////////////
      // a class that isn't in the menu should not cause a failure //
      ///////////////////////////////////////////////////////////////
      Class<CustomMenuItem> customMenuItemClass = CustomMenuItem.class;
      assertFalse(doesMenuHaveItemOfType(menu.getItems(), customMenuItemClass));
      assertFalse(QMenuAdjuster.removeFirst(menu, new QMenuItemMatcher(customMenuItemClass)));
      assertFalse(doesMenuHaveItemOfType(menu.getItems(), customMenuItemClass));

      ///////////////////////////////////////////////////
      // a label that isn't in the menu should be fine //
      ///////////////////////////////////////////////////
      String label = "Foobar";
      assertFalse(doesMenuHaveItemWithLabel(menu.getItems(), label));
      assertFalse(QMenuAdjuster.removeFirst(menu, new QMenuItemMatcher(QCriteriaOperator.EQUALS, label)));
      assertFalse(doesMenuHaveItemWithLabel(menu.getItems(), label));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRemovingByClass()
   {
      QMenuDefaultViewScreenActionsMenu menu = new QMenuDefaultViewScreenActionsMenu();

      Class<QMenuDefaultViewScreenActionsMenu.FooterSubList> footerSubListClass = QMenuDefaultViewScreenActionsMenu.FooterSubList.class;
      assertTrue(doesMenuHaveItemOfType(menu.getItems(), footerSubListClass));
      assertTrue(QMenuAdjuster.removeFirst(menu, new QMenuItemMatcher(footerSubListClass)));
      assertFalse(doesMenuHaveItemOfType(menu.getItems(), footerSubListClass));

      /////////////////////////////////////////////////////////////////////////////
      // there are two dividers - first remove them one-by-one with remove-first //
      /////////////////////////////////////////////////////////////////////////////
      Class<QMenuItemDivider> dividerClass = QMenuItemDivider.class;
      assertTrue(doesMenuHaveItemOfType(menu.getItems(), dividerClass));
      assertTrue(QMenuAdjuster.removeFirst(menu, new QMenuItemMatcher(dividerClass)));
      assertTrue(doesMenuHaveItemOfType(menu.getItems(), dividerClass));
      assertTrue(QMenuAdjuster.removeFirst(menu, new QMenuItemMatcher(dividerClass)));
      assertFalse(doesMenuHaveItemOfType(menu.getItems(), dividerClass));

      //////////////////////////////////////////////////////
      // now do them both at the same time with removeAll //
      //////////////////////////////////////////////////////
      menu = new QMenuDefaultViewScreenActionsMenu();
      assertTrue(doesMenuHaveItemOfType(menu.getItems(), dividerClass));
      assertEquals(2, QMenuAdjuster.removeAll(menu, new QMenuItemMatcher(dividerClass)));
      assertFalse(doesMenuHaveItemOfType(menu.getItems(), dividerClass));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRemovingByLabel()
   {
      QMenuDefaultViewScreenActionsMenu menu = new QMenuDefaultViewScreenActionsMenu();

      assertTrue(doesMenuHaveItemWithLabel(menu.getItems(), "Copy"));
      assertTrue(QMenuAdjuster.removeFirst(menu, new QMenuItemMatcher(QCriteriaOperator.EQUALS, "Copy")));
      assertFalse(doesMenuHaveItemWithLabel(menu.getItems(), "Copy"));

      ///////////////////////////////////////////
      // there are two that start with "De"... //
      ///////////////////////////////////////////
      assertTrue(doesMenuHaveItemWithLabel(menu.getItems(), "Delete"));
      assertTrue(doesMenuHaveItemWithLabel(menu.getItems(), "Developer Mode"));
      assertEquals(2, QMenuAdjuster.removeAll(menu, new QMenuItemMatcher(QCriteriaOperator.STARTS_WITH, "De")));
      assertFalse(doesMenuHaveItemWithLabel(menu.getItems(), "Delete"));
      assertFalse(doesMenuHaveItemWithLabel(menu.getItems(), "Developer Mode"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAdding()
   {
      ////////////////////////////////////////////////////////////////////
      // add after the first item (NEW)                                 //
      // note, NEW is in a sub-list, so it comes back as index 1, not 0 //
      ////////////////////////////////////////////////////////////////////
      {
         QMenuDefaultViewScreenActionsMenu menu = new QMenuDefaultViewScreenActionsMenu();
         QMenuAdjuster.addAfter(menu, new CustomMenuItem(), new QMenuItemMatcher(QMenuItemBuiltIn.DefaultOptions.NEW));
         List<QMenuItemInterface> flatItems = flattenItems(menu.getItems());
         assertEquals("New", flatItems.get(1).getLabel());
         assertEquals("Custom", flatItems.get(2).getLabel());
      }

      /////////////////////////////////////
      // add before the first item (NEW) //
      /////////////////////////////////////
      {
         QMenuDefaultViewScreenActionsMenu menu = new QMenuDefaultViewScreenActionsMenu();
         QMenuAdjuster.addBefore(menu, new CustomMenuItem(), new QMenuItemMatcher(QMenuItemBuiltIn.DefaultOptions.NEW));
         List<QMenuItemInterface> flatItems = flattenItems(menu.getItems());
         assertEquals("Custom", flatItems.get(1).getLabel());
         assertEquals("New", flatItems.get(2).getLabel());
      }

      /////////////////////////////////////////////////////////
      // now add after the true first item (the header list) //
      /////////////////////////////////////////////////////////
      {
         QMenuDefaultViewScreenActionsMenu menu = new QMenuDefaultViewScreenActionsMenu();
         QMenuAdjuster.addAfter(menu, new CustomMenuItem(), new QMenuItemMatcher(QMenuDefaultViewScreenActionsMenu.HeaderSubList.class));
         assertEquals(QMenuDefaultViewScreenActionsMenu.HeaderSubList.class, menu.getItems().get(0).getClass());
         assertEquals("Custom", menu.getItems().get(1).getLabel());
      }

      /////////////////////////////////////////////
      // add before the first item (header list) //
      /////////////////////////////////////////////
      {
         QMenuDefaultViewScreenActionsMenu menu = new QMenuDefaultViewScreenActionsMenu();
         QMenuAdjuster.addBefore(menu, new CustomMenuItem(), new QMenuItemMatcher(QMenuDefaultViewScreenActionsMenu.HeaderSubList.class));
         assertEquals("Custom", menu.getItems().get(0).getLabel());
         assertEquals(QMenuDefaultViewScreenActionsMenu.HeaderSubList.class, menu.getItems().get(1).getClass());
      }

      //////////////////////////////////////////////////////////////////
      // do before and after the last item in the footer list (audit) //
      //////////////////////////////////////////////////////////////////
      {
         QMenuDefaultViewScreenActionsMenu menu = new QMenuDefaultViewScreenActionsMenu();
         QMenuAdjuster.addAfter(menu, new CustomMenuItem(), new QMenuItemMatcher(QMenuItemBuiltIn.DefaultOptions.AUDIT));
         List<QMenuItemInterface> flatItems = flattenItems(menu.getItems());
         assertEquals("Audit", flatItems.get(flatItems.size() - 2).getLabel());
         assertEquals("Custom", flatItems.get(flatItems.size() - 1).getLabel());
      }
      {
         QMenuDefaultViewScreenActionsMenu menu = new QMenuDefaultViewScreenActionsMenu();
         QMenuAdjuster.addBefore(menu, new CustomMenuItem(), new QMenuItemMatcher(QMenuItemBuiltIn.DefaultOptions.AUDIT));
         List<QMenuItemInterface> flatItems = flattenItems(menu.getItems());
         assertEquals("Custom", flatItems.get(flatItems.size() - 2).getLabel());
         assertEquals("Audit", flatItems.get(flatItems.size() - 1).getLabel());
      }

      /////////////////////////////////////////////////
      // and before and after the footer list itself //
      /////////////////////////////////////////////////
      {
         QMenuDefaultViewScreenActionsMenu menu = new QMenuDefaultViewScreenActionsMenu();
         QMenuAdjuster.addAfter(menu, new CustomMenuItem(), new QMenuItemMatcher(QMenuDefaultViewScreenActionsMenu.FooterSubList.class));
         assertEquals(QMenuDefaultViewScreenActionsMenu.FooterSubList.class, menu.getItems().get(menu.getItems().size() - 2).getClass());
         assertEquals("Custom", menu.getItems().get(menu.getItems().size() - 1).getLabel());
      }
      {
         QMenuDefaultViewScreenActionsMenu menu = new QMenuDefaultViewScreenActionsMenu();
         QMenuAdjuster.addBefore(menu, new CustomMenuItem(), new QMenuItemMatcher(QMenuDefaultViewScreenActionsMenu.FooterSubList.class));
         assertEquals("Custom", menu.getItems().get(menu.getItems().size() - 2).getLabel());
         assertEquals(QMenuDefaultViewScreenActionsMenu.FooterSubList.class, menu.getItems().get(menu.getItems().size() - 1).getClass());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReplaceItems()
   {
      QMenuDefaultViewScreenActionsMenu menu = new QMenuDefaultViewScreenActionsMenu();

      assertTrue(doesMenuHaveItemWithLabel(menu.getItems(), "New"));
      assertFalse(doesMenuHaveItemWithLabel(menu.getItems(), "Create"));
      QMenuAdjuster.replaceItem(menu,
         new QMenuItemBuiltIn(QMenuItemBuiltIn.DefaultOptions.NEW).withLabel("Create"),
         new QMenuItemMatcher(QMenuItemBuiltIn.DefaultOptions.NEW));
      assertFalse(doesMenuHaveItemWithLabel(menu.getItems(), "New"));
      assertTrue(doesMenuHaveItemWithLabel(menu.getItems(), "Create"));

   }



   /***************************************************************************
    *
    ***************************************************************************/
   private List<QMenuItemInterface> flattenItems(List<QMenuItemInterface> input)
   {
      List<QMenuItemInterface> rs = new ArrayList<>();
      for(QMenuItemInterface item : input)
      {
         rs.add(item);

         if(item instanceof QMenuItemSubList subList)
         {
            rs.addAll(flattenItems(subList.getItems()));
         }

         if(item instanceof QMenuItemSubMenu subMenu)
         {
            rs.addAll(flattenItems(subMenu.getItems()));
         }
      }
      return (rs);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private boolean doesMenuMatchPredicate(List<QMenuItemInterface> items, Predicate<QMenuItemInterface> predicate)
   {
      for(QMenuItemInterface item : CollectionUtils.nonNullList(items))
      {
         if(predicate.test(item))
         {
            return (true);
         }

         if(item instanceof QMenuItemSubList subList && doesMenuMatchPredicate(subList.getItems(), predicate))
         {
            return (true);
         }

         if(item instanceof QMenuItemSubMenu subMenu && doesMenuMatchPredicate(subMenu.getItems(), predicate))
         {
            return (true);
         }
      }

      return (false);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private boolean doesMenuHaveBuiltInOption(List<QMenuItemInterface> items, QMenuItemBuiltIn.BuiltInOptionInterface option)
   {
      return (doesMenuMatchPredicate(items, item -> item instanceof QMenuItemBuiltIn b && b.getOption().equals(option)));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private boolean doesMenuHaveItemOfType(List<QMenuItemInterface> items, Class<? extends QMenuItemInterface> itemType)
   {
      return (doesMenuMatchPredicate(items, item -> itemType.isAssignableFrom(item.getClass())));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private boolean doesMenuHaveItemWithLabel(List<QMenuItemInterface> items, String label)
   {
      return (doesMenuMatchPredicate(items, item -> Objects.equals(item.getLabel(), label)));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private enum CustomBuiltInOptions implements QMenuItemBuiltIn.BuiltInOptionInterface
   {
      RED
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static class CustomMenuItem extends QMenuItemBase
   {
      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public CustomMenuItem()
      {
         setLabel("Custom");
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public String getItemType()
      {
         return "CUSTOM";
      }
   }

}