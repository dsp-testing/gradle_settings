package org.commcare.formplayer.utils;

import com.timgroup.statsd.StatsDClient;

import org.commcare.formplayer.application.CommCareSessionFactory;
import org.commcare.formplayer.application.FormSessionFactory;
import org.commcare.formplayer.application.FormSubmissionHelper;
import org.commcare.formplayer.application.SQLiteProperties;
import org.commcare.formplayer.installers.FormplayerInstallerFactory;
import org.commcare.formplayer.mocks.MockLockRegistry;
import org.commcare.formplayer.mocks.TestInstallService;
import org.commcare.formplayer.objects.FormVolatilityRecord;
import org.commcare.formplayer.services.CaseSearchHelper;
import org.commcare.formplayer.services.CategoryTimingHelper;
import org.commcare.formplayer.services.FormDefinitionService;
import org.commcare.formplayer.services.FormSessionService;
import org.commcare.formplayer.services.FormplayerFormSendCalloutHandler;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.HqUserDetailsService;
import org.commcare.formplayer.services.InstallService;
import org.commcare.formplayer.services.MediaMetaDataService;
import org.commcare.formplayer.services.MenuSessionFactory;
import org.commcare.formplayer.services.MenuSessionRunnerService;
import org.commcare.formplayer.services.MenuSessionService;
import org.commcare.formplayer.services.NewFormResponseFactory;
import org.commcare.formplayer.services.ResponseMetaDataTracker;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.services.SubmitService;
import org.commcare.formplayer.services.VirtualDataInstanceService;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerDatadog;
import org.commcare.formplayer.util.NotificationLogger;
import org.commcare.formplayer.web.client.WebClient;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.Duration;
import java.util.ArrayList;


public class TestContext {

    public TestContext() {
        MockitoAnnotations.openMocks(this);
    }

    @Bean
    public SQLiteProperties sqliteProperties() {
        SQLiteProperties sqLiteProperties = new SQLiteProperties();
        sqLiteProperties.setDataDir("testdbs/");
        return sqLiteProperties;
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();

        messageSource.setBasename("i18n/messages");
        messageSource.setUseCodeAsDefaultMessage(true);

        return messageSource;
    }

    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver internalResourceViewResolver =
                new InternalResourceViewResolver();
        internalResourceViewResolver.setPrefix("/WEB-INF/jsp/view/");
        internalResourceViewResolver.setSuffix(".jsp");
        return internalResourceViewResolver;
    }

    @MockBean
    public MediaMetaDataService mediaMetaDataService;

    @MockBean
    public FormSessionService formSessionService;

    @MockBean
    public FormDefinitionService formDefinitionService;

    @MockBean
    public MenuSessionService menuSessionService;

    @MockBean
    public VirtualDataInstanceService virtualDataInstanceService;

    @MockBean
    public WebClient webClient;

    @MockBean
    public HqUserDetailsService userDetailsService;

    @MockBean
    public NotificationLogger notificationLogger;

    @Bean
    public ValueOperations<String, Long> redisTemplateLong() {
        return Mockito.mock(ValueOperations.class);
    }

    @Bean
    public StringRedisTemplate redisTemplate() {
        return Mockito.mock(StringRedisTemplate.class);
    }

    @Bean
    public ValueOperations<String, Long> redisTemplateString() {
        return Mockito.mock(ValueOperations.class);
    }

    @Bean
    public SetOperations<String, String> redisSetTemplate() {
        return Mockito.mock(SetOperations.class, Mockito.RETURNS_DEEP_STUBS);
    }

    @Bean
    public ValueOperations<String, FormVolatilityRecord> redisVolatilityDict() {
        return Mockito.mock(ValueOperations.class);
    }

    @Bean
    public RestoreFactory restoreFactory() {
        return Mockito.spy(RestoreFactory.class);
    }

    @Bean
    public ResponseMetaDataTracker responseMetaDataTracker() {
        return Mockito.spy(ResponseMetaDataTracker.class);
    }

    @Bean
    public FormplayerStorageFactory storageFactory() {
        return Mockito.spy(FormplayerStorageFactory.class);
    }

    @Bean
    public InstallService installService() {
        return Mockito.spy(TestInstallService.class);
    }

    @Bean
    public SubmitService submitService() {
        return Mockito.mock(SubmitService.class);
    }

    @Bean
    public FormplayerDatadog datadog() {
        return Mockito.spy(new FormplayerDatadog(datadogStatsDClient(), new ArrayList<String>()));
    }

    @Bean
    public LockRegistry userLockRegistry() {
        return Mockito.spy(MockLockRegistry.class);
    }

    @Bean
    public NewFormResponseFactory newFormResponseFactory() {
        return Mockito.spy(NewFormResponseFactory.class);
    }

    @Bean
    public FormplayerInstallerFactory installerFactory() {
        return Mockito.spy(FormplayerInstallerFactory.class);
    }

    @Bean
    public ArchiveFileRoot formplayerArchiveFileRoot() {
        return Mockito.spy(ArchiveFileRoot.class);
    }

    @Bean
    public CaseSearchHelper caseSearchHelper() {
        return new CaseSearchHelper();
    }


    @Bean
    public CategoryTimingHelper categoryTimingHelper() {
        return Mockito.spy(CategoryTimingHelper.class);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(Constants.CONNECT_TIMEOUT))
                .setReadTimeout(Duration.ofMillis(Constants.READ_TIMEOUT)).build();
    }

    @Bean
    public StatsDClient datadogStatsDClient() {
        return Mockito.mock(StatsDClient.class);
    }

    @Bean
    public FormSendCalloutHandler formSendCalloutHandler() {
        return Mockito.mock(FormplayerFormSendCalloutHandler.class);
    }

    @Bean
    public MenuSessionRunnerService menuSessionRunnerService() {
        return Mockito.spy(MenuSessionRunnerService.class);
    }

    @Bean
    public FormSubmissionHelper formSubmissionHelper() {
        return Mockito.spy(FormSubmissionHelper.class);
    }

    @Bean
    public MenuSessionFactory menuSessionFactory() {
        return Mockito.spy(MenuSessionFactory.class);
    }

    @Bean
    public FormSessionFactory formSessionFactory() {
        return Mockito.spy(FormSessionFactory.class);
    }

    @Bean
    public CommCareSessionFactory commcareSessionFactory() {
        return Mockito.spy(CommCareSessionFactory.class);
    }
}
