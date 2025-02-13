import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";
import {ContentUrl} from "../../../../modeles/ContentUrl";

@Component({
  selector: 'app-content-code-urls',
  templateUrl: './content-code-urls.component.html',
  styleUrl: './content-code-urls.component.css'
})
export class ContentCodeUrlsComponent {

  @Input()
  currentContent: ContentNode;

  addUrl() {
    if (!this.currentContent.urls)
      this.currentContent.urls = [];

    let url: ContentUrl = new ContentUrl();
    url.url = "";
    url.type = "";
    url.description = "";
    this.currentContent.urls.push(url);

  }

  deleteUrl(url: ContentUrl) {

    if (url) {
      this.currentContent.urls = this.currentContent.urls.filter((v: ContentUrl) => (v.url !== url.url && v.type !== url.type && v.description !== url.description));
    }
  }


  @Output() close = new EventEmitter<void>();
  @Output() validate = new EventEmitter<void>();


  closeFactory(): void {
    this.close.next();
  }

  validateFactory(): void {
    this.validate.next();
  }
}
