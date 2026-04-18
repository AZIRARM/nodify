import { Component, Input, inject, signal, WritableSignal } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { ContentNode } from "../../../../modeles/ContentNode";
import { ContentUrl } from "../../../../modeles/ContentUrl";

@Component({
  selector: 'app-content-code-urls',
  templateUrl: './content-code-urls.component.html',
  styleUrl: './content-code-urls.component.css',
  standalone: false
})
export class ContentCodeUrlsComponent {

  @Input() contentNode: ContentNode;
  @Input() dialogRef: MatDialogRef<any>;

  addUrl() {
    if (!this.contentNode.urls) {
      this.contentNode.urls = [];
    }

    let url: ContentUrl = new ContentUrl();
    url.url = "";
    url.type = "";
    url.description = "";
    this.contentNode.urls.push(url);
  }

  deleteUrl(url: ContentUrl) {
    if (url) {
      this.contentNode.urls = this.contentNode.urls.filter((v: ContentUrl) =>
        v.url !== url.url || v.type !== url.type || v.description !== url.description
      );
    }
  }
}