import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ResourceParameter } from 'src/app/modeles/ResourceParameter';
import { ResourceParameterService } from 'src/app/services/ResourceParameterService';
import { ResourceParameterDialogComponent } from './resource-parameter-dialog/resource-parameter-dialog.component';

@Component({
  selector: 'app-resource-parameters',
  templateUrl: './resource-parameters.component.html',
  styleUrl: './resource-parameters.component.css'
})
export class ResourceParametersComponent implements OnInit {


  resourceParameters: ResourceParameter[] = [];
  displayedColumns: string[] = ['code', 'action', 'value', 'description', 'actions'];

  
  constructor(private resourceParameterService: ResourceParameterService,
    private dialog: MatDialog
  ){

  }
  ngOnInit(): void {
    this.init();
  }

  init(){
    this.resourceParameterService.findAll().subscribe((data:any)=>{
        this.resourceParameters = data;
    });
  }

  
  // Ouvre la popup pour ajouter un nouveau ResourceParameter
  openAddResourceParamDialog() {
    const dialogRef = this.dialog.open(ResourceParameterDialogComponent, {
        height: '80vh',
        width: '80vw',
        disableClose: true,
        data: { resourceParameter: new ResourceParameter() }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.resourceParameterService.save(result).subscribe((response:any)=>{
          this.init();
        });
      }
    });
  }

  // Ouvre la popup pour éditer la description
  openEditDescriptionDialog(param: ResourceParameter) {
    const dialogRef = this.dialog.open(ResourceParameterDialogComponent, {
        height: '80vh',
        width: '80vw',
        disableClose: true,
        data: { resourceParameter: { ...param } } // copie pour éditer
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        param.description = result.description;
      }
    });
  }

  saveResourceParameter(param: ResourceParameter) {
    // Appel au service pour sauvegarder le param
    console.log('Saving', param);
  }

  deleteResourceParameter(param: ResourceParameter) {
    // Appel au service pour supprimer le param
    this.resourceParameters = this.resourceParameters.filter(p => p.id !== param.id);
    console.log('Deleted', param);
  }
}
