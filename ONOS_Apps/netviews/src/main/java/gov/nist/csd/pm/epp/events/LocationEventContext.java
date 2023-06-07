package gov.nist.csd.pm.epp.events;

import gov.nist.csd.pm.pdp.services.UserContext;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;

public class LocationEventContext extends EventContext {
    private Node userDevicePair;

    public LocationEventContext(UserContext userCtx, Node target, Node userDevice) {
        super(userCtx, ASSIGN_TO_EVENT, target);
        this.userDevicePair = userDevice;
    }

    public Node getUserDevicePair() {
        return userDevicePair;
    }
}
