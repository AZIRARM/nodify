import { Component, Inject, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { MatTableDataSource } from "@angular/material/table";
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from "@angular/material/dialog";
import { ValidationDialogComponent } from "../../commons/validation-dialog/validation-dialog.component";
import { TranslateService } from "@ngx-translate/core";
import { LoggerService } from "../../../services/LoggerService";
import { UserService } from "../../../services/UserService";
import { UserAccessService } from "../../../services/UserAccessService";
import { Observable, of } from "rxjs";
import { catchError, finalize, switchMap } from "rxjs/operators";

export interface DeleteService {
  getDeleted(parentCode: string | null): Observable<any>;
  activate(code: string): Observable<any>;
  deleteDefinitively(code: string): Observable<any>;
}

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

  titleKey: string = 'DELETED_NODES';
  icon: string = 'delete_sweep';
  displayTypeColumn: boolean = true;

  user: WritableSignal<any> = signal(null);
  parentNode: any;
  deleteService: DeleteService;
  displayedColumns: string[] = ['Name', 'Version', 'Last Modification', 'Modified by'];
  dataSource: WritableSignal<MatTableDataSource<DeletableItem>> = signal(new MatTableDataSource<DeletableItem>([]));

  private dialogRef = inject(MatDialogRef<DeletedItemsDialogComponent>);
  private translate = inject(TranslateService);
  private userService = inject(UserService);
  private userAccessService = inject(UserAccessService);
  private loggerService = inject(LoggerService);
  private dialog = inject(MatDialog);
  private data = inject(MAT_DIALOG_DATA);

  constructor() {
    this.parentNode = this.data?.parentNode;
    this.deleteService = this.data?.deleteService;

    if (this.data?.titleKey) this.titleKey = this.data.titleKey;
    if (this.data?.icon) this.icon = this.data.icon;
    if (this.data?.displayTypeColumn !== undefined) this.displayTypeColumn = this.data.displayTypeColumn;
  }

  ngOnInit() {
    this.user.set(this.userAccessService.getCurrentUser());

    if (this.displayTypeColumn && !this.displayedColumns.includes('Type')) {
      this.displayedColumns.splice(2, 0, 'Type');
    }

    this.displayedColumns.push('Actions');
    this.init();
  }

  init() {
    if (!this.deleteService) {
      console.error('DeleteService non fourni');
      return;
    }


    const parentCode = this.parentNode?.code ?? null;

    this.deleteService.getDeleted(parentCode).pipe(
      catchError((error: any) => {
        console.error(error);
        return of([]);
      }),

    ).subscribe((response: any) => {
      if (response) {
        this.dataSource.set(new MatTableDataSource(response));
      }
    });
  }

  activate(element: DeletableItem) {

    const dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "ACTIVATE_DELETE_NODE_TITLE",
        message: "ACTIVATE_DELETE_NODE_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });

    dialogValidationRef.afterClosed().pipe(
      switchMap((result: any) => {
        if (result && result.data && result.data === "validated") {
          return this.deleteService.activate(element.code).pipe(
            switchMap(() => this.translate.get("ACTIVATION_SUCCESS")),
            catchError((error: any) => {
              return this.translate.get("ACTIVATION_ERROR").pipe(
                switchMap((trad: string) => {
                  this.loggerService.error(trad);
                  throw error;
                })
              );
            })
          );
        }
        return of(null);
      }),

    ).subscribe((trad: string | null) => {
      if (trad) {
        this.loggerService.success(trad);
        this.init();
      }
    });
  }

  delete(element: DeletableItem) {

    const dialogValidationRef = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_NODE_TITLE",
        message: "DELETE_NODE_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });

    dialogValidationRef.afterClosed().pipe(
      switchMap((result: any) => {
        if (result && result.data !== 'canceled') {
          return this.deleteService.deleteDefinitively(element.code).pipe(
            switchMap(() => this.translate.get("DELETE_SUCCESS")),
            catchError((error: any) => {
              return this.translate.get("DELETE_ERROR").pipe(
                switchMap((trad: string) => {
                  this.loggerService.error(trad);
                  throw error;
                })
              );
            })
          );
        }
        return of(null);
      }),

    ).subscribe((trad: string | null) => {
      if (trad) {
        this.loggerService.success(trad);
        this.init();
      }
    });
  }

  close() {
    this.dialogRef.close();
  }
}