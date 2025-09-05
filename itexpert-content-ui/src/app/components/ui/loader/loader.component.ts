import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import { LoaderService } from '../../../services/Loader.service';

@Component({
  selector: 'app-loader-component',
  templateUrl: './loader.component.html',
  styleUrls: ['./loader.component.css']
})
export class LoaderComponent implements OnInit{
  isLoading = false;
  constructor(
    public loaderService: LoaderService,
    private cdr: ChangeDetectorRef) {}


  ngOnInit() {
    this.loaderService.loading$.subscribe((value: boolean) => {
      setTimeout(() => this.isLoading = value);
    });
  }

}
