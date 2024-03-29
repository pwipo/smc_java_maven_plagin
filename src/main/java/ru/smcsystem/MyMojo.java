package ru.smcsystem;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.plugins.jar.AbstractJarMojo;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Goal which touches a timestamp file.
 *
 * @goal touch
 * @phase process-sources
 */
@Mojo(name = "smc", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class MyMojo
        extends AbstractJarMojo {
    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File classesDirectory;
    // @Parameter(defaultValue = "${project.basedir}")
    // private String baseDir;
    @Parameter(defaultValue = "${project.build.directory}")
    private File outputDirectory2;
    @Parameter(defaultValue = "${project.build.finalName}")
    private String packageName;
    @Parameter(defaultValue = "smcm")
    private String suffix;
    @Parameter(defaultValue = "properties.xml")
    private String propertiesFileName;
    @Parameter(defaultValue = "data")
    private String folderDataName;
    @Component
    private MavenProjectHelper projectHelper2;

    @Override
    protected File getClassesDirectory() {
        return classesDirectory;
    }

    @Override
    protected String getClassifier() {
        return null;
    }

    @Override
    protected String getType() {
        return "smcm";
    }

    public void execute()
            throws MojoExecutionException {

        getProject().getArtifact().setFile(null);

        super.execute();

        File f = new File(outputDirectory2, packageName + "." + suffix);
        getLog().info(f.getPath());
        try (FileOutputStream fos = new FileOutputStream(f); ZipOutputStream out = new ZipOutputStream(fos)) {
            File dataFolder = new File(getProject().getBasedir(), folderDataName);
            if (dataFolder.isDirectory()) {
                File[] files = dataFolder.listFiles();
                if (files != null && files.length > 0) {
                    for (File file : files)
                        zipFileOrDirectory(out, file, null);
                }
            }
            zipFile(out, null, new File(getProject().getBasedir(), propertiesFileName));
            zipFile(out, null, getProject().getArtifact().getFile());
            for (Artifact artifact : getProject().getArtifacts())
                zipFile(out, null, artifact.getFile());
        } catch (Exception e) {
            throw new MojoExecutionException("zip create error", e);
        }
        projectHelper2.attachArtifact(getProject(), getType(), f);
        // getProject().getArtifact().setFile(f);
    }

    private void zipFile(ZipOutputStream out, String fileName, File fileToZip) throws IOException {
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName != null ? fileName : fileToZip.getName());
            out.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0)
                out.write(bytes, 0, length);
        } finally {
            getLog().info(fileToZip.getPath());
            out.closeEntry();
        }
    }

    private void zipFileOrDirectory(ZipOutputStream out, File src, String dest) throws IOException {
        if (out == null || src == null)
            return;

        if (dest == null) {
            dest = "";
        } else if (!dest.endsWith("/")) {
            dest = dest + "/";
        }
        if (src.isDirectory()) {
            dest = dest + src.getName() + "/";
            out.putNextEntry(new ZipEntry(dest));
            out.closeEntry();
            File[] files = src.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files)
                    zipFileOrDirectory(out, file, dest);
            }
        } else {
            zipFile(out, dest + src.getName(), src);
        }
    }

}
