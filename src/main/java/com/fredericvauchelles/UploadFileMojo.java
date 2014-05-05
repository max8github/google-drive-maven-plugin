package com.fredericvauchelles;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.api.services.drive.model.Permission;
import org.apache.maven.plugin.*;
import org.apache.maven.shared.model.fileset.util.*;
import org.apache.maven.shared.model.fileset.FileSet;

import org.apache.maven.project.MavenProject;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.*;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.Drive.Files.Insert;

/**
 * Goal which touches a timestamp file.
 *
 * @goal upload-file
 */
public class UploadFileMojo extends AbstractMojo
{
    /**
     * @parameter
     * @required
     */
    private java.io.File googleClientProperties;

   /**
     * @parameter expression="${basedir}/src/main/resources"
     */
    private java.io.File googleDrivePropertiesDirectory;

    /**
     * Location of the file.
     * @parameter 
     * @required
     */
    private FileSet fileset;

    /**
     * @paramter
     * @required
     */
    private String mimeType;

    /**
     * @parameter
     */
    private String parentId;

    /**
     * @parameter default-value="${project}"
     */
    private MavenProject project;

    /**
     * fileInfoPropertyPrefix.fileId and fileInfoPropertyPrefix.webContentLink will be set if this property set. It is
     * allowed to use this property when uploading single file.
     * @parameter
     */
    private String fileInfoPropertyPrefix;


    /**
     * If true files will be shared to anyone with link
     * @parameter
     */
    private boolean share;


    public void execute() throws MojoExecutionException
    {
        getLog().debug("Start Upload File Mojo");

        try{
            MavenCredentialStore store = new MavenCredentialStore( googleDrivePropertiesDirectory, getLog() );

            Drive service = Connect.getDriveService(googleClientProperties, store);

            FileSetManager fileSetManager = new FileSetManager(getLog());
            String[] includedFiles = fileSetManager.getIncludedFiles( fileset );

            for(String sourceString : includedFiles) {
                java.io.File source = new java.io.File(fileset.getDirectory(), sourceString);
                getLog().info("Sending file : " + sourceString);
                //Insert a file  
                File body = new File();
                body.setTitle(source.getName());
                //body.setDescription("A test document");
                body.setMimeType(mimeType);
                
                FileContent mediaContent = new FileContent(mimeType, source);

                Insert insert = service.files().insert(body, mediaContent);
                insert.getMediaHttpUploader().setDirectUploadEnabled(true);
                File file = insert.execute();

                if(parentId != null) {
                    getLog().info("Setting parent " + parentId);
                    ChildReference child = new ChildReference();
                    child.setId(file.getId());
                    service.children().insert(parentId, child).execute();
                    service.parents().delete(file.getId(), "root").execute();
                }
                else
                    getLog().info("No parent");

                getLog().info("File ID: " + file.getId());

                if (share) {
                    getLog().info("Sharing file");
                    Permission anyonePermission = new Permission();
                    anyonePermission.setRole("reader");
                    anyonePermission.setType("anyone");
                    service.permissions().insert(file.getId(), anyonePermission).execute();
                }

                if (fileInfoPropertyPrefix != null) {
                    if (includedFiles.length == 1) {
                        setProjectPropertyWithLogging("fileId", file.getId());
                        setProjectPropertyWithLogging("webContentLink", file.getWebContentLink());
                    } else {
                        String message = "It is allowed to use fileInfoPropertyPrefix only with fileset containing one file";
                        getLog().error(message);
                        throw new Exception(message);
                    }
                }
            }

            getLog().info("Number of file sent : " + includedFiles.length);

        }
        catch(Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void setProjectPropertyWithLogging(String key, String value) {
        key = fileInfoPropertyPrefix + "." + key;
        project.getProperties().setProperty(key, value);
        getLog().info(String.format("Set property %s to '%s'", key, value));
    }

}
