import {AfterViewInit, Component, Input, Output, ViewChild} from '@angular/core';
import {ContentNode} from "../../../../modeles/ContentNode";
import {ContentNodeService} from "../../../../services/ContentNodeService";
import {StatusEnum} from "../../../../modeles/StatusEnum";
import {CodemirrorComponent} from "@ctrl/ngx-codemirror";

@Component({
  selector: 'app-content-code-html',
  templateUrl: './content-code-html.component.html',
  styleUrl: './content-code-html.component.css'
})
export class ContentCodeHtmlComponent implements AfterViewInit {

  @Output()
  @Input()
  contentNode!: ContentNode;
  contentFilled!: string;

  code: boolean = true;

  constructor(private contentService: ContentNodeService) {

  }

  @ViewChild(CodemirrorComponent) codeMirrorComponent!: CodemirrorComponent;

  ngAfterViewInit(): void {
    this.toHtml(this.contentNode.content);
    setTimeout(() => {
      if (this.codeMirrorComponent?.codeMirror) {
        this.codeMirrorComponent.codeMirror.refresh();
      }
    }, 300);
  }

  setCodeEdition(codeEdition: boolean) {
    this.code = codeEdition;
  }

  toHtml(content: string) {
    if (content) {
      this.contentService.fillAllValuesByContentCodeStatusAndContent(
        {
          code: this.contentNode.code,
          status: StatusEnum.SNAPSHOT,
          content: content
        }).subscribe(
        (response: any) => {
          this.contentFilled = response.content;
        },
        (error) => {                              //error() callback
          console.error('Request failed with error');
        });
    }
  }
}
