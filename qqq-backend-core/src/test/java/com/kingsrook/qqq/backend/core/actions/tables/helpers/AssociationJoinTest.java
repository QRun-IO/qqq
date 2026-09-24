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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Association identity and tuple conversion cannot depend on table-name guesses.
 *******************************************************************************/
class AssociationJoinTest extends BaseTest
{
   private QTableMetaData parent;
   private QTableMetaData child;
   private Association association;
   private QJoinMetaData registered;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp()
   {
      association = new Association().withName("care / primary").withAssociatedTableName("tupleChild").withJoinName("tupleJoin");
      parent = new QTableMetaData().withName("tupleParent").withAssociation(association)
         .withField(new QFieldMetaData("number", QFieldType.INTEGER)).withField(new QFieldMetaData("enabled", QFieldType.BOOLEAN));
      child = new QTableMetaData().withName("tupleChild")
         .withField(new QFieldMetaData("parentNumber", QFieldType.LONG)).withField(new QFieldMetaData("parentEnabled", QFieldType.BOOLEAN));
      registered = new QJoinMetaData().withName("tupleJoin").withLeftTable(parent.getName()).withRightTable(child.getName())
         .withJoinOn(new JoinOn("number", "parentNumber")).withJoinOn(new JoinOn("enabled", "parentEnabled"));
      QContext.getQInstance().addTable(parent);
      QContext.getQInstance().addTable(child);
      QContext.getQInstance().addJoin(registered);
   }



   /*******************************************************************************
    ** Zero and false are relationship values, and numeric attachment uses child types.
    *******************************************************************************/
   @Test
   void testTypedCompositeAssignmentPreservesZeroFalseAndParent() throws Exception
   {
      AssociationJoin join = AssociationJoin.resolve(parent, association);
      QRecord parentRecord = new QRecord().withValue("number", 0).withValue("enabled", false);
      QRecord childRecord = new QRecord().withValue("parentNumber", 99L).withValue("payload", "unchanged");
      join.assignParentValues(parentRecord, childRecord);
      assertThat(join.parentValues(parentRecord)).containsExactly(0L, false);
      assertEquals(join.parentValues(parentRecord), join.childValues(childRecord));
      assertEquals("unchanged", childRecord.getValueString("payload"));
      assertThat((Object) parentRecord.getValue("number")).isInstanceOf(Integer.class);
      assertThat((Object) childRecord.getValue("parentNumber")).isInstanceOf(Long.class);
   }



   /*******************************************************************************
    ** Null remains null instead of being confused with a numeric zero.
    *******************************************************************************/
   @Test
   void testNullComponentsArePreserved() throws Exception
   {
      AssociationJoin join = AssociationJoin.resolve(parent, association);
      QRecord parentRecord = new QRecord().withValue("number", null).withValue("enabled", false);
      QRecord childRecord = new QRecord().withValue("parentNumber", 99L);
      join.assignParentValues(parentRecord, childRecord);
      assertThat(join.parentValues(parentRecord)).containsExactly(null, false);
      assertEquals(join.parentValues(parentRecord), join.childValues(childRecord));
      assertThat(childRecord.getValues()).containsKey("parentNumber");
   }



   /*******************************************************************************
    ** Reorientation never mutates the registered join or its pairs.
    *******************************************************************************/
   @Test
   void testReversePairsAreIndependent() throws Exception
   {
      registered.setLeftTable(child.getName());
      registered.setRightTable(parent.getName());
      registered.setJoinOns(List.of(new JoinOn("parentNumber", "number"), new JoinOn("parentEnabled", "enabled")));
      AssociationJoin join = AssociationJoin.resolve(parent, association);
      QRecord childRecord = new QRecord();
      join.assignParentValues(new QRecord().withValue("number", 7).withValue("enabled", false), childRecord);
      assertThat(join.childValues(childRecord)).containsExactly(7L, false);
      join.getJoinOns().get(0).setLeftField("changed");
      assertEquals("parentNumber", registered.getJoinOns().get(0).getLeftField());
      assertEquals(child.getName(), registered.getLeftTable());
   }



   /*******************************************************************************
    ** Self joins use the same left-parent interpretation as metadata validation.
    *******************************************************************************/
   @Test
   void testSelfJoinKeepsParentOnLeft() throws Exception
   {
      parent.withField(new QFieldMetaData("parentNumber", QFieldType.LONG));
      association.setAssociatedTableName(parent.getName());
      registered.setRightTable(parent.getName());
      registered.setJoinOns(List.of(new JoinOn("number", "parentNumber")));
      AssociationJoin join = AssociationJoin.resolve(parent, association);
      QRecord childRecord = new QRecord().withValue("number", 99);
      join.assignParentValues(new QRecord().withValue("number", 7), childRecord);
      assertEquals(7L, childRecord.getValue("parentNumber"));
      assertEquals(99, childRecord.getValueInteger("number"));
   }



   /*******************************************************************************
    ** A same-name association cannot substitute another join or target.
    *******************************************************************************/
   @Test
   void testRejectsUndeclaredAndSubstitutedAssociations()
   {
      assertThrows(QException.class, () -> AssociationJoin.resolve(parent, new Association().withName("unknown")));
      assertThrows(QException.class, () -> AssociationJoin.resolve(parent,
         new Association().withName(association.getName()).withJoinName("other").withAssociatedTableName(child.getName())));
      assertThrows(QException.class, () -> AssociationJoin.resolve(parent,
         new Association().withName(association.getName()).withJoinName(registered.getName()).withAssociatedTableName(parent.getName())));
   }



   /*******************************************************************************
    ** Invalid endpoints and fields fail with a metadata error before assignment.
    *******************************************************************************/
   @Test
   void testRejectsInvalidEndpointsAndFields()
   {
      registered.setRightTable("unrelated");
      assertThrows(QException.class, () -> AssociationJoin.resolve(parent, association));
      registered.setRightTable(child.getName());
      registered.setJoinOns(List.of(new JoinOn("missing", "parentNumber")));
      assertThrows(QException.class, () -> AssociationJoin.resolve(parent, association));
      registered.setJoinOns(List.of(new JoinOn("number", "missing")));
      assertThrows(QException.class, () -> AssociationJoin.resolve(parent, association));
      registered.setJoinOns(List.of());
      assertThrows(QException.class, () -> AssociationJoin.resolve(parent, association));
   }
}
