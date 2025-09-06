import {Component, Inject, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {Node} from "../../../modeles/Node";
import {ContentNode} from "../../../modeles/ContentNode";
import {User} from "../../../modeles/User";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {TranslateService} from "@ngx-translate/core";
import {LoggerService} from "../../../services/LoggerService";
import {ContentNodeService} from "../../../services/ContentNodeService";
import {ContentFile} from "../../../modeles/ContentFile";
import { LockService } from 'src/app/services/LockService';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-code-dialog',
  templateUrl: './content-code.component.html',
  styleUrls: ['./content-code.component.css']
})
export class ContentCodeComponent implements OnInit, OnDestroy {
  @Input() @Output() node: Node;
  @Input() @Output() contentNode: ContentNode;
  @Input() @Output() type: string;
  @Input() @Output() user: User;
  @Input() @Output() hasChanged: boolean = false;
      
  private lockCheckSub: Subscription;

  constructor(
    private translateService: TranslateService,
    private loggerService: LoggerService,
    private contentNodeService: ContentNodeService,
    @Inject(MAT_DIALOG_DATA) public data: any,
    public dialogRef: MatDialogRef<ContentCodeComponent>,
    private lockService: LockService
  ) {
    if (data) {
      this.node = data.node;
      this.contentNode = data.contentNode;
      this.type = data.type;
      this.user = data.user;
    }
  }

  
  ngOnInit(): void {

    // 🔒 Tente d’acquérir le lock en entrant dans l’édition
    this.lockService.acquire(this.contentNode.code).subscribe(acquired => {
      if (!acquired) {
         this.translateService.get("RESOURCE_LOCKED")
            .subscribe(translation => {
              this.loggerService.warn(translation);
            });
        this.dialogRef.close();
      } else {
        // Si acquis → démarre la surveillance d’inactivité à 30 min
        this.lockService.startInactivityWatcher(30 * 60 * 1000, () => {
         this.translateService.get("RESOURCE_RELEASED")
            .subscribe(translation => {
              this.loggerService.warn(translation);
            });
          this.dialogRef.close();
        });
        
                
                
        // 🔄 Vérifie le lock toutes les 10s
        this.lockCheckSub = interval(10000).subscribe(() => {
          this.lockService.getLockInfo(this.contentNode.code).subscribe((lockInfo:any) => {
            if (lockInfo.locked) {
              this.translateService.get("RESOURCE_LOCKED_BY_OTHER")
                .subscribe(translation => this.loggerService.warn(translation));
              this.dialogRef.close();
            }
          });
        });

      }
    });
  }


  ngOnDestroy(): void {
    if (this.lockCheckSub) {
      this.lockCheckSub.unsubscribe();
    }
    this.lockService.release();
   }

  close(refesh: boolean): void {
    this.dialogRef.close({refresh: refesh});
  }

  validate() {
    this.contentNode.modifiedBy = this.user!.id;
    this.contentNode.parentCode = this.node.code;
    this.contentNode.parentCodeOrigin = this.node.parentCodeOrigin;

    this.contentNodeService.save(this.contentNode).subscribe(
      (response: any) => {

        this.contentNode = response;
        this.translateService.get("SAVE_SUCCESS").subscribe(trad => {
          this.loggerService.success(trad);
          this.close(true);
        })

      },
      error => {
        this.translateService.get("SAVE_ERROR").subscribe(trad1 => {
          this.translateService.get("CHANGE_PROJECT_CODE_MESSAGE").subscribe(trad2 => {
            this.loggerService.error(trad1 + ",  " + trad2);
          })
        })
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
