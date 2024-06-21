package org.commcare.formplayer.session;

import static org.commcare.formplayer.util.SessionUtils.resolveInstallReference;
import static org.commcare.session.SessionFrame.isEntitySelectionDatum;
import static org.commcare.util.screen.MultiSelectEntityScreen.USE_SELECTED_VALUES;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.core.interfaces.RemoteInstanceFetcher;
import org.commcare.formplayer.engine.FormplayerConfigEngine;
import org.commcare.formplayer.objects.SerializableFormDefinition;
import org.commcare.formplayer.objects.SerializableMenuSession;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.screens.FormplayerQueryScreen;
import org.commcare.formplayer.screens.FormplayerSyncScreen;
import org.commcare.formplayer.services.FormDefinitionService;
import org.commcare.formplayer.services.FormplayerRemoteInstanceFetcher;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.InstallService;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.util.FormplayerHereFunctionHandler;
import org.commcare.formplayer.util.SessionUtils;
import org.commcare.formplayer.util.StringUtils;
import org.commcare.formplayer.util.serializer.SessionSerializer;
import org.commcare.modern.database.TableBuilder;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.session.CommCareSession;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.Endpoint;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.FormIdDatum;
import org.commcare.suite.model.MultiSelectEntityDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.util.screen.CommCareSessionException;
import org.commcare.util.screen.EntityScreen;
import org.commcare.util.screen.EntityScreenContext;
import org.commcare.util.screen.MenuScreen;
import org.commcare.util.screen.MultiSelectEntityScreen;
import org.commcare.util.screen.QueryScreen;
import org.commcare.util.screen.Screen;
import org.commcare.util.screen.ScreenUtils;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.HereFunctionHandlerListener;
import org.javarosa.core.util.MD5;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import datadog.trace.api.Trace;


/**
 * This (along with FormSession) is a total god object. This manages everything from installation to form entry.
 * This primarily includes module and form navigation, along with case list/details and case selection. When ready,
 * this object will create and hand off flow control to a FormSession object, loading up the proper session data.
 *
 * A lot of this is copied from the CLI. We need to merge that. Big TODO
 */
public class MenuSession implements HereFunctionHandlerListener {
    private final SerializableMenuSession session;
    private FormplayerConfigEngine engine;
    private UserSqlSandbox sandbox;
    private SessionWrapper sessionWrapper;

    private final Log log = LogFactory.getLog(MenuSession.class);
    ArrayList<String> breadcrumbs;
    private ArrayList<String> selections = new ArrayList<>();

    private String currentBrowserLocation;
    private boolean hereFunctionEvaluated;

    // Stores the entity screens created to manage state for the lifecycle of this request
    private Map<String, EntityScreen> entityScreenCache = new HashMap<>();
    private boolean oneQuestionPerScreen;
    private FormplayerRemoteInstanceFetcher instanceFetcher;

    private String smartLinkRedirect;

    public MenuSession(SerializableMenuSession session,
            FormplayerConfigEngine engine, CommCareSession commCareSession, RestoreFactory restoreFactory,
            FormplayerRemoteInstanceFetcher instanceFetcher) throws Exception {
        this.instanceFetcher = instanceFetcher;
        this.session = session;
        this.engine = engine;
        this.sandbox = restoreFactory.getSandbox();
        this.sessionWrapper = new FormplayerSessionWrapper(
                commCareSession, engine.getPlatform(), sandbox, instanceFetcher);
        SessionUtils.setLocale(session.getLocale());
        sessionWrapper.syncState();
        initializeBreadcrumbs();
    }

    public MenuSession(String username, String domain, String appId, String locale,
            InstallService installService, RestoreFactory restoreFactory, String host,
            boolean oneQuestionPerScreen, String asUser, boolean preview,
            FormplayerRemoteInstanceFetcher instanceFetcher)
            throws Exception {
        this.oneQuestionPerScreen = oneQuestionPerScreen;
        this.instanceFetcher = instanceFetcher;
        String resolvedInstallReference = resolveInstallReference(appId, host, domain);
        this.session = new SerializableMenuSession(
                TableBuilder.scrubName(username),
                domain,
                appId,
                resolvedInstallReference,
                locale,
                asUser,
                preview
        );
        Pair<FormplayerConfigEngine, Boolean> install = installService.configureApplication(
                resolvedInstallReference, preview);
        this.engine = install.first;
        if (install.second && !preview && !restoreFactory.getHasRestored()) {
            this.sandbox = restoreFactory.performTimedSync();
        }
        this.sandbox = restoreFactory.getSandbox();
        this.sessionWrapper = new FormplayerSessionWrapper(engine.getPlatform(), sandbox,
                instanceFetcher);
        SessionUtils.setLocale(locale);
        initializeBreadcrumbs();
    }

