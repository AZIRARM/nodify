import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {MatDialogRef} from "@angular/material/dialog";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {UserService} from "../../../services/UserService";
import {NotificationService} from "../../../services/NotificationService";
import {Notification} from "../../../modeles/Notification";
import {MatPaginator} from "@angular/material/paginator";
import {UserAccessService} from "../../../services/UserAccessService";
import { AuthenticationService } from 'src/app/services/AuthenticationService';

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

  constructor(private notificationService: NotificationService,
              private userService: UserService,
              private userAccessService: UserAccessService,
              private authenticationService: AuthenticationService
  ) {
  }

  ngAfterViewInit(): void {
    console.log('Paginator:', this.paginator);
    this.dataSource = new MatTableDataSource();
    this.dataSource.paginator = this.paginator;
  }

  ngOnInit() {
   this.user = this.userAccessService.getCurrentUser()
   this.init();
  }

  init() {
    this.nextPage(this.paginator ? this.paginator?.pageIndex : 0, this.paginator ? this.paginator.pageSize : 10);
  }

  gotoNextPage(event: any) {
    this.nextPage(event.pageIndex, event.pageSize);
  }

  nextPage(nbPage: number, limit: number) {

     if (this.authenticationService.isAuthenticated()) {
      this.notificationService.connectWebSocket(this.authenticationService.getAccessToken(), nbPage, limit).subscribe({
        next: (data: any) => {
          this.total = data.count;
          this.dataSource = new MatTableDataSource(data.unread);
        },
        error: (err:any) => console.error("Erreur WebSocket:", err),
        complete: () => console.log("Socket fermé")
      });
    }
  }

  markAsReaded(element: Notification) {
    this.notificationService.markAsReaded(element.id).subscribe((user: any) => {
      this.init();
    }, (error: any) => {

    });
  }


  markAllAsReaded() {
    this.notificationService.markAllReaded().subscribe((user: any) => {
      this.init();
    }, (error: any) => {
    });
  }

  allreadyReaded() {
    this.paginator.pageIndex = 0;
    this.paginator.pageSize = 10;
    this.nextPage(this.paginator.pageIndex, this.paginator.pageSize);
  }
}
