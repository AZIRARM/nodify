import { Component, OnInit, inject, signal, ChangeDetectorRef } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { LockService } from 'src/app/services/LockService';
import { LoggerService } from 'src/app/services/LoggerService';
import { UserAccessService } from 'src/app/services/UserAccessService';
import { NodeService } from 'src/app/services/NodeService';
import { ContentNodeService } from 'src/app/services/ContentNodeService';

@Component({
  selector: 'app-release-locks',
  templateUrl: './release-locks.component.html',
  styleUrls: ['./release-locks.component.css'],
  standalone: false
})
export class ReleaseLocksComponent implements OnInit {
  private translate = inject(TranslateService);
  private toast = inject(ToastrService);
  private loggerService = inject(LoggerService);
  public userAccessService = inject(UserAccessService);
  private nodeService = inject(NodeService);
  private contentNodeService = inject(ContentNodeService);
  public lockService = inject(LockService);
  private dialog = inject(MatDialog);
  private cdRef = inject(ChangeDetectorRef);

  displayedColumns: string[] = ["Code", "Owner", "Locked", "Actions"];
  dataSource = new MatTableDataSource<any>([]);

  locksList = signal<any[]>([]);

  ngOnInit(): void {
    this.loadLocks();
  }

  loadLocks(): void {
    this.lockService.handleAllLocks().subscribe({
      next: (locks: any) => {
        let data: any[] = [];
        if (Array.isArray(locks)) {
          data = locks;
        } else if (locks && Array.isArray(locks.data)) {
          data = locks.data;
        }

        this.locksList.set(data);
        this.dataSource.data = this.locksList();
        this.cdRef.detectChanges();
      },
      error: (err: any) => {
        this.toast.error(this.translate.instant("LOCKS_LOAD_FAIL"));
      }
    });
  }

  unlock(element: any): void {
    this.lockService.adminRelease(element.resourceCode).subscribe({
      next: (success: boolean) => {
        if (success) {
          this.toast.success(this.translate.instant("LOCK_RELEASED_SUCCESS"));
          this.loadLocks();
        } else {
          this.toast.error(this.translate.instant("LOCK_RELEASED_FAIL"));
        }
      },
      error: (err: any) => {
        this.toast.error(this.translate.instant("LOCK_RELEASED_FAIL"));
      }
    });
  }
}