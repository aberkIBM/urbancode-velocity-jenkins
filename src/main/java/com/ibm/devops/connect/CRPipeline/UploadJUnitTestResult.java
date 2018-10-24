/**
 * (c) Copyright IBM Corporation 2018.
 * This is licensed under the following license.
 * The Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

package com.ibm.devops.connect.CRPipeline;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.remoting.VirtualChannel;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;

import jenkins.tasks.SimpleBuildStep;


import java.io.File;
import java.io.IOException;
import net.sf.json.JSONObject;

import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Map;

import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;

import com.ibm.devops.connect.CloudPublisher;

public class UploadJUnitTestResult extends Builder implements SimpleBuildStep {

    private Map<String, String> properties;

    @DataBoundConstructor
    public UploadJUnitTestResult(Map<String, String> properties) {
        this.properties = properties;
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }

    @Override
    public void perform(final Run<?, ?> build, FilePath workspace, Launcher launcher, final TaskListener listener)
            throws AbortException, InterruptedException, IOException {

        Object fatalFailure = this.properties.get("fatal");

        boolean success = workspace.act(new FileUploader(this.properties, listener));
        if (!success) {
            if (fatalFailure != null && fatalFailure.toString().equals("true")) {
                build.setResult(Result.FAILURE);
            } else {
                build.setResult(Result.UNSTABLE);
            }
        }
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

        public FileUploader(Map<String, String> properties, TaskListener listener) {
            this.properties = properties;
            this.listener = listener;
        }

        @Override public Boolean invoke(File f, VirtualChannel channel) {
            listener.getLogger().println("Uploading JUnint File");

            String filePath = properties.get("filePath");
            String tenantId = properties.get("tenant_id");
            String name = properties.get("name");
            String testSetName = properties.get("testSetName");
            String appId = properties.get("appId");
            String appExtId = properties.get("appExtId");
            String appName = properties.get("appName");
            Object combineTestSuites = properties.get("combineTestSuites");

            JSONObject payload = new JSONObject();
            JSONObject data = new JSONObject();
            JSONObject application = new JSONObject();

            application.put("id", appId);
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
            if (combineTestSuites != null) {
                data.put("combineTestSuites", combineTestSuites.toString());
            }

            payload.put("data", data);

            HttpEntity entity = MultipartEntityBuilder
                .create()
                .addTextBody("payload", payload.toString())
                .addBinaryBody("testArtifact", new File(f, filePath), ContentType.create("application/octet-stream"), "filename")
                .build();

            CloudPublisher cloudPublisher = new CloudPublisher();
            boolean success = false;
            try {
                success = cloudPublisher.uploadQualityData(entity);
            } catch (Exception ex) {
                listener.error("Error uploading quality data: " + ex.getClass() + " - " + ex.getMessage());
            }
            return success;
        }
        /**
         * Check the role of the executing node to follow jenkins new file access rules
         */
        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {
            // no-op
        }
    }
}