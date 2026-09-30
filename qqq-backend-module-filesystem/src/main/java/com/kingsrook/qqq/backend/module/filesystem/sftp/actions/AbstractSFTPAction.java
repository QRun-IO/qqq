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

package com.kingsrook.qqq.backend.module.filesystem.sftp.actions;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantSetting;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsUtil;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.actions.AbstractBaseFilesystemAction;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.AbstractFilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.exceptions.FilesystemException;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.SFTPDirEntryWithPath;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPBackendVariantSetting;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Base class for all SFTP filesystem actions
 *******************************************************************************/
public class AbstractSFTPAction extends AbstractBaseFilesystemAction<SFTPDirEntryWithPath>
{
   private static final QLogger LOG = QLogger.getLogger(AbstractSFTPAction.class);



   /***************************************************************************
    ** singleton implementing Initialization-on-Demand Holder idiom
    ** to help ensure only a single SshClient object exists in a server.
    ***************************************************************************/
   private static class SshClientManager
   {

      /***************************************************************************
       **
       ***************************************************************************/
      private static class Holder
      {
         private static final SshClient INSTANCE = SshClient.setUpDefaultClient();

         static
         {
            INSTANCE.start();
         }
      }



      /***************************************************************************
       **
       ***************************************************************************/
      public static SshClient getInstance()
      {
         return Holder.INSTANCE;
      }
   }

   ////////////////////////////////////////////////////////////////
   // open clientSessionFirst, then sftpClient                   //
   // and close them in reverse (sftpClient, then clientSession) //
   ////////////////////////////////////////////////////////////////
   private ClientSession clientSession;
   private SftpClient    sftpClient;



