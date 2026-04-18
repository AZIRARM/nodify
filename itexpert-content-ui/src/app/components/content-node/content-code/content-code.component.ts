import { Component, Inject, Input, OnDestroy, OnInit, Output, inject, signal, WritableSignal } from '@angular/core';
import { Node } from "../../../modeles/Node";
import { ContentNode } from "../../../modeles/ContentNode";
import { User } from "../../../modeles/User";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { TranslateService } from "@ngx-translate/core";
import { LoggerService } from "../../../services/LoggerService";
import { ContentNodeService } from "../../../services/ContentNodeService";
import { ContentFile } from "../../../modeles/ContentFile";
import { LockService } from 'src/app/services/LockService';
import { interval, Subscription } from 'rxjs';
import { AuthenticationService } from "../../../services/AuthenticationService";
import { switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-code-dialog',
  templateUrl: './content-code.component.html',
  styleUrls: ['./content-code.component.css'],
  standalone: false
})
export class ContentCodeComponent implements OnInit, OnDestroy {
  @Input() @Output() node: Node;
  @Input() @Output() contentNode: ContentNode;
  @Input() @Output() type: string;
  @Input() @Output() user: User;
  @Input() @Output() hasChanged: WritableSignal<boolean> = signal(false);
  isLoading: WritableSignal<boolean> = signal(false);

  private lockCheckSub?: Subscription;

  private translateService = inject(TranslateService);
  private loggerService = inject(LoggerService);
  private contentNodeService = inject(ContentNodeService);
  private authenticationService = inject(AuthenticationService);
  private lockService = inject(LockService);
  public dialogRef = inject(MatDialogRef<ContentCodeComponent>);
  private data = inject(MAT_DIALOG_DATA);

  constructor() {
    if (this.data) {
      this.node = this.data.node;
      this.contentNode = this.data.contentNode;
      this.type = this.data.type;
      this.user = this.data.user;
    }
  }

  ngOnInit(): void {
    if (this.contentNode && this.contentNode.code) {
      this.lockService.acquire(this.contentNode.code).subscribe(acquired => {
        if (!acquired) {
          this.translateService.get("RESOURCE_LOCKED")
            .subscribe(translation => {
              this.loggerService.warn(translation);
            });
          this.dialogRef.close();
        } else {
          this.lockService.startInactivityWatcher(30 * 60 * 1000, () => {
            this.translateService.get("RESOURCE_RELEASED")
              .subscribe(translation => {
                this.loggerService.warn(translation);
              });
            this.dialogRef.close();
          });

          this.lockService.getLockInfoSocket(this.contentNode.code, this.authenticationService.getAccessToken()).subscribe((lockInfo: any) => {
            if (lockInfo.locked) {
              this.translateService.get("RESOURCE_LOCKED_BY_OTHER")
                .subscribe(translation => this.loggerService.warn(translation));
              this.dialogRef.close();
            }
          });
        }
      });
    }
  }

  ngOnDestroy(): void {
    if (this.lockCheckSub) {
      this.lockCheckSub.unsubscribe();
    }
    this.lockService.release();
  }

  close(refresh: boolean): void {
    this.dialogRef.close({ refresh: refresh });
  }

  validate() {
    this.isLoading.set(true);
    this.contentNode.modifiedBy = this.user!.id;
    this.contentNode.parentCode = this.node.code;
    this.contentNode.parentCodeOrigin = this.node.parentCodeOrigin;

    this.contentNodeService.save(this.contentNode).pipe(
      switchMap((response: any) => {
        this.contentNode = response;
        return this.translateService.get("SAVE_SUCCESS");
      })
    ).subscribe({
      next: (trad: string) => {
        this.loggerService.success(trad);
        this.isLoading.set(false);
        this.close(true);
      },
      error: () => {
        this.translateService.get("SAVE_ERROR").subscribe(trad1 => {
          this.translateService.get("CHANGE_PROJECT_CODE_MESSAGE").subscribe(trad2 => {
            this.loggerService.error(trad1 + ",  " + trad2);
            this.isLoading.set(false);
          });
        });
      }
    });
  }

  onFileChange(event: any) {
    let file = event.target.files[0];
    let reader = new FileReader();

    reader.onload = () => {
      let base64Content: string = reader.result as string;

      this.contentNode.file = new ContentFile();
      this.contentNode.file.data = base64Content;
      this.contentNode.file.name = file.name;
      this.contentNode.file.type = file.type;
      this.contentNode.file.size = file.size;
    }

    if (file) {
      reader.readAsDataURL(file);
    }
  }
}