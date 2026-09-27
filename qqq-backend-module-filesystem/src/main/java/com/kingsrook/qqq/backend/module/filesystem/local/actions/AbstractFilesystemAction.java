/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.filesystem.local.actions;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.actions.AbstractBaseFilesystemAction;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.AbstractFilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.base.utils.SharedFilesystemBackendModuleUtils;
import com.kingsrook.qqq.backend.module.filesystem.exceptions.FilesystemException;
import org.apache.commons.io.FileUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Base class for all (local) Filesystem actions
 *******************************************************************************/
public class AbstractFilesystemAction extends AbstractBaseFilesystemAction<File>
{
   private static final QLogger LOG = QLogger.getLogger(AbstractFilesystemAction.class);



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public Long getFileSize(File file)
   {
      return (file.length());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Instant getFileCreateDate(File file)
   {
      try
      {
         Path                path         = file.toPath();
         BasicFileAttributes attrs        = Files.readAttributes(path, BasicFileAttributes.class);
         FileTime            creationTime = attrs.creationTime();
         return creationTime.toInstant();
      }
      catch(IOException e)
      {
         LOG.warn("Error getting file createDate", e, logPair("file", file));
         return (null);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Instant getFileModifyDate(File file)
   {
      return Instant.ofEpochMilli(file.lastModified());
   }



   /*******************************************************************************
    ** List the files for this table.
    *******************************************************************************/
   @Override
   public List<File> listFiles(QTableMetaData table, QBackendMetaData backendBase, String requestedPath) throws QException
   {
      try
      {
         String fullPath  = getFullBasePath(table, backendBase);
         File   directory = new File(fullPath);

         AbstractFilesystemTableBackendDetails tableBackendDetails = getTableBackendDetails(AbstractFilesystemTableBackendDetails.class, table);

         String pattern = "regex:.*";
         if(StringUtils.hasContent(tableBackendDetails.getGlob()))
         {
            pattern = "glob:" + tableBackendDetails.getGlob();
         }
         List<String> matchedFiles = recursivelyListFilesMatchingPattern(directory.toPath(), pattern, backendBase, table);
         List<File>   rs           = new ArrayList<>();

         for(String matchedFile : matchedFiles)
         {
            boolean isMatch = true;
            if(StringUtils.hasContent(requestedPath))
            {
               QQueryFilter filter = new QQueryFilter(new QFilterCriteria(tableBackendDetails.getFileNameFieldName(), QCriteriaOperator.EQUALS, requestedPath));
               isMatch = SharedFilesystemBackendModuleUtils.doesFilePathMatchFilter(matchedFile, filter, tableBackendDetails);
            }

            if(isMatch)
            {
               rs.add(new File(fullPath + File.separatorChar + matchedFile));
            }
         }

         return (rs);
      }
      catch(Exception e)
      {
         throw (new QException("Error searching files", e));
      }
   }



   /*******************************************************************************
    ** Credit: https://www.baeldung.com/java-files-match-wildcard-strings
    *******************************************************************************/
   List<String> recursivelyListFilesMatchingPattern(Path rootDir, String pattern, QBackendMetaData backend, QTableMetaData table) throws IOException
   {
      List<String> matchesList = new ArrayList<>();

      FileVisitor<Path> matcherVisitor = new SimpleFileVisitor<>()
      {
         @Override
         public FileVisitResult visitFile(Path file, BasicFileAttributes attribs)
         {
            FileSystem  fs      = FileSystems.getDefault();
            PathMatcher matcher = fs.getPathMatcher(pattern);
            Path        path    = Path.of(stripBackendAndTableBasePathsFromFileName(file.toAbsolutePath().toString(), backend, table));

            if(matcher.matches(path))
            {
               matchesList.add(path.toString());
            }
            return FileVisitResult.CONTINUE;
         }
      };

      if(rootDir.toFile().exists())
      {
         Files.walkFileTree(rootDir, matcherVisitor);
      }

      return matchesList;
   }



   /*******************************************************************************
    ** Read the contents of a file.
    *******************************************************************************/
   @Override
   public InputStream readFile(File file) throws IOException
   {
      return (new FileInputStream(file));
   }



   /*******************************************************************************
    ** Write a file - to be implemented in module-specific subclasses.
    *******************************************************************************/
   @Override
   public void writeFile(QBackendMetaData backend, QTableMetaData table, QRecord record, String path, byte[] contents) throws IOException
   {
      FileUtils.writeByteArrayToFile(new File(path), contents);
   }



   /*******************************************************************************
    ** Get a string that represents the full path to a file.
    *******************************************************************************/
   @Override
   public String getFullPathForFile(File file)
   {
      return (file.getAbsolutePath());
   }



   /*******************************************************************************
    ** In contrast with the DeleteAction, which deletes RECORDS - this is a
    ** filesystem-(or s3, sftp, etc)-specific extension to delete an entire FILE
    ** e.g., for post-ETL.
    **
    ** @throws FilesystemException if the delete is known to have failed, and the file is thought to still exit
    *******************************************************************************/
   @Override
   public void deleteFile(QTableMetaData table, String fileReference) throws FilesystemException
   {
      File file = new File(fileReference);
      if(!file.exists())
      {
         //////////////////////////////////////////////////////////////////////////////////////////////
         // if the file doesn't exist, just exit with noop.  don't throw an error - that should only //
         // happen if the "contract" of the method is broken, and the file still exists              //
         //////////////////////////////////////////////////////////////////////////////////////////////
         LOG.debug("Not deleting file, because it does not exist.", logPair("file", file));
         return;
      }

      if(!file.delete())
      {
         throw (new FilesystemException("Failed to delete file: " + fileReference));
      }
   }



   /*******************************************************************************
    ** Move a file from a source path, to a destination path.
    **
    ** @param destination assumed to be a file path - not a directory
    ** @throws FilesystemException if the delete is known to have failed
    *******************************************************************************/
   @Override
   public void moveFile(QInstance instance, QTableMetaData table, String source, String destination) throws FilesystemException
   {
      File sourceFile        = new File(source);
      File destinationFile   = new File(destination);
      File destinationParent = destinationFile.getParentFile();

      if(!sourceFile.exists())
      {
         throw (new FilesystemException("Cannot move file " + source + ", as it does not exist."));
      }

      //////////////////////////////////////////////////////////////////////////////////////
      // if the destination folder doesn't exist, try to make it - and fail if that fails //
      //////////////////////////////////////////////////////////////////////////////////////
      if(!destinationParent.exists())
      {
         LOG.debug("Making destination directory for move", logPair("directory", destinationParent.getAbsolutePath()));
         if(!destinationParent.mkdirs())
         {
            throw (new FilesystemException("Failed to make destination directory " + destinationParent.getAbsolutePath() + " to move " + source + " into."));
         }
      }

      if(!sourceFile.renameTo(destinationFile))
      {
         throw (new FilesystemException("Failed to move (rename) file " + source + " to " + destination));
      }
   }



   /*******************************************************************************
    ** e.g., with a base path of /foo/
    ** and a table path of /bar/
    ** and a file at /foo/bar/baz.txt
    ** give us just the baz.txt part.
    *******************************************************************************/
   @Override
   public String stripBackendAndTableBasePathsFromFileName(String filePath, QBackendMetaData backend, QTableMetaData table)
   {
      String tablePath    = getFullBasePath(table, backend);
      String strippedPath = filePath.replaceFirst(".*" + tablePath, "");
      return (strippedPath);
   }

}
