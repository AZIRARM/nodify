import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TranslationsDialogComponent } from './translations-dialog.component';

describe('TranslationsDialogComponent', () => {
  let component: TranslationsDialogComponent;
  let fixture: ComponentFixture<TranslationsDialogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TranslationsDialogComponent]
    });
    fixture = TestBed.createComponent(TranslationsDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
