import { Component, Inject, OnDestroy, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { Node } from "../../../modeles/Node";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { Value } from "../../../modeles/Value";
import { MatTableDataSource } from "@angular/material/table";
import { UserAccessService } from "../../../services/UserAccessService";
import { LoggerService } from 'src/app/services/LoggerService';
import { LockService } from 'src/app/services/LockService';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { AuthenticationService } from "../../../services/AuthenticationService";

@Component({
  selector: 'app-values-dialog',
  templateUrl: './values-dialog.component.html',
  styleUrls: ['./values-dialog.component.css'],
  standalone: false
})
export class ValuesDialogComponent implements OnInit, OnDestroy {

  workingValues: WritableSignal<Value[]> = signal<Value[]>([]);
  key: WritableSignal<string> = signal<string>('');
  value: WritableSignal<string> = signal<string>('');
  displayedColumns: string[] = ['Key', 'Value', 'Actions'];
  dataSource: WritableSignal<MatTableDataSource<Value>> = signal(new MatTableDataSource<Value>([]));
  editingItem: WritableSignal<Value | null> = signal<Value | null>(null);
  originalItemBackup: any = null;
  private lockCheckSub?: Subscription;

  public dialogRef = inject(MatDialogRef<ValuesDialogComponent>);
  public userAccessService = inject(UserAccessService);
  private loggerService = inject(LoggerService);
  private lockService = inject(LockService);
  private translateService = inject(TranslateService);
  private authenticationService = inject(AuthenticationService);
  private node = inject<Node>(MAT_DIALOG_DATA);

  constructor() {
    if (this.node) {
      if (!this.node.values) {
        this.node.values = [];
      }
      this.workingValues.set(this.node.values.map(v => ({ ...v })));
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
    this.node.values = this.workingValues();
    this.dialogRef.close({ data: this.node });
  }

  remove(key: string) {
    const currentValues = this.workingValues();
    this.workingValues.set(currentValues.filter(v => v.key !== key));
    this.init();
  }

  add() {
    const currentKey = this.key();
    const currentValue = this.value();
    if (!currentKey || !currentValue) return;

    let val = new Value();
    val.key = currentKey;
    val.value = currentValue;

    const currentValues = this.workingValues();
    currentValues.push(val);
    this.workingValues.set(currentValues);
    this.init();
    this.resetForm();
  }

  startEdit(element: Value) {
    if (this.editingItem()) {
      this.cancelEdit();
    }
    this.originalItemBackup = {
      key: element.key,
      value: element.value
    };
    this.editingItem.set(element);
  }

  saveEdit(element: Value) {
    if (!element.key || !element.value) {
      this.translateService.get("FIELDS_REQUIRED").subscribe(translation => {
        this.loggerService.warn(translation);
      });
      return;
    }

    const currentValues = this.workingValues();
    const keyExists = currentValues.some(v =>
      v !== element && v.key === element.key
    );

    if (keyExists) {
      this.translateService.get("KEY_ALREADY_EXISTS").subscribe(translation => {
        this.loggerService.warn(translation);
      });
      return;
    }

    const index = currentValues.findIndex(v => v === element);
    if (index !== -1) {
      currentValues[index] = element;
      this.workingValues.set(currentValues);
      this.init();
    }

    this.editingItem.set(null);
    this.originalItemBackup = null;
  }

  cancelEdit() {
    const editingItemValue = this.editingItem();
    if (editingItemValue && this.originalItemBackup) {
      editingItemValue.key = this.originalItemBackup.key;
      editingItemValue.value = this.originalItemBackup.value;
    }
    this.editingItem.set(null);
    this.originalItemBackup = null;
  }

  addNewRow() {
    const newValue = new Value();
    newValue.key = '';
    newValue.value = '';
    const currentValues = this.workingValues();
    currentValues.push(newValue);
    this.workingValues.set(currentValues);
    this.init();
    this.startEdit(newValue);
  }

  hasChanges(): boolean {
    if (this.editingItem() !== null) return true;

    const originalValues = this.node.values || [];
    const currentValues = this.workingValues() || [];

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
    this.dataSource.set(new MatTableDataSource(this.workingValues()));
  }

  resetForm() {
    this.key.set('');
    this.value.set('');
  }
}