import {Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {Node} from "../../../modeles/Node";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {Value} from "../../../modeles/Value";
import {MatTableDataSource} from "@angular/material/table";
import {UserAccessService} from "../../../services/UserAccessService";
import { LoggerService } from 'src/app/services/LoggerService';
import { LockService } from 'src/app/services/LockService';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import {AuthenticationService} from "../../../services/AuthenticationService";

@Component({
    selector: 'app-values-dialog',
    templateUrl: './values-dialog.component.html',
    styleUrls: ['./values-dialog.component.css'],
    standalone: false
})
export class ValuesDialogComponent implements OnInit, OnDestroy {

  workingValues: Value[] = [];
  key: string = '';
  value: string = '';

  displayedColumns: string[] = ['Key', 'Value', 'Actions'];
  dataSource: MatTableDataSource<Value>;

  private lockCheckSub: Subscription;

  editingItem: Value | null = null;
  originalItemBackup: any = null;

  constructor(
    public dialogRef: MatDialogRef<ValuesDialogComponent>,
    public userAccessService: UserAccessService,
    private loggerService: LoggerService,
    private lockService: LockService,
    private translateService: TranslateService,
    private authenticationService: AuthenticationService,
    @Inject(MAT_DIALOG_DATA) public node: Node
  ) {
    if (node) {
      if (!node.values) {
        node.values = [];
      }
      this.workingValues = node.values.map(v => ({...v}));
    }
  }

  ngOnInit() {
    this.init();

    this.lockService.acquire(this.node.code).subscribe(acquired => {
      if (!acquired) {
         this.translateService.get("RESOURCE_LOCKED")
            .subscribe(translation => {
              this.loggerService.warn(translation);
            });
        this.dialogRef.close();
      } else {
        this.lockService.startInactivityWatcher(30 * 60 * 1000, () => {
         this.translateService.get("RESOURCE_RELEASED")
            .subscribe(translation => {
              this.loggerService.warn(translation);
            });
          this.dialogRef.close();
        });

       this.lockService.getLockInfoSocket(this.node.code, this.authenticationService.getAccessToken()).subscribe((lockInfo: any) => {
         if (lockInfo.locked) {
           this.translateService.get("RESOURCE_LOCKED_BY_OTHER")
             .subscribe(translation => this.loggerService.warn(translation));
           this.dialogRef.close();
         }
       });

      }
    });
  }

  ngOnDestroy(): void {
    if (this.lockCheckSub) {
      this.lockCheckSub.unsubscribe();
    }
    this.lockService.release();
  }

  cancel() {
    this.dialogRef.close();
  }

  validate() {
    this.node.values = this.workingValues;
    this.dialogRef.close({data: this.node});
  }

  remove(key: string) {
    this.workingValues = this.workingValues.filter(v => v.key !== key);
    this.init();
  }

  add() {
    if (!this.key || !this.value) return;

    let val = new Value();
    val.key = this.key;
    val.value = this.value;

    this.workingValues.push(val);
    this.init();
    this.resetForm();
  }

  startEdit(element: Value) {
    if (this.editingItem) {
      this.cancelEdit();
    }
    this.originalItemBackup = {
      key: element.key,
      value: element.value
    };
    this.editingItem = element;
  }

  saveEdit(element: Value) {
    if (!element.key || !element.value) {
      this.translateService.get("FIELDS_REQUIRED").subscribe(translation => {
        this.loggerService.warn(translation);
      });
      return;
    }

    const keyExists = this.workingValues.some(v =>
      v !== element && v.key === element.key
    );

    if (keyExists) {
      this.translateService.get("KEY_ALREADY_EXISTS").subscribe(translation => {
        this.loggerService.warn(translation);
      });
      return;
    }

    const index = this.workingValues.findIndex(v => v === element);
    if (index !== -1) {
      this.workingValues[index] = element;
      this.init();
    }

    this.editingItem = null;
    this.originalItemBackup = null;
  }

  cancelEdit() {
    if (this.editingItem && this.originalItemBackup) {
      this.editingItem.key = this.originalItemBackup.key;
      this.editingItem.value = this.originalItemBackup.value;
    }
    this.editingItem = null;
    this.originalItemBackup = null;
  }

  addNewRow() {
    const newValue = new Value();
    newValue.key = '';
    newValue.value = '';
    this.workingValues.push(newValue);
    this.init();
    this.startEdit(newValue);
  }

  hasChanges(): boolean {
    if (this.editingItem !== null) return true;

    const originalValues = this.node.values || [];
    const currentValues = this.workingValues || [];

    if (originalValues.length !== currentValues.length) return true;

    for (let i = 0; i < currentValues.length; i++) {
      const original = originalValues[i];
      const current = currentValues[i];
      if (!original || !current) return true;
      if (current.key !== original.key || current.value !== original.value) {
        return true;
      }
    }

    return false;
  }

  private init() {
    this.dataSource = new MatTableDataSource(this.workingValues);
  }

  resetForm() {
    this.key = '';
    this.value = '';
  }
}
