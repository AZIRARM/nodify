import { AfterViewInit, Component, Input, Output, ViewChild, signal, WritableSignal, inject } from '@angular/core';
import { ContentNode } from "../../../../modeles/ContentNode";
import { ContentNodeService } from "../../../../services/ContentNodeService";
import { StatusEnum } from "../../../../modeles/StatusEnum";
import { CodemirrorComponent } from "@ctrl/ngx-codemirror";
import { MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-content-code-html',
  templateUrl: './content-code-html.component.html',
  styleUrl: './content-code-html.component.css',
  standalone: false
})
export class ContentCodeHtmlComponent implements AfterViewInit {

  @Output()
  @Input()
  contentNode!: ContentNode;

  @Input()
  @Output()
  dialogRef: MatDialogRef<any>;

  contentFilled!: string;
  code: WritableSignal<boolean> = signal(true);
  isFullscreen: WritableSignal<boolean> = signal(false);
  isLoading: WritableSignal<boolean> = signal(false);

  private contentService = inject(ContentNodeService);

  @ViewChild(CodemirrorComponent) codeMirrorComponent!: CodemirrorComponent;

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (this.codeMirrorComponent?.codeMirror) {
        this.codeMirrorComponent.codeMirror.refresh();
      }
    }, 300);
  }

  setCodeEdition(codeEdition: boolean) {
    this.code.set(codeEdition);
    if (!codeEdition && this.contentNode?.content) {
      this.toHtml(this.contentNode.content);
    }
  }

  toHtml(content: string) {
    if (content) {
      this.isLoading.set(true);
      this.contentService.fillAllValuesByContentCodeStatusAndContent(
        {
          code: this.contentNode.code,
          status: StatusEnum.SNAPSHOT,
          content: content
        }).subscribe(
          (response: any) => {
            this.contentFilled = response.content;
            this.isLoading.set(false);
          },
          (error) => {
            console.error('Request failed with error');
            this.isLoading.set(false);
          });
    }
  }

  toggleFullscreen() {
    this.isFullscreen.set(!this.isFullscreen());

    setTimeout(() => {
      if (this.codeMirrorComponent?.codeMirror) {
        this.codeMirrorComponent.codeMirror.refresh();
      }
    }, 50);
  }
}