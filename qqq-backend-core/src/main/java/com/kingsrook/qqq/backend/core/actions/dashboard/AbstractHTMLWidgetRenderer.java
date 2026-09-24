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

package com.kingsrook.qqq.backend.core.actions.dashboard;


import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionCheckResult;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.QQueryFilterDeduper;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Base class for rendering qqq HTML dashboard widgets
 **
 *******************************************************************************/
public abstract class AbstractHTMLWidgetRenderer extends AbstractWidgetRenderer
{
   private static final QLogger LOG = QLogger.getLogger(AbstractHTMLWidgetRenderer.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String openTopLevelBulletList()
   {
      return ("""
         <div style="padding-left: 2rem;">
            <ul>""");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String closeTopLevelBulletList()
   {
      return ("""
            </ul>
         </div>""");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected String bulletItalics(String text)
   {
      return ("<li><i>" + text + "</i></li>");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected String bulletLink(String href, String text)
   {
      return ("<li><a href=\"" + href + "\">" + text + "</a></li>");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected String bulletNameLink(String name, String href, String text)
   {
      return (bulletNameValue(name, "<a href=\"" + href + "\">" + text + "</a>"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected String bulletNameValue(String name, String value)
   {
      return ("<li><b>" + name + "</b> &nbsp; " + Objects.requireNonNullElse(value, "--") + "</li>");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String linkTableBulkLoad(RenderWidgetInput input, String tableName) throws QException
   {
      String tablePath = QContext.getQInstance().getTablePath(tableName);
      if(tablePath == null || !doesHaveTablePermission(tableName))
      {
         return (null);
      }
      return (tablePath + "/" + tableName + ".bulkInsert");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String linkTableBulkLoadChildren(RenderWidgetInput input, String tableName) throws QException
   {
      String tablePath = QContext.getQInstance().getTablePath(tableName);
      if(tablePath == null || !doesHaveTablePermission(tableName))
      {
         return (null);
      }

      return ("#/launchProcess=" + tableName + ".bulkInsert");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String linkTableCreate(RenderWidgetInput input, String tableName) throws QException
   {
      String tablePath = QContext.getQInstance().getTablePath(tableName);
      if(tablePath == null || !doesHaveTablePermission(tableName))
      {
         return (null);
      }
      return (tablePath + "/create");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String linkTableCreateWithDefaultValues(RenderWidgetInput input, String tableName, Map<String, Serializable> defaultValues) throws QException
   {
      String tablePath = QContext.getQInstance().getTablePath(tableName);
      if(tablePath == null || !doesHaveTablePermission(tableName))
      {
         return (null);
      }
      return (tablePath + "/create#defaultValues=" + URLEncoder.encode(JsonUtils.toJson(defaultValues), StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getCountLink(RenderWidgetInput input, String tableName, QQueryFilter filter, int count) throws QException
   {
      String totalString = QValueFormatter.formatValue(DisplayFormat.COMMAS, count);
      String tablePath   = QContext.getQInstance().getTablePath(tableName);
      if(tablePath == null || filter == null || !doesHaveTablePermission(tableName))
      {
         return (totalString);
      }
      filter = QQueryFilterDeduper.dedupeFilter(filter);
      return ("<a href='" + tablePath + "?filter=" + JsonUtils.toJson(filter) + "'>" + totalString + "</a>");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void addTableFilterToListIfPermissed(RenderWidgetInput input, String tableName, List<String> urls, QQueryFilter filter) throws QException
   {
      String tablePath = QContext.getQInstance().getTablePath(tableName);
      if(tablePath == null || !doesHaveTablePermission(tableName))
      {
         return;
      }

      filter = QQueryFilterDeduper.dedupeFilter(filter);
      urls.add(tablePath + "?filter=" + JsonUtils.toJson(filter));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String linkTableFilterUnencoded(RenderWidgetInput input, String tableName, QQueryFilter filter) throws QException
   {
      String tablePath = QContext.getQInstance().getTablePath(tableName);
      if(tablePath == null || !doesHaveTablePermission(tableName))
      {
         return (null);
      }

      filter = QQueryFilterDeduper.dedupeFilter(filter);
      return (tablePath + "?filter=" + JsonUtils.toJson(filter));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String linkTableFilter(String tableName, QQueryFilter filter) throws QException
   {
      String tablePath = QContext.getQInstance().getTablePath(tableName);
      if(tablePath == null || !doesHaveTablePermission(tableName))
      {
         return (null);
      }

      filter = QQueryFilterDeduper.dedupeFilter(filter);
      return (tablePath + "?filter=" + URLEncoder.encode(JsonUtils.toJson(filter), StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String aHrefTableFilterNoOfRecords(String tableName, QQueryFilter filter, Integer noOfRecords, String singularLabel, String pluralLabel) throws QException
   {
      return (aHrefTableFilterNoOfRecords(tableName, filter, noOfRecords, singularLabel, pluralLabel, false));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String aHrefTableFilterNoOfRecords(String tableName, QQueryFilter filter, Integer noOfRecords, String singularLabel, String pluralLabel, boolean onlyLinkCount) throws QException
   {
      String plural      = StringUtils.plural(noOfRecords, singularLabel, pluralLabel);
      String countString = QValueFormatter.formatValue(DisplayFormat.COMMAS, noOfRecords);
      String displayText = StringUtils.hasContent(plural) ? (" " + plural) : "";
      String tablePath   = QContext.getQInstance().getTablePath(tableName);
      if(tablePath == null || !doesHaveTablePermission(tableName))
      {
         return (countString + displayText);
      }

      String href = linkTableFilter(tableName, filter);
      if(onlyLinkCount)
      {
         return ("<a href=\"" + href + "\">" + countString + "</a>" + displayText);
      }
      else
      {
         return ("<a href=\"" + href + "\">" + countString + displayText + "</a>");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String aHrefViewRecord(String tableName, Serializable id, String linkText) throws QException
   {
      String tablePath = QContext.getQInstance().getTablePath(tableName);
      if(tablePath == null || !doesHaveTablePermission(tableName))
      {
         return (linkText);
      }

      return ("<a href=\"" + linkRecordView(tableName, id) + "\">" + linkText + "</a>");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String linkRecordEdit(String tableName, Serializable recordId) throws QException
   {
      String tablePath = QContext.getQInstance().getTablePath(tableName);
      if(tablePath == null || !doesHaveTablePermission(tableName))
      {
         return (null);
      }
      return (tablePath + "/" + recordId + "/edit");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String linkRecordView(String tableName, Serializable recordId) throws QException
   {
      String tablePath = QContext.getQInstance().getTablePath(tableName);
      if(tablePath == null || !doesHaveTablePermission(tableName))
      {
         return (null);
      }

      return (tablePath + "/" + recordId);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String linkProcessForFilter(String processName, QQueryFilter filter) throws QException
   {
      QProcessMetaData process = QContext.getQInstance().getProcess(processName);
      if(process == null)
      {
         return (null);
      }
      String tableName = process.getTableName();
      if(tableName == null)
      {
         return (null);
      }

      String tablePath = QContext.getQInstance().getTablePath(tableName);
      filter = QQueryFilterDeduper.dedupeFilter(filter);
      return (tablePath + "/" + processName + "?recordsParam=filterJSON&filterJSON=" + URLEncoder.encode(JsonUtils.toJson(filter), StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String linkProcessForRecord(String processName, Serializable recordId) throws QException
   {
      QProcessMetaData process   = QContext.getQInstance().getProcess(processName);
      String           tableName = process.getTableName();
      String           tablePath = QContext.getQInstance().getTablePath(tableName);

      return (tablePath + "/" + recordId + "/" + processName);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String linkTableCreateChild(RenderWidgetInput input, String childTableName, Map<String, Serializable> defaultValues) throws QException
   {
      return (linkTableCreateChild(input, childTableName, defaultValues, defaultValues.keySet()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String aHrefTableCreateChild(RenderWidgetInput input, String childTableName, Map<String, Serializable> defaultValues) throws QException
   {
      return (aHrefTableCreateChild(input, childTableName, defaultValues, defaultValues.keySet()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String linkTableCreateChild(RenderWidgetInput input, String childTableName, Map<String, Serializable> defaultValues, Set<String> disabledFields) throws QException
   {
      String tablePath = QContext.getQInstance().getTablePath(childTableName);
      if(tablePath == null || !doesHaveTablePermission(childTableName))
      {
         return (null);
      }

      Map<String, Integer> disabledFieldsMap = disabledFields.stream().collect(Collectors.toMap(k -> k, k -> 1));

      return ("#/createChild=" + childTableName
         + "/defaultValues=" + URLEncoder.encode(JsonUtils.toJson(defaultValues), StandardCharsets.UTF_8).replaceAll("\\+", "%20")
         + "/disabledFields=" + URLEncoder.encode(JsonUtils.toJson(disabledFieldsMap), StandardCharsets.UTF_8).replaceAll("\\+", "%20"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getChipElement(String icon, String label, String color) throws QException
   {
      color = color != null ? color : "info";
      color = StringUtils.ucFirst(color);

      String html = "<span style='display: flex;'>";
      html += "<div style='overflow: hidden; flex: none; display: flex; align-content: flex-start; align-items: center; height: 24px; padding-right: 8px; font-size: 13px; font-weight: 500; border: 1px solid; border-radius: 16px; color: " + color + "'>";
      if(icon != null)
      {
         html += "<span style='font-size: 16px; padding: 5px' class='material-icons-round notranslate MuiIcon-root MuiIcon-fontSizeInherit MuiChip-icon MuiChip-iconSmall MuiChip-iconColor" + color + "'>" + icon + "</span>";
      }
      html += "<span class='MuiChip-label MuiChip-labelSmall'>" + label + "</span></div></span>";
      return (html);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String aHrefTableCreateChild(RenderWidgetInput input, String childTableName, Map<String, Serializable> defaultValues, Set<String> disabledFields) throws QException
   {
      String tablePath = QContext.getQInstance().getTablePath(childTableName);
      if(tablePath == null || !doesHaveTablePermission(childTableName))
      {
         return (null);
      }

      return ("<a href=\"" + linkTableCreateChild(input, childTableName, defaultValues, defaultValues.keySet()) + "\">Create new</a>");
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static boolean doesHaveTablePermission(String tableName)
   {
      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      if(table == null)
      {
         LOG.debug("Returning no-permission for table, because of unrecognized table name", logPair("tableName", tableName));
         return (false);
      }

      PermissionCheckResult permissionResult = PermissionsHelper.getPermissionCheckResult(new MetaDataInput(), table);
      if(PermissionCheckResult.ALLOW.equals(permissionResult))
      {
         return (true);
      }

      return (false);
   }
}
