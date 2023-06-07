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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import java.net.InetAddress;
import org.onosproject.netviews.GetIPService;
/**
 * Sample Apache Karaf CLI command.
 */
@Service
@Command(scope = "onos", name = "get-ip",
         description = "gets and sets the ip of the container the controller is controlling")
public class GetIP extends AbstractShellCommand {

    @Override
    protected void doExecute() throws Exception {
	GetIPService service = get(GetIPService.class);
	try {
		InetAddress ip;
                ip = InetAddress.getLocalHost();
		service.setIP(ip.getHostAddress());
	}
	catch (Exception e) {
		print("no host ip");
	}
	
	//String command = "/tmp/trigger-obligations";
        //Process p2 = Runtime.getRuntime().exec(command);
        print("got ip");
    }

}