   /*******************************************************************************
    ** Set up the sftp utils object to be used for this action.
    *******************************************************************************/
   @Override
   public void preAction(QBackendMetaData backendMetaData) throws QException
   {
      super.preAction(backendMetaData);

      if(sftpClient != null)
      {
         LOG.debug("sftpClient object is already set - not re-setting it.");
         return;
      }

      try
      {
         SFTPBackendMetaData sftpBackendMetaData = getBackendMetaData(SFTPBackendMetaData.class, backendMetaData);

         String  username   = sftpBackendMetaData.getUsername();
         String  password   = sftpBackendMetaData.getPassword();
         String  hostName   = sftpBackendMetaData.getHostName();
         Integer port       = sftpBackendMetaData.getPort();
         byte[]  privateKey = sftpBackendMetaData.getPrivateKey();

         if(backendMetaData.getUsesVariants())
         {
            QRecord variantRecord = BackendVariantsUtil.getVariantRecord(backendMetaData);
            LOG.debug("Getting SFTP connection credentials from variant record",
               logPair("tableName", backendMetaData.getBackendVariantsConfig().getOptionsTableName()),
               logPair("id", variantRecord.getValue("id")),
               logPair("name", variantRecord.getRecordLabel()));
            Map<BackendVariantSetting, String> fieldNameMap = backendMetaData.getBackendVariantsConfig().getBackendSettingSourceFieldNameMap();

            if(fieldNameMap.containsKey(SFTPBackendVariantSetting.USERNAME))
            {
               username = variantRecord.getValueString(fieldNameMap.get(SFTPBackendVariantSetting.USERNAME));
            }

            if(fieldNameMap.containsKey(SFTPBackendVariantSetting.PASSWORD))
            {
               password = variantRecord.getValueString(fieldNameMap.get(SFTPBackendVariantSetting.PASSWORD));
            }

            if(fieldNameMap.containsKey(SFTPBackendVariantSetting.PRIVATE_KEY))
            {
               privateKey = variantRecord.getValueByteArray(fieldNameMap.get(SFTPBackendVariantSetting.PRIVATE_KEY));
            }

            if(fieldNameMap.containsKey(SFTPBackendVariantSetting.HOSTNAME))
            {
               hostName = variantRecord.getValueString(fieldNameMap.get(SFTPBackendVariantSetting.HOSTNAME));
            }

            if(fieldNameMap.containsKey(SFTPBackendVariantSetting.PORT))
            {
               port = variantRecord.getValueInteger(fieldNameMap.get(SFTPBackendVariantSetting.PORT));
            }
         }

         makeConnection(username, hostName, port, password, privateKey);
      }
      catch(Exception e)
      {
         throw (new QException("Error setting up SFTP connection", e));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void postAction()
   {
      Consumer<AutoCloseable> closer = closable ->
      {
         if(closable != null)
         {
            try
            {
               closable.close();
            }
            catch(Exception e)
            {
               LOG.info("Error closing SFTP resource", e, logPair("type", closable.getClass().getSimpleName()));
            }
         }
      };

      closer.accept(sftpClient);
      closer.accept(clientSession);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected SftpClient makeConnection(String username, String hostName, Integer port, String password, byte[] privateKeyBytes) throws Exception
   {
      this.clientSession = SshClientManager.getInstance().connect(username, hostName, port).verify().getSession();

      //////////////////////////////////////////////////////////////////////
      // if we have private key bytes, use them to add publicKey identity //
      //////////////////////////////////////////////////////////////////////
      if(privateKeyBytes != null && privateKeyBytes.length > 0)
      {
         PKCS8EncodedKeySpec keySpec    = new PKCS8EncodedKeySpec(privateKeyBytes);
         KeyFactory          keyFactory = KeyFactory.getInstance("RSA");
         PrivateKey          privateKey = keyFactory.generatePrivate(keySpec);
         PublicKey           publicKey  = KeyUtils.recoverPublicKey(privateKey);
         clientSession.addPublicKeyIdentity(new KeyPair(publicKey, privateKey));
      }

      //////////////////////////////////////////////////
      // if we have a password, add password identity //
      //////////////////////////////////////////////////
      if(StringUtils.hasContent(password))
      {
         clientSession.addPasswordIdentity(password);
      }

      clientSession.auth().verify();

      this.sftpClient = SftpClientFactory.instance().createSftpClient(clientSession);
      return (this.sftpClient);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Long getFileSize(SFTPDirEntryWithPath sftpDirEntryWithPath)
   {
      try
      {
         return sftpDirEntryWithPath.dirEntry().getAttributes().getSize();
      }
      catch(Exception e)
      {
         return (null);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Instant getFileCreateDate(SFTPDirEntryWithPath sftpDirEntryWithPath)
   {
      try
      {
         return sftpDirEntryWithPath.dirEntry().getAttributes().getCreateTime().toInstant();
      }
      catch(Exception e)
      {
         return (null);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Instant getFileModifyDate(SFTPDirEntryWithPath sftpDirEntryWithPath)
   {
      try
      {
         return sftpDirEntryWithPath.dirEntry().getAttributes().getModifyTime().toInstant();
      }
      catch(Exception e)
      {
         return (null);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<SFTPDirEntryWithPath> listFiles(QTableMetaData table, QBackendMetaData backendBase, String requestedPath) throws QException
   {
      String fullPath = null;
      try
      {
         fullPath = getFullBasePath(table, backendBase);
         if(StringUtils.hasContent(requestedPath))
         {
            fullPath = stripDuplicatedSlashes(fullPath + File.separatorChar + requestedPath + File.separatorChar);
         }

         List<SFTPDirEntryWithPath> rs = new ArrayList<>();

         /////////////////////////////////////////////////////////////////////////////////////
         // at least in some cases, listing / seems to be interpreted by the server as      //
         // a listing from the root of the system, not just the user's dir.  so, converting //
         // paths starting with / to instead be ./ is giving us better results.             //
         /////////////////////////////////////////////////////////////////////////////////////
         if(fullPath.startsWith("/"))
         {
            fullPath = "." + fullPath;
         }

         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // in case we were asked to list a single file name, make sure we don't put a slash on the end (which wouldn't be found) //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         if(fullPath.endsWith("/"))
         {
            fullPath = fullPath.substring(0, fullPath.length() - 1);
         }

         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // make a 'stat' call, to find out if the path is found, and if it is, if it describes a single file, or a directory //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         String tableRoot = getFullBasePath(table, backendBase);
         if(tableRoot.startsWith("/"))
         {
            tableRoot = "." + tableRoot;
         }
         String resolvedPath = validateTableFilePath(tableRoot, fullPath, true, false);
         SftpClient.Attributes stat = sftpClient.stat(resolvedPath);
         if(stat == null)
         {
            return (rs);
         }
         else if(stat.isRegularFile())
         {
            ///////////////////////////////////////////////////////////////////////////
            // split up the fullPath into its directory prefix, and file name suffix //
            ///////////////////////////////////////////////////////////////////////////
            int    lastSlashIndex = fullPath.lastIndexOf("/");
            String directory      = lastSlashIndex == -1 ? "./" : fullPath.substring(0, lastSlashIndex);
            String fileBaseName   = lastSlashIndex == -1 ? fullPath : fullPath.substring(lastSlashIndex + 1);

            SftpClient.DirEntry dirEntry = new SftpClient.DirEntry(fileBaseName, fullPath, stat);
            rs.add(new SFTPDirEntryWithPath(directory, dirEntry));
         }
         else if(stat.isDirectory())
         {
            for(SftpClient.DirEntry dirEntry : sftpClient.readEntries(fullPath))
            {
               if(".".equals(dirEntry.getFilename()) || "..".equals(dirEntry.getFilename()))
               {
                  continue;
               }

               if(dirEntry.getAttributes().isDirectory())
               {
                  // todo - recursive??
                  continue;
               }

               rs.add(new SFTPDirEntryWithPath(fullPath, dirEntry));
            }
         }

         AbstractFilesystemTableBackendDetails details = getTableBackendDetails(AbstractFilesystemTableBackendDetails.class, table);
         String pattern = StringUtils.hasContent(details.getGlob()) ? "glob:" + details.getGlob() : "regex:.*";
         PathMatcher matcher = FileSystems.getDefault().getPathMatcher(pattern);
         String basePath = getFullBasePath(table, backendBase);
         Path relativeBase = Path.of(basePath.startsWith("/") ? "." + basePath : basePath).normalize();
         rs.removeIf(entry -> !matcher.matches(relativeBase.relativize(Path.of(getFullPathForFile(entry)).normalize())));
         for(SFTPDirEntryWithPath entry : rs)
         {
            validateTableFilePath(tableRoot, getFullPathForFile(entry), false, false);
         }
         return (rs);
      }
      catch(Exception e)
      {
         SftpException sftpException = ExceptionUtils.findClassInRootChain(e, SftpException.class);
         if(sftpException != null)
         {
            throw new QUserFacingException("SFTP Exception listing [" + Objects.requireNonNullElse(fullPath, "") + "]: " + sftpException.getMessage());
         }

         throw (new QException("Error listing files", e));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public InputStream readFile(SFTPDirEntryWithPath dirEntry) throws IOException
   {
      return (sftpClient.read(getFullPathForFile(dirEntry)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void writeFile(QBackendMetaData backend, QTableMetaData table, QRecord record, String path, byte[] contents) throws IOException
   {
      validateTableFilePath(getFullBasePath(table, backend), path, false, true);
      sftpClient.put(new ByteArrayInputStream(contents), path);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String getFullPathForFile(SFTPDirEntryWithPath dirEntry)
   {
      return (dirEntry.path() + "/" + dirEntry.dirEntry().getFilename());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void deleteFile(QTableMetaData table, String fileReference) throws FilesystemException
   {
      try
      {
         QBackendMetaData backend = QContext.getQInstance().getBackend(table.getBackendName());
         validateTableFilePath(getFullBasePath(table, backend), fileReference, false, true);
         sftpClient.remove(fileReference);
      }
      catch(Exception e)
      {
         throw (new FilesystemException("Error deleting file from SFTP", e));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void moveFile(QInstance instance, QTableMetaData table, String source, String destination) throws FilesystemException
   {
      throw (new QRuntimeException("Not yet implemented"));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   protected SftpClient getSftpClient(QBackendMetaData backend) throws QException
   {
      if(sftpClient == null)
      {
         preAction(backend);
      }

      return (sftpClient);
   }



   /*******************************************************************************
    ** Compare remote targets to the table root without changing the path used for I/O.
    *******************************************************************************/
   protected String validateTableFilePath(String tableRoot, String filePath, boolean allowRoot, boolean allowMissingLeaf) throws IOException
   {
      String base = resolveRemotePath(tableRoot, false);
      String file = resolveRemotePath(filePath, allowMissingLeaf);
      if(file.equals(base) ? !allowRoot : !file.startsWith(base.endsWith("/") ? base : base + "/"))
      {
         throw new IOException("File path must remain inside its table directory");
      }
      return file;
   }



   /*******************************************************************************
    ** REALPATH alone can normalize without resolving symlinks (including on SSHD).
    ** Resolve link components remotely before canonicalizing the existing target or parent.
    *******************************************************************************/
   private String resolveRemotePath(String path, boolean allowMissingLeaf) throws IOException
   {
      String absolutePath = path.startsWith("/") ? path : sftpClient.canonicalPath(".") + "/" + path;
      Deque<String> components = new ArrayDeque<>(List.of(absolutePath.split("/")));
      String resolved = "/";
      int followedLinks = 0;
      while(!components.isEmpty())
      {
         String component = components.removeFirst();
         if(component.isEmpty() || component.equals("."))
         {
            continue;
         }
         if(component.equals(".."))
         {
            resolved = resolved.substring(0, Math.max(1, resolved.lastIndexOf('/')));
            continue;
         }
         String candidate = resolved.endsWith("/") ? resolved + component : resolved + "/" + component;
         SftpClient.Attributes attributes;
         try
         {
            attributes = sftpClient.lstat(candidate);
         }
         catch(SftpException e)
         {
            if(allowMissingLeaf && components.isEmpty() && (e.getStatus() == SftpConstants.SSH_FX_NO_SUCH_FILE || e.getStatus() == SftpConstants.SSH_FX_NO_SUCH_PATH))
            {
               String parent = sftpClient.canonicalPath(resolved);
               return parent.endsWith("/") ? parent + component : parent + "/" + component;
            }
            throw e;
         }
         if(attributes.isSymbolicLink())
         {
            if(++followedLinks > 40)
            {
               throw new IOException("Too many symbolic links in SFTP path");
            }
            String target = sftpClient.readLink(candidate);
            if(target.startsWith("/"))
            {
               resolved = "/";
            }
            String[] targetComponents = target.split("/");
            for(int i = targetComponents.length - 1; i >= 0; i--)
            {
               components.addFirst(targetComponents[i]);
            }
         }
         else
         {
            if(!components.isEmpty() && !attributes.isDirectory())
            {
               throw new IOException("Not a directory: " + candidate);
            }
            resolved = candidate;
         }
      }
      return sftpClient.canonicalPath(resolved);
   }



   /***************************************************************************
    ** take a string, which is the contents of a PEM file (like a private key)
    ** - and if it has the -----BEGIN...----- and -----END...---- lines, strip
    ** them away, and strip away any whitespace, and then base-64 decode it.
    ***************************************************************************/
   public static byte[] pemStringToDecodedBytes(String pemString)
   {
      String base64 = pemString.replaceAll("-----BEGIN (.*?)-----", "")
         .replaceAll("-----END (.*?)-----", "")
         .replaceAll("\\s", "");
      return Base64.getDecoder().decode(base64);
   }
}
