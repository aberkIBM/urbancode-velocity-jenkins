/**
 * (c) Copyright IBM Corporation 2018.
 * This is licensed under the following license.
 * The Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

package com.ibm.devops.connect.CRPipeline;

import org.apache.http.impl.client.DefaultHttpClient;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Descriptor.FormException;
import hudson.remoting.VirtualChannel;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.Secret;

import jenkins.tasks.SimpleBuildStep;

import java.nio.file.Paths;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import net.sf.json.JSONObject;

import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Map;

import com.ibm.devops.connect.Status.CrAction;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;

import com.ibm.devops.connect.CloudPublisher;

public class UploadJUnitTestResult extends Builder implements SimpleBuildStep {

    private Map<String, String> properties;

    @DataBoundConstructor
    public UploadJUnitTestResult(
            Map<String, String> properties) {
        this.properties = properties;
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }

    @Override
    public void perform(final Run<?, ?> build, FilePath workspace, Launcher launcher, final TaskListener listener)
            throws AbortException, InterruptedException, IOException {

        // listener.getLogger().append("We are running a Step HERE!!!!");
        // listener.getLogger().append(this.properties.toString());
        // listener.getLogger().println("Uploading JUnint File");

        // String filePath = properties.get("filePath");
        // String tenantId = properties.get("tenant_id");
        // String name = properties.get("name");
        // String testSetName = properties.get("testSetName");
        // String appExtId = properties.get("appExtId");
        // String appName = properties.get("appName");

        // JSONObject payload = new JSONObject();
        // JSONObject data = new JSONObject();
        // JSONObject application = new JSONObject();
        // payload.put("type", "junitXML");
        // payload.put("dataFormat", "xml");
        // payload.put("application", "fooApplication");
        // payload.put("environment", "Prod");
        // payload.put("authToken", "12345");

        // data.put("tenant_id", tenantId);
        // data.put("name", name);
        // data.put("testSetName", testSetName);
        // data.put("enricherType", "JUnit Quality Data");
        // data.put("category", "Unit Tests");

        // application.put("name", appName);
        // application.put("externalId", appExtId);

        // data.put("application", application);

        // payload.put("data", data);

        // HttpEntity entity = MultipartEntityBuilder
        //     .create()
        //     .addTextBody("payload", payload.toString())
        //     .addBinaryBody("testArtifact", new File(filePath), ContentType.create("application/octet-stream"), "filename")
        //     .build();

        // CloudPublisher cloudPublisher = new CloudPublisher();
        // cloudPublisher.uploadQualityData(entity);
        workspace.act(new FileUploader(this.properties, listener));
    }

    @Extension
    public static class UploadJUnitTestResultDescriptor extends BuildStepDescriptor<Builder> {

        public UploadJUnitTestResultDescriptor() {
            load();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/ibm-ucdeploy-build-steps/publish.html";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

        /**
         * {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return "Pass Properties to Continuous Release Version";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }

    private static final class FileUploader implements FileCallable<Boolean> {
        private Map<String, String> properties;
        private TaskListener listener;

        public FileUploader(
            Map<String, String> properties,
            TaskListener listener) {
            this.properties = properties;
            this.listener = listener;
        }
        @Override public Boolean invoke(File f, VirtualChannel channel) {
            listener.getLogger().append("We are running a Step HERE!!!!");
            listener.getLogger().append(this.properties.toString());
            listener.getLogger().println("Uploading JUnint File");

            String filePath = properties.get("filePath");
            String tenantId = properties.get("tenant_id");
            String name = properties.get("name");
            String testSetName = properties.get("testSetName");
            String appExtId = properties.get("appExtId");
            String appName = properties.get("appName");

            JSONObject payload = new JSONObject();
            JSONObject data = new JSONObject();
            JSONObject application = new JSONObject();

            application.put("name", appName);
            application.put("externalId", appExtId);

            payload.put("type", "junitXML");
            payload.put("dataFormat", "xml");
            payload.put("environment", "Prod");
            payload.put("authToken", "12345");
            payload.put("application", application);

            data.put("tenant_id", tenantId);
            data.put("name", name);
            data.put("testSetName", testSetName);
            data.put("enricherType", "JUnit Quality Data");
            data.put("category", "Unit Tests");



            payload.put("data", data);

            HttpEntity entity = MultipartEntityBuilder
                .create()
                .addTextBody("payload", payload.toString())
                .addBinaryBody("testArtifact", new File(f, filePath), ContentType.create("application/octet-stream"), "filename")
                .build();

            CloudPublisher cloudPublisher = new CloudPublisher();
            return cloudPublisher.uploadQualityData(entity);
        }
        /**
         * Check the role of the executing node to follow jenkins new file access rules
         */
        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {
            this.checkRoles(checker);
        }
    }
}