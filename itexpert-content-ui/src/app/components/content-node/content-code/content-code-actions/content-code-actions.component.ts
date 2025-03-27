import {AfterViewInit, Component, EventEmitter, Input, Output} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {ContentNode} from "../../../../modeles/ContentNode";
import {StatusEnum} from "../../../../modeles/StatusEnum";
import {LoggerService} from "../../../../services/LoggerService";
import {ContentNodeService} from "../../../../services/ContentNodeService";

@Component({
  selector: 'app-content-code-actions',
  templateUrl: './content-code-actions.component.html',
  styleUrl: './content-code-actions.component.css'
})
export class ContentCodeActionsComponent implements AfterViewInit {
  @Output()
  @Input()
  contentNode: ContentNode;

  contentModels: any;

  constructor(
    private translate: TranslateService,
    private logger: LoggerService,
    private contentNodeService: ContentNodeService) {
  }

  ngAfterViewInit(): void {
    this.initContentNodeModels();
  }

  @Output() close = new EventEmitter<void>();
  @Output() validate = new EventEmitter<void>();

  closeFactory(): void {
    this.close.next();
  }

  validateFactory(): void {
    this.validate.next();
  }

  initContentNodeModels() {
    this.contentNodeService.getAllByParentCodeAndStatus(this.contentNode.parentCode, StatusEnum.SNAPSHOT).subscribe(
      (response: any) => {
        this.contentModels = response.filter((content: ContentNode) =>
          this.contentNode.code !== content.code && content.type === this.contentNode.type);
      },
      (error) => {                              //error() callback
        this.logger.error('Request failed with error');
      });
  }
}
