/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.metadata;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Represents the graph of table-to-table joins in a QQQ Instance, treating
 ** each join as a non-directional edge between two tables.
 **
 ** <p>The primary purpose of this class is to answer the question: "given a
 ** starting table, what other tables can be reached through joins, and via
 ** which paths?"  This is used during instance enrichment and validation to
 ** discover multi-hop join paths (e.g., order → orderLine → item).</p>
 **
 ** <p>Key behaviors:</p>
 ** <ul>
 **    <li><b>Deduplication:</b> If the instance defines both A → B and B → A
 **        joins (on the same fields), they are normalized into a single edge
 **        so the graph does not contain redundant paths.</li>
 **    <li><b>Flipped-join awareness:</b> Even though duplicate joins are
 **        collapsed into one edge, the {@code flippedJoins} map remembers all
 **        original join names for each table pair, so that
 **        {@link JoinConnectionList#matchesJoinPath(List, JoinGraph, QInstance)}
 **        can match a path by any equivalent join name, not just the one
 **        stored in the edge.</li>
 **    <li><b>Path-length limiting:</b> To keep traversal performant on large
 **        instances, paths longer than {@code maxPathLength} (default 3) are
 **        pruned.  This limit is configurable via the system property
 **        {@code qqq.instance.joinGraph.maxPathLength} or environment variable
 **        {@code QQQ_INSTANCE_JOIN_GRAPH_MAX_PATH_LENGTH}.</li>
 ** </ul>
 *******************************************************************************/
public class JoinGraph
{
   private static final QLogger LOG = QLogger.getLogger(JoinGraph.class);

   private Set<Edge> edges = new HashSet<>();

   //////////////////////////////////////////////////////////////////////////////
   // since the joins are considered non-directional edges, if an instance has //
   // joins A -> B, and B -> A, only one of them gets built (say, A -> B)      //
   // But then later, in {@code JoinConnectionList.matchesJoinPath}, a false   //
   // negative could be returned if the other one (B -> A) was tested for.     //
   // so - this listing hash keeps track of all joins that are equivalent      //
   // to one another from this POV, so that any/all can be considered to match //
   //////////////////////////////////////////////////////////////////////////////
   private ListingHash<NormalizedJoin, String> flippedJoins = new ListingHash<>();

   ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // as an instance grows, with the number of joins (say, more than 50?), especially as they may have a lot of connections, //
   // it can become very very slow to process a full join graph (e.g., 10 seconds, maybe much worse, per Big-O...)           //
   // also, it's not frequently useful to look at a join path that's more than a handful of tables long.                     //
   // thus - this property exists - to limit the max length of a join path.  Keeping it small keeps instance enrichment      //
   // and validation reasonably performant, at the possible cost of, some join-path that's longer than this limit may not    //
   // be found - but - chances are, you don't want some 12-element join path to be used anyway, thus, this makes sense.      //
   // but - it can be adjusted, per system property or ENV var.                                                              //
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   private int maxPathLength = new QMetaDataVariableInterpreter().getIntegerFromPropertyOrEnvironment("qqq.instance.joinGraph.maxPathLength", "QQQ_INSTANCE_JOIN_GRAPH_MAX_PATH_LENGTH", 3);



   /*******************************************************************************
    ** Graph edge (no graph nodes needed in here)
    *******************************************************************************/
   private record Edge(String joinName, String leftTable, String rightTable)
   {
   }



   /***************************************************************************
    ** In this class, we are treating joins as non-directional graph edges - so -
    ** use this class to "normalize" what may otherwise be duplicated joins in the
    ** qInstance (e.g., A -> B and B -> A -- in the instance, those are valid, but
    ** in our graph here, we want to consider those the same).
    ***************************************************************************/
   private record NormalizedJoin(String tableA, String tableB, List<String> joinFieldA, List<String> joinFieldB)
   {
      /***************************************************************************
       *
       ***************************************************************************/
      static NormalizedJoin build(QJoinMetaData joinMetaData)
      {
         List<String> leftFields  = joinMetaData.getJoinOns().stream().map(jo -> jo.getLeftField()).toList();
         List<String> rightFields = joinMetaData.getJoinOns().stream().map(jo -> jo.getRightField()).toList();

         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // to normalize the join, we'll first compare table names.  if they match (a self-join), then we'll compare join fields //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         Boolean leftFirst    = null;
         int     tableCompare = joinMetaData.getLeftTable().compareTo(joinMetaData.getRightTable());
         if(tableCompare < 0)
         {
            leftFirst = true;
         }
         else if(tableCompare > 0)
         {
            leftFirst = false;
         }
         else
         {
            for(int i = 0; i < Math.min(leftFields.size(), rightFields.size()); i++)
            {
               int fieldCompare = leftFields.get(i).compareTo(rightFields.get(i));
               if(fieldCompare < 0)
               {
                  leftFirst = true;
                  break;
               }
               else if(fieldCompare > 0)
               {
                  leftFirst = false;
                  break;
               }
            }
         }

         if(leftFirst == null)
         {
            ///////////////////////////////////////////////////////////////////////////////////////////////////
            // if the sides of the joins were identical (e.g., foo.id -> foo.id), that's probably bad setup. //
            // so warn the user about it, and choose something...                                            //
            ///////////////////////////////////////////////////////////////////////////////////////////////////
            LOG.warn("There appears to be a join between a table and itself, with all matching join-fields.  This could introduce unexpected behavior.", logPair("joinName", joinMetaData.getName()));
            leftFirst = true;
         }

         if(leftFirst)
         {
            return (new NormalizedJoin(joinMetaData.getLeftTable(), joinMetaData.getRightTable(), leftFields, rightFields));
         }
         else
         {
            return (new NormalizedJoin(joinMetaData.getRightTable(), joinMetaData.getLeftTable(), rightFields, leftFields));
         }
      }
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public JoinGraph(QInstance qInstance)
   {
      Set<NormalizedJoin> usedJoins = new HashSet<>();
      for(QJoinMetaData join : CollectionUtils.nonNullMap(qInstance.getJoins()).values())
      {
         NormalizedJoin normalizedJoin = NormalizedJoin.build(join);
         flippedJoins.add(normalizedJoin, join.getName());

         if(usedJoins.contains(normalizedJoin))
         {
            continue;
         }

         usedJoins.add(normalizedJoin);
         edges.add(new Edge(join.getName(), join.getLeftTable(), join.getRightTable()));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public record JoinConnection(String joinTable, String viaJoinName) implements Comparable<JoinConnection>
   {

      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public int compareTo(JoinConnection that)
      {
         Comparator<JoinConnection> comparator = Comparator.comparing((JoinConnection jc) -> jc.joinTable())
            .thenComparing((JoinConnection jc) -> jc.viaJoinName());
         return (comparator.compare(this, that));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public record JoinConnectionList(List<JoinConnection> list) implements Comparable<JoinConnectionList>
   {

      /*******************************************************************************
       **
       *******************************************************************************/
      public JoinConnectionList copy()
      {
         return new JoinConnectionList(new ArrayList<>(list));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public int compareTo(JoinConnectionList that)
      {
         if(this.equals(that))
         {
            return (0);
         }

         for(int i = 0; i < Math.min(this.list.size(), that.list.size()); i++)
         {
            int comp = this.list.get(i).compareTo(that.list.get(i));
            if(comp != 0)
            {
               return (comp);
            }
         }

         return (this.list.size() - that.list.size());
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public boolean matchesJoinPath(List<String> joinPath)
      {
         if(list.size() != joinPath.size())
         {
            return (false);
         }

         for(int i = 0; i < list.size(); i++)
         {
            if(!list.get(i).viaJoinName().equals(joinPath.get(i)))
            {
               return (false);
            }
         }

         return (true);
      }



      /*******************************************************************************
       * version of matchesJoinPath that considers flippedJoins, rather than only
       * strictly matching the exact join names in the path (which, given the fact that
       * the join graph may contain flipped joins, this allows for more flexible
       * (and probably accurate for what you're looking for) matching).
       *******************************************************************************/
      public boolean matchesJoinPath(List<String> joinPath, JoinGraph joinGraph, QInstance qInstance)
      {
         if(list.size() != joinPath.size())
         {
            return (false);
         }

         OUTER:
         for(int i = 0; i < list.size(); i++)
         {
            JoinConnection joinConnection = list.get(i);
            if(joinConnection.viaJoinName().equals(joinPath.get(i)))
            {
               /////////////////////////////////////////////////////////////////////////
               // if the name is an exact match, move on to the next join in the path //
               /////////////////////////////////////////////////////////////////////////
               continue OUTER;
            }

            ///////////////////////////////////////////////////////////////////////////////
            // else consider if any flipped joins match this entry - and if so, continue //
            ///////////////////////////////////////////////////////////////////////////////
            QJoinMetaData join = qInstance.getJoin(joinConnection.viaJoinName);
            if(join != null)
            {
               List<String> joinNames = joinGraph.flippedJoins.get(NormalizedJoin.build(join));
               for(String joinName : CollectionUtils.nonNullList(joinNames))
               {
                  if(joinName.equals(joinPath.get(i)))
                  {
                     continue OUTER;
                  }
               }
            }

            /////////////////////////////////////////////////////////////////
            // if both checks above fail, then the join path doesn't match //
            /////////////////////////////////////////////////////////////////
            return (false);
         }

         return (true);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public String getJoinNamesAsString()
      {
         return (StringUtils.join(", ", list().stream().map(jc -> jc.viaJoinName()).toList()));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public List<String> getJoinNamesAsList()
      {
         return (list().stream().map(jc -> jc.viaJoinName()).toList());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Set<JoinConnectionList> getJoinConnections(String tableName)
   {
      Set<JoinConnectionList> rs = new TreeSet<>();
      doGetJoinConnections(rs, tableName, new ArrayList<>(), new JoinConnectionList(new ArrayList<>()));
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void doGetJoinConnections(Set<JoinConnectionList> joinConnections, String tableName, List<String> path, JoinConnectionList connectionList)
   {
      for(Edge edge : edges)
      {
         if(edge.leftTable.equals(tableName) || edge.rightTable.equals(tableName))
         {
            if(path.contains(edge.joinName))
            {
               continue;
            }

            List<String> newPath = new ArrayList<>(path);
            newPath.add(edge.joinName);
            if(!joinConnectionsContain(joinConnections, newPath))
            {
               String otherTableName = null;
               if(!edge.leftTable.equals(tableName))
               {
                  otherTableName = edge.leftTable;
               }
               else if(!edge.rightTable.equals(tableName))
               {
                  otherTableName = edge.rightTable;
               }

               if(otherTableName != null)
               {
                  if(newPath.size() > maxPathLength)
                  {
                     ////////////////////////////////////////////////////////////////
                     // performance hack.  see comment at maxPathLength definition //
                     ////////////////////////////////////////////////////////////////
                     continue;
                  }

                  JoinConnectionList newConnectionList = connectionList.copy();
                  JoinConnection     joinConnection    = new JoinConnection(otherTableName, edge.joinName);
                  newConnectionList.list.add(joinConnection);
                  joinConnections.add(newConnectionList);
                  doGetJoinConnections(joinConnections, otherTableName, new ArrayList<>(newPath), newConnectionList);
               }
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean joinConnectionsContain(Set<JoinConnectionList> joinPaths, List<String> newPath)
   {
      for(JoinConnectionList joinConnections : joinPaths)
      {
         List<String> joinConnectionJoins = joinConnections.list.stream().map(jc -> jc.viaJoinName).toList();
         if(joinConnectionJoins.equals(newPath))
         {
            return (true);
         }
      }
      return (false);
   }

}
