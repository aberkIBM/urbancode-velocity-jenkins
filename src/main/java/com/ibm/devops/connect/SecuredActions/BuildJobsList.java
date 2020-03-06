package com.ibm.devops.connect.SecuredActions;

import com.ibm.devops.connect.CloudItemListener;
import jenkins.model.Jenkins;
import hudson.model.AbstractItem;

import com.ibm.cloud.urbancode.connect.client.ConnectSocket;

import java.util.List;

public class BuildJobsList extends AbstractSecuredAction {

    protected void run(AbstractSecuredAction.ParamObj paramObj) {
        CloudItemListener cil = new CloudItemListener();
        cil.buildJobsList();
    }

    public class BuildJobListParamObj extends AbstractSecuredAction.ParamObj {

        public ConnectSocket socket;
        public String event;
        public Object[] args;

        public BuildJobListParamObj(ConnectSocket socket, String event, Object... args) {
            this.socket = socket;
            this.event = event;
            this.args = args;
        }
    }
}