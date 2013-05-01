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

import org.apache.maven.plugin.*;
import org.apache.maven.shared.model.fileset.util.*;
import org.apache.maven.shared.model.fileset.FileSet;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.*;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.Drive.Files.Insert;

import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Properties;

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
            }

            getLog().info("Number of file sent : " + includedFiles.length);

        }
        catch(Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
