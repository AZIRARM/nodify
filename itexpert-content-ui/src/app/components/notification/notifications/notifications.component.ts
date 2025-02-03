import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {MatDialogRef} from "@angular/material/dialog";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {TranslateService} from "@ngx-translate/core";
import {UserService} from "../../../services/UserService";
import {NotificationService} from "../../../services/NotificationService";
import {Notification} from "../../../modeles/Notification";
import {MatPaginator} from "@angular/material/paginator";

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.css']
})
export class NotificationsComponent implements OnInit, AfterViewInit {

  user: any;
  notifications: Notification[];
  displayedColumns: string[] = ['Date', 'Type', 'Code', 'Message', 'Version', 'By', 'Actions'];

  dialogRef: MatDialogRef<ValidationDialogComponent>;

  @ViewChild(MatPaginator)
  paginator!: MatPaginator;

  dataSource: MatTableDataSource<Notification>;

  total: number;

  readed: boolean = false;

  constructor(
    private translate: TranslateService,
    private notificationService: NotificationService,
    private userService: UserService,
  ) {
  }

  ngAfterViewInit(): void {
    console.log('Paginator:', this.paginator);
    this.dataSource.paginator = this.paginator;
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
    this.nextPage(this.paginator ? this.paginator?.pageIndex : 0, this.paginator ? this.paginator.pageSize : 5);
  }

  gotoNextPage(event: any) {
    this.nextPage(event.pageIndex, event.pageSize);
  }

  nextPage(nbPage: number, limit: number) {
    if (!this.readed) {
      this.notificationService.countUnreadedNotification(this.user.id).subscribe(
        (data: any) => {
          this.total = data;
          this.notificationService.findPaginated(this.user.id, nbPage, limit).subscribe(
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
        });
    } else {
      this.notificationService.countReadedNotification(this.user.id).subscribe(
        (data: any) => {
          this.total = data;
          this.notificationService.findReadedByUserId(this.user.id, nbPage, limit).subscribe(
            (response: any) => {
              if (response) {
                response.map((param: any) => this.setUserName(param));
                this.dataSource = new MatTableDataSource(response);
              }
            },
            error => {
              console.error(error);
            });
        });
    }
  }

  setUserName(param: any) {
    this.userService.setUserName(param);
  }

  markAsReaded(element: Notification) {
    this.notificationService.markAsReaded(element.id, this.user.id).subscribe((user: any) => {
      this.init();
    }, (error: any) => {

    });
  }

  markAsNotReaded(element: Notification) {
    this.notificationService.markAsNotReaded(element.id, this.user.id).subscribe((user: any) => {
      this.init();
    }, (error: any) => {

    });
  }

  markAllAsReaded() {
    this.notificationService.markAllReaded(this.user.id).subscribe((user: any) => {
      this.init();
    }, (error: any) => {
    });
  }

  allreadyReaded() {
    this.readed = !this.readed;
    this.paginator.pageIndex=0;
    this.paginator.pageSize=5;
    this.nextPage(0, 5);
  }
}
