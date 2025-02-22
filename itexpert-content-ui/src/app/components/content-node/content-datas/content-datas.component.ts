import {AfterViewInit, Component, Inject, ViewChild} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {LoggerService} from "../../../services/LoggerService";
import {ContentNode} from "../../../modeles/ContentNode";
import {DataService} from "../../../services/DataService";
import {MatTableDataSource} from "@angular/material/table";
import {Data} from "../../../modeles/Data";
import {ToastrService} from "ngx-toastr";
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from "@angular/material/dialog";
import {MatPaginator} from "@angular/material/paginator";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";

@Component({
  selector: 'app-content-datas',
  templateUrl: './content-datas.component.html',
  styleUrl: './content-datas.component.css'
})
export class ContentDatasComponent implements AfterViewInit {

  contentNode: ContentNode;
  datas: Data[] = [];


  displayedColumns: string[] = ['Content', 'Type', 'Key', 'Creation', 'Modification', 'Data', 'Actions'];

  @ViewChild(MatPaginator)
  paginator!: MatPaginator;
  dataSource: MatTableDataSource<Data>;
  total: number;

  currentIndex: number = 0;
  currentPageSize: number = 5;

  dialogRefValidation: MatDialogRef<ValidationDialogComponent>;


  constructor(private translate: TranslateService,
              private loggerService: LoggerService,
              private toast: ToastrService,
              private dataService: DataService,
              private dialog: MatDialog,
              @Inject(MAT_DIALOG_DATA) public content: ContentNode,
              public dialogRef: MatDialogRef<ContentDatasComponent>,
  ) {
    this.contentNode = content
  }

  ngAfterViewInit() {
    this.dataSource = new MatTableDataSource();
    this.dataSource.paginator = this.paginator;
    this.nextPage(this.currentIndex, this.currentPageSize);
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
        if (result.data === 'validated') {
          this.dataService.delete(id).subscribe(
            (response: any) => {
              this.translate.get("DELETE_SUCCESS").subscribe(trad => {
                this.loggerService.success(trad);
                this.nextPage(this.currentIndex, this.currentPageSize);
              });
            });
        }
      });


  }

  gotoNextPage(event: any) {
    this.nextPage(event.pageIndex, event.pageSize);
    this.currentIndex = event.pageIndex;
    this.currentPageSize = event.pageIndex;
  }

  nextPage(nbPage: number, limit: number) {
    this.dataService.countDatasByContentNodeCode(this.contentNode.code).subscribe(
      (data: any) => {
        this.total = data;

        this.dataService.getByContentCode(this.contentNode.code, nbPage, limit).subscribe(
          (response: any) => {
            this.dataSource = new MatTableDataSource(response);
          },
          (error) => {                              //error() callback
            this.toast.error('Request failed with error');
          });
      });
  }

  close() {
    this.dialogRef.close();
  }

}