    public void resetSession() throws RemoteInstanceFetcher.RemoteInstanceException {
        this.sessionWrapper = new FormplayerSessionWrapper(engine.getPlatform(), sandbox,
                instanceFetcher);
        clearEntityScreenCache();
        initializeBreadcrumbs();
        selections.clear();
    }

    private void initializeBreadcrumbs() {
        this.breadcrumbs = new ArrayList<>();
        this.breadcrumbs.add(ScreenUtils.getAppTitle());
    }

    /**
     * @param screen                Current Screen if avaialble, null otherwise
     * @param input                 The user step input
     * @param needsFullEntityScreen Whether a full entity screen is required for this request
     *                              or if a list of references is sufficient
     * @param inputValidated        Whether the input has been previously validated,
     *                              allowing this step to skip validation
     * @param allowAutoLaunch       If this step is allowed to automatically launch an action,
     *                              assuming it has an autolaunch action specified.
     * @param respectRelevancy      Whether to respect display conditions on a module or form while
     *                              handling input
     */
    public boolean handleInput(@Nullable Screen screen, String input, boolean needsFullEntityScreen, boolean inputValidated,
            boolean allowAutoLaunch, EntityScreenContext entityScreenContext, boolean respectRelevancy)
            throws CommCareSessionException {
        if (screen == null) {
            screen = getNextScreen(needsFullEntityScreen, entityScreenContext);
        }

        log.info("Screen " + screen + " handling input " + input);
        if (screen == null) {
            return false;
        }
        try {
            boolean addBreadcrumb = true;
            String[] selectedValues = entityScreenContext.getSelectedValues();
            if (screen instanceof EntityScreen) {
                EntityScreen entityScreen = (EntityScreen)screen;
                boolean autoLaunch = entityScreen.getAutoLaunchAction() != null && allowAutoLaunch;
                addBreadcrumb = !autoLaunch;
                if (input.startsWith("action ") || (autoLaunch) || !inputValidated) {
                    screen.init(sessionWrapper);
                    // auto-launch takes preference over auto-select
                    if (screen.shouldBeSkipped() && !autoLaunch &&
                            entityScreen.autoSelectEntities(sessionWrapper)) {
                        return handleInput(screen, input, true, inputValidated, allowAutoLaunch, entityScreenContext, respectRelevancy);
                    }
                    screen.handleInputAndUpdateSession(sessionWrapper, input, allowAutoLaunch, selectedValues,
                            respectRelevancy);
                } else {
                    entityScreen.updateDatum(sessionWrapper, input);
                }
            } else {
                screen.handleInputAndUpdateSession(sessionWrapper, input, allowAutoLaunch, selectedValues, respectRelevancy);
            }

            if (screen instanceof MultiSelectEntityScreen && input.contentEquals(
                    USE_SELECTED_VALUES)) {
                addSelection(((MultiSelectEntityScreen)screen).getStorageReferenceId());
            }

            if (addBreadcrumb) {
                breadcrumbs.add(screen.getBreadcrumb(input, sandbox, getSessionWrapper()));
            }

            return true;
        } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
            throw new RuntimeException("Screen " + screen + "  handling input " + input +
                    " threw exception " + e.getMessage() + ". Please try reloading this application"
                    +
                    " and if the problem persists please report a bug.", e);
        }
    }

    /**
     * @param screen           The current screen that has been navigated to.
     * @param autoAdvanceMenu  Whether the menu navigation should be advanced if it can be.
     * @param respectRelevancy Whether to respect menu relevancy conditions while trying to auto-advance
     * @return true if the session was advanced
     * @throws CommCareSessionException
     */
    public boolean autoAdvanceMenu(Screen screen, boolean autoAdvanceMenu, boolean respectRelevancy)
            throws CommCareSessionException {
        if (!autoAdvanceMenu || !(screen instanceof MenuScreen)) {
            return false;
        }
        return ((MenuScreen)screen).handleAutoMenuAdvance(sessionWrapper, respectRelevancy);
    }

    /**
     * Get next screen for current request, based on current state of session,
     * with autolaunching of actions not allowed.
     *
     * @param needsFullEntityScreen Whether a full entity screen is required for this request
     *                              or if a list of references is sufficient
     * @param entityScreenContext   Entity Screen context
     */
    @Trace
    public Screen getNextScreen(boolean needsFullEntityScreen, EntityScreenContext entityScreenContext)
            throws CommCareSessionException {
        String next = sessionWrapper.getNeededData(sessionWrapper.getEvaluationContext());
        if (next == null) {
            if (sessionWrapper.isViewCommand(sessionWrapper.getCommand())) {
                sessionWrapper.stepBack();
                return getNextScreen(needsFullEntityScreen, entityScreenContext);
            }
            //XFORM TIME!
            return null;
        } else if (next.equals(SessionFrame.STATE_COMMAND_ID)) {
            MenuScreen menuScreen = new MenuScreen();
            menuScreen.init(sessionWrapper);
            return menuScreen;
        } else if (isEntitySelectionDatum(next)) {
            EntityScreen entityScreen = getEntityScreenForSession(needsFullEntityScreen, entityScreenContext);
            return entityScreen;
        } else if (next.equalsIgnoreCase(SessionFrame.STATE_DATUM_COMPUTED)) {
            computeDatum();
            return getNextScreen(needsFullEntityScreen, entityScreenContext);
        } else if (next.equalsIgnoreCase(SessionFrame.STATE_QUERY_REQUEST)) {
            QueryScreen queryScreen = new FormplayerQueryScreen(
                    this.instanceFetcher.getVirtualDataInstanceStorage());
            queryScreen.init(sessionWrapper);
            return queryScreen;
        } else if (next.equalsIgnoreCase(SessionFrame.STATE_SYNC_REQUEST)) {
            String username = session.getAsUser() != null ?
                    StringUtils.getFullUsername(session.getAsUser(), session.getDomain()) : null;
            FormplayerSyncScreen syncScreen = new FormplayerSyncScreen(username);
            syncScreen.init(sessionWrapper);
            return syncScreen;
        }
        throw new RuntimeException("Unexpected Frame Request: " + sessionWrapper.getNeededData());
    }

    private void clearEntityScreenCache() {
        entityScreenCache.clear();
    }

    @Trace
    private EntityScreen getEntityScreenForSession(boolean needsFullEntityScreen,
            EntityScreenContext entityScreenContext)
            throws CommCareSessionException {
        EntityDatum datum = (EntityDatum)sessionWrapper.getNeededDatum();

        //This is only needed because with remote queries there can be nested datums with the same
        //datum ID in the same http request lifecycle.
        String nodesetHash = MD5.toHex(MD5.hash(datum.getNodeset().toString(true).getBytes()));

        String datumKey = datum.getDataId() + ", " + nodesetHash;
        if (!entityScreenCache.containsKey(datumKey)) {
            EntityScreen entityScreen = createFreshEntityScreen(needsFullEntityScreen, datum, entityScreenContext);
            entityScreenCache.put(datumKey, entityScreen);
            return entityScreen;
        } else {
            return entityScreenCache.get(datumKey);
        }
    }

    @Trace
    private EntityScreen createFreshEntityScreen(boolean needsFullEntityScreen,
            EntityDatum datum, EntityScreenContext entityScreenContext)
            throws CommCareSessionException {
        if (datum instanceof MultiSelectEntityDatum) {
            return new MultiSelectEntityScreen(false, needsFullEntityScreen,
                    sessionWrapper, instanceFetcher.getVirtualDataInstanceStorage(), entityScreenContext);
        } else {
            return new EntityScreen(false, needsFullEntityScreen, sessionWrapper, entityScreenContext);
        }
    }

    private void computeDatum() {
        //compute
        SessionDatum datum = sessionWrapper.getNeededDatum();
        XPathExpression form;
        try {
            form = XPathParseTool.parseXPath(datum.getValue());
        } catch (XPathSyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        EvaluationContext ec = sessionWrapper.getEvaluationContext();
        if (datum instanceof FormIdDatum) {
            sessionWrapper.setXmlns(FunctionUtils.toString(form.eval(ec)));
            sessionWrapper.setEntityDatum("", "awful");
        } else {
            sessionWrapper.setEntityDatum(datum, FunctionUtils.toString(form.eval(ec)));
        }
    }

    @Trace
    private HashMap<String, String> getSessionData() {
        OrderedHashtable<String, String> sessionData = sessionWrapper.getData();
        HashMap<String, String> ret = new HashMap<>();
        for (String key : sessionData.keySet()) {
            ret.put(key, sessionData.get(key));
        }
        return ret;
    }

    @Trace
    public FormSession getFormEntrySession(FormSendCalloutHandler formSendCalloutHandler,
            FormplayerStorageFactory storageFactory,
            FormDefinitionService formDefinitionService) throws Exception {
        String formXmlns = sessionWrapper.getForm();
        FormDef formDef = this.engine.loadFormByXmlns(formXmlns);
        SerializableFormDefinition serializableFormDefinition = formDefinitionService.getOrCreateFormDefinition(
                this.getAppId(),
                formXmlns,
                this.getAppVersion(),
                formDef
        );
        HashMap<String, String> sessionData = getSessionData();

        String postUrl = sessionWrapper.getPlatform().getPropertyManager().getSingularProperty(
                "PostURL");
        return new FormSession(sandbox, serializableFormDefinition, formDef, session.getUsername(),
                session.getDomain(), sessionData, postUrl, session.getLocale(), session.getId(), null,
                oneQuestionPerScreen, session.getAsUser(), session.getAppId(), null, formSendCalloutHandler,
                storageFactory, false, null, new SessionFrame(sessionWrapper.getFrame()),
                instanceFetcher);
    }

    public SessionWrapper getSessionWrapper() {
        return sessionWrapper;
    }

    public String getId() {
        return session.getId();
    }

    public String getAppId() {
        return session.getAppId();
    }

    public String getCommCareVersionString() {
        return sessionWrapper.getIIF().getVersionString();
    }

    public String getAppVersion() {
        return "" + this.engine.getPlatform().getCurrentProfile().getVersion();
    }

    public String[] getBreadcrumbs() {
        String[] ret = new String[breadcrumbs.size()];
        breadcrumbs.toArray(ret);
        return ret;
    }

    public String[] getSelections() {
        String[] ret = new String[selections.size()];
        selections.toArray(ret);
        return ret;
    }

    public void addSelection(String currentStep) {
        selections.add(currentStep);
    }

    public Endpoint getEndpoint(String id) {
        return engine.getPlatform().getAllEndpoints().get(id);
    }

    public String getSmartLinkRedirect() {
        return smartLinkRedirect;
    }

    public void setSmartLinkRedirect(String url) {
        smartLinkRedirect = url;
    }

    public void setCurrentBrowserLocation(String location) {
        this.currentBrowserLocation = location;
    }

    public String getCurrentBrowserLocation() {
        return this.currentBrowserLocation;
    }

    @Override
    public void onEvalLocationChanged() {
    }

    @Override
    public void onHereFunctionEvaluated() {
        this.hereFunctionEvaluated = true;
    }

    public boolean locationRequestNeeded() {
        return this.hereFunctionEvaluated && this.currentBrowserLocation == null;
    }

    public boolean hereFunctionEvaluated() {
        return this.hereFunctionEvaluated;
    }

    @Trace
    public EvaluationContext getEvalContextWithHereFuncHandler() {
        EvaluationContext ec = sessionWrapper.getEvaluationContext();
        ec.addFunctionHandler(new FormplayerHereFunctionHandler(this, currentBrowserLocation));
        return ec;
    }

    public SerializableMenuSession serialize() {
        session.setCommcareSession(SessionSerializer.serialize(sessionWrapper));
        return session;
    }

}
