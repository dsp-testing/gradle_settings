package org.commcare.formplayer.beans.menus;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.session.RemoteQuerySessionManager;
import org.commcare.suite.model.QueryPrompt;
import org.commcare.suite.model.QueryGroup;
import org.commcare.util.screen.QueryScreen;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.utils.ItemSetUtils;
import org.javarosa.core.util.OrderedHashtable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by willpride on 4/13/16.
 */
public class QueryResponseBean extends MenuBean {

    private DisplayElement[] displays;
    private final String type = "query";
    private String description;
    private Hashtable<String, String> groupHeaders;
    private Boolean searchOnClear;

    QueryResponseBean() {
    }

    public DisplayElement[] getDisplays() {
        return displays;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    private void setDisplays(DisplayElement[] displays) {
        this.displays = displays;
    }

    public Hashtable<String, String> getGroupHeaders(){
        return groupHeaders;
    }

    public void setGroupHeaders(Hashtable<String, String> groupHeaders){
        this.groupHeaders = groupHeaders;
    }

    public boolean isSearchOnClear() {
        return searchOnClear;
    }

    public void setIsSearchOnClear(boolean searchOnClear) {
        this.searchOnClear = searchOnClear;
    }

    public QueryResponseBean(QueryScreen queryScreen) {
        OrderedHashtable<String, QueryPrompt> queryPromptMap = queryScreen.getUserInputDisplays();
        Hashtable<String, String> currentAnswers = queryScreen.getCurrentAnswers();
        Hashtable<String, String> errors = queryScreen.getErrors();
        Hashtable<String, Boolean> requiredPrompts = queryScreen.getRequiredPrompts();
        displays = new DisplayElement[queryPromptMap.size()];
        int count = 0;
        SessionWrapper querySession = queryScreen.getSession();
        EvaluationContext ec = querySession.getEvaluationContext();
        for (String key : Collections.list(queryPromptMap.keys())) {
            QueryPrompt queryPromptItem = queryPromptMap.get(key);
            String currentAnswer = currentAnswers.get(key);

            // Map the current Answer to the itemset index of the answer
            String[] choiceLabels = null;
            String[] choiceKeys = null;
            if (queryPromptItem.isSelect()) {
                Pair<String[], String[]> choices = ItemSetUtils.getChoices(queryPromptItem.getItemsetBinding());
                choiceKeys = choices.first;
                choiceLabels = choices.second;
            }

            String requiredMessage = queryPromptItem.getRequiredMessage(ec);
            boolean isRequired = requiredPrompts.containsKey(key) && requiredPrompts.get(key);
            displays[count] = new DisplayElement(queryPromptItem.getDisplay(),
                    ec,
                    key,
                    queryPromptItem.getInput(),
                    queryPromptItem.getReceive(),
                    queryPromptItem.getHidden(),
                    currentAnswer,
                    choiceKeys,
                    choiceLabels,
                    queryPromptItem.isAllowBlankValue(),
                    isRequired,
                    requiredMessage,
                    errors.get(key),
                    queryPromptItem.getGroupKey()
                    );
            count++;
        }

        setGroupHeaders(queryScreen.evalGroupHeaders());
        setTitle(queryScreen.getScreenTitle());
        setDescription(queryScreen.getDescriptionText());
        setQueryKey(queryScreen.getQueryKey());
        setDynamicSearch(queryScreen.getDynamicSearch());
        setIsSearchOnClear(queryScreen.isSearchOnClear());
    }

    @Override
    public String toString() {
        return "QueryResponseBean [displays=" + Arrays.toString(displays)
                + "description=" + getDescription()
                + "MenuBean= " + super.toString() + "]";
    }

    public String getType() {
        return type;
    }
}
