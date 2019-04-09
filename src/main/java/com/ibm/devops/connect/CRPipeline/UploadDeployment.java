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

public class UploadDeployment extends Builder implements SimpleBuildStep {

    enum STATUS {
        SUCCESS,
        FAILURE
    }

    private String tenantId;
    private String name;
    private STATUS status;
    private String initiator;
    private String versionName;
    private String versionExtId;
    private String type;
    private String environmentId;
    private String environmentName;
    private String description;
    private Long startTime;
    private Long endTime;
    private String appName;
    private String appId;
    private String appExtId;

    @DataBoundConstructor
    public UploadDeployment(
        String tenantId,
        String name,
        STATUS status,
        String initiator,
        String versionName,
        String versionExtId,
        String type,
        String environmentId,
        String environmentName,
        String description,
        Long startTime,
        Long endTime,
        String appName,
        String appId,
        String appExtId
    ) {
        this.tenantId = tenantId;
        this.name = name;
        this.status = status;
        this.initiator = initiator;
        this.versionName = versionName;
        this.versionExtId = versionExtId;
        this.type = type;
        this.environmentId = environmentId;
        this.environmentName = environmentName;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.appName = appName;
        this.appId = appId;
        this.appExtId = appExtId;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public String getName() {
        return this.name;
    }

    public STATUS getStatus() {
        return this.status;
    }

    public String getInitiator() {
        return this.initiator;
    }

    public String getVersionName() {
        return this.versionName;
    }

    public String getVersionExtId() {
        return this.versionExtId;
    }

    public String getType() {
        return this.type;
    }

    public String getEnvironmentId() {
        return this.environmentId;
    }

    public String getEnvironmentName() {
        return this.environmentName;
    }

    public String getDescription() {
        return this.description;
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
        payload.put("tenant_id", this.tenantId);
        payload.put("version_name", this.versionName);
        payload.put("version_id_external", this.versionExtId);
        payload.put("type", this.type);
        payload.put("environment_id", this.environmentId);
        payload.put("environment_name", this.environmentName);
        payload.put("description", this.description);
        JSONObject application = new JSONObject();
        application.put("id", this.appId);
        application.put("name", this.appName);
        application.put("externalId", this.appExtId);
        payload.put("application", application);

        // user-provided inputs with fallbacks
        if (this.name != null) {
            payload.put("name", this.name);
        } else {
            payload.put("name", build.getParent().getName());
        }
        if (this.initiator != null) {
            payload.put("by_user", this.initiator);
        } else {
            for (Cause cause : build.getCauses()) {
                if (cause instanceof UserIdCause) {
                    UserIdCause userCause = (UserIdCause)cause;
                    payload.put("by_user", userCause.getUserName());
                } else if (cause instanceof UpstreamCause) {
                    UpstreamCause upstreamCause = (UpstreamCause)cause;
                    payload.put("by_user", "Upstream job \"" + upstreamCause.getUpstreamProject() + "\", build \"" + upstreamCause.getUpstreamBuild() + "\"");
                }
            }
        }
        if (this.status != null) {
            payload.put("result", this.status.equals(STATUS.SUCCESS) ? "Success" : "Failed");
        } else {
            String computedStatus = "Failed";
            if (build.getResult() == null || build.getResult().equals(Result.SUCCESS)) {
                computedStatus = "Success";
            }
            payload.put("result", computedStatus);
        }
        if (this.startTime != null) {
            payload.put("start_time", this.startTime);
        } else {
            payload.put("start_time", build.getStartTimeInMillis());
        }
        if (this.endTime != null) {
            payload.put("end_time", this.endTime);
        } else {
            payload.put("end_time", System.currentTimeMillis());
        }


        System.out.println("TEST payload: " + payload.toString(2));

        try {
            CloudPublisher.uploadDeployment(payload.toString());
        } catch (Exception ex) {
            listener.error("Error uploading deployment data: " + ex.getClass() + " - " + ex.getMessage());
            build.setResult(Result.FAILURE);
        }
    }

    @Extension
    public static class UploadDeploymentDescriptor extends BuildStepDescriptor<Builder> {

        public UploadDeploymentDescriptor() {
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
            return "Upload deployment information to UrbanCode Velocity";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
