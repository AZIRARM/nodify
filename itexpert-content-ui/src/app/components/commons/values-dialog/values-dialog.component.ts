import {Component, Inject} from '@angular/core';
import {Node} from "../../../modeles/Node";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {ContentNode} from "../../../modeles/ContentNode";
import {Value} from "../../../modeles/Value";
import {MatTableDataSource} from "@angular/material/table";
import {Translation} from "../../../modeles/Translation";

@Component({
  selector: 'app-values-dialog',
  templateUrl: './values-dialog.component.html',
  styleUrls: ['./values-dialog.component.css']
})
export class ValuesDialogComponent {

  node: Node;
  key: string;
  value: string;

  displayedColumns: string[] = [ 'Key', 'Value', 'Actions'];
  dataSource: MatTableDataSource<Value>;

  constructor(
    public dialogRef: MatDialogRef<ValuesDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public content: any
  ) {
    if (content) {
      this.node = content;
      if (!this.node.values) {
        this.node.values = [];
      }
      this.init();
    }
  }

  cancel() {
    this.dialogRef.close();
  }

  validate() {
    this.dialogRef.close({data: this.node});
  }

  remove(key: string) {
    console.log('<--------------' + this.node.values);
    this.node.values = this.node.values.filter(v => v.key !== key);
    console.log('------------------->' + this.node.values);
    this.init();
  }

  add() {
    let values: any = this.node.values?.filter(val => val.key !== this.key);

    let val = new Value();
    val.key = this.key;
    val.value = this.value;

    values.push(val);

    this.node.values = values;
    this.init();
  }

  edit(val: Value) {
    this.key = val.key;
    this.value = val.value;
  }

  private init() {
    this.dataSource = new MatTableDataSource(this.node.values );
  }
}
