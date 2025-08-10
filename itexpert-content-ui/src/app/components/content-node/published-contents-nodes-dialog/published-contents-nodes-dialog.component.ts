import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from "@angular/material/dialog";
import {TranslateService} from "@ngx-translate/core";
import {LoggerService} from "../../../services/LoggerService";
import {StatusEnum} from "../../../modeles/StatusEnum";
import {Node} from "../../../modeles/Node";
import {MatTableDataSource} from "@angular/material/table";
import {ContentNode} from "../../../modeles/ContentNode";
import {ContentNodeService} from "../../../services/ContentNodeService";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {UserService} from "../../../services/UserService";
import {UserAccessService} from "../../../services/UserAccessService";

@Component({
  selector: 'app-published-contents-nodes-dialog',
  templateUrl: './published-contents-nodes-dialog.component.html',
  styleUrls: ['./published-contents-nodes-dialog.component.css']
})
export class PublishedContentsNodesDialogComponent implements OnInit {

  user: any;

  contentNode: ContentNode;

  displayedColumns: string[] = ['Status', 'Version', 'Last Modification', 'Modified by', 'Actions'];

  dataSource: MatTableDataSource<ContentNode>;

  dialogRefPublish: MatDialogRef<ValidationDialogComponent>;

  constructor(
    public dialogRef: MatDialogRef<PublishedContentsNodesDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public content: ContentNode,
    private translate: TranslateService,
    private contentNodeService: ContentNodeService,
    private userService: UserService,
    private userAccessService: UserAccessService,
    private loggerService: LoggerService,
    private dialog: MatDialog
  ) {
    if (content) {
      this.contentNode = content;
    }
  }

  ngOnInit() {
   this.userAccessService.user$.subscribe((user: any) => {
      this.user = user;
    });
    this.init();
  }


  cancel() {
    this.dialogRef.close();
  }

  validate() {
    this.dialogRef.close({data: this.contentNode});
  }


  init() {
    this.contentNodeService.getAllByCode(this.contentNode.code).subscribe(
      (response: any) => {
        if (response) {
          let filtredNodes: ContentNode[] = response.filter((element: ContentNode) => (element.status === StatusEnum.ARCHIVE || element.status === StatusEnum.PUBLISHED));
          filtredNodes.map((content: any) => this.setUserName(content));
          this.dataSource = new MatTableDataSource(filtredNodes);
        }
      },
      error => {
        console.error(error);
      }
    );
  }


  isPublished(element: Node) {
    if (element)
      return element.status === StatusEnum.PUBLISHED;
    return false
  }

  isArchived(element: Node) {
    if (element)
      return element.status === StatusEnum.ARCHIVE;
    return false
  }

  isSnapshot(element: Node) {
    if (element)
      return element.status === StatusEnum.SNAPSHOT;
    return false
  }

  deploy(element: ContentNode) {

    this.dialogRefPublish = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DEPLOY_VERSION_TITLE",
        message: "DEPLOY_VERSION_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefPublish.afterClosed()
      .subscribe((result: any) => {
        if (result && result.data && result.data === "validated") {
          this.contentNodeService.deployVersion(element.code, element.version, this.user!.id).subscribe(() => {
            this.translate.get("SAVE_SUCCESS").subscribe(trad => {
              this.loggerService.success(trad);
              this.init();
            });
          }, (error) => {
            this.translate.get("SAVE_ERROR").subscribe(trad => {
              this.loggerService.error(trad);
            });
          });
        }
      });
  }

  revert(element: ContentNode) {
    this.dialogRefPublish = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "REVERT_VERSION_TITLE",
        message: "REVERT_VERSION_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefPublish.afterClosed()
      .subscribe((result: any) => {
        if (result && result.data && result.data === "validated") {
          this.contentNodeService.revertToVersion(element.code, element.version, this.user!.id).subscribe(() => {
            this.translate.get("SAVE_SUCCESS").subscribe(trad => {
              this.loggerService.success(trad);
              this.init();
            });
          }, (error) => {
            this.translate.get("SAVE_ERROR").subscribe(trad => {
              this.loggerService.error(trad);
            });
          });
        }
      });
  }

  setUserName(param: any) {
    this.userService.setUserName(param);
  }
}
