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
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Cause;
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

    private String id;
    private String tenantId;
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
        String id,
        String tenantId,
        String revision,
        String requestor,
        STATUS status,
        Long startTime,
        Long endTime,
        String appName,
        String appId,
        String appExtId
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.revision = revision;
        this.requestor = requestor;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.appName = appName;
        this.appId = appId;
        this.appExtId = appExtId;
    }

    public String getId() {
        return this.id;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public String getRevision() {
        return this.revision;
    }

    public String getRequestor() {
        return this.requestor;
    }

    public STATUS getStatus() {
        return this.status;
    }

    public Long getStartTime() {
        return this.startTime;
    }

    public Long getEndTime() {
        return this.endTime;
    }

    public String getAppName() {
        return this.appName;
    }

    public String getAppId() {
        return this.appId;
    }

    public String getAppExtId() {
        return this.appExtId;
    }

    @Override
    public void perform(final Run<?, ?> build, FilePath workspace, Launcher launcher, final TaskListener listener)
            throws AbortException, InterruptedException, IOException {

        JSONObject payload = new JSONObject();

        // user-provided inputs
        payload.put("tenantId", this.tenantId);
        payload.put("revision", this.revision);
        JSONObject application = new JSONObject();
        application.put("id", this.appId);
        application.put("name", this.appName);
        application.put("externalId", this.appExtId);
        payload.put("application", application);

        // user-provided inputs with fallbacks
        if (this.requestor != null) {
            payload.put("requestor", this.requestor);
        } else {
            for (Cause cause : build.getCauses()) {
                if (cause instanceof UserIdCause) {
                    UserIdCause userCause = (UserIdCause)cause;
                    payload.put("requestor", userCause.getUserName());
                }
            }
        }
        if (this.status != null) {
          payload.put("status", this.status.equals(STATUS.SUCCESS) ? "success" : "failure");
        } else {
            String computedStatus = "failure";
            if (build.getResult() == null || build.getResult().equals(Result.SUCCESS)) {
                computedStatus = "success";
            }
            payload.put("status", computedStatus);
        }
        if (this.startTime != null) {
            payload.put("startTime", this.startTime);
        } else {
            payload.put("startTime", build.getStartTimeInMillis());
        }
        if (this.endTime != null) {
            payload.put("endTime", this.endTime);
        } else {
            payload.put("endTime", System.currentTimeMillis());
        }

        // build-derived inputs
        payload.put("name", build.getDisplayName());
        payload.put("url", Jenkins.getInstance().getRootUrl() + build.getUrl());
        payload.put("id", build.getParent().getName() + " - " + build.getId());


        System.out.println("TEST payload: " + payload.toString(2));

        try {
            CloudPublisher.uploadBuild(payload.toString());
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
            return "Upload build information to UrbanCode Velocity";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
