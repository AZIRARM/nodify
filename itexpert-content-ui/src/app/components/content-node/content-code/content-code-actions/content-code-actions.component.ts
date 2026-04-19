import { AfterViewInit, Component, EventEmitter, Input, Output, inject, signal, WritableSignal } from '@angular/core';
import { TranslateService } from "@ngx-translate/core";
import { ContentNode } from "../../../../modeles/ContentNode";
import { StatusEnum } from "../../../../modeles/StatusEnum";
import { LoggerService } from "../../../../services/LoggerService";
import { ContentNodeService } from "../../../../services/ContentNodeService";
import { SlugService } from 'src/app/services/SlugService';
import { tap } from 'rxjs/operators';
import { EMPTY, of } from 'rxjs';

@Component({
  selector: 'app-content-code-actions',
  templateUrl: './content-code-actions.component.html',
  styleUrl: './content-code-actions.component.css',
  standalone: false
})
export class ContentCodeActionsComponent implements AfterViewInit {

  @Input() contentNode: ContentNode;
  @Output() close = new EventEmitter<void>();
  @Output() validate = new EventEmitter<void>();

  contentModels: WritableSignal<any> = signal(null);
  canSave: WritableSignal<boolean> = signal(true);

  private translate = inject(TranslateService);
  private logger = inject(LoggerService);
  private contentNodeService = inject(ContentNodeService);
  private slugService = inject(SlugService);

  ngAfterViewInit(): void {
    this.initContentNodeModels();
  }

  closeFactory(): void {
    this.close.emit();
  }

  validateFactory(): void {
    this.slugService.exists(this.contentNode.slug)
      .subscribe((codes: any) => {
        const filtered = codes.filter((c: string) => c != null);
        const available = filtered.length === 0 ||
          (filtered.length === 1 && filtered[0] === this.contentNode.code);

        if (available) {
          this.validate.emit();
        } else {
          this.translate.get('SLUG_ALREADY_USED').pipe(
            tap(translation => this.logger.error(translation))
          ).subscribe();
        }
      });
  }

  initContentNodeModels() {
    this.contentNodeService.getAllByParentCodeAndStatus(this.contentNode.parentCode, StatusEnum.SNAPSHOT).subscribe(
      (response: any) => {
        this.contentModels.set(response.filter((content: ContentNode) =>
          this.contentNode.code !== content.code && content.type === this.contentNode.type));
      },
      (error) => {
        this.logger.error('Request failed with error');
      });
  }
}