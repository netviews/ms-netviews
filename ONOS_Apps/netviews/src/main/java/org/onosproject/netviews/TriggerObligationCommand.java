/*
 * Copyright 2021-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.netviews;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;

/**
 * CLI command to trigger obligation
 */
@Service
@Command(scope = "onos", name = "trigger-obligation",
         description = "Triggers obligation")
public class TriggerObligationCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "userIP", description = "IP of user",
            required = true, multiValued = false)
    private String userIP = "";

    @Argument(index = 1, name = "userLocation", description = "location of user",
            required = true, multiValued = false)
    private String userLocation = "";

    @Argument(index = 2, name = "thisSite", description = "the site this controller is on",
            required = true, multiValued = false)
    private String thisSite = "";

    @Argument(index = 3, name = "deviceName", description = "device name",
            required = true, multiValued = false)
    private String deviceName = "";

    @Override
    protected void doExecute() throws Exception {
        TriggerObligationService service = get(TriggerObligationService.class);
        service.triggerObligation(userIP, userLocation, thisSite, deviceName);
        //print("Obligation Triggered");
    }

}
