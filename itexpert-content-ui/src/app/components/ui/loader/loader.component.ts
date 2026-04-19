import { Component, OnInit, inject, signal, ChangeDetectorRef } from '@angular/core';
import { LoaderService } from '../../../services/Loader.service';

@Component({
  selector: 'app-loader-component',
  templateUrl: './loader.component.html',
  styleUrls: ['./loader.component.css'],
  standalone: false
})
export class LoaderComponent implements OnInit {
  private loaderService = inject(LoaderService);
  private cdr = inject(ChangeDetectorRef);

  isLoading = signal<boolean>(false);

  ngOnInit(): void {
    this.loaderService.loading$.subscribe((value: boolean) => {
      setTimeout(() => {
        this.isLoading.set(value);
        this.cdr.detectChanges();
      });
    });
  }
}