package gov.nist.csd.pm.epp.functions;

import gov.nist.csd.pm.epp.FunctionEvaluator;
import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.epp.events.LocationEventContext;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pdp.services.UserContext;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;

public class CurrentUserDevicePairNameExecutor implements FunctionExecutor {
    @Override
    public String getFunctionName() {
        return "current_userDevicePair_name";
    }

    @Override
    public int numParams() {
        return 0;
    }

    @Override
    public String exec(UserContext obligationUser, EventContext eventCtx, PDP pdp, Function function, FunctionEvaluator functionEvaluator) throws PMException {
        LocationEventContext locationEventContext = (LocationEventContext) eventCtx;
        return locationEventContext.getUserDevicePair().getName();
    }


}