package com.itexpert.content.lib.models;

import java.util.List;

import lombok.Data;

@Data
public class NewsletterContent {
    private String newsLetterCode;
    private String newsLetterTitle;
    private String newsLetterSubject;
    private String newsLetterCampaignCode;
    private List<String> newsLetterCampaignRecipients;
    private String newsLetterCampaignStartDate;
    private String newsLetterCampaignEndDate;
    private String newsLetterCampaignCodeScheduleDate;
    private String newsLetterTriggerUrl;
    private String newsLetterTriggerSecret;
}
