package org.commcare.formplayer.beans;

import org.commcare.cases.model.Case;
import org.commcare.formplayer.objects.FormSessionListView;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.util.SessionUtils;

import java.util.NoSuchElementException;

/**
 * Individual display item in list of incomplete form sessions
 */
public class SessionListItem {

    private String title;
    private String dateOpened;
    private String sessionId;
    private String caseName;

    public SessionListItem(SqlStorage<Case> caseStorage, FormSessionListView session) {
        this.title = session.getTitle();
        this.dateOpened = session.getDateCreated().toString();
        this.sessionId = session.getId();
        this.caseName = loadCaseName(caseStorage, session);
    }

    private String loadCaseName(SqlStorage<Case> caseStorage, FormSessionListView session) {
        String caseId = session.getSessionData().get("case_id");
        try {
            return SessionUtils.tryLoadCaseName(caseStorage, caseId);
        } catch (NoSuchElementException e) {
            // This handles the case where the case is no longer open in the database.
            // The form will crash on open, but I don't know if there's a more elegant but
            // not-opaque way to handle
            return "Case with id " + caseId + "does not exist!";
        }
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDateOpened() {
        return dateOpened;
    }

    public void setDateOpened(String dateOpened) {
        this.dateOpened = dateOpened;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCaseName() {
        return caseName;
    }

    @Override
    public String toString() {
        return "SessionListItem [title = " + title + " id " + sessionId + " opened " + dateOpened
                + "]";
    }
}
