import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Node } from 'src/app/modeles/Node';
import { ResourceParameter } from 'src/app/modeles/ResourceParameter';
import { NodeService } from 'src/app/services/NodeService';

@Component({
  selector: 'app-resource-parameter-dialog',
  templateUrl: './resource-parameter-dialog.component.html',
  styleUrls: ['./resource-parameter-dialog.component.css']
})
export class ResourceParameterDialogComponent implements OnInit{

  resourceParameter: ResourceParameter;

  // valeurs fixes pour l’instant
  codes: string[] = [];
  actions: string[] = ['ARCHIVE'];
  types: string[] = ['NODE'];

  constructor(
    public dialogRef: MatDialogRef<ResourceParameterDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { resourceParameter: ResourceParameter, },
    private nodeService: NodeService
  ) {
    this.resourceParameter = { ...data.resourceParameter }; // clone
  }
  ngOnInit(): void {
    this.nodeService.getAllParentOrigin().subscribe((data:any)=>{
      this.codes = data?.map((node:Node)=>node.code);
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    this.dialogRef.close(this.resourceParameter);
  }
}
