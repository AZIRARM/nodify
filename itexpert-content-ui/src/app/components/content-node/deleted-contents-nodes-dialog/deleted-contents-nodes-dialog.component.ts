import {Component} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {MatDialog, MatDialogRef} from "@angular/material/dialog";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {TranslateService} from "@ngx-translate/core";
import {Router} from "@angular/router";
import {LoggerService} from "../../../services/LoggerService";
import {StatusEnum} from "../../../modeles/StatusEnum";
import {ContentNode} from "../../../modeles/ContentNode";
import {ContentNodeService} from "../../../services/ContentNodeService";
import {UserService} from "../../../services/UserService";
import {Env} from "../../../../assets/configurations/environment";

@Component({
  selector: 'app-deleted-contents-nodes-dialog',
  templateUrl: './deleted-contents-nodes-dialog.component.html',
  styleUrls: ['./deleted-contents-nodes-dialog.component.css']
})
export class DeletedContentsNodesDialogComponent {
  user:any;

  displayedColumns: string[] = ['Name', 'Version', 'Last Modification', 'Modified by', 'Actions'];

  dataSource: MatTableDataSource<ContentNode>;
  dialogValidationRef: MatDialogRef<ValidationDialogComponent>;

  constructor(
    public dialogRef: MatDialogRef<DeletedContentsNodesDialogComponent>,
    private translate: TranslateService,
    private contentNodeService: ContentNodeService,
    private userService: UserService,
    private loggerService: LoggerService,
    private router: Router,
    private dialog: MatDialog
  ) {
  }

  ngOnInit(): void {

    this.user = JSON.parse(
      JSON.parse(
        JSON.stringify((window.localStorage.getItem('userInfo')))
      )
    );
    this.init();
  }

  init() {
    this.contentNodeService.getDeleted().subscribe(
      (response: any) => {
        if (response) {
          response.map((content:any)=>this.setUserName(content));
          this.dataSource = new MatTableDataSource(response);
        }
      },
      error => {
        console.error(error);
      }
    );
  }


  activate(element: ContentNode) {

    this.dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "ACTIVATE_DELETE_CONTENT_NODE_TITLE",
        message: "ACTIVATE_DELETE_CONTENT_NODE_MESSAGE"
      },
      height: '80vh',
      width:  '80vw',
      disableClose: true
    });
    this.dialogValidationRef.afterClosed()
      .subscribe((result: any) => {
        if (result && result.data && result.data === "validated") {
          this.contentNodeService.activate(element.code, this.user.id).subscribe(() => {
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

  setUserName(param:any){
    this.userService.setUserName(param);
  }

  delete(element:ContentNode) {

    this.dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_CONTENT_NODE_TITLE",
        message: "DELETE_CONTENT_NODE_MESSAGE"
      },
      height: '80vh',
      width:  '80vw',
      disableClose: true
    });
    this.dialogValidationRef.afterClosed()
      .subscribe(result => {
        if (result && result.data !== 'canceled') {
          this.contentNodeService.deleteDefinitively(element.code).subscribe(
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
