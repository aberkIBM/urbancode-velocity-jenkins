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
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.tasks.SimpleBuildStep;

import java.io.IOException;
import java.util.Iterator;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.ibm.devops.connect.CloudPublisher;

public class CheckGate extends Builder implements SimpleBuildStep {

    private String pipelineId;
    private String stageName;
    private String versionId;

    @DataBoundConstructor
    public CheckGate(
        String pipelineId,
        String stageName,
        String versionId
    ) {
        this.pipelineId = pipelineId;
        this.stageName = stageName;
        this.versionId = versionId;
    }

    public String getPipelineId() {
        return this.pipelineId;
    }

    public String getStageName() {
        return this.stageName;
    }

    public String getVersionId() {
        return this.versionId;
    }

    @Override
    public void perform(final Run<?, ?> build, FilePath workspace, Launcher launcher, final TaskListener listener)
            throws AbortException, InterruptedException, IOException {

        try {
            String result = CloudPublisher.checkGate(this.pipelineId, this.stageName, this.versionId);
            JSONObject resultObj = JSONObject.fromObject(result);
            Iterator<?> keys = resultObj.keys();
            Boolean anyGatePassed = false;
            Boolean anyGateFailed = false;
            while(keys.hasNext()) {
                String key = keys.next().toString();
                String value = resultObj.get(key).toString();
                if (value.equals("true")) {
                    listener.getLogger().println("Gate \"" + key + "\" passed");
                    anyGatePassed = true;
                } else if (value.equals("false")) {
                    listener.getLogger().println("Gate \"" + key + "\" failed");
                    anyGateFailed = true;
                }
            }
            if (!anyGatePassed || anyGateFailed) {
                build.setResult(Result.FAILURE);
            }
        } catch (Exception ex) {
            listener.error("Error checking gate: " + ex.getClass() + " - " + ex.getMessage());
            build.setResult(Result.FAILURE);
        }
    }

    @Extension
    public static class CheckGateDescriptor extends BuildStepDescriptor<Builder> {

        public CheckGateDescriptor() {
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
            return "Check a gate in UrbanCode Velocity";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
