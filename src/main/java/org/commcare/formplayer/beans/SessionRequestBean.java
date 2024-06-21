package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by benrudolph on 9/8/16.
 */
public class SessionRequestBean extends AuthenticatedRequestBean {
    protected String sessionId;

    @JsonGetter(value = "session_id")
    public String getSessionId() {
        return sessionId;
    }

    @JsonSetter(value = "session_id")
    public void setSessionId(String sessionId) {
        if (sessionId != null) {
            this.sessionId = sessionId;
        }
    }

    @Deprecated
    @JsonSetter(value = "session-id")
    public void setSessionDashId(String sessionId) {
        if (this.sessionId == null) {
            setSessionId(sessionId);
        }
    }

    @Deprecated
    @JsonSetter(value = "sessionId")
    public void setSessionCamelCaseId(String sessionId) {
        if (this.sessionId == null) {
            setSessionId(sessionId);
        }
    }

    @Override
    public String toString() {
        return "SessionRequestBean [sessionId=" + sessionId + "]";
    }
}
