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

import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

import java.io.File;
import java.io.IOException;
import net.sf.json.JSONObject;

import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;

import com.ibm.devops.connect.CloudPublisher;

public class UploadMetricsFile extends Builder implements SimpleBuildStep {

    private String tenantId;
    private String name;
    private String filePath;
    private String testSetName;
    private String environment;
    private Boolean combineTestSuites;
    private Boolean fatal;
    private String pluginType;
    private String dataFormat;
    private String recordName;
    private String metricDefinitionId;
    private String buildId;
    private String appId;
    private String appName;
    private String appExtId;

    @DataBoundConstructor
    public UploadMetricsFile(
        String tenantId,
        String name,
        String filePath,
        String testSetName,
        String environment,
        Boolean combineTestSuites,
        Boolean fatal,
        String pluginType,
        String dataFormat,
        String recordName,
        String metricDefinitionId,
        String buildId,
        String appId,
        String appName,
        String appExtId
    ) {
        this.tenantId = tenantId;
        this.name = name;
        this.filePath = filePath;
        this.testSetName = testSetName;
        this.environment = environment;
        this.combineTestSuites = combineTestSuites;
        this.fatal = fatal;
        this.pluginType = pluginType;
        this.dataFormat = dataFormat;
        this.recordName = recordName;
        this.metricDefinitionId = metricDefinitionId;
        this.buildId = buildId;
        this.appId = appId;
        this.appName = appName;
        this.appExtId = appExtId;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public String getName() {
        return this.name;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public String getTestSetName() {
        return this.testSetName;
    }

    public String getEnvironment() {
        return this.environment;
    }

    public Boolean getCombineTestSuites() {
        return this.combineTestSuites;
    }

    public Boolean getFatal() {
        return this.fatal;
    }

    public String getPluginType() {
        return this.pluginType;
    }

    public String getDataFormat() {
        return this.dataFormat;
    }

    public String getRecordName() {
        return this.recordName;
    }

    public String getMetricDefinitionId() {
        return this.metricDefinitionId;
    }

    public String getBuildId() {
        return this.buildId;
    }

    public String getAppId() {
        return this.appId;
    }

    public String getAppName() {
        return this.appName;
    }

    public String getAppExtId() {
        return this.appExtId;
    }

    @Override
    public void perform(final Run<?, ?> build, FilePath workspace, Launcher launcher, final TaskListener listener)
            throws AbortException, InterruptedException, IOException {

        boolean success = workspace.act(new FileUploader(this, listener, Jenkins.getInstance().getRootUrl() + build.getUrl()));

        if (!success) {
            if (this.fatal != null && this.fatal.toString().equals("true")) {
                build.setResult(Result.FAILURE);
            } else {
                build.setResult(Result.UNSTABLE);
            }
        } else {
            listener.getLogger().println("Successfully uploaded metric file to UrbanCode Velocity.");
        }
    }

    @Extension
    public static class UploadMetricsFileDescriptor extends BuildStepDescriptor<Builder> {

        public UploadMetricsFileDescriptor() {
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
            return "Upload a metric file to UrbanCode Velocity";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }

    private static final class FileUploader implements FileCallable<Boolean> {
        private UploadMetricsFile instance;
        private TaskListener listener;
        private String buildUrl;

        public FileUploader(UploadMetricsFile instance, TaskListener listener, String buildUrl) {
            this.instance = instance;
            this.listener = listener;
            this.buildUrl = buildUrl;
        }

        @Override public Boolean invoke(File f, VirtualChannel channel) {

            JSONObject payload = new JSONObject();
            payload.put("metricName", instance.testSetName);
            payload.put("environment", instance.environment);
            payload.put("tenant_id", instance.tenantId);

            JSONObject application = new JSONObject();
            application.put("id", instance.appId);
            application.put("name", instance.appName);
            application.put("externalId", instance.appExtId);
            payload.put("application", application);

            JSONObject record = new JSONObject();
            record.put("pluginType", instance.pluginType);
            record.put("dataFormat", instance.dataFormat);
            record.put("recordName", instance.name);
            record.put("metricDefinitionId", instance.metricDefinitionId);
            payload.put("record", record);

            JSONObject options = new JSONObject();
            options.put("combineTestSuites", instance.combineTestSuites != null ? instance.combineTestSuites.toString() : "true");
            payload.put("options", options);

            JSONObject build = new JSONObject();
            if (instance.buildId != null) {
                build.put("buildId", instance.buildId);
            }
            build.put("url", this.buildUrl);
            payload.put("build", build);

            System.out.println("TEST payload: " + payload.toString(2));

            listener.getLogger().println("Uploading metric \"" + instance.name + "\" to UrbanCode Velocity...");
            HttpEntity entity = MultipartEntityBuilder
                .create()
                .addTextBody("payload", payload.toString())
                .addBinaryBody("testArtifact", new File(f, instance.filePath), ContentType.create("application/octet-stream"), "filename")
                .build();

            boolean success = false;
            try {
                success = CloudPublisher.uploadQualityData(entity);
            } catch (Exception ex) {
                listener.error("Error uploading metric file: " + ex.getClass() + " - " + ex.getMessage());
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
