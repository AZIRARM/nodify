import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ResourceParameter } from 'src/app/modeles/ResourceParameter';
import { ResourceParameterService } from 'src/app/services/ResourceParameterService';
import { ResourceParameterDialogComponent } from '../../resource-parameters/resource-parameter-dialog/resource-parameter-dialog.component';

@Component({
  selector: 'app-clean-archived',
  templateUrl: './clean-archived.component.html',
  styleUrl: './clean-archived.component.css'
})
export class CleanArchivedComponent implements OnInit {


  resourceParameters: ResourceParameter[] = [];
  displayedColumns: string[] = ['code', 'action', 'value', 'description', 'actions'];

  
  constructor(private resourceParameterService: ResourceParameterService,
    private dialog: MatDialog
  ){

  }
  ngOnInit(): void {
    this.resourceParameterService.findAllResourceParameterToArchive().subscribe((data:any)=>{
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
        this.resourceParameters.push(result);
      }
    });
  }

  // Ouvre la popup pour éditer la description
  openEditDescriptionDialog(param: ResourceParameter) {
    const dialogRef = this.dialog.open(ResourceParameterDialogComponent, {
      width: '400px',
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
