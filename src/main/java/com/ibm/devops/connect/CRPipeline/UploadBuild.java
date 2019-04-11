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
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Cause.RemoteCause;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Cause.UserIdCause;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

import java.io.IOException;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.ibm.devops.connect.CloudPublisher;

public class UploadBuild extends Builder implements SimpleBuildStep {

    enum STATUS {
        SUCCESS,
        FAILURE
    }

    private String tenantId;
    private String id;
    private String name;
    private String revision;
    private String requestor;
    private STATUS status;
    private Long startTime;
    private Long endTime;
    private String appName;
    private String appId;
    private String appExtId;

    @DataBoundConstructor
    public UploadBuild(
        String tenantId,
        String id,
        String name,
        String revision,
        String requestor,
        STATUS status,
        Long startTime,
        Long endTime,
        String appName,
        String appId,
        String appExtId
    ) {
        this.tenantId = tenantId;
        this.id = id;
        this.name = name;
        this.revision = revision;
        this.requestor = requestor;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.appName = appName;
        this.appId = appId;
        this.appExtId = appExtId;
    }

    public String getId() { return this.id; }
    public String getName() {return this.name; }
    public String getTenantId() { return this.tenantId; }
    public String getRevision() { return this.revision; }
    public String getRequestor() { return this.requestor; }
    public STATUS getStatus() { return this.status; }
    public Long getStartTime() { return this.startTime; }
    public Long getEndTime() { return this.endTime; }
    public String getAppName() { return this.appName; }
    public String getAppId() { return this.appId; }
    public String getAppExtId() { return this.appExtId; }

    @Override
    public void perform(final Run<?, ?> build, FilePath workspace, Launcher launcher, final TaskListener listener)
            throws AbortException, InterruptedException, IOException {

        EnvVars envVars = build.getEnvironment(listener);

        String id = envVars.expand(this.id);
        String name = envVars.expand(this.name);
        String tenantId = envVars.expand(this.tenantId);
        String revision = envVars.expand(this.revision);
        String requestor = envVars.expand(this.requestor);
        String status = envVars.expand(this.status == null ? "" : this.status.toString());
        String startTime = envVars.expand(this.startTime == null ? "0" : this.startTime.toString());
        String endTime = envVars.expand(this.endTime == null ? "0" : this.endTime.toString());
        String appName = envVars.expand(this.appName);
        String appId = envVars.expand(this.appId);
        String appExtId = envVars.expand(this.appExtId);

        JSONObject payload = new JSONObject();

        // user-provided inputs
        payload.put("tenantId", tenantId);
        payload.put("revision", revision);
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

        // user-provided inputs with fallbacks
        if (requestor != null && !requestor.equals("")) {
            payload.put("requestor", requestor);
        } else {
            for (Cause cause : build.getCauses()) {
                if (cause instanceof UserIdCause) {
                    UserIdCause userCause = (UserIdCause)cause;
                    payload.put("requestor", userCause.getUserName());
                } else if (cause instanceof UpstreamCause) {
                    UpstreamCause upstreamCause = (UpstreamCause)cause;
                    payload.put("requestor", "Upstream job \"" + upstreamCause.getUpstreamProject() + "\", build \"" + upstreamCause.getUpstreamBuild() + "\"");
                } else if (cause instanceof RemoteCause) {
                    RemoteCause remoteCause = (RemoteCause)cause;
                    payload.put("requestor", remoteCause.getAddr());
                } else {
                    payload.put("requestor", cause.getShortDescription());
                }
            }
        }
        if (status != null && !status.equals("")) {
          payload.put("status", status.equals(STATUS.SUCCESS.toString()) ? "success" : "failure");
        } else {
            String computedStatus = "failure";
            if (build.getResult() == null || build.getResult().equals(Result.SUCCESS)) {
                computedStatus = "success";
            }
            payload.put("status", computedStatus);
        }
        if (startTime != null && !startTime.equals("0")) {
            payload.put("startTime", startTime);
        } else {
            payload.put("startTime", build.getStartTimeInMillis());
        }
        if (endTime != null && !endTime.equals("0")) {
            payload.put("endTime", endTime);
        } else {
            payload.put("endTime", System.currentTimeMillis());
        }
        if (id != null && !id.equals("")) {
            payload.put("id", id);
        } else {
            payload.put("id", build.getParent().getName() + " - " + build.getId());
        }
        if (name != null && !name.equals("")) {
            payload.put("name", name);
        } else {
            payload.put("name", build.getDisplayName());
        }

        // build-derived inputs
        payload.put("url", Jenkins.getInstance().getRootUrl() + build.getUrl());

        System.out.println("TEST payload: " + payload.toString(2));

        listener.getLogger().println("Uploading build \"" + payload.get("id") + "\" to UrbanCode Velocity...");
        try {
            String response = CloudPublisher.uploadBuild(payload.toString());
            JSONObject json = JSONObject.fromObject(response);
            if (json.isEmpty() || !json.has("_id") || json.get("_id").equals("")) {
                throw new RuntimeException("Did not receive successful response: " + response);
            }
            listener.getLogger().println("Successfully uploaded build to UrbanCode Velocity.");
        } catch (Exception ex) {
            listener.error("Error uploading build data: " + ex.getClass() + " - " + ex.getMessage());
            build.setResult(Result.FAILURE);
        }
    }

    @Extension
    public static class UploadBuildDescriptor extends BuildStepDescriptor<Builder> {

        public UploadBuildDescriptor() {
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
            return "UCV - Upload Build to UrbanCode Velocity";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
