import {Component, Inject, OnInit} from '@angular/core';
import {Node} from "../../../modeles/Node";
import {MatTableDataSource} from "@angular/material/table";
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from "@angular/material/dialog";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {TranslateService} from "@ngx-translate/core";
import {NodeService} from "../../../services/NodeService";
import {LoggerService} from "../../../services/LoggerService";
import {UserService} from "../../../services/UserService";
import {UserAccessService} from "../../../services/UserAccessService";

@Component({
  selector: 'app-deleted-nodes-dialog',
  templateUrl: './deleted-nodes-dialog.component.html',
  styleUrls: ['./deleted-nodes-dialog.component.css']
})
export class DeletedNodesDialogComponent implements OnInit {

  user: any;

  displayedColumns: string[] = ['Name', 'Version', 'Last Modification', 'Modified by', 'Actions'];

  dataSource: MatTableDataSource<Node>;
  dialogValidationRef: MatDialogRef<ValidationDialogComponent>;

  constructor(
    public dialogRef: MatDialogRef<DeletedNodesDialogComponent>,
    private translate: TranslateService,
    private nodeService: NodeService,
    private userService: UserService,
    private userAccessService: UserAccessService,
    private loggerService: LoggerService,
    @Inject(MAT_DIALOG_DATA) private parentNode: Node,
    private dialog: MatDialog
  ) {
  }

  ngOnInit() {
   this.userAccessService.user$.subscribe((user: any) => {
      this.user = user;
    });
    this.init();
  }

  init() {
    this.nodeService.getDeleted(this.parentNode?.code ?? null).subscribe(
      (response: any) => {
        if (response) {
          response.map((param: any) => this.setUserName(param));
          this.dataSource = new MatTableDataSource(response);
        }
      },
      error => {
        console.error(error);
      }
    );
  }


  activate(element: Node) {

    this.dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "ACTIVATE_DELETE_NODE_TITLE",
        message: "ACTIVATE_DELETE_NODE_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogValidationRef.afterClosed()
      .subscribe((result: any) => {
        if (result && result.data && result.data === "validated") {
          this.nodeService.activate(element.code, this.user!.id).subscribe(() => {
            this.translate.get("ACTIVATION_SUCCESS").subscribe(trad => {
              this.loggerService.success(trad);
              this.init();
            });
          }, (error) => {
            this.translate.get("ACTIVATION_ERROR").subscribe(trad => {
              this.loggerService.error(trad);
            });
          });
        }
      });
  }

  setUserName(param: any) {
    this.userService.setUserName(param);
  }

  delete(element: Node) {
    this.dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_NODE_TITLE",
        message: "DELETE_NODE_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogValidationRef.afterClosed()
      .subscribe(result => {
        if (result && result.data !== 'canceled') {
          this.nodeService.deleteDefinitively(element.code).subscribe(
            response => {
              this.translate.get("DELETE_SUCCESS").subscribe(trad => {
                this.loggerService.success(trad);
                this.init();
              })

            },
            error => {
              this.translate.get("DELETE_ERROR").subscribe(trad => {
                this.loggerService.error(trad);
              })
            });
        }
      });
  }

  close() {
    this.dialogRef.close();
  }

}
