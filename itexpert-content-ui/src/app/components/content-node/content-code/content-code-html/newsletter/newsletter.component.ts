import { Component, Input, Output, signal, WritableSignal } from '@angular/core';
import { NewsletterContent } from 'src/app/modeles/NewsletterContent';

@Component({
  selector: 'app-newsletter',
  templateUrl: './newsletter.component.html',
  styleUrls: ['./newsletter.component.css'],
  standalone: false
})
export class NewsletterComponent {
  @Input() newsletterContent!: NewsletterContent;
  @Output() hasChanged: WritableSignal<boolean> = signal(false);

  ngOnInit(): void {
    if (this.newsletterContent?.newsLetterCampaignStartDate && typeof this.newsletterContent.newsLetterCampaignStartDate === 'number') {
      this.newsletterContent.newsLetterCampaignStartDate = this.formatDateForDatetimeLocal(new Date(this.newsletterContent.newsLetterCampaignStartDate)) as any;
    }
    if (this.newsletterContent?.newsLetterCampaignEndDate && typeof this.newsletterContent.newsLetterCampaignEndDate === 'number') {
      this.newsletterContent.newsLetterCampaignEndDate = this.formatDateForDatetimeLocal(new Date(this.newsletterContent.newsLetterCampaignEndDate)) as any;
    }
    if (this.newsletterContent?.newsLetterCampaignCodeScheduleDate && typeof this.newsletterContent.newsLetterCampaignCodeScheduleDate === 'number') {
      this.newsletterContent.newsLetterCampaignCodeScheduleDate = this.formatDateForDatetimeLocal(new Date(this.newsletterContent.newsLetterCampaignCodeScheduleDate)) as any;
    }
  }

  formatDateForDatetimeLocal(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  getRecipientsString(): string {
    if (!this.newsletterContent?.newsLetterCampaignRecipients) {
      return '';
    }
    return this.newsletterContent.newsLetterCampaignRecipients.join(', ');
  }

  updateRecipients(event: any): void {
    const value = event.target.value;
    if (value) {
      this.newsletterContent.newsLetterCampaignRecipients = value.split(',').map((s: string) => s.trim());
    } else {
      this.newsletterContent.newsLetterCampaignRecipients = [];
    }
  }

  onContentChange(): void {
    this.hasChanged.set(true);
  }
}