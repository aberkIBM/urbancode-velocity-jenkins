/**
 * (c) Copyright IBM Corporation 2018.
 * This is licensed under the following license.
 * The Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

package com.ibm.devops.connect.CRPipeline;

import hudson.AbortException;
import hudson.EnvVars;
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

    public String getTenantId() { return this.tenantId; }
    public String getName() { return this.name; }
    public String getFilePath() { return this.filePath; }
    public String getTestSetName() { return this.testSetName; }
    public String getEnvironment() { return this.environment; }
    public Boolean getCombineTestSuites() { return this.combineTestSuites; }
    public Boolean getFatal() { return this.fatal; }
    public String getPluginType() { return this.pluginType; }
    public String getDataFormat() { return this.dataFormat; }
    public String getRecordName() { return this.recordName; }
    public String getMetricDefinitionId() { return this.metricDefinitionId; }
    public String getBuildId() { return this.buildId; }
    public String getAppId() { return this.appId; }
    public String getAppName() { return this.appName; }
    public String getAppExtId() { return this.appExtId; }

    @Override
    public void perform(final Run<?, ?> build, FilePath workspace, Launcher launcher, final TaskListener listener)
            throws AbortException, InterruptedException, IOException {

        boolean success = workspace.act(new FileUploader(this, build, listener, Jenkins.getInstance().getRootUrl() + build.getUrl()));

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
            return "UCV - Upload Metrics File to UrbanCode Velocity";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }

    private static final class FileUploader implements FileCallable<Boolean> {
        private UploadMetricsFile instance;
        private Run<?, ?> build;
        private TaskListener listener;
        private String buildUrl;

        public FileUploader(UploadMetricsFile instance, Run<?, ?> build, TaskListener listener, String buildUrl) {
            this.instance = instance;
            this.build = build;
            this.listener = listener;
            this.buildUrl = buildUrl;
        }

        @Override public Boolean invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {

            EnvVars envVars = build.getEnvironment(listener);

            String testSetName = envVars.expand(instance.testSetName);
            String filePath = envVars.expand(instance.filePath);
            String environment = envVars.expand(instance.environment);
            String tenantId = envVars.expand(instance.tenantId);
            String appId = envVars.expand(instance.appId);
            String appName = envVars.expand(instance.appName);
            String appExtId = envVars.expand(instance.appExtId);
            String pluginType = envVars.expand(instance.pluginType);
            String dataFormat = envVars.expand(instance.dataFormat);
            String name = envVars.expand(instance.name);
            String metricDefinitionId = envVars.expand(instance.metricDefinitionId);
            String combineTestSuites = envVars.expand(instance.combineTestSuites == null ? "" : instance.combineTestSuites.toString());
            String buildId = envVars.expand(instance.buildId);

            JSONObject payload = new JSONObject();

            payload.put("dataSet", testSetName);
            if (environment != null && !environment.equals("")) {
                payload.put("environment", environment);
            }
            payload.put("tenant_id", tenantId);

            JSONObject application = new JSONObject();
            if (appId != null && !appId.equals("")) {
                application.put("id", appId);
            }
            if (appName != null && !appName.equals("")) {
                application.put("name", appName);
            }
            if (appExtId != null && !appExtId.equals("")) {
                application.put("externalId", appExtId);
            }
            if (application.isEmpty()) {
                throw new RuntimeException("Must specify at least one of: 'appId', 'appName', 'appExtId'");
            }
            payload.put("application", application);

            JSONObject record = new JSONObject();
            record.put("pluginType", pluginType);
            record.put("dataFormat", dataFormat);
            if (name != null && !name.equals("")) {
                record.put("recordName", name);
            }
            if (metricDefinitionId != null && !metricDefinitionId.equals("")) {
                record.put("metricDefinitionId", metricDefinitionId);
            }
            payload.put("record", record);

            JSONObject options = new JSONObject();
            options.put("combineTestSuites", combineTestSuites != null && !combineTestSuites.equals("") ? combineTestSuites.toString() : "true");
            payload.put("options", options);

            JSONObject build = new JSONObject();
            if (buildId != null && !buildId.equals("")) {
                build.put("buildId", buildId);
            }
            build.put("url", this.buildUrl);
            payload.put("build", build);

            System.out.println("TEST payload: " + payload.toString(2));

            listener.getLogger().println("Uploading metric \"" + name + "\" to UrbanCode Velocity...");
            HttpEntity entity = MultipartEntityBuilder
                .create()
                .addTextBody("payload", payload.toString())
                .addBinaryBody("testArtifact", new File(f, filePath), ContentType.create("application/octet-stream"), "filename")
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
