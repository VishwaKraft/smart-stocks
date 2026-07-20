package com.smartstocks.product.service.impl;

import com.smartstocks.product.models.*;
import com.smartstocks.product.repository.*;
import com.smartstocks.product.service.CampaignEventLogger;
import com.smartstocks.product.service.ICampaignService;
import com.smartstocks.product.service.renderer.DefaultRenderer;
import com.smartstocks.product.service.renderer.ITemplateRenderer;
import com.smartstocks.product.service.renderer.RenderedTemplate;
import com.smartstocks.product.service.renderer.TemplateRendererFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CampaignActivityServiceImplTest {

    @Mock
    private CampaignActivityRepository activityRepository;
    @Mock
    private CampaignActivityWeekdayRepository weekdayRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private VoiceTemplateRepository voiceTemplateRepository;
    @Mock
    private SegmentRepository segmentRepository;
    @Mock
    private SegmentUserRepository segmentUserRepository;
    @Mock
    private CampaignSegmentUserRepository campaignSegmentUserRepository;
    @Mock
    private CampaignEventLogger eventLogger;
    @Mock
    private ICampaignService campaignService;
    @Mock
    private TemplateRendererFactory templateRendererFactory;
    
    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ITemplateRenderer templateRenderer = new DefaultRenderer();

    @InjectMocks
    private CampaignActivityServiceImpl campaignActivityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(campaignActivityService, "restTemplate", restTemplate);
    }

    @Test
    void testTrigger_FetchesDataFromApi() {
        // Arrange
        Long activityId = 1L;
        List<String> emailIds = Arrays.asList("test@example.com");

        Campaign campaign = new Campaign();
        campaign.setId(1L);
        campaign.setCampaignCode("CAMP-123");
        campaign.setGoogleAccessToken("fake_token");
        // We use GMAIL provider and let it fail on auth/send, since we only want to test the renderer setup
        campaign.setEmailProviderType(EmailProviderType.GMAIL);

        Template template = new Template();
        template.setId(1L);
        template.setSubject("Test Subject");
        template.setHtmlBody("Articles: {{articles}}");
        template.setRendererType(RendererType.DEFAULT);
        template.setDataSourceUrl("https://fake-api.com/data");

        CampaignActivity activity = new CampaignActivity();
        activity.setId(activityId);
        activity.setCampaign(campaign);
        activity.setTemplate(template);

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(templateRendererFactory.get(RendererType.DEFAULT)).thenReturn(templateRenderer);
        when(campaignService.injectTrackingPixel(anyString(), anyString(), anyString(), anyLong(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0) + "<img src='pixel'/>");
        
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("data", Arrays.asList(Collections.singletonMap("title", "Mock Title")));
        when(restTemplate.getForObject(eq("https://fake-api.com/data"), eq(Map.class))).thenReturn(mockResponse);



        // Act
        try {
            campaignActivityService.testTrigger(activityId, emailIds);
        } catch (Exception e) {
            // Ignore exception if it fails downstream (like email provider), we just care about renderer variables
        }

        // Assert
        verify(restTemplate, times(1)).getForObject("https://fake-api.com/data", Map.class);
        
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(templateRenderer, times(1)).render(anyString(), anyString(), variablesCaptor.capture());
        
        Map<String, Object> capturedVars = variablesCaptor.getValue();
        
        // Let's verify the loop rendering by calling render manually with the captured variables
        RenderedTemplate testRender = templateRenderer.render(
            "Subject",
            template.getHtmlBody(),
            capturedVars
        );
        
        String outputHtml = testRender.getRenderedBody();
        // With DefaultRenderer, String.valueOf on the array will result in something like [{title=Mock Title}]
        assertTrue(outputHtml.contains("Mock Title"), "The simple regex replacement should include the article title from toString().");
    }
}
