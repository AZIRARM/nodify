import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from "@angular/material/dialog";
import {TranslateService} from "@ngx-translate/core";
import {NodeService} from "../../../services/NodeService";
import {LoggerService} from "../../../services/LoggerService";
import {StatusEnum} from "../../../modeles/StatusEnum";
import {Node} from "../../../modeles/Node";
import {MatTableDataSource} from "@angular/material/table";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {UserService} from "../../../services/UserService";
import {UserAccessService} from "../../../services/UserAccessService";

@Component({
  selector: 'app-published-nodes-dialog',
  templateUrl: './published-nodes-dialog.component.html',
  styleUrls: ['./published-nodes-dialog.component.css']
})
export class PublishedNodesDialogComponent implements OnInit {
  user: any;

  node: Node;

  displayedColumns: string[] = ['Status', 'Version', 'Last Modification', 'Modified by', 'Actions'];

  dataSource: MatTableDataSource<Node>;
  dialogRefPublish: MatDialogRef<ValidationDialogComponent>;

  constructor(
    public dialogRef: MatDialogRef<PublishedNodesDialogComponent>,
    public userAccessService: UserAccessService,
    @Inject(MAT_DIALOG_DATA) public content: Node,
    private translate: TranslateService,
    private nodeService: NodeService,
    private userService: UserService,
    private loggerService: LoggerService,
    private dialog: MatDialog
  ) {
    if (content) {
      this.node = content;
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
    this.dialogRef.close({data: this.node});
  }


  init() {
    this.nodeService.getAllNodesByCode(this.node.code).subscribe(
      (response: any) => {
        if (response) {
          let filtredNodes: Node[] =
            response.filter((element: Node) => (element.status === StatusEnum.ARCHIVE || element.status === StatusEnum.PUBLISHED));
          response.map((node: any) => this.setUserName(node));
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

  deploy(element: Node) {

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
          this.nodeService.deployVersion(element.code, element.version, this.user!.id).subscribe(() => {
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

  revert(element: Node) {
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
          this.nodeService.revertToVersion(element.code, element.version, this.user!.id).subscribe(() => {
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
