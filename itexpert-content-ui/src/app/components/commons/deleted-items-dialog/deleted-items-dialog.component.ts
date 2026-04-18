// deleted-items-dialog.component.ts
import {Component, Inject, OnInit} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from "@angular/material/dialog";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {TranslateService} from "@ngx-translate/core";
import {LoggerService} from "../../../services/LoggerService";
import {UserService} from "../../../services/UserService";
import {UserAccessService} from "../../../services/UserAccessService";
import {Observable} from "rxjs";

// Interface pour les services de suppression
export interface DeleteService {
  getDeleted(parentCode: string | null): Observable<any>;
  activate(code: string): Observable<any>;
  deleteDefinitively(code: string): Observable<any>;
}

// Interface pour les éléments supprimés
export interface DeletableItem {
  code: string;
  name: string;
  version: string;
  modificationDate: string;
  modifiedBy: string;
  type?: string;
  IsGranted?: boolean;
}

@Component({
    selector: 'app-deleted-items-dialog',
    templateUrl: './deleted-items-dialog.component.html',
    styleUrls: ['./deleted-items-dialog.component.css'],
    standalone: false
})
export class DeletedItemsDialogComponent implements OnInit {

  // Propriétés
  titleKey: string = 'DELETED_NODES';
  icon: string = 'delete_sweep';
  displayTypeColumn: boolean = true;

  user: any;
  parentNode: any;
  deleteService: DeleteService;
  displayedColumns: string[] = ['Name', 'Version', 'Last Modification', 'Modified by'];
  dataSource: MatTableDataSource<DeletableItem> = new MatTableDataSource<DeletableItem>([]);
  dialogValidationRef: MatDialogRef<ValidationDialogComponent>;

  constructor(
    public dialogRef: MatDialogRef<DeletedItemsDialogComponent>,
    private translate: TranslateService,
    private userService: UserService,
    private userAccessService: UserAccessService,
    private loggerService: LoggerService,
    @Inject(MAT_DIALOG_DATA) private data: any,
    private dialog: MatDialog
  ) {
    // Récupérer toutes les données
    this.parentNode = data?.parentNode;
    this.deleteService = data?.deleteService; // Récupérer le service

    // Surcharger les propriétés si fournies
    if (data?.titleKey) this.titleKey = data.titleKey;
    if (data?.icon) this.icon = data.icon;
    if (data?.displayTypeColumn !== undefined) this.displayTypeColumn = data.displayTypeColumn;
  }

  ngOnInit() {
    this.user = this.userAccessService.getCurrentUser();

    // Ajouter la colonne Type si nécessaire
    if (this.displayTypeColumn && !this.displayedColumns.includes('Type')) {
      this.displayedColumns.splice(2, 0, 'Type');
    }

    // Ajouter la colonne Actions
    this.displayedColumns.push('Actions');

    this.init();
  }

  init() {
    if (!this.deleteService) {
      console.error('DeleteService non fourni');
      return;
    }

    const parentCode = this.parentNode?.code ?? null;
    this.deleteService.getDeleted(parentCode).subscribe(
      (response: any) => {
        if (response) {
          this.dataSource = new MatTableDataSource(response);
        }
      },
      (error: any) => {
        console.error(error);
      }
    );
  }

  activate(element: DeletableItem) {
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
          this.deleteService.activate(element.code).subscribe(() => {
            this.translate.get("ACTIVATION_SUCCESS").subscribe((trad: string) => {
              this.loggerService.success(trad);
              this.init();
            });
          }, (error: any) => {
            this.translate.get("ACTIVATION_ERROR").subscribe((trad: string) => {
              this.loggerService.error(trad);
            });
          });
        }
      });
  }

  delete(element: DeletableItem) {
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
      .subscribe((result: any) => {
        if (result && result.data !== 'canceled') {
          this.deleteService.deleteDefinitively(element.code).subscribe(
            (response: any) => {
              this.translate.get("DELETE_SUCCESS").subscribe((trad: string) => {
                this.loggerService.success(trad);
                this.init();
              });
            },
            (error: any) => {
              this.translate.get("DELETE_ERROR").subscribe((trad: string) => {
                this.loggerService.error(trad);
              });
            });
        }
      });
  }

  close() {
    this.dialogRef.close();
  }
}
