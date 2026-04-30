import { AfterViewInit, Component, Inject, ViewChild, inject, signal, WritableSignal } from '@angular/core';
import { TranslateService } from "@ngx-translate/core";
import { LoggerService } from "../../../services/LoggerService";
import { ContentNode } from "../../../modeles/ContentNode";
import { DataService } from "../../../services/DataService";
import { MatTableDataSource } from "@angular/material/table";
import { Data } from "../../../modeles/Data";
import { ToastrService } from "ngx-toastr";
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from "@angular/material/dialog";
import { MatPaginator } from "@angular/material/paginator";
import { ValidationDialogComponent } from "../../commons/validation-dialog/validation-dialog.component";
import { catchError, finalize, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-content-datas',
  templateUrl: './content-datas.component.html',
  styleUrl: './content-datas.component.css',
  standalone: false
})
export class ContentDatasComponent implements AfterViewInit {

  contentNode: ContentNode;
  datas: WritableSignal<Data[]> = signal<Data[]>([]);
  displayedColumns: string[] = ['Content', 'Type', 'Key', 'Creation', 'Modification', 'Data', 'Actions'];

  @ViewChild(MatPaginator)
  paginator!: MatPaginator;
  dataSource: WritableSignal<MatTableDataSource<Data>> = signal(new MatTableDataSource<Data>([]));
  total: WritableSignal<number> = signal(0);
  currentIndex: WritableSignal<number> = signal(0);
  currentPageSize: WritableSignal<number> = signal(5);


  dialogRefValidation: MatDialogRef<ValidationDialogComponent>;

  private translate = inject(TranslateService);
  private loggerService = inject(LoggerService);
  private toast = inject(ToastrService);
  private dataService = inject(DataService);
  private dialog = inject(MatDialog);
  public dialogRef = inject(MatDialogRef<ContentDatasComponent>);
  public content = inject(MAT_DIALOG_DATA);

  constructor() {
    this.contentNode = this.content;
  }

  ngAfterViewInit() {
    this.dataSource.set(new MatTableDataSource());
    const currentDataSource = this.dataSource();
    currentDataSource.paginator = this.paginator;
    this.dataSource.set(currentDataSource);
    this.nextPage(this.currentIndex(), this.currentPageSize());
  }

  deleteData(id: string) {
    this.dialogRefValidation = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_CONTENT_NODE_TITLE",
        message: "DELETE_CONTENT_NODE_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefValidation.afterClosed()
      .subscribe(result => {
        if (result?.data === 'validated') {

          this.dataService.delete(id).pipe(
            switchMap(() => this.translate.get("DELETE_SUCCESS")),
            catchError((error) => {
              return of(null);
            }),

          ).subscribe((trad: string | null) => {
            if (trad) {
              this.loggerService.success(trad);
              this.nextPage(this.currentIndex(), this.currentPageSize());
            }
          });
        }
      });
  }

  gotoNextPage(event: any) {
    this.nextPage(event.pageIndex, event.pageSize);
    this.currentIndex.set(event.pageIndex);
    this.currentPageSize.set(event.pageIndex);
  }

  nextPage(nbPage: number, limit: number) {

    this.dataService.countDatasByContentNodeCode(this.contentNode.code).pipe(
      catchError((error) => {
        this.toast.error('Request failed with error');
        return of(0);
      })
    ).subscribe((data: any) => {
      this.total.set(data);

      this.dataService.getByContentCode(this.contentNode.code, nbPage, limit).pipe(
        catchError((error) => {
          this.toast.error('Request failed with error');
          return of([]);
        }),

      ).subscribe((response: any) => {
        this.dataSource.set(new MatTableDataSource(response));
      });
    });
  }

  close() {
    this.dialogRef.close();
  }
}