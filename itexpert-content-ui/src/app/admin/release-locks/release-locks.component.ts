import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { LockService } from 'src/app/services/LockService';
import { LoggerService } from 'src/app/services/LoggerService';
import { UserAccessService } from 'src/app/services/UserAccessService';
import { MatTableDataSource } from '@angular/material/table';
import { NodeService } from 'src/app/services/NodeService';
import { ContentNodeService } from 'src/app/services/ContentNodeService';

@Component({
  selector: 'app-release-locks',
  templateUrl: './release-locks.component.html',
  styleUrls: ['./release-locks.component.css']
})
export class ReleaseLocksComponent implements OnInit {

  displayedColumns: string[] = ["Code", "Name", "Owner", "Locked", "Actions"];
  dataSource = new MatTableDataSource<any>([]);

  constructor(
    private translate: TranslateService,
    private toast: ToastrService,
    private loggerService: LoggerService,
    public userAccessService: UserAccessService,
    private nodeService: NodeService,
    private contentNodeService: ContentNodeService,
    public lockService: LockService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    // TODO: appeler ton service backend pour charger la liste des ressources avec lockInfo
    this.dataSource.data = [
      { code: "NODE-123", name: "Projet A", lockInfo: { owner: "user1", isOwner: false, locked: true } },
      { code: "NODE-456", name: "Projet B", lockInfo: { owner: null, isOwner: false, locked: false } }
    ];
  }

  unlock(element: any) {
    this.lockService.adminRelease(element.code).subscribe(success => {
      if (success) {
        element.lockInfo = { owner: null, isOwner: false, locked: false };
        this.toast.success(this.translate.instant("LOCK_RELEASED_SUCCESS"));
      } else {
        this.toast.error(this.translate.instant("LOCK_RELEASED_FAIL"));
      }
    });
  }
}
