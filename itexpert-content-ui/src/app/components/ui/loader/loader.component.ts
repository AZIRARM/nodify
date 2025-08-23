import {Component, OnInit} from '@angular/core';
import { LoaderService } from '../../../services/Loader.service';

@Component({
  selector: 'app-loader-component',
  templateUrl: './loader.component.html',
  styleUrls: ['./loader.component.css']
})
export class LoaderComponent implements OnInit{
  isLoading = false;
  constructor(public loaderService: LoaderService) {}


  ngOnInit() {
    // On s'abonne au service pour mettre à jour isLoading
    this.loaderService.loading$.subscribe((value: boolean) => {
      console.log('Loader state:', value); // debug
      this.isLoading = value;
    });
  }

}
