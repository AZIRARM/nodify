import {Component, Inject, Input, Output} from '@angular/core';
import {Node} from "../../../modeles/Node";
import {ContentNode} from "../../../modeles/ContentNode";
import {User} from "../../../modeles/User";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {TranslateService} from "@ngx-translate/core";
import {LoggerService} from "../../../services/LoggerService";
import {ContentNodeService} from "../../../services/ContentNodeService";
import {ContentFile} from "../../../modeles/ContentFile";

@Component({
  selector: 'app-code-dialog',
  templateUrl: './content-code.component.html',
  styleUrls: ['./content-code.component.css']
})
export class ContentCodeComponent {
  @Input() @Output() node: Node;
  @Input() @Output() contentNode: ContentNode;
  @Input() @Output() type: string;
  @Input() @Output() user: User;
  @Input() @Output() hasChanged: boolean = false;

  constructor(
    private translate: TranslateService,
    private loggerService: LoggerService,
    private contentNodeService: ContentNodeService,
    @Inject(MAT_DIALOG_DATA) public data: any,
    public dialogRef: MatDialogRef<ContentCodeComponent>,
  ) {
    if (data) {
      this.node = data.node;
      this.contentNode = data.contentNode;
      this.type = data.type;
      this.user = data.user;
    }
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
        this.translate.get("SAVE_SUCCESS").subscribe(trad => {
          this.loggerService.success(trad);
          this.close(true);
        })

      },
      error => {
        this.translate.get("SAVE_ERROR").subscribe(trad1 => {
          this.translate.get("CHANGE_PROJECT_CODE_MESSAGE").subscribe(trad2 => {
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
